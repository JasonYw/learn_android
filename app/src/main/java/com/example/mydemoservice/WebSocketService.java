package com.example.mydemoservice;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;


public class WebSocketService extends Service {

    String host;
    String port;
    WebSocketClientUtil client;
    Intent rc_intent = new Intent();
    private WebSocketServiceBinder wbbinder = new WebSocketServiceBinder();

    class WebSocketServiceBinder extends Binder{
        public WebSocketService getService(){
            return  WebSocketService.this;
        }

    }



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
        rc_intent.setAction("MRecevier");
        NotificationChannel channel = new NotificationChannel("1", "WebSocketService", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(getApplicationContext(), "1").build();
            startForeground(1001, notification);
        }
        initClient();
        return super.onStartCommand(intent, flags, startId);
    }

    public void initClient() {
        String url = "ws://" + host + ":" + port + "/ws";
        URI uri = URI.create(url);
        client = new WebSocketClientUtil(uri) {

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                super.onOpen(handshakedata);
            }

            @Override
            public void onError(Exception ex) {
                Log.i("onError","SEND is_connect");
                rc_intent.putExtra("is_connect",false);
                sendBroadcast(rc_intent);
                super.onError(ex);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("WebSocketClientUtil","onClose");
                super.onClose(code, reason, remote);
            }
        };
        client.setConnectionLostTimeout(110*1000);
        Toast.makeText(WebSocketService.this,"connecting...",Toast.LENGTH_LONG).show();
        new Thread() {
            @Override
            public void run() {
                client.connect();
                while(!client.getReadyState().equals(ReadyState.OPEN)){};
                rc_intent.putExtra("is_connect",client.isOpen());
                sendBroadcast(rc_intent);
            }
        }.start();
        while(!client.getReadyState().equals(ReadyState.OPEN)){};
        sendDeviceData();
    }

    private void sendDeviceData(){
        JSONObject data = new JSONObject();
        try {
            data.put("local_ip", getLocalIp());
            data.put("android_id", getAndroidId());
            data.put("OsName",getOsName());
            client.send(data.toString());
        } catch (JSONException ex) {
            client.send(data.toString());
        }
    }

    private String getOsName() {
        return android.os.Build.VERSION.CODENAME;
    }


    private String getLocalIp () {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
        return ipAddress;
    }


    private String getAndroidId(){
        return Settings.Secure.getString(this.getContentResolver(),Settings.Secure.ANDROID_ID);
    }

    private String getFilesAndPackage(){
        JSONObject data = new JSONObject();
        File file_persisit = new File("/data/system/xsettings/mydemo/persisit");
        File[] templist;
        templist = file_persisit.listFiles();
        ArrayList<String> persisit_list = new ArrayList<>();
        for(int i =0;i<templist.length;i++){
            File tempfile = new File("/data/system/xsettings/mydemo/persisit/"+templist[i].getName()+"/persist_mydemo");
            if(tempfile.exists()){
                persisit_list.add(templist[i].getName());
            }
        }

        try {
            data.put("persisit", persisit_list);
        }catch (JSONException ex){
            Log.i("getFilesAndPackage",ex.toString());
        }
        File file_jsconfig = new File("/data/system/xsettings/mydemo/jscfg");
        templist = file_jsconfig.listFiles();
        ArrayList<String> jsconfig_list = new ArrayList<>();
        for(int i =0;i<templist.length;i++){
            File tempfile = new File("/data/system/xsettings/mydemo/jscfg/"+templist[i].getName()+"/config.js");
            if(tempfile.exists()){
                jsconfig_list.add(templist[i].getName());
            }
        }
        try {
            data.put("jsconfig",jsconfig_list);
        }catch (JSONException ex){
            Log.i("getFilesAndPackage",ex.toString());
        }
        Log.i("getFilesAndPackage",data.toString());
        return  data.toString();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return wbbinder;
    }

    @Override
    public void onDestroy() {
        Log.i("WebSocketService","onDestroy");
        super.onDestroy();
    }
}
