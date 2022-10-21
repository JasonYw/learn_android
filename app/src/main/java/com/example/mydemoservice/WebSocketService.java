package com.example.mydemoservice;


import android.app.ActivityManager;
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
import android.system.ErrnoException;
import android.system.Os;
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
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class WebSocketService extends Service {

    String host;
    String port;
    private Process process;
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
        Log.i("WebSocketService:onCreate","onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        host = intent.getStringExtra("connect_host");
        port = intent.getStringExtra("connect_port");
        Log.i("WebSocketService:onStartCommand",host);
        Log.i("WebSocketService:onStartCommand",port);
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
                Log.i("WebSocketService:reconnect_thread","reconnect_thread start");
                if (client != null && client.isClosed()) {
                    Log.i("WebSocketService:reconnect_thread","retry:" + host + ":"+ port);
                    try {
                        client.reconnectBlocking();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };
        connect_thread = new Thread() {
            @Override
            public void run() {
                Log.i("WebSocketService:connect_thread","connect_thread start");
                try {
                    client.connectBlocking();
                }catch (InterruptedException ex){
                    Log.i("WebSocketService:connect_thread",ex.toString());

                }
            }
        };

        initClient();
        return super.onStartCommand(intent, flags, startId);
    }

    public void initClient() {
        String url = "ws://" + host + ":" + port + "/ws";
        URI uri = URI.create(url);
        client = new WebSocketClientUtil(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i("WebSocketService:onOpen","isOpen:" + String.valueOf(client.isOpen()));
                rc_intent.putExtra("is_connect",client.isOpen());
                sendBroadcast(rc_intent);
                sendDeviceData();
                super.onOpen(handshakedata);
            }

            @Override
            public void onError(Exception ex) {
                Log.i("WebSocketService:onError",ex.toString());
                if(ex.toString().indexOf("Connection refused") != -1){
                    connect_thread.interrupt();
                }else{
                    rc_intent.putExtra("is_connect",false);
                    sendBroadcast(rc_intent);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("WebSocketClientUtil:onClose","code:"+String.valueOf(code)+"reason:"+reason);
                super.onClose(code, reason, remote);
                if(code != 1000){
                    reconnect_thread.interrupt();
                    reconnect_thread.start();
                }
            }

            @Override
            public void onMessage(String message) {
                try {
                    JSONObject json_message = new JSONObject(message);
                    String task_name =  json_message.getString("task_name");
                    Log.i("WebSocketClientUtil:onMessage","task_name:"+task_name);
                    String package_name = json_message.getString("package_name");
                    String data  = json_message.getString("data");
                    switch (task_name){
                        case "open_app":
                            openOtherApp(package_name);
                            break;
                        case "check_config":
                            send(getJsConfig(package_name));
                            break;
                        case "start_hook":
                            controlHook(package_name,true);
                            break;
                        case "stop_hook":
                            controlHook(package_name,false);
                            break;
                        case "update_config":
                            updateJsConfig(package_name,data);
                            break;
                        case "delete_config":
                            deleteJsConfig(package_name);
                            break;
                        default:
                            send("task_name not correct");
                            break;
                    }
                    sendDeviceData();
                }catch (JSONException ex) {
                    this.send(ex.toString());
                }
            }
        };
        client.setConnectionLostTimeout(5*1000);
        reconnect_thread.interrupt();
        connect_thread.interrupt();
        connect_thread.start();
    }


    private void sendDeviceData(){
        JSONObject data = new JSONObject();
        try {
            data.put("local_ip", getLocalIp());
            data.put("android_id", getAndroidId());
            data.put("OsName",getOsName());
            data.put("package_info",getPackageInfo());
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
        Log.i("WebSocketService:controlHook","on:"+on.toString());
        if(on) {
            Boolean start = file.mkdirs();
            Log.i("WebSocketService:controlHook",package_name +" start:"+start.toString());
            try {
                Os.chmod("/data/system/xsettings/mydemo/persisit/"+package_name, 0777);
                Os.chmod(file.getAbsolutePath(), 0777);
                Log.i("WebSocketService:controlHook","chmod:"+file.getAbsolutePath()+" success");
            }catch (ErrnoException ex){
                Log.i("WebSocketService:controlHook","chmod:"+file.getAbsolutePath()+" error:"+ex.toString());
            }
        }else{
            Boolean stop = file.delete();
            Log.i("WebSocketService:controlHook",package_name +" stop:"+stop.toString());
        }
        closeOtherApp(package_name);
        openOtherApp(package_name);
    }

    private String getJsConfig(String package_name){
        try {
            File file = new File("/data/system/xsettings/mydemo/jscfg/" + package_name + "/config.js");
            FileInputStream config_js = new FileInputStream(file);
            byte[] buffer = new byte[config_js.available()];
            config_js.read(buffer);
            String result = new String(buffer);
            return result;
        } catch (FileNotFoundException ex){
            Log.i("WebSocketService:getJsConfig","/data/system/xsettings/mydemo/jscfg/" + package_name + "/config.js not Found");
            return "";
        } catch (IOException ex){
            Log.i("WebSocketService:getJsConfig",package_name + ":" + ex.toString());
            return "";
        }
    }

    private boolean updateJsConfig(String package_name,String text){
        File config_dir = new File("/data/system/xsettings/mydemo/jscfg/" + package_name);
        if(!config_dir.exists()){
            if(!config_dir.mkdirs()){
                Log.i("WebSocketService:updateJsConfig",package_name + " jsdir 创建失败");
                return  false;
            }
        }
        try {
            Os.chmod(config_dir.getAbsolutePath(), 0777);
            Log.i("WebSocketService:updateJsConfig","chmod:"+config_dir.getAbsolutePath()+" success");
        }catch (ErrnoException ex){
            Log.i("WebSocketService:updateJsConfig","chmod:"+config_dir.getAbsolutePath()+" error:"+ex.toString());
        }
        Log.i("WebSocketService:updateJsConfig",package_name + " jsdir create success");
        try {
            File config_js = new File("/data/system/xsettings/mydemo/jscfg/" + package_name + "/config.js");
            FileOutputStream file = new FileOutputStream(config_js);
            file.write(text.getBytes());
            file.flush();
            file.close();
            Os.chmod(config_js.getAbsolutePath(), 0777);
            Log.i("WebSocketService:updateJsConfig","创建文件成功");
            closeOtherApp(package_name);
            openOtherApp(package_name);
            return true;
        }catch (FileNotFoundException ex){
            Log.i("WebSocketService:updateJsConfig","创建文件失败:"+ex.toString());
            return false;
        }catch (IOException ex){
            Log.i("WebSocketService:updateJsConfig","创建文件失败:"+ex.toString());
            return false;
        }catch (ErrnoException ex){
            Log.i("WebSocketService:updateJsConfig","创建文件失败:"+ex.toString());
            return false;
        }

    }

    private boolean deleteJsConfig(String package_name){
        File config_dir = new File("/data/system/xsettings/mydemo/jscfg/" + package_name + "/config.js");
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
                Log.i("WebSocketService:openOtherApp","open "+packageName+" success");
            }
        }
    }

    public void closeOtherApp(String packageName){
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        am.killBackgroundProcesses(packageName);
        Log.i("WebSocketService:closeOtherApp","close "+packageName+" success");
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
            Log.i("WebSocketService:getPackageInfo",ex.toString());
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
            Log.i("WebSocketService:getPackageInfo",ex.toString());
        }

        //获取所有app的版本信息
        PackageManager pckMan = getPackageManager();
        ArrayList<HashMap<String, Object>> items = new ArrayList<HashMap<String, Object>>();
        List<PackageInfo> packageInfo = pckMan.getInstalledPackages(0);
        for (PackageInfo pInfo : packageInfo) {
            HashMap<String, Object> item = new HashMap<String, Object>();
            item.put("packageName", pInfo.packageName);
            item.put("versionName", pInfo.versionName);
            items.add(item);
        }
        try {
            data.put("appinfo",items);
        }catch (JSONException ex){
            Log.i("WebSocketService:getPackageInfo",ex.toString());
        }

        Log.i("WebSocketService:getPackageInfo",data.toString());
        return  data.toString();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return wbbinder;
    }

    @Override
    public void onDestroy() {
        Log.i("WebSocketService:onDestroy","onDestroy");
        super.onDestroy();
    }
}
