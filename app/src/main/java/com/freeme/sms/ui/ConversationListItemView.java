package com.freeme.sms.ui;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.freeme.sms.R;
import com.freeme.sms.model.SmsMessage;
import com.freeme.sms.util.PhoneUtils;
import com.freeme.sms.util.Utils;


public class ConversationListItemView extends LinearLayout implements View.OnClickListener {
    private static final String TAG = "CListItemView";
    private SmsMessage mSmsMessage;
    private HostInterface mHostInterface;

    private TextView mOppositeText;
    private TextView mMyselfText;
    private TextView mContentText;

    public ConversationListItemView(@NonNull Context context) {
        this(context, null);
    }

    public ConversationListItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConversationListItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mOppositeText = findViewById(R.id.tv_opposite);
        mMyselfText = findViewById(R.id.tv_myself);
        mContentText = findViewById(R.id.tv_content);
    }

    @Override
    public void onClick(View v) {
        if (mHostInterface != null) {
            mHostInterface.onConversationClicked(mSmsMessage, this);
        }
    }

    public void bind(final Cursor cursor, final HostInterface hostInterface) {
        if (cursor == null) {
            Log.w(TAG, "list item view not bind null cursor");
            return;
        }

        bind(SmsMessage.get(cursor), hostInterface);
    }

    public void bind(final SmsMessage smsMessage, final HostInterface hostInterface) {
        if (smsMessage == null) {
            Log.w(TAG, "list item view not bind null message");
            return;
        }

        mHostInterface = hostInterface;
        mSmsMessage = smsMessage;
        setOnClickListener(this);

        final Resources res = getResources();
        String oppositeText = mSmsMessage.mAddress;
        mOppositeText.setText(res.getString(R.string.format_string,
                res.getString(R.string.opposite_phone_number),
                Utils.nonNull(oppositeText)));

        String myselfText = PhoneUtils.get(mSmsMessage.mSubId).getSelfRawNumber(true);
        mMyselfText.setText(res.getString(R.string.format_string,
                res.getString(R.string.myself_phone_number),
                Utils.nonNull(myselfText)));

        String contentText = mSmsMessage.mBody;
        mContentText.setText(res.getString(R.string.format_string,
                res.getString(R.string.content),
                Utils.nonNull(contentText)));
    }

    public interface HostInterface {
        void onConversationClicked(final SmsMessage smsMessage,
                                   final ConversationListItemView conversationView);
    }
}
