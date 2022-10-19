package com.example.mydemoservice;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class WebSocketService extends Service {

    String host;
    String port;
    WebSocketClientUtil client;
    Intent rc_intent = new Intent();
    Thread connect_thread;
    Thread reconnect_thread;
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
        reconnect_thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (client != null && client.isClosed()) {
                        try {
                            client.reconnectBlocking();
                            while(!client.getReadyState().equals(ReadyState.OPEN)){};
                            sendDeviceData();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        connect_thread = new Thread() {
            @Override
            public void run() {
                client.connect();
                while(!client.getReadyState().equals(ReadyState.OPEN)){};
                rc_intent.putExtra("is_connect",client.isOpen());
                sendBroadcast(rc_intent);
                sendDeviceData();
            }
        };

        initClient();
        reconnect_thread.start();
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
                Log.i("error",ex.toString());
                if(ex.toString().indexOf("Host unreachable") != -1){
                    rc_intent.putExtra("is_connect",false);
                    sendBroadcast(rc_intent);
                }
                if(ex.toString().indexOf("Connection refused") != -1){
                    try {
                        connect_thread.join();
                        Log.i("onError","connect_thread join");
                    }catch (InterruptedException ex1){
                        Log.i("onError",ex.toString());
                    }

                }
                super.onError(ex);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("WebSocketClientUtil:onClose",reason);
                super.onClose(code, reason, remote);
            }

            @Override
            public void onMessage(String message) {
                try {
                    JSONObject json_message = new JSONObject(message);
                    String task_name =  json_message.getString("task_name");
                    String package_name = json_message.getString("package_name");
                    String data  = json_message.getString("data");
                    switch (task_name){
                        case "open_app":
                            openOtherApp(package_name);
                        default:
                            super.onMessage(message);
                    }
                }catch (JSONException ex) {
                    this.send(ex.toString());
                }
            }
        };
        client.setConnectionLostTimeout(110*1000);
        Toast.makeText(WebSocketService.this,"connecting...",Toast.LENGTH_LONG).show();
        try{
            reconnect_thread.join();
        }catch (InterruptedException ex){
            Log.i("reconnect_thread",ex.toString());
        }
        connect_thread.start();
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

    private void controlHook(String package_name, Boolean on){
        File file = new File("/data/system/xsettings/mydemo/persisit/"+package_name+"/persist_mydemo");
        if(on){
            if(!file.exists()){
                Log.i("Control_hook","CREATE");
                if(file.mkdirs()){
                    Toast.makeText(WebSocketService.this,file.getAbsolutePath() + ":create success",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(WebSocketService.this,file.getAbsolutePath() + ":create fail",Toast.LENGTH_SHORT).show();
                }
            }
            Log.i("Control_hook",String.valueOf(file.exists()));
        }else{
            if(file.exists()){
                Log.i("Control_hook","DELETE");
                if(file.delete()){
                    Toast.makeText(WebSocketService.this,file.getAbsolutePath() + ":delete success",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(WebSocketService.this,file.getAbsolutePath() + ":delete fail",Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(WebSocketService.this,file.getAbsolutePath() + ":delete success",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getJsConfig(String package_name){
        try {
            FileInputStream file = openFileInput("/data/system/xsettings/mydemo/jscfg/" + package_name + "config.js");
            byte[] buffer = new byte[file.available()];
            file.read(buffer);
            String result = new String(buffer);
            return result;
        } catch (FileNotFoundException ex){
            Toast.makeText(WebSocketService.this, "/data/system/xsettings/mydemo/jscfg/" + package_name + "config.js not Found",Toast.LENGTH_SHORT).show();
            return "";
        } catch (IOException ex){
            Toast.makeText(WebSocketService.this, "/data/system/xsettings/mydemo/jscfg/" + package_name + "config.js io error",Toast.LENGTH_SHORT).show();
            return "";
        }
    }

    private boolean updateJsConfig(String package_name,String text){
        File config_dir = new File("/data/system/xsettings/mydemo/jscfg/" + package_name);
        if(!config_dir.exists()){
            Log.i("Control_hook","CREATE");
            if(!config_dir.mkdirs()){
                Toast.makeText(WebSocketService.this,config_dir.getAbsolutePath() + ":create fail",Toast.LENGTH_SHORT).show();
                return  false;
            }
        }
        try {
            FileOutputStream file = openFileOutput("/data/system/xsettings/mydemo/jscfg/" + package_name + " config.js", MODE_PRIVATE + MODE_WORLD_READABLE);
            file.write(text.getBytes());
            file.flush();
            file.close();
            Toast.makeText(WebSocketService.this,"/data/system/xsettings/mydemo/jscfg/" + package_name + " config.js:update success",Toast.LENGTH_SHORT).show();
            return true;
        }catch (FileNotFoundException ex){
            Toast.makeText(WebSocketService.this,"/data/system/xsettings/mydemo/jscfg/" + package_name + " config.js:update fail",Toast.LENGTH_SHORT).show();
            return false;
        }catch (IOException ex){
            Toast.makeText(WebSocketService.this,"/data/system/xsettings/mydemo/jscfg/" + package_name + " config.js:update fail",Toast.LENGTH_SHORT).show();
            return false;
        }

    }

    private boolean deleteJsConfig(String package_name){
        File config_dir = new File("/data/system/xsettings/mydemo/jscfg/" + package_name + "config.js");
        if(config_dir.exists()){
            if(config_dir.delete()){
                return true;
            }else{
                return false;
            }
        }else{
            return true;
        }
    }

    private void openOtherApp(String packageName){
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            if (!getPackageName().equals(packageName)) {
                startActivity(intent);
            }
        }
    }

    private String getPackageInfo(){
        JSONObject data = new JSONObject();
        File[] templist;

        //获取哪些app开启了hook
        File file_persisit = new File("/data/system/xsettings/mydemo/persisit");
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

        //获取app中的config
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

        //获取所有app的版本信息
        PackageManager pckMan = getPackageManager();
        ArrayList<HashMap<String, Object>> items = new ArrayList<HashMap<String, Object>>();
        List<PackageInfo> packageInfo = pckMan.getInstalledPackages(0);
        for (PackageInfo pInfo : packageInfo) {
            HashMap<String, Object> item = new HashMap<String, Object>();
            item.put("appimage", pInfo.applicationInfo.loadIcon(pckMan));
            item.put("packageName", pInfo.packageName);
            item.put("versionCode", pInfo.versionCode);
            item.put("versionName", pInfo.versionName);
            item.put("appName", pInfo.applicationInfo.loadLabel(pckMan).toString());
            items.add(item);
        }
        try {
            data.put("appinfo",packageInfo);
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
