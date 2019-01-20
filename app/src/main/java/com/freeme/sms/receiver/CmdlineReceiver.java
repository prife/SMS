package com.freeme.sms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.freeme.sms.EchoServer;
import com.freeme.sms.Factory;
import com.freeme.sms.util.PhoneUtils;
import com.freeme.sms.util.Utils;
import com.wetest.tookit.log.Logger;
import com.wetest.tookit.report.RequestManager;

/**
 * receives from command line messages
 *  am broadcast -a freeme.action.intent.GET_PHONE_NUMBER -n com.freeme.sms/.receiver.CmdlineReceiver
 *
 *  am broadcast -a freeme.action.intent.GET_ECHO_SERVER -n com.freeme.sms/.receiver.CmdlineReceiver
 *  am broadcast -a freeme.action.intent.SET_ECHO_SERVER -n com.freeme.sms/.receiver.CmdlineReceiver -es para 18900000000
 *
 *  am broadcast -a freeme.action.intent.GET_REPORT_URL -n com.freeme.sms/.receiver.CmdlineReceiver
 *  am broadcast -a freeme.action.intent.GET_REPORT_URL -n com.freeme.sms/.receiver.CmdlineReceiver -es para http://...
 */
public class CmdlineReceiver extends BroadcastReceiver {
    private static final String TAG = "CmdlineReceiver";
    private static final String EXTRA_SUB_ID = "para";
    private static final String ACTION_GET_PHONE_NUMBER = "freeme.action.intent.GET_PHONE_NUMBER";

    private static final String ACTION_GET_ECHO_SERVER = "freeme.action.intent.GET_ECHO_SERVER";
    private static final String ACTION_SET_ECHO_SERVER = "freeme.action.intent.SET_ECHO_SERVER";

    private static final String ACTION_SET_REPORT_URL = "freeme.action.intent.SET_REPORT_URL";
    private static final String ACTION_GET_REPORT_URL = "freeme.action.intent.GET_REPORT_URL";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive:" + intent);
        if (ACTION_GET_PHONE_NUMBER.equals(intent.getAction())) {
            String phoneNumber = PhoneUtils.getDefault().getSelfRawNumber(true);
            Log.i(TAG, "PHONE_NUMBER: " + phoneNumber);
            Logger.getLogger().info("PHONE_NUMBER: " + phoneNumber);
        } else if (ACTION_GET_ECHO_SERVER.equals(intent.getAction())) {
            Log.i(TAG, "GET_SERVER: " + EchoServer.getServerNumber(true));
        } else if (ACTION_SET_ECHO_SERVER.equals(intent.getAction())) {
            String serverIp = intent.getStringExtra(EXTRA_SUB_ID);
            Log.i(TAG, "SET_SERVER: " + serverIp);
            EchoServer.saveServerNumber(serverIp);
        } else if (ACTION_GET_REPORT_URL.equals(intent.getAction())) {
            Log.i(TAG, "GET_REPORT_URL: " + RequestManager.getReportUrl(true));
        } else if (ACTION_SET_REPORT_URL.equals(intent.getAction())) {
            String url = intent.getStringExtra(EXTRA_SUB_ID);
            Log.i(TAG, "SET_REPORT_URL: " + url);
            RequestManager.saveReportUrl(url);
        }
    }
}
