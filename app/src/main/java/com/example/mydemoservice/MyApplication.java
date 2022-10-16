package com.example.mydemoservice;

import android.app.Application;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


}
