package com.freeme.sms.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

public class SpaceItemDecoration extends RecyclerView.ItemDecoration {
    private int mSpace;

    public SpaceItemDecoration(Context context, int dpValue) {
        mSpace = dp2px(context, dpValue);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) > 0) {
            outRect.top = mSpace;
        }
    }

    private int dp2px(Context context, int dpValue) {
        int pxValue = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue,
                context.getResources().getDisplayMetrics());
        return pxValue;
    }
}
