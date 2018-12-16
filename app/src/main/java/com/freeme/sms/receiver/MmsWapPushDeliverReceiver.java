package com.freeme.sms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;

import com.freeme.sms.util.ContentType;
import com.freeme.sms.util.PhoneUtils;

/**
 * Class that handles MMS WAP push intent from telephony on KLP+ Devices.
 */
public class MmsWapPushDeliverReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION.equals(intent.getAction())
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            // Always convert negative subIds into -1
            int subId = PhoneUtils.getDefault().getEffectiveIncomingSubIdFromSystem(
                    intent, MmsWapPushReceiver.EXTRA_SUBSCRIPTION);
            byte[] data = intent.getByteArrayExtra(MmsWapPushReceiver.EXTRA_DATA);
            MmsWapPushReceiver.mmsReceived(subId, data);
        }
    }
}
