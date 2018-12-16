package com.freeme.sms.util;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

import com.freeme.sms.Factory;

public class ToastUtils {
    private static Toast sToast;

    private static final int H_MSG_SHOW_DEFAULT = 0;
    private static final int H_MSG_SHOW_BOTTOM = 1;

    private static Handler sHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (sToast != null) {
                sToast.cancel();
            }

            String message = (String) msg.obj;
            if (TextUtils.isEmpty(message)) {
                return;
            }

            final int gravity;
            switch (msg.what) {
                case H_MSG_SHOW_BOTTOM:
                    gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    break;
                case H_MSG_SHOW_DEFAULT:
                default:
                    gravity = Gravity.CENTER_HORIZONTAL;
                    break;
            }

            sToast = Toast.makeText(Factory.get().getApplicationContext(), message, msg.arg2);
            sToast.setGravity(gravity, 0, 0);
            sToast.show();
        }
    };

    private ToastUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static void toast(String message, final int duration) {
        toast(message, duration, H_MSG_SHOW_DEFAULT);
    }

    public static void toast(String message) {
        toast(message, Toast.LENGTH_SHORT);
    }

    /**
     * Show a simple toast at the default position
     */
    public static void toast(final int messageId, final int duration) {
        String message = Factory.get().getApplicationContext().getString(messageId);
        toast(message, duration, H_MSG_SHOW_DEFAULT);
    }

    /**
     * Show a simple toast at the default position
     */
    public static void toast(final int pluralsMessageId, final int count, final int duration) {
        String message = Factory.get().getApplicationContext().getResources()
                .getQuantityString(pluralsMessageId, count);
        toast(message, duration, H_MSG_SHOW_DEFAULT);
    }

    /**
     * Show a simple toast at the bottom
     */
    public static void toastAtBottom(final int messageId) {
        toastAtBottom(Factory.get().getApplicationContext().getString(messageId));
    }

    /**
     * Show a simple toast at the bottom
     */
    public static void toastAtBottom(final String message) {
        toast(message, Toast.LENGTH_LONG, H_MSG_SHOW_BOTTOM);
    }

    private static void toast(String message, final int duration, final int what) {
        sHandler.sendMessage(sHandler.obtainMessage(what, 0, duration, message));
    }
}
