package com.freeme.sms.service;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.util.Log;

import com.freeme.sms.Factory;
import com.freeme.sms.model.SmsMessage;

import com.wetest.tookit.log.Logger;
import com.wetest.tookit.report.RequestManager;


public class ReadSmsService extends Service {
    private static final String TAG = "ReadSmsService";
    private static final String SMS_INBOX_URI = "content://sms/inbox";
    private static final String SMS_RAW_URI = "content://sms/raw";
    private static final String SMS_URI = "content://sms";

    private ContentObserver mReadSmsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri != null && (uri.toString().contains(SMS_RAW_URI)
                    || uri.toString().equals(SMS_URI))) {
                Log.d(TAG, "uri = " + uri + ", which should't notify change");
                return;
            }
            Cursor cursor = getContentResolver().query(Uri.parse(SMS_INBOX_URI),
                    SmsMessage.getProjection(),
                    Telephony.Sms.READ + "=?", new String[]{"0"},
                    Telephony.Sms.Inbox.DEFAULT_SORT_ORDER);
            getLastSmsFromCursor(cursor);
        }
    };

    public ReadSmsService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerObserver();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterObserver();
    }

    private void registerObserver() {
        Logger.getLogger().info("ReadSmsService->onStartCommand->registerObserver()");
        getContentResolver().registerContentObserver(Uri.parse(SMS_URI), true,
                mReadSmsObserver);
    }

    private void unRegisterObserver() {
        Logger.getLogger().info("ReadSmsService->onStartCommand->unRegisterObserver()");
        if (mReadSmsObserver == null) return;

        getContentResolver().unregisterContentObserver(mReadSmsObserver);
    }

    private void getLastSmsFromCursor(final Cursor cursor) {
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    final int columnCount = cursor.getColumnCount();
                    if (columnCount > 0) {
                        SmsMessage smsMessage = SmsMessage.get(cursor);
                        Factory.get().setSmsMessage(smsMessage);
                        Logger.getLogger().info("ReadSmsService->getLastSmsFromCursor()");
                    } else {
                        Logger.getLogger().error("cursor.getColumnCount()=" + columnCount);
                    }
                } else {
                    Logger.getLogger().error("getLastSmsFromCursor is empty");
                }
            } catch (Exception e) {
                Logger.getLogger().error("getLastSmsFromCursor error" + e.toString());
            } finally {
                try {
                    cursor.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            Logger.getLogger().info("getLastSmsFromCursor is null");
        }
    }
}
