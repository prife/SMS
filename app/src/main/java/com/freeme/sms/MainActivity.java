package com.freeme.sms;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.freeme.sms.model.SmsMessage;
import com.freeme.sms.util.OsUtil;
import com.freeme.sms.util.SmsUtils;
import com.freeme.sms.util.SqliteWrapper;
import com.freeme.sms.util.ThreadPool;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 1;

    private static final String ORDER_BY_DATE_DESC = "date DESC";

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.tv_show);

        if (hasRequiredPermissions()) {
            readSms();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE) {
            // We do not use grantResults as some of the granted permissions might have been
            // revoked while the permissions dialog box was being shown for the missing permissions.
            if (OsUtil.hasRequiredPermissions()) {
                readSms();
            }
        }
    }

    private boolean hasRequiredPermissions() {
        final boolean hasRequiredPermissions = OsUtil.hasRequiredPermissions();
        if (!hasRequiredPermissions) {
            tryRequestPermission();
        }

        return hasRequiredPermissions;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void tryRequestPermission() {
        final String[] missingPermissions = OsUtil.getMissingRequiredPermissions();
        if (missingPermissions.length == 0) {
            return;
        }

        requestPermissions(missingPermissions, REQUIRED_PERMISSIONS_REQUEST_CODE);
    }

    private void readSms() {
        ThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = getSmsCursor(SmsUtils.getSmsTypeSelectionSql());
                if (cursor != null) {
                    StringBuilder sb = new StringBuilder();
                    try {
                        cursor.moveToPosition(-1);
                        int index = 1;
                        while (cursor.moveToNext()) {
                            sb.append("sms-" + index++ + ":\n");
                            SmsMessage smsMessage = SmsMessage.get(cursor);
                            Log.d(TAG, "smsMessage = " + smsMessage);
                            sb.append(smsMessage).append("\n");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            cursor.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    final String msg = sb.toString();
                    if (!TextUtils.isEmpty(msg)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextView.setText(msg);
                            }
                        });
                    }
                }
            }
        });
    }

    private Cursor getSmsCursor(String smsSelection) {
        Cursor smsCursor = null;

        try {
            smsCursor = SqliteWrapper.query(this,
                    Sms.CONTENT_URI,
                    SmsMessage.getProjection(),
                    smsSelection,
                    null /* selectionArgs */,
                    ORDER_BY_DATE_DESC);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return smsCursor;
    }
}
