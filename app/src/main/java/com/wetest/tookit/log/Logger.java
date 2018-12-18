package com.wetest.tookit.log;

public class Logger {
    public static final String TAG = "wetestsms";

    private static boolean need_debug_in_system = false;
    static ILogger localInstance = null;

    public static ILogger getLocalLogger(String appName) {
        if (localInstance == null) {
            synchronized (Logger.class) {

                if (localInstance == null) {
                    localInstance = new LocalLogger(appName);
                }
            }
        }
        return localInstance;
    }

    static ILogger systemInstance = null;

    public static ILogger getSystemLogger(String appName) {
        if (systemInstance == null) {
            synchronized (Logger.class) {

                if (systemInstance == null) {
                    systemInstance = new AndroidSystemLogger(appName);
                }
            }
        }
        return systemInstance;
    }

    // default logger
    public static ILogger getLogger(String appName) {
        if (need_debug_in_system) {
            return getSystemLogger(appName);
        } else {
            return getLocalLogger(appName);
        }
    }

    public static ILogger getLogger() {
        return getLogger(TAG);
    }

    public static boolean isNeedDebugInSystem() {
        return need_debug_in_system;
    }

    public static void setNeedDebugInSystem(boolean need_debug_in_system) {
        Logger.need_debug_in_system = need_debug_in_system;
    }
}