package com.freeme.sms;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.provider.Telephony.Sms;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.freeme.sms.base.DialogFragment;
import com.freeme.sms.model.SmsMessage;
import com.freeme.sms.service.ReadSmsService;
import com.freeme.sms.ui.ConversationListAdapter;
import com.freeme.sms.ui.ConversationListItemView;
import com.freeme.sms.ui.SpaceItemDecoration;
import com.freeme.sms.util.DialogFragmentHelper;
import com.freeme.sms.util.OsUtil;
import com.freeme.sms.util.PhoneUtils;
import com.freeme.sms.util.SmsPrefs;
import com.freeme.sms.util.SmsUtils;
import com.freeme.sms.util.Utils;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        ConversationListItemView.HostInterface,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "MainActivity";

    private static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 1;

    private static final int REQUEST_SET_DEFAULT_SMS_APP = 1;

    private static final String ORDER_BY_DATE_DESC = "date DESC";

    // Saved Instance State Data - only for temporal data which is nice to maintain but not
    // critical for correctness.
    private static final String SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY =
            "conversationListViewState";
    private Parcelable mListState;

    private TextView mNumberTitle;
    private Button mSaveButton;
    private TextInputLayout mInputLayoutSim1;
    private TextInputLayout mInputLayoutSim2;
    private ConversationListItemView mLastSmsLayout;
    private RecyclerView mRecyclerView;
    private ConversationListAdapter mAdapter;

    private LoaderManager mLoaderManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY);
        }

        if (hasRequiredPermissions()) {
            Utils.updateAppConfig(this);
            readSmsAfterEnterSelfNumber();
        }

        trySetAsDefaultSmsApp();
    }

    @Override
    public void onPause() {
        super.onPause();
        mListState = mRecyclerView.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mListState != null) {
            outState.putParcelable(SAVED_INSTANCE_STATE_LIST_VIEW_STATE_KEY, mListState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLoaderManager != null) {
            mLoaderManager.destroyLoader(CONVERSATION_LIST_LOADER);
            mLoaderManager = null;
        }
    }

    private static final int MENU_ITEM_SETTING_NUMBER = 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_SETTING_NUMBER, MENU_ITEM_SETTING_NUMBER, R.string.myself_phone_number);
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
                        read();
                    }
                }, false);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        switch (id) {
            case R.id.btn_save:
                savePhoneNumberFromInputLayout(mInputLayoutSim1);
                savePhoneNumberFromInputLayout(mInputLayoutSim2);
                read();
                break;
            default:
                break;
        }
    }

    @Override
    public void onConversationClicked(final SmsMessage smsMessage,
                                      ConversationListItemView conversationView) {
        DialogFragmentHelper.showSmsMessageCopyDialog(getSupportFragmentManager(), smsMessage, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_SET_DEFAULT_SMS_APP:
                Log.d(TAG, "resultCode:" + resultCode);
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE) {
            // We do not use grantResults as some of the granted permissions might have been
            // revoked while the permissions dialog box was being shown for the missing permissions.
            if (OsUtil.hasRequiredPermissions()) {
                Utils.updateAppConfig(this);
                invalidateOptionsMenu();
                readSmsAfterEnterSelfNumber();
            }
        }
    }

    private static final int CONVERSATION_LIST_LOADER = 1;

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle bundle) {
        Log.v(TAG, "onCreateLoader:" + id);
        Loader<Cursor> loader = null;
        switch (id) {
            case CONVERSATION_LIST_LOADER:
                loader = new CursorLoader(this,
                        Sms.CONTENT_URI,
                        SmsMessage.getProjection(),
                        SmsUtils.getSmsTypeSelectionSql(),
                        null,
                        ORDER_BY_DATE_DESC);
                break;
            default:
                Log.w(TAG, "wrong loader id:" + id);
                break;
        }

        return loader;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        final int id = loader.getId();
        Log.v(TAG, "onLoadFinished:" + id);
        switch (id) {
            case CONVERSATION_LIST_LOADER:
                onConversationListCursorUpdated(cursor);
                break;
            default:
                Log.w(TAG, "wrong loader id:" + id);
                break;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        final int id = loader.getId();
        Log.v(TAG, "onLoaderReset:" + id);
        switch (id) {
            case CONVERSATION_LIST_LOADER:
                onConversationListCursorUpdated(null);
                break;
            default:
                Log.w(TAG, "wrong loader id:" + id);
                break;
        }
    }

    private void initView() {
        mNumberTitle = findViewById(R.id.tv_number_title);
        mSaveButton = findViewById(R.id.btn_save);
        mSaveButton.setOnClickListener(this);
        mInputLayoutSim1 = findViewById(R.id.ti_sim1);
        mInputLayoutSim2 = findViewById(R.id.ti_sim2);
        mLastSmsLayout = findViewById(R.id.last_sms_layout);
        mRecyclerView = findViewById(android.R.id.list);

        mLoaderManager = getSupportLoaderManager();
        mAdapter = new ConversationListAdapter(this, null, this);
        final LinearLayoutManager manager = new LinearLayoutManager(this) {
            @Override
            public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        };
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addItemDecoration(new SpaceItemDecoration(this, 8));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);
    }

    private boolean trySetAsDefaultSmsApp() {
        if (PhoneUtils.getDefault().isDefaultSmsApp()) {
            Log.d(TAG, "set as default sms app.");
            return true;
        } else {
            final Intent intent = Utils.getChangeDefaultSmsAppIntent(this);
            startActivityForResult(intent, REQUEST_SET_DEFAULT_SMS_APP);
        }

        return false;
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

    private void initPhoneNumberEditText() {
        List<SubscriptionInfo> infoList = PhoneUtils.getDefault().toLMr1()
                .getActiveSubscriptionInfoList();
        final int count;
        if (infoList != null && (count = infoList.size()) > 0) {
            mNumberTitle.setVisibility(View.VISIBLE);
            mSaveButton.setVisibility(View.VISIBLE);
            for (int i = 0; i < count; i++) {
                SubscriptionInfo info = infoList.get(i);
                final int slotIndex = info.getSimSlotIndex();
                final int subId = info.getSubscriptionId();
                String number = PhoneUtils.get(subId).getSelfRawNumber(true);
                final TextInputLayout inputLayout;
                switch (slotIndex) {
                    case PhoneUtils.SIM_SLOT_INDEX_1:
                        inputLayout = mInputLayoutSim1;
                        break;
                    case PhoneUtils.SIM_SLOT_INDEX_2:
                        inputLayout = mInputLayoutSim2;
                        break;
                    default:
                        inputLayout = null;
                        break;
                }

                if (inputLayout != null) {
                    inputLayout.setVisibility(View.VISIBLE);
                    inputLayout.setTag(subId);
                    if (TextUtils.isEmpty(number)) {
                        inputLayout.getEditText().setText("");
                    } else {
                        inputLayout.getEditText().setText(number);
                    }
                }
            }
        }
    }

    private void savePhoneNumberFromInputLayout(TextInputLayout inputLayout) {
        if (inputLayout != null && inputLayout.getVisibility() == View.VISIBLE) {
            String number = inputLayout.getEditText().getText().toString();
            final SmsPrefs subPrefs = Factory.get().getSubscriptionPrefs((Integer) inputLayout.getTag());
            subPrefs.putString(Factory.get().getApplicationContext()
                            .getString(R.string.sms_phone_number_pref_key),
                    number);
        }
    }

    private void readSmsAfterEnterSelfNumber() {
        initPhoneNumberEditText();
        startReadService();
        read();
    }

    private void read() {
        if (mLoaderManager != null) {
            mLoaderManager.initLoader(CONVERSATION_LIST_LOADER, null, this);
        } else {
            Log.w(TAG, "loader manger is null.");
        }
    }

    private void startReadService() {
        Intent intent = new Intent(this, ReadSmsService.class);
        startService(intent);
    }

    private void onConversationListCursorUpdated(Cursor cursor) {
        final Cursor oldCursor = mAdapter.swapCursor(cursor);
        final boolean isEmpty = cursor == null || cursor.getCount() == 0;
        Log.d(TAG, "updated, isEmpty=" + isEmpty);
        if (mListState != null && cursor != null && oldCursor == null) {
            mRecyclerView.getLayoutManager().onRestoreInstanceState(mListState);
        }
        bindLastSmsLayout();
    }

    private void bindLastSmsLayout() {
        SmsMessage smsMessage = Factory.get().getSmsMessage();
        if (smsMessage == null) {
            Log.d(TAG, "load last sms from factory is null");
            if (mAdapter != null && mAdapter.getItemCount() > 0) {
                Cursor cursor = (Cursor) mAdapter.getItem(0);
                if (cursor != null) {
                    smsMessage = SmsMessage.get(cursor);
                }
            }
        }
        Log.d(TAG, "load last sms:" + smsMessage);
        mLastSmsLayout.bind(smsMessage, this);
    }
}
