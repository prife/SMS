package com.freeme.sms.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
    private static final int THREAD_POOL_COUNT = 10;
    private static ExecutorService sExecutor = Executors.newFixedThreadPool(THREAD_POOL_COUNT);

    public static void execute(Runnable runnable) {
        sExecutor.execute(runnable);
    }
}
