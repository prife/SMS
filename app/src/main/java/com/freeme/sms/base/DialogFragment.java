package com.freeme.sms.base;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class DialogFragment extends android.support.v4.app.DialogFragment {
    private OnCallDialog mOnCallDialog;
    private OnDialogCancelListener mCancelListener;

    public static DialogFragment newInstance(OnCallDialog callDialog, boolean cancelable) {
        return newInstance(callDialog, cancelable, null);
    }

    public static DialogFragment newInstance(OnCallDialog callDialog, boolean cancelable,
                                             OnDialogCancelListener cancelListener) {
        DialogFragment dialogFragment = new DialogFragment();
        dialogFragment.mOnCallDialog = callDialog;
        dialogFragment.mCancelListener = cancelListener;
        dialogFragment.setCancelable(cancelable);

        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (mOnCallDialog == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        return mOnCallDialog.getDialog(getActivity());
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mCancelListener != null) {
            mCancelListener.onCancel();
        }
    }

    public interface OnDialogCancelListener {
        void onCancel();
    }

    public interface OnCallDialog {
        Dialog getDialog(Context context);
    }

    public interface IDialogResultListener<T> {
        void onDataResult(T result);
    }
}
