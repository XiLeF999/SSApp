package com.sansan.radio.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 使用线程池提供异步加载的操作
 */
public class ThreadPoolExecutorHelper {
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private Handler mHandler;

    private ThreadPoolExecutorHelper() {
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(8);
        mHandler = new Handler(Looper.getMainLooper());
    }

    private static class SingleInstance {
        private static final ThreadPoolExecutorHelper instance = new ThreadPoolExecutorHelper();
    }

    public static ThreadPoolExecutorHelper getInstance() {
        return SingleInstance.instance;
    }

    public void execute(Runnable runnable) {
        if (runnable != null) {
            scheduledThreadPoolExecutor.execute(runnable);
        }
    }

    public void schedule(Runnable runnable, long delay) {
        if (runnable != null) {
            scheduledThreadPoolExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
        }
    }

    public void cancel(Runnable runnable){
        if (runnable != null){
            scheduledThreadPoolExecutor.remove(runnable);
        }
    }


    public void runUIThread(Runnable runnable) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            runnable.run();
        } else if (mHandler != null) {
            mHandler.post(runnable);
        }
    }

    public void runUIThread(Runnable runnable, long delay) {
        if (mHandler != null) {
            mHandler.postDelayed(runnable, delay);
        }
    }

    public void cancelUIThread(Runnable runnable){
        if (runnable != null){
            mHandler.removeCallbacks(runnable);
        }
    }
}
