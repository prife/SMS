package com.freeme.sms;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;

import com.freeme.sms.util.PhoneUtils;

import java.util.List;

public class SmsSender {
    private static final String TAG = "SmsSender";

    private static SmsSender sInstance;

    public static final String ACTION_SEND_SMS = "freeme.action.intent.SEND_SMS";
    public static final String ACTION_DELIVERED_SMS = "freeme.action.intent.DELIVERED_SMS";

    private Context mContext;

    private SmsSender(Context context) {
        mContext = context.getApplicationContext();
    }

    public static SmsSender getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SmsSender.class) {
                if (sInstance == null) {
                    sInstance = new SmsSender(context);
                }
            }
        }

        return sInstance;
    }

    public void sendSms(String strDestAddress, String strMessage, final int subId) {
        SmsManager smsManager = PhoneUtils.get(subId).getSmsManager();
        try {
            Intent itSend = new Intent(ACTION_SEND_SMS);
            Intent itDeliver = new Intent(ACTION_DELIVERED_SMS);

            PendingIntent mSendPI = PendingIntent.getBroadcast(mContext, 0, itSend, 0);

            PendingIntent mDeliverPI = PendingIntent.getBroadcast(mContext, 0, itDeliver, 0);
            List<String> divideContents = smsManager.divideMessage(strMessage);
            for (String text : divideContents) {
                smsManager.sendTextMessage(strDestAddress, null, text, mSendPI, mDeliverPI);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendToSMS(@NonNull String phoneNumber, @NonNull String message) {
        if (PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phoneNumber));
            intent.putExtra("sms_body", message);
            mContext.startActivity(intent);
        }
    }
}
