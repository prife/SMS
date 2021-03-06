package com.freeme.sms;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.freeme.sms.model.SmsMessage;
import com.freeme.sms.util.SmsPrefs;
import com.freeme.sms.util.ToastUtils;

public class EchoServer {
    private static final String TAG = "EchoServer";

    private static final String KEY_SERVER_NUMBER = "server_number";

    private static final String CMD_REQUEST_WHO_AM_I = "CMD#REQUEST#WHO#AM#I";
    private static final String CMD_ECHO_WHO_AM_I = "CMD#ECHO#WHO#AM#I#";

    public static String getServerNumber(final boolean allowOverride) {
        String serverNumber = "";
        if (allowOverride) {
            serverNumber = Factory.get().getSmsPrefs().getString(KEY_SERVER_NUMBER, serverNumber);
        }

        if (TextUtils.isEmpty(serverNumber)) {
            serverNumber = Factory.get().getApplicationContext().getString(R.string.def_server_number);
        }

        return serverNumber;
    }

    public static void saveServerNumber(String serverNumber) {
        Factory.get().getSmsPrefs().putString(KEY_SERVER_NUMBER, serverNumber);
    }

    public static void sendToServer(final int subId) {
        String strDestAddress = getServerNumber(true);
        if (TextUtils.isEmpty(strDestAddress)) {
            Log.w(TAG, "server number is empty");
            return;
        }
        SmsSender.getInstance(Factory.get().getApplicationContext())
                .sendSms(strDestAddress, CMD_REQUEST_WHO_AM_I, subId);
        ToastUtils.toast(R.string.sending, Toast.LENGTH_SHORT);
    }

    public static boolean performRequest(SmsMessage smsMessage) {
        if (smsMessage == null) {
            Log.w(TAG, "performRequest sms message is null");
            return false;
        }

        if (!CMD_REQUEST_WHO_AM_I.equals(smsMessage.mBody)) {
            Log.w(TAG, "performRequest sms message not request.");
            return false;
        }

        String address = smsMessage.mAddress;
        int subId = smsMessage.mSubId;
        String message = CMD_ECHO_WHO_AM_I + address;
        Log.d(TAG, "address=" + address + ", subId = " + subId + "message:" + message);
        SmsSender.getInstance(Factory.get().getApplicationContext())
                .sendSms(address, message, subId);
        ToastUtils.toast(R.string.sending, Toast.LENGTH_SHORT);

        return true;
    }

    public static boolean echoWhoAmI(SmsMessage smsMessage) {
        if (smsMessage == null) {
            Log.w(TAG, "echoWhoAmI sms message is null");
            return false;
        }

        if (smsMessage.mBody == null || !smsMessage.mBody.startsWith(CMD_ECHO_WHO_AM_I)) {
            Log.w(TAG, "echoWhoAmI sms message not echo.");
            return false;
        }

        String address = smsMessage.mAddress;
        int subId = smsMessage.mSubId;
        String mySelfNumber = smsMessage.mBody.replaceFirst(CMD_ECHO_WHO_AM_I, "");
        final SmsPrefs subPrefs = Factory.get().getSubscriptionPrefs(subId);
        subPrefs.putString(Factory.get().getApplicationContext()
                        .getString(R.string.sms_phone_number_pref_key),
                mySelfNumber);
        ToastUtils.toast("已保存" + mySelfNumber);

        return true;
    }
}
