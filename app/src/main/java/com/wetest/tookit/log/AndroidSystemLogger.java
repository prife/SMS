package com.wetest.tookit.log;

import android.util.Log;

class AndroidSystemLogger implements ILogger {
    public AndroidSystemLogger(String appName){
        tag=appName;
    }
    protected static String tag = null;

    public void info(String t) {
        Log.i(tag, t);
    }

    public void error(String t) {
        Log.e(tag, t);
    }

    public void error(String t, Throwable e) {
        Log.e(tag, t, e);
    }

    public void debug(String t) {
        Log.d(tag, t);
    }

    public void clearLog(){}

    public String getLogPosition(){return "/dev/log/main";}
}
