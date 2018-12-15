package com.freeme.sms.util;

import android.content.Context;

/**
 * Provides interface to access per-subscription shared preferences. We have one instance of
 * this per active subscription.
 */
public class SmsSubscriptionPrefs extends SmsPrefs {
    private final int mSubId;

    public SmsSubscriptionPrefs(Context context, int subId) {
        super(context);
        mSubId = subId;
    }

    @Override
    protected String getSharedPreferencesName() {
        return SHARED_PREFERENCES_PER_SUBSCRIPTION_PREFIX + String.valueOf(mSubId);
    }
}
