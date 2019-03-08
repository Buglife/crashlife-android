package com.buglife.crashlife.android.example;

import android.app.Application;

import com.buglife.crashlife.sdk.Crashlife;


public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        System.loadLibrary("native-lib");
        Crashlife.initWithApiKey(getApplicationContext(), "YOUR_API_KEY_HERE");
    }
}
