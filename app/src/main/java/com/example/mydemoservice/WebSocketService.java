package com.example.mydemoservice;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;


public class WebSocketService extends Service {

    String host;
    String port;
    URI uri;
    WebSocketClientUtil client;



    @Override
    public void onCreate() {
        Log.i("WebSocketService","onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        host = intent.getStringExtra("connect_host");
        port = intent.getStringExtra("connect_port");
        Log.i("WebSocketService",host);
        Log.i("WebSocketService",port);
        NotificationChannel channel = new NotificationChannel("1", "WebSocketService", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(getApplicationContext(), "1").build();
            startForeground(1001, notification);
        }
        Intent rc_intent = new Intent();
        rc_intent.setAction("MRecevier");
        rc_intent.putExtra("is_connect",initClient());
        sendBroadcast(rc_intent);
        return super.onStartCommand(intent, flags, startId);
    }

    public boolean initClient(){
        String url = "ws://10.120.66.180:8425/ws";
        URI uri = URI.create(url);
        client = new WebSocketClientUtil(uri);
        client.connect();
        Log.i("WebSocketService",String.valueOf(WebSocketService.this.client.isOpen()));
        return  client.isOpen();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i("WebSocketService","onDestroy");
        super.onDestroy();
    }
}
