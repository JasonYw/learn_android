package com.example.mydemoservice;


import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.net.URI;

public class WebSocketService extends Service {

    private URI uri;
    public  WebSocketClientUtil client;
    private WebSocketClientBinder mBinder = new WebSocketClientBinder();
    private static final int GRAY_SERVICE_ID = 1001;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("WebSocketService","onCreate");
    }


    public class WebSocketClientBinder extends Binder{
        public  WebSocketService getService(){
            return  WebSocketService.this;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }




}
