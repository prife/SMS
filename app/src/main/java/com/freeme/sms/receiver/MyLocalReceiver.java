package com.freeme.sms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyLocalReceiver extends BroadcastReceiver {
    private static final String TAG = "MyLocalReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onLocalReceive:" + intent);
        if (mHostInterface != null) {
            mHostInterface.onLocalReceive(context, intent);
        } else {
            Log.w(TAG, "host is null");
        }
    }

    private HostInterface mHostInterface;

    public void setHostInterface(HostInterface hostInterface) {
        mHostInterface = hostInterface;
    }

    public interface HostInterface {
        void onLocalReceive(Context context, Intent intent);
    }
}
