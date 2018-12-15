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

import com.freeme.sms.Factory;
import com.freeme.sms.R;
import com.freeme.sms.base.DialogFragment;
import com.freeme.sms.base.DialogFragment.IDialogResultListener;

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

                builder.setTitle(R.string.phone_number)
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
}
