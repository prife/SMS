package com.freeme.sms.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;

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
}
