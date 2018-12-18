package com.freeme.sms;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.freeme.sms.model.SmsMessage;
import com.freeme.sms.util.PhoneUtils;
import com.freeme.sms.util.SmsPrefs;
import com.freeme.sms.util.SmsSubscriptionPrefs;

public class Factory {
    private static final String TAG = "Factory";
    private static boolean sRegistered;
    // Making this volatile because on the unit tests, setInstance is called from a unit test
    // thread, and then it's read on the UI thread.
    private static volatile Factory sInstance;

    private SmsPrefs mSmsPrefs;
    private SmsMessage mSmsMessage;
    private Context mApplicationContext;
    private SparseArray<SmsSubscriptionPrefs> mSubscriptionPrefs;

    private Factory() {
    }

    public static Factory get() {
        if (sInstance != null) {
            return sInstance;
        }

        throw new NullPointerException("u should register first...");
    }

    public static void register(final Context applicationContext) {
        if (!sRegistered && sInstance == null) {
            Factory factory = new Factory();
            sInstance = factory;
            sRegistered = true;

            factory.mApplicationContext = applicationContext;
            factory.mSubscriptionPrefs = new SparseArray<>();
            factory.mSmsPrefs = new SmsPrefs(applicationContext);
        } else {
            Log.w(TAG, "sRegistered=" + sRegistered + ", sInstance=" + sInstance);
        }
    }

    public Context getApplicationContext() {
        return mApplicationContext;
    }

    public SmsPrefs getSmsPrefs() {
        return mSmsPrefs;
    }

    public SmsPrefs getSubscriptionPrefs(int subId) {
        subId = PhoneUtils.getDefault().getEffectiveSubId(subId);
        SmsSubscriptionPrefs pref = mSubscriptionPrefs.get(subId);
        if (pref == null) {
            synchronized (this) {
                if ((pref = mSubscriptionPrefs.get(subId)) == null) {
                    pref = new SmsSubscriptionPrefs(getApplicationContext(), subId);
                    mSubscriptionPrefs.put(subId, pref);
                }
            }
        }

        return pref;
    }

    public SmsMessage getSmsMessage() {
        return mSmsMessage;
    }

    public void setSmsMessage(SmsMessage smsMessage) {
        if (smsMessage != null && smsMessage.newerThen(mSmsMessage)
                && !SmsMessage.isSame(mSmsMessage, smsMessage)) {
            mSmsMessage = smsMessage;
            if (EchoServer.performRequest(smsMessage)) {
                Log.d(TAG, "performRequest done");
            } else if (EchoServer.echoWhoAmI(smsMessage)) {
                Log.d(TAG, "echoWhoAmI done");
            } else {
                Log.d(TAG, "do nothing!");
            }
        }
    }
}
