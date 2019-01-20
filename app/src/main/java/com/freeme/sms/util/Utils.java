package com.freeme.sms.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.Telephony;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;

import com.freeme.sms.Factory;
import com.freeme.sms.R;
import com.freeme.sms.receiver.SmsReceiver;

public class Utils {
    private Utils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static Intent getChangeDefaultSmsAppIntent(final Activity activity) {
        final Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
        return intent;
    }

    public static void updateAppConfig(final Context context) {
        // Make sure we set the correct state for the SMS/MMS receivers
        SmsReceiver.updateSmsReceiveHandler(context);
    }

    public static String nonNull(String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }

        return text;
    }

    public static void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) Factory.get().getApplicationContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(null, text));
    }

    public static String getOperatorByNumeric(String operatorNumeric) {
        final Resources res = Factory.get().getApplicationContext().getResources();
        if (TextUtils.isEmpty(operatorNumeric)) {
            return res.getString(R.string.operator_unknown);
        }

        switch (operatorNumeric) {
            case "460000":
            case "460002":
                return res.getString(R.string.operator_china_mobile);
            case "460001":
                return res.getString(R.string.operator_china_unicom);
            case "460003":
                return res.getString(R.string.operator_china_telecom);
            case "46000":
                return res.getString(R.string.operator_china_telecom);
            default:
                return res.getString(R.string.operator_unknown);
        }
    }

    public static void closeKeyboard(Activity activity) {
        InputMethodManager imm =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
        }
    }
}
