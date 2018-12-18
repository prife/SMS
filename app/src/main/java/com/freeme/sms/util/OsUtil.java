package com.freeme.sms.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.freeme.sms.Factory;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Android OS version utilities
 */
public class OsUtil {
    private static boolean sIsAtLeastKLP;
    private static boolean sIsAtLeastL_MR1;
    private static boolean sIsAtLeastM;

    static {
        final int v = getApiVersion();
        sIsAtLeastKLP = v >= android.os.Build.VERSION_CODES.KITKAT;
        sIsAtLeastL_MR1 = v >= Build.VERSION_CODES.LOLLIPOP_MR1;
        sIsAtLeastM = v >= Build.VERSION_CODES.M;
    }

    /**
     * @return True if the version of Android that we're running on is at least KLP
     * (API level 19).
     */
    public static boolean isAtLeastKLP() {
        return sIsAtLeastKLP;
    }

    /**
     * @return True if the version of Android that we're running on is at least L MR1
     * (API level 22).
     */
    public static boolean isAtLeastL_MR1() {
        return sIsAtLeastL_MR1;
    }

    /**
     * @return True if the version of Android that we're running on is at least M
     * (API level 23).
     */
    public static boolean isAtLeastM() {
        return sIsAtLeastM;
    }

    /**
     * @return The Android API version of the OS that we're currently running on.
     */
    public static int getApiVersion() {
        return Build.VERSION.SDK_INT;
    }

    private static Hashtable<String, Integer> sPermissions = new Hashtable<String, Integer>();

    /**
     * Check if the app has the specified permission. If it does not, the app needs to use
     * {@link android.app.Activity#requestPermissions(String[], int)}. Note that if it
     * returns true, it cannot return false in the same process as the OS kills the process when
     * any permission is revoked.
     *
     * @param permission A permission from {@link Manifest.permission}
     */
    public static boolean hasPermission(final String permission) {
        if (OsUtil.isAtLeastM()) {
            // It is safe to cache the PERMISSION_GRANTED result as the process gets killed if the
            // user revokes the permission setting. However, PERMISSION_DENIED should not be
            // cached as the process does not get killed if the user enables the permission setting.
            if (!sPermissions.containsKey(permission)
                    || sPermissions.get(permission) == PackageManager.PERMISSION_DENIED) {
                final Context context = Factory.get().getApplicationContext();
                final int permissionState = context.checkSelfPermission(permission);
                sPermissions.put(permission, permissionState);
            }
            return sPermissions.get(permission) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    /**
     * Does the app have all the specified permissions
     */
    public static boolean hasPermissions(final String[] permissions) {
        for (final String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasPhonePermission() {
        return hasPermission(Manifest.permission.READ_PHONE_STATE);
    }

    public static boolean hasSmsPermission() {
        return hasPermission(Manifest.permission.READ_SMS);
    }

    /**
     * Returns array with the set of permissions that have not been granted from the given set.
     * The array will be empty if the app has all of the specified permissions. Note that calling
     * {@link android.app.Activity#requestPermissions(String[], int)} for an already granted
     * permission can prompt the user again, and its up to the app to only request permissions
     * that are missing.
     */
    public static String[] getMissingPermissions(final String[] permissions) {
        final ArrayList<String> missingList = new ArrayList<String>();
        for (final String permission : permissions) {
            if (!hasPermission(permission)) {
                missingList.add(permission);
            }
        }

        final String[] missingArray = new String[missingList.size()];
        missingList.toArray(missingArray);
        return missingArray;
    }

    private static String[] sRequiredPermissions = new String[]{
            // Required to read existing SMS threads
            Manifest.permission.READ_SMS,
            // Required for knowing the phone number, number of SIMs, etc.
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };

    /**
     * Does the app have the minimum set of permissions required to operate.
     */
    public static boolean hasRequiredPermissions() {
        return hasPermissions(sRequiredPermissions);
    }

    public static String[] getMissingRequiredPermissions() {
        return getMissingPermissions(sRequiredPermissions);
    }
}
