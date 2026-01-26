package com.sansan.radio;

import android.app.Application;
import android.content.Context;

public class SSApplication extends Application {
    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

    }
    public static Context getContext(){
        return context;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}
