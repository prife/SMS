package com.wetest.tookit.report;

import android.content.Context;
import android.util.Log;
import com.wetest.tookit.log.Logger;

public class OnReportSmsWtResponseListener implements RequestManager.OnWtResponseListener {
    Context mContext;

    public OnReportSmsWtResponseListener(Context context) {
        mContext = context;
    }

    @Override
    public void onResponse(RequestManager.WtResponse wtResponse) {
        Log.i(Logger.TAG, wtResponse.toString());
        Logger.getLogger().info("onResponse: <" + wtResponse.toString() + ">");
    }
}
