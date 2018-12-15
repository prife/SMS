package com.freeme.sms;

import android.Manifest;
import android.annotation.TargetApi;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.provider.Telephony.Sms;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.freeme.sms.base.DialogFragment;
import com.freeme.sms.model.SmsMessage;
import com.freeme.sms.util.DialogFragmentHelper;
import com.freeme.sms.util.OsUtil;
import com.freeme.sms.util.PhoneUtils;
import com.freeme.sms.util.SmsUtils;
import com.freeme.sms.util.SqliteWrapper;
import com.freeme.sms.util.ThreadPool;

import java.util.List;

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
            readSmsAfterEnterSelfNumber();
        }
    }

    private static final int MENU_ITEM_SETTING_NUMBER = 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_SETTING_NUMBER, MENU_ITEM_SETTING_NUMBER, R.string.phone_number);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item;
        item = menu.findItem(MENU_ITEM_SETTING_NUMBER);
        if (item != null) {
            final boolean hasPhonePermission = OsUtil.hasPhonePermission();
            final boolean visible = hasPhonePermission
                    && PhoneUtils.getDefault().getActiveSubscriptionCount() > 0;
            item.setVisible(visible);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case MENU_ITEM_SETTING_NUMBER:
                DialogFragmentHelper.showSmsSubscriptionsDialog(getSupportFragmentManager(), new DialogFragment.IDialogResultListener() {
                    @Override
                    public void onDataResult(Object result) {
                        readSms();
                    }
                }, false);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE) {
            // We do not use grantResults as some of the granted permissions might have been
            // revoked while the permissions dialog box was being shown for the missing permissions.
            if (OsUtil.hasRequiredPermissions()) {
                invalidateOptionsMenu();
                readSmsAfterEnterSelfNumber();
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

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private boolean mayNeedEnterSelfNumber() {
        List<SubscriptionInfo> infoList = PhoneUtils.getDefault().toLMr1()
                .getActiveSubscriptionInfoList();
        final int count;
        if (infoList != null && (count = infoList.size()) > 0) {
            for (int i = 0; i < count; i++) {
                SubscriptionInfo info = infoList.get(i);
                final int subId = info.getSubscriptionId();
                String number = PhoneUtils.get(subId).getSelfRawNumber(true);
                if (TextUtils.isEmpty(number)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void readSmsAfterEnterSelfNumber() {
        if (mayNeedEnterSelfNumber()) {
            DialogFragmentHelper.showSmsSubscriptionsDialog(getSupportFragmentManager(), new DialogFragment.IDialogResultListener() {
                @Override
                public void onDataResult(Object result) {
                    readSms();
                }
            }, false);
        } else {
            readSms();
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.SEND_SMS})
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

    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.SEND_SMS})
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
