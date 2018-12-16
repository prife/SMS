package com.freeme.sms.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Respond to a special intent and send an SMS message without the user's intervention, unless
 * the intent extra "showUI" is true.
 */
public class NoConfirmationSmsSendService extends IntentService {

    public NoConfirmationSmsSendService() {
        // Class name will be the thread name.
        super(NoConfirmationSmsSendService.class.getName());

        // Intent should be redelivered if the process gets killed before completing the job.
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}
