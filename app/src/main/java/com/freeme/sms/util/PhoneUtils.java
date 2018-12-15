package com.freeme.sms.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.freeme.sms.Factory;
import com.freeme.sms.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PhoneUtils {
    private static final String TAG = "PhoneUtils";

    // Cached instance for Pre-L_MR1
    private static final Object PHONEUTILS_INSTANCE_LOCK = new Object();
    private static PhoneUtils sPhoneUtilsInstancePreLMR1 = null;
    // Cached subId->instance for L_MR1 and beyond
    private static final ConcurrentHashMap<Integer, PhoneUtils> sPhoneUtilsInstanceCacheLMR1 =
            new ConcurrentHashMap<>();

    // We always use -1 as default/invalid sub id although system may give us anything negative
    public static final int DEFAULT_SELF_SUB_ID = -1;
    public static final int SIM_SLOT_INDEX_1 = 0;
    public static final int SIM_SLOT_INDEX_2 = 1;

    protected final int mSubId;
    protected final Context mContext;
    protected final TelephonyManager mTelephonyManager;

    public PhoneUtils(int subId) {
        mSubId = subId;
        mContext = Factory.get().getApplicationContext();
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * Check if there is SIM inserted on the device
     *
     * @return true if there is SIM inserted, false otherwise
     */
    public abstract boolean hasSim();

    /**
     * Get the SIM's self raw number, i.e. not canonicalized
     *
     * @param allowOverride Whether to use the app's setting to override the self number
     * @return the original self number
     * @throws IllegalStateException if no active subscription on L-MR1+
     */
    public abstract String getSelfRawNumber(final boolean allowOverride);

    /**
     * Returns the "effective" subId, or the subId used in the context of actual messages,
     * conversations and subscription-specific settings, for the given "nominal" sub id.
     * <p>
     * For pre-L-MR1 platform, this should always be
     * {@value #DEFAULT_SELF_SUB_ID};
     * <p>
     * On the other hand, for L-MR1 and above, DEFAULT_SELF_SUB_ID will be mapped to the system
     * default subscription id for SMS.
     *
     * @param subId The input subId
     * @return the real subId if we can convert
     */
    public abstract int getEffectiveSubId(int subId);

    /**
     * Returns the number of active subscriptions in the device.
     */
    public abstract int getActiveSubscriptionCount();

    /**
     * Get the default SMS subscription id
     *
     * @return the default sub ID
     */
    public abstract int getDefaultSmsSubscriptionId();

    /**
     * Get the subscription_id column value from a telephony provider cursor
     *
     * @param cursor     The database query cursor
     * @param subIdIndex The index of the subId column in the cursor
     * @return the subscription_id column value from the cursor
     */
    public abstract int getSubIdFromTelephony(Cursor cursor, int subIdIndex);

    /**
     * This interface packages methods should only compile on L_MR1.
     * This is needed to make unit tests happy when mockito tries to
     * mock these methods. Calling on these methods on L_MR1 requires
     * an extra invocation of toMr1().
     */
    public interface LMr1 {
        /**
         * Get this SIM's information. Only applies to L_MR1 above
         *
         * @return the subscription info of the SIM
         */
        SubscriptionInfo getActiveSubscriptionInfo();

        /**
         * Get the list of active SIMs in system. Only applies to L_MR1 above
         *
         * @return the list of subscription info for all inserted SIMs
         */
        List<SubscriptionInfo> getActiveSubscriptionInfoList();
    }

    /**
     * The PhoneUtils class for pre L_MR1
     */
    public static class PhoneUtilsPreLMR1 extends PhoneUtils {
        public PhoneUtilsPreLMR1() {
            super(DEFAULT_SELF_SUB_ID);
        }

        @Override
        public boolean hasSim() {
            return mTelephonyManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT;
        }

        @Override
        public String getSelfRawNumber(final boolean allowOverride) {
            if (allowOverride) {
                final String userDefinedNumber = getNumberFromPrefs(mContext, DEFAULT_SELF_SUB_ID);
                if (!TextUtils.isEmpty(userDefinedNumber)) {
                    return userDefinedNumber;
                }
            }
            return mTelephonyManager.getLine1Number();
        }

        @Override
        public int getEffectiveSubId(int subId) {
            return DEFAULT_SELF_SUB_ID;
        }

        @Override
        public int getActiveSubscriptionCount() {
            return hasSim() ? 1 : 0;
        }

        @Override
        public int getDefaultSmsSubscriptionId() {
            Log.w(TAG, "getDefaultSmsSubscriptionId(): not supported before L MR1");
            return DEFAULT_SELF_SUB_ID;
        }

        @Override
        public int getSubIdFromTelephony(Cursor cursor, int subIdIndex) {
            // No subscription_id column before L_MR1
            return DEFAULT_SELF_SUB_ID;
        }
    }

    /**
     * The PhoneUtils class for L_MR1
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public static class PhoneUtilsLMR1 extends PhoneUtils implements LMr1 {
        private static final List<SubscriptionInfo> EMPTY_SUBSCRIPTION_LIST = new ArrayList<>();

        private final SubscriptionManager mSubscriptionManager;

        public PhoneUtilsLMR1(int subId) {
            super(subId);
            mSubscriptionManager = (SubscriptionManager) mContext
                    .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        }

        @Override
        public boolean hasSim() {
            return mSubscriptionManager.getActiveSubscriptionInfoCount() > 0;
        }

        @Override
        public String getSelfRawNumber(final boolean allowOverride) {
            if (allowOverride) {
                final String userDefinedNumber = getNumberFromPrefs(mContext, mSubId);
                if (!TextUtils.isEmpty(userDefinedNumber)) {
                    return userDefinedNumber;
                }
            }

            final SubscriptionInfo subInfo = getActiveSubscriptionInfo();
            if (subInfo != null) {
                String phoneNumber = subInfo.getNumber();
                if (TextUtils.isEmpty(phoneNumber)) {
                    Log.d(TAG, "SubscriptionInfo phone number for self is empty!");
                }
                return phoneNumber;
            }
            Log.w(TAG, "getSelfRawNumber: subInfo is null for " + mSubId);

            return null;
        }

        @Override
        public int getEffectiveSubId(int subId) {
            if (subId == DEFAULT_SELF_SUB_ID) {
                return getDefaultSmsSubscriptionId();
            }
            return subId;
        }

        @Override
        public int getActiveSubscriptionCount() {
            return mSubscriptionManager.getActiveSubscriptionInfoCount();
        }

        @Override
        public SubscriptionInfo getActiveSubscriptionInfo() {
            try {
                final SubscriptionInfo subInfo =
                        mSubscriptionManager.getActiveSubscriptionInfo(mSubId);
                if (subInfo == null) {
                    // This is possible if the sub id is no longer available.
                    Log.d(TAG, "getActiveSubscriptionInfo(): empty sub info for " + mSubId);
                }
                return subInfo;
            } catch (Exception e) {
                Log.e(TAG, "getActiveSubscriptionInfo: system exception for " + mSubId, e);
            }
            return null;
        }

        @Override
        public List<SubscriptionInfo> getActiveSubscriptionInfoList() {
            final List<SubscriptionInfo> subscriptionInfos =
                    mSubscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptionInfos != null) {
                return subscriptionInfos;
            }
            return EMPTY_SUBSCRIPTION_LIST;
        }

        @Override
        public int getDefaultSmsSubscriptionId() {
            final int systemDefaultSubId = SmsManager.getDefaultSmsSubscriptionId();
            if (systemDefaultSubId < 0) {
                // Always use -1 for any negative subId from system
                return DEFAULT_SELF_SUB_ID;
            }
            return systemDefaultSubId;
        }

        @Override
        public int getSubIdFromTelephony(Cursor cursor, int subIdIndex) {
            return getEffectiveIncomingSubIdFromSystem(cursor.getInt(subIdIndex));
        }

        private int getEffectiveIncomingSubIdFromSystem(int subId) {
            if (subId < 0) {
                if (mSubscriptionManager.getActiveSubscriptionInfoCount() > 1) {
                    // For multi-SIM device, we can not decide which SIM to use if system
                    // does not know either. So just make it the invalid sub id.
                    return DEFAULT_SELF_SUB_ID;
                }
                // For single-SIM device, it must come from the only SIM we have
                return getDefaultSmsSubscriptionId();
            }
            return subId;
        }
    }

    /**
     * A convenient get() method that uses the default SIM. Use this when SIM is
     * not relevant, e.g. isDefaultSmsApp
     *
     * @return an instance of PhoneUtils for default SIM
     */
    public static PhoneUtils getDefault() {
        return getPhoneUtils(DEFAULT_SELF_SUB_ID);
    }

    /**
     * Get an instance of PhoneUtils associated with a specific SIM, which is also platform
     * specific.
     *
     * @param subId The SIM's subscription ID
     * @return the instance
     */
    public static PhoneUtils get(int subId) {
        return getPhoneUtils(subId);
    }

    private static PhoneUtils getPhoneUtils(int subId) {
        if (OsUtil.isAtLeastL_MR1()) {
            if (subId == DEFAULT_SELF_SUB_ID) {
                subId = SmsManager.getDefaultSmsSubscriptionId();
            }
            if (subId < 0) {
                Log.w(TAG, "getForLMR1(): invalid subId = " + subId);
                subId = DEFAULT_SELF_SUB_ID;
            }
            PhoneUtils instance = sPhoneUtilsInstanceCacheLMR1.get(subId);
            if (instance == null) {
                instance = new PhoneUtils.PhoneUtilsLMR1(subId);
                sPhoneUtilsInstanceCacheLMR1.putIfAbsent(subId, instance);
            }
            return instance;
        } else {
            Log.w(TAG, "getForPreLMR1(): subId should be " + DEFAULT_SELF_SUB_ID + ", but is " + subId);
            if (sPhoneUtilsInstancePreLMR1 == null) {
                synchronized (PHONEUTILS_INSTANCE_LOCK) {
                    if (sPhoneUtilsInstancePreLMR1 == null) {
                        sPhoneUtilsInstancePreLMR1 = new PhoneUtils.PhoneUtilsPreLMR1();
                    }
                }
            }
            return sPhoneUtilsInstancePreLMR1;
        }
    }

    private static String getNumberFromPrefs(final Context context, final int subId) {
        final SmsPrefs prefs = Factory.get().getSubscriptionPrefs(subId);
        final String mmsPhoneNumberPrefKey = context.getString(R.string.sms_phone_number_pref_key);
        final String userDefinedNumber = prefs.getString(mmsPhoneNumberPrefKey, null);
        if (!TextUtils.isEmpty(userDefinedNumber)) {
            return userDefinedNumber;
        }
        return null;
    }

    public LMr1 toLMr1() {
        if (OsUtil.isAtLeastL_MR1()) {
            return (LMr1) this;
        } else {
            Log.w(TAG, "toLMr1(): invalid OS version");
            return null;
        }
    }
}
