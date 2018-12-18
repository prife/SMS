package com.freeme.sms.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.freeme.sms.SmsSender;
import com.freeme.sms.util.ToastUtils;

public class MySmsReceiver extends BroadcastReceiver {
    private static final String TAG = "MySmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "action:" + action);
        if (SmsSender.ACTION_SEND_SMS.equals(action)) {
            try {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        ToastUtils.toast("短信发送成功", Toast.LENGTH_SHORT);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        ToastUtils.toast("短信发送失败", Toast.LENGTH_SHORT);
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "got exception", e);
            }
        } else if (SmsSender.ACTION_DELIVERED_SMS.equals(action)) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    ToastUtils.toast("短信已送达", Toast.LENGTH_SHORT);
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    ToastUtils.toast("短信未送达", Toast.LENGTH_SHORT);
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    break;
            }
        }

    }
}
