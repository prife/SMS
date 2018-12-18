package com.wetest.tookit.log;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

/**
 * Local log output:  /sdcard/<appName>.log
 */
class LocalLogger implements ILogger {
    private static final String TAG = Logger.TAG;
    private String logPath;
    Writer mWriter = null;
    boolean enableLog = true;

    public LocalLogger(String appName) {
        logPath = Environment.getExternalStorageDirectory() + "/" + appName + ".log";
        resetWriter();
    }

    public String getLogPosition() {
        return logPath;
    }

    private void resetWriter() {
        try {
            Log.i(TAG, "new file writer " + logPath);
//            mWriter = new OutputStreamWriter(
//                    new FileOutputStream(logPath), "GBK");
            mWriter = new FileWriter(logPath);
        } catch (IOException e) {
            Log.i(TAG, "file writer create failed");
            e.printStackTrace();
        }
    }

    protected void finalize() {
        try {
            if (mWriter != null) {
                mWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearLog() {
        File f = new File(logPath);
        if (f.exists()) {
            f.delete();
        }
    }

    private String getCurSystemTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
        Date curTime = new Date(System.currentTimeMillis());
        return formatter.format(curTime);
    }

    // write log to file
    private boolean printNewLine(String type, String content) {
        String newline = getCurSystemTime() + "  " + type + "  " + content + '\n';

        File f = new File(logPath);
        if (!f.exists()) {
            resetWriter();
        }

        try {
            mWriter.write(newline);
            mWriter.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "write failed", e);
            e.printStackTrace();
            return false;
        }
    }

    public void info(String t) {
        printNewLine("I", t);
    }

    public void error(String t) {
        printNewLine("E", t);
    }

    public void error(String t, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        printNewLine("E", t + '\n' + sw.toString());
    }

    public void debug(String t) {
        printNewLine("D", t);
    }
}
