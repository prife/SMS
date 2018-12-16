package com.freeme.sms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;

import com.freeme.sms.util.ContentType;
import com.freeme.sms.util.PhoneUtils;

/**
 * Class that handles MMS WAP push intent from telephony on pre-KLP Devices.
 */
public class MmsWapPushReceiver extends BroadcastReceiver {
    static final String EXTRA_SUBSCRIPTION = "subscription";
    static final String EXTRA_DATA = "data";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction())
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            if (PhoneUtils.getDefault().isSmsEnabled()) {
                // Always convert negative subIds into -1
                final int subId = PhoneUtils.getDefault().getEffectiveIncomingSubIdFromSystem(
                        intent, MmsWapPushReceiver.EXTRA_SUBSCRIPTION);
                final byte[] data = intent.getByteArrayExtra(MmsWapPushReceiver.EXTRA_DATA);
                mmsReceived(subId, data);
            }
        }
    }

    static void mmsReceived(final int subId, final byte[] data) {
        if (!PhoneUtils.getDefault().isSmsEnabled()) {
            return;
        }

        // TODO
    }
}

