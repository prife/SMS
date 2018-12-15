package com.freeme.sms;

import android.app.Application;

public class SmsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Factory.register(getApplicationContext());
    }
}
