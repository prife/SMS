package com.freeme.sms.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.telephony.SubscriptionInfo;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.freeme.expandablelayout.ExpandableLayout;
import com.freeme.sms.R;
import com.freeme.sms.util.PhoneUtils;
import com.freeme.sms.util.Utils;

import java.util.List;

public class SubscriptionsNumberLayout extends LinearLayout implements View.OnClickListener {
    private Button mEditButton;
    private TextView mNumberTitle;
    private TextView mSim1;
    private TextView mSim2;
    private ExpandableLayout mExpandableLayout;

    public SubscriptionsNumberLayout(Context context) {
        super(context);
    }

    public SubscriptionsNumberLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SubscriptionsNumberLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSim1 = findViewById(R.id.tv_sim1);
        mSim2 = findViewById(R.id.tv_sim2);
        mEditButton = findViewById(R.id.btn_edit);
        mNumberTitle = findViewById(R.id.tv_number_title);
        mExpandableLayout = findViewById(R.id.expand_layout);
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        switch (id) {
            case R.id.tv_number_title:
                mExpandableLayout.toggle();
                break;
            case R.id.btn_edit:
                if (mHostInterface != null) {
                    mHostInterface.editPhoneNumber();
                }
                break;
            default:
                break;
        }
    }

    public void updatePhoneNumberLayout(boolean init) {
        List<SubscriptionInfo> infoList = PhoneUtils.getDefault().toLMr1()
                .getActiveSubscriptionInfoList();
        final int count;
        if (infoList != null && (count = infoList.size()) > 0) {
            setVisibility(VISIBLE);
            if (init) {
                mNumberTitle.setOnClickListener(this);
                mEditButton.setOnClickListener(this);
                mExpandableLayout.setOnExpansionUpdateListener(new ExpandableLayout.OnExpansionUpdateListener() {
                    @Override
                    public void onExpansionUpdate(float expansionFraction, int state) {
                        final boolean isExpand = mExpandableLayout.isExpanded();
                        final int arrowRes = isExpand ? R.drawable.ic_up_arrow : R.drawable.ic_down_arrow;
                        mNumberTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, arrowRes, 0);
                    }
                });
            }

            for (int i = 0; i < count; i++) {
                SubscriptionInfo info = infoList.get(i);
                final int slotIndex = info.getSimSlotIndex();
                final int subId = info.getSubscriptionId();
                String number = PhoneUtils.get(subId).getSelfRawNumber(true);
                final TextView simText;
                final int operatorFormatRes;
                switch (slotIndex) {
                    case PhoneUtils.SIM_SLOT_INDEX_1:
                        simText = mSim1;
                        operatorFormatRes = R.string.sim_slot_1_with_operator;
                        break;
                    case PhoneUtils.SIM_SLOT_INDEX_2:
                        simText = mSim2;
                        operatorFormatRes = R.string.sim_slot_2_with_operator;
                        break;
                    default:
                        simText = null;
                        operatorFormatRes = 0;
                        break;
                }

                if (simText != null) {
                    String operatorNumeric = PhoneUtils.get(subId).getSimOperatorNumeric();
                    String operator = Utils.getOperatorByNumeric(operatorNumeric);
                    String start = getContext().getString(operatorFormatRes, operator);
                    number = Utils.nonNull(number);
                    String simNumber = getContext().getString(R.string.format_string, start, number);
                    simText.setTag(subId);
                    simText.setText(simNumber);
                    simText.setVisibility(View.VISIBLE);
                }
            }
        } else {
            setVisibility(GONE);
        }
    }

    private HostInterface mHostInterface;

    public void setHostInterface(HostInterface hostInterface) {
        mHostInterface = hostInterface;
    }

    public interface HostInterface {
        void editPhoneNumber();
    }
}
