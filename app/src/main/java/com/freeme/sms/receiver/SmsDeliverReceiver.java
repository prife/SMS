package com.freeme.sms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * receives incoming SMS messages on KLP+ Devices.
 */
public class SmsDeliverReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsDeliverReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive:" + intent);
        SmsReceiver.deliverSmsIntent(context, intent);
    }
}
