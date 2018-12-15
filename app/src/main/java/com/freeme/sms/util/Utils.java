package com.freeme.sms.util;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

public final class Utils {
    private static Application sApplication;

    private Utils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * Init utils.
     * <p>Init it in the class of Application</p>
     *
     * @param context
     */
    public static void init(@NonNull final Context context) {
        sApplication = (Application) context.getApplicationContext();
    }

    /**
     * Return the context of Application object.
     *
     * @return
     */
    public static Application getApplication() {
        if (sApplication != null) {
            return sApplication;
        }

        throw new NullPointerException("u should init first...");
    }
}
