package com.freeme.sms;

import android.app.Application;

import com.freeme.sms.util.Utils;

public class SmsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
    }
}
