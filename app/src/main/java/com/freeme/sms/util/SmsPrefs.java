package com.freeme.sms.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SmsPrefs {
    /**
     * Shared preferences name for preferences applicable to the entire app.
     */
    public static final String SHARED_PREFERENCES_NAME = "sms";

    /**
     * Shared preferences name for subscription-specific preferences.
     */
    public static final String SHARED_PREFERENCES_PER_SUBSCRIPTION_PREFIX = "sms_sub_";

    private final Context mContext;

    public SmsPrefs(Context context) {
        mContext = context;
    }

    /**
     * Returns the shared preferences file name to use.
     * Subclasses should override and return the shared preferences file.
     */
    protected String getSharedPreferencesName() {
        return SHARED_PREFERENCES_NAME;
    }

    public String getString(final String key, final String defaultValue) {
        final SharedPreferences prefs = mContext.getSharedPreferences(
                getSharedPreferencesName(), Context.MODE_PRIVATE);
        return prefs.getString(key, defaultValue);
    }

    public void putString(final String key, final String value) {
        final SharedPreferences prefs = mContext.getSharedPreferences(
                getSharedPreferencesName(), Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }
}
