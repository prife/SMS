package com.freeme.sms.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.util.Log;

import com.freeme.sms.Factory;
import com.freeme.sms.model.SmsMessage;
import com.freeme.sms.service.NoConfirmationSmsSendService;
import com.freeme.sms.util.OsUtil;
import com.freeme.sms.util.PhoneUtils;
import com.freeme.sms.util.SmsUtils;

import com.wetest.tookit.log.Logger;
import com.wetest.tookit.report.RequestManager;

/**
 * Class that receives incoming SMS messages through android.provider.Telephony.SMS_RECEIVED
 * <p>
 * This class serves two purposes:
 * - Process phone verification SMS messages
 * - Handle SMS messages when the user has enabled us to be the default SMS app (Pre-KLP)
 */
public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    private static final String EXTRA_ERROR_CODE = "errorCode";
    private static final String EXTRA_SUB_ID = "subscription";

    /**
     * Enable or disable the SmsReceiver as appropriate. Pre-KLP we use this receiver for
     * receiving incoming SMS messages. For KLP+ this receiver is not used when running as the
     * primary user and the SmsDeliverReceiver is used for receiving incoming SMS messages.
     */
    public static void updateSmsReceiveHandler(final Context context) {
        boolean smsReceiverEnabled;
        boolean mmsWapPushReceiverEnabled;
        boolean respondViaMessageEnabled;

        if (OsUtil.isAtLeastKLP()) {
            // When we're running as the secondary user, we don't get the new SMS_DELIVER intent,
            // only the primary user receives that. As secondary, we need to go old-school and
            // listen for the SMS_RECEIVED intent. For the secondary user, use this SmsReceiver
            // for both sms and mms notification. For the primary user on KLP (and above), we don't
            // use the SmsReceiver.
            smsReceiverEnabled = false;
            // On KLP use the new deliver event for mms
            mmsWapPushReceiverEnabled = false;
            // On KLP we need to always enable this handler to show in the list of sms apps
            respondViaMessageEnabled = true;
        } else {
            // On JB we use the sms receiver for both sms/mms delivery
            final boolean carrierSmsEnabled = PhoneUtils.getDefault().isSmsEnabled();
            smsReceiverEnabled = carrierSmsEnabled;

            // On JB we use the mms receiver when sms/mms is enabled
            mmsWapPushReceiverEnabled = carrierSmsEnabled;
            // On JB this is dynamic to make sure we don't show in dialer if sms is disabled
            respondViaMessageEnabled = carrierSmsEnabled;
        }

        final PackageManager packageManager = context.getPackageManager();
        if (smsReceiverEnabled) {
            Log.v(TAG, "Enabling SMS message receiving");
            packageManager.setComponentEnabledSetting(
                    new ComponentName(context, SmsReceiver.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        } else {
            Log.v(TAG, "Disabling SMS message receiving");
            packageManager.setComponentEnabledSetting(
                    new ComponentName(context, SmsReceiver.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        if (mmsWapPushReceiverEnabled) {
            Log.v(TAG, "Enabling MMS message receiving");
            packageManager.setComponentEnabledSetting(
                    new ComponentName(context, MmsWapPushReceiver.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        } else {
            Log.v(TAG, "Disabling MMS message receiving");
            packageManager.setComponentEnabledSetting(
                    new ComponentName(context, MmsWapPushReceiver.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }

        if (respondViaMessageEnabled) {
            Log.v(TAG, "Enabling respond via message intent");
            packageManager.setComponentEnabledSetting(
                    new ComponentName(context, NoConfirmationSmsSendService.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        } else {
            Log.v(TAG, "Disabling respond via message intent");
            packageManager.setComponentEnabledSetting(
                    new ComponentName(context, NoConfirmationSmsSendService.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    /**
     * Get the SMS messages from the specified SMS intent.
     *
     * @return the messages. If there is an error or the message should be ignored, return null.
     */
    public static android.telephony.SmsMessage[] getMessagesFromIntent(Intent intent) {
        final android.telephony.SmsMessage[] messages = Sms.Intents.getMessagesFromIntent(intent);

        // Check messages for validity
        if (messages == null || messages.length < 1) {
            return null;
        }

        return messages;
    }

    public static void deliverSmsIntent(final Context context, final Intent intent) {
        final android.telephony.SmsMessage[] messages = getMessagesFromIntent(intent);

        // Check messages for validity
        if (messages == null || messages.length < 1) {
            Log.e(TAG, "processReceivedSms: null or zero or ignored message");
            return;
        }

        final int errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, 0);
        // Always convert negative subIds into -1
        int subId = PhoneUtils.getDefault().getEffectiveIncomingSubIdFromSystem(
                intent, EXTRA_SUB_ID);
        deliverSmsMessages(context, subId, errorCode, messages);
    }

    public static void deliverSmsMessages(final Context context, final int subId,
                                          final int errorCode, final android.telephony.SmsMessage[] messages) {
        final ContentValues messageValues =
                SmsUtils.parseReceivedSmsMessage(context, messages, errorCode);

        Log.v(TAG, "deliverSmsMessages");

        final long nowInMillis = System.currentTimeMillis();
        final long receivedTimestampMs = SmsUtils.getMessageDate(messages[0], nowInMillis);

        messageValues.put(Sms.Inbox.DATE, receivedTimestampMs);
        // Default to unread and unseen for us but ReceiveSmsMessageAction will override
        // seen for the telephony db.
        messageValues.put(Sms.Inbox.READ, 0);
        messageValues.put(Sms.Inbox.SEEN, 0);
        if (OsUtil.isAtLeastL_MR1()) {
            messageValues.put(Sms.SUBSCRIPTION_ID, subId);
        }

        Log.d(TAG, "messageValues = " + messageValues);
        if (messages[0].getMessageClass() == android.telephony.SmsMessage.MessageClass.CLASS_0) {
            Log.d(TAG, "class_0");
        } else {
            insert(context, messageValues);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive:" + intent);
        // On KLP+ we only take delivery of SMS messages in SmsDeliverReceiver.
        if (PhoneUtils.getDefault().isSmsEnabled() && !OsUtil.isAtLeastKLP()) {
            deliverSmsIntent(context, intent);
        }
    }

    // Insert message into telephony database sms message table.
    private static void insert(Context context, final ContentValues messageValues) {
        Logger.getLogger().info("SmsReceiver.insert()");
        // Make sure we have a sender address
        String address = messageValues.getAsString(Sms.ADDRESS);
        // Make sure we've got a thread id
        final long threadId = SmsUtils.Threads.getOrCreateThreadId(context, address);
        messageValues.put(Sms.THREAD_ID, threadId);

        // Insert into telephony
        final Uri messageUri = context.getContentResolver().insert(Sms.Inbox.CONTENT_URI,
                messageValues);

        SmsMessage message = SmsMessage.get(messageValues);
        Factory.get().setSmsMessage(message);

        Logger.getLogger().info("messageUri:" + messageUri + ", message = " + message);
    }
}
