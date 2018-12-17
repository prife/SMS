package com.freeme.sms.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.freeme.sms.Factory;
import com.freeme.sms.R;
import com.freeme.sms.base.DialogFragment;
import com.freeme.sms.base.DialogFragment.IDialogResultListener;
import com.freeme.sms.model.SmsMessage;

import java.util.List;

public class DialogFragmentHelper {
    private static final String TAG = "DialogFragmentHelper";

    public static void showSmsSubscriptionsDialog(FragmentManager fm,
                                                  final IDialogResultListener resultListener,
                                                  final boolean cancelable) {
        int sim1SubId = PhoneUtils.DEFAULT_SELF_SUB_ID;
        int sim2SubId = PhoneUtils.DEFAULT_SELF_SUB_ID;
        List<SubscriptionInfo> infoList = PhoneUtils.getDefault().toLMr1()
                .getActiveSubscriptionInfoList();
        final int count;
        final SparseArray<String> phoneNumberArray;
        if (infoList != null && (count = infoList.size()) > 0) {
            phoneNumberArray = new SparseArray<>(count);
            for (int i = 0; i < count; i++) {
                SubscriptionInfo info = infoList.get(i);
                final int slotIndex = info.getSimSlotIndex();
                final int subId = info.getSubscriptionId();
                String number = PhoneUtils.get(subId).getSelfRawNumber(true);
                phoneNumberArray.put(subId, number);
                switch (slotIndex) {
                    case PhoneUtils.SIM_SLOT_INDEX_1:
                        sim1SubId = subId;
                        break;
                    case PhoneUtils.SIM_SLOT_INDEX_2:
                        sim2SubId = subId;
                        break;
                    default:
                        break;
                }
            }
        } else {
            Log.d(TAG, "showSmsSubscriptionsDialog: no Active SubscriptionInfo");
            return;
        }

        final int subId1 = sim1SubId;
        final int subId2 = sim2SubId;
        DialogFragment dialogFragment = DialogFragment.newInstance(new DialogFragment.OnCallDialog() {
            @Override
            public Dialog getDialog(Context context) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View view = LayoutInflater.from(context).inflate(R.layout.layout_sms_subscriptions, null);
                final TextInputLayout sim1 = view.findViewById(R.id.ti_sim1);
                final TextInputLayout sim2 = view.findViewById(R.id.ti_sim2);
                fillPhoneNumber(sim1, subId1);
                fillPhoneNumber(sim2, subId2);

                builder.setTitle(R.string.myself_phone_number)
                        .setView(view);

                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (subId1 != PhoneUtils.DEFAULT_SELF_SUB_ID) {
                            String number = sim1.getEditText().getText().toString();
                            savePhoneNumberInPrefs(number, subId1);
                        }

                        if (subId2 != PhoneUtils.DEFAULT_SELF_SUB_ID) {
                            String number = sim2.getEditText().getText().toString();
                            savePhoneNumberInPrefs(number, subId2);
                        }

                        if (resultListener != null) {
                            resultListener.onDataResult(null);
                        }
                    }
                });

                return builder.create();
            }

            private void fillPhoneNumber(TextInputLayout inputLayout, int subId) {
                if (subId != PhoneUtils.DEFAULT_SELF_SUB_ID) {
                    inputLayout.getEditText().setText(phoneNumberArray.get(subId));
                    inputLayout.setVisibility(View.VISIBLE);
                } else {
                    inputLayout.setVisibility(View.GONE);
                }
            }

            private void savePhoneNumberInPrefs(final String newPhoneNumber, final int subId) {
                final SmsPrefs subPrefs = Factory.get().getSubscriptionPrefs(subId);
                subPrefs.putString(Factory.get().getApplicationContext()
                                .getString(R.string.sms_phone_number_pref_key),
                        newPhoneNumber);
            }
        }, cancelable);

        dialogFragment.show(fm, dialogFragment.getClass().getSimpleName());
    }

    public static void showSmsMessageCopyDialog(FragmentManager fm, final SmsMessage smsMessage,
                                                final boolean cancelable) {
        DialogFragment dialogFragment = DialogFragment.newInstance(new DialogFragment.OnCallDialog() {
            @Override
            public Dialog getDialog(Context context) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View view = LayoutInflater.from(context).inflate(R.layout.layout_sms_message_detail, null);
                final TextView oppositeTv = view.findViewById(R.id.tv_opposite);
                oppositeTv.setText(Utils.nonNull(smsMessage.mAddress));

                final TextView myselfTv = view.findViewById(R.id.tv_myself);
                myselfTv.setText(Utils.nonNull(PhoneUtils.get(smsMessage.mSubId).getSelfRawNumber(true)));

                final TextView contentTv = view.findViewById(R.id.tv_content);
                contentTv.setText(Utils.nonNull(smsMessage.mBody));

                final Button oppositeBtn = view.findViewById(R.id.btn_copy_opposite);
                oppositeBtn.setOnClickListener(mClickListener);
                final Button myselfBtn = view.findViewById(R.id.btn_copy_myself);
                myselfBtn.setOnClickListener(mClickListener);
                final Button contentBtn = view.findViewById(R.id.btn_copy_content);
                contentBtn.setOnClickListener(mClickListener);
                final Button allBtn = view.findViewById(R.id.btn_copy_all);
                allBtn.setOnClickListener(mClickListener);

                builder.setTitle(R.string.copy)
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, null);

                return builder.create();
            }

            private View.OnClickListener mClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int id = v.getId();
                    String text = null;
                    switch (id) {
                        case R.id.btn_copy_opposite:
                            text = Utils.nonNull(smsMessage.mAddress);
                            break;
                        case R.id.btn_copy_myself:
                            text = Utils.nonNull(PhoneUtils.get(smsMessage.mSubId)
                                    .getSelfRawNumber(true));
                            break;
                        case R.id.btn_copy_content:
                            text = Utils.nonNull(smsMessage.mBody);
                            break;
                        case R.id.btn_copy_all:
                            text = smsMessage.toString();
                            break;
                        default:
                            break;
                    }

                    if (text != null) {
                        Utils.copyToClipboard(text);
                        ToastUtils.toast(R.string.copy_success, Toast.LENGTH_SHORT);
                    }
                }
            };
        }, cancelable);

        dialogFragment.show(fm, dialogFragment.getClass().getSimpleName());
    }
}
