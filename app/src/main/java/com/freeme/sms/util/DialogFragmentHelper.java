package com.freeme.sms.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.freeme.sms.EchoServer;
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
        final List<SubscriptionInfo> infoList = PhoneUtils.getDefault().toLMr1()
                .getActiveSubscriptionInfoList();
        final int count;
        if (infoList == null || (count = infoList.size()) <= 0) {
            Log.w(TAG, "showSmsSubscriptionsDialog: no Active SubscriptionInfo");
            return;
        }

        DialogFragment dialogFragment = DialogFragment.newInstance(new DialogFragment.OnCallDialog() {
            @Override
            public Dialog getDialog(Context context) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View view = LayoutInflater.from(context).inflate(R.layout.layout_edit_sms_subscriptions, null);
                final TextInputLayout sim1 = view.findViewById(R.id.ti_sim1);
                final TextInputLayout sim2 = view.findViewById(R.id.ti_sim2);
                initPhoneNumberEditText(context, sim1, sim2);

                builder.setTitle(R.string.myself_phone_number)
                        .setView(view);

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        savePhoneNumberFromInputLayout(sim1);
                        savePhoneNumberFromInputLayout(sim2);

                        if (resultListener != null) {
                            resultListener.onDataResult(null);
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null);

                return builder.create();
            }

            private void initPhoneNumberEditText(Context context, TextInputLayout sim1,
                                                 TextInputLayout sim2) {
                for (int i = 0; i < count; i++) {
                    SubscriptionInfo info = infoList.get(i);
                    final int slotIndex = info.getSimSlotIndex();
                    final int subId = info.getSubscriptionId();
                    String number = PhoneUtils.get(subId).getSelfRawNumber(true);
                    final TextInputLayout inputLayout;
                    final int operatorFormatRes;
                    switch (slotIndex) {
                        case PhoneUtils.SIM_SLOT_INDEX_1:
                            inputLayout = sim1;
                            operatorFormatRes = R.string.sim_slot_1_with_operator;
                            break;
                        case PhoneUtils.SIM_SLOT_INDEX_2:
                            inputLayout = sim2;
                            operatorFormatRes = R.string.sim_slot_2_with_operator;
                            break;
                        default:
                            inputLayout = null;
                            operatorFormatRes = 0;
                            break;
                    }

                    if (inputLayout != null) {
                        if (operatorFormatRes != 0) {
                            String operatorNumeric = PhoneUtils.get(subId).getSimOperatorNumeric();
                            String operator = Utils.getOperatorByNumeric(operatorNumeric);
                            String hint = context.getString(operatorFormatRes, operator);
                            inputLayout.setHint(hint);
                        }
                        inputLayout.setTag(subId);
                        if (TextUtils.isEmpty(number)) {
                            inputLayout.getEditText().setText("");
                        } else {
                            inputLayout.getEditText().setText(number);
                        }
                        inputLayout.setVisibility(View.VISIBLE);
                    }
                }
            }

            private void savePhoneNumberFromInputLayout(TextInputLayout inputLayout) {
                if (inputLayout != null && inputLayout.getVisibility() == View.VISIBLE) {
                    String number = inputLayout.getEditText().getText().toString();
                    final SmsPrefs subPrefs = Factory.get()
                            .getSubscriptionPrefs((Integer) inputLayout.getTag());
                    subPrefs.putString(Factory.get().getApplicationContext()
                                    .getString(R.string.sms_phone_number_pref_key),
                            number);
                }
            }
        }, cancelable);

        dialogFragment.show(fm, dialogFragment.getClass().getSimpleName());
    }

    public static void showSmsMessageCopyDialog(FragmentManager fm, final SmsMessage smsMessage,
                                                final boolean cancelable) {
        DialogFragment dialogFragment = DialogFragment.newInstance(new DialogFragment.OnCallDialog() {
            private AlertDialog mDialog;

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
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null);

                mDialog = builder.create();
                return mDialog;
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

                    // dismiss dialog.
                    mDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
                }
            };
        }, cancelable);

        dialogFragment.show(fm, dialogFragment.getClass().getSimpleName());
    }

    public static void showSendDialog(FragmentManager fm, boolean cancelable) {
        final List<SubscriptionInfo> infoList = PhoneUtils.getDefault().toLMr1()
                .getActiveSubscriptionInfoList();
        final int count;
        if (infoList == null || (count = infoList.size()) <= 0) {
            Log.w(TAG, "showSendDialog: no Active SubscriptionInfo");
            return;
        }

        final DialogFragment dialogFragment = DialogFragment.newInstance(new DialogFragment.OnCallDialog() {
            private AlertDialog mDialog;

            @Override
            public Dialog getDialog(Context context) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View view = LayoutInflater.from(context).inflate(R.layout.layout_send_sms, null);
                final TextInputLayout server = view.findViewById(R.id.ti_server);
                final String serverNumber = EchoServer.getServerNumber(true);
                server.getEditText().setText(serverNumber);

                Button sim1 = view.findViewById(R.id.btn_sim1);
                Button sim2 = view.findViewById(R.id.btn_sim2);
                initSendButton(context, sim1, sim2);

                builder.setTitle(R.string.echo_server)
                        .setView(view)
                        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String number = server.getEditText().getText().toString();
                                EchoServer.saveServerNumber(number);
                                ToastUtils.toast(R.string.save_success, Toast.LENGTH_SHORT);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null);

                mDialog = builder.create();
                return mDialog;
            }

            private void initSendButton(Context context, Button sim1, Button sim2) {
                for (int i = 0; i < count; i++) {
                    SubscriptionInfo info = infoList.get(i);
                    final int slotIndex = info.getSimSlotIndex();
                    final int subId = info.getSubscriptionId();
                    final Button sendButton;
                    final int operatorFormatRes;
                    switch (slotIndex) {
                        case PhoneUtils.SIM_SLOT_INDEX_1:
                            operatorFormatRes = R.string.sim_slot_1_with_operator;
                            sendButton = sim1;
                            break;
                        case PhoneUtils.SIM_SLOT_INDEX_2:
                            operatorFormatRes = R.string.sim_slot_2_with_operator;
                            sendButton = sim2;
                            break;
                        default:
                            sendButton = null;
                            operatorFormatRes = 0;
                            break;
                    }

                    if (sendButton != null) {
                        if (operatorFormatRes != 0) {
                            String operatorNumeric = PhoneUtils.get(subId).getSimOperatorNumeric();
                            String operator = Utils.getOperatorByNumeric(operatorNumeric);
                            String hint = context.getString(operatorFormatRes, operator);
                            sendButton.setText(hint);
                        }
                        sendButton.setTag(subId);
                        sendButton.setVisibility(View.VISIBLE);
                        sendButton.setOnClickListener(mClickListener);
                    }
                }
            }

            private View.OnClickListener mClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int id = v.getId();
                    final int subId;
                    switch (id) {
                        case R.id.btn_sim1:
                        case R.id.btn_sim2:
                            subId = (Integer) v.getTag();
                            break;
                        default:
                            subId = PhoneUtils.DEFAULT_SELF_SUB_ID;
                            break;
                    }
                    if (subId != PhoneUtils.DEFAULT_SELF_SUB_ID) {
                        EchoServer.sendToServer(subId);
                    } else {
                        Log.w(TAG, "check subId=" + subId);
                    }

                    // dismiss dialog.
                    mDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
                }
            };
        }, cancelable);

        dialogFragment.show(fm, dialogFragment.getClass().getSimpleName());
    }
}
