package com.example.mydemoservice;


import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.text.format.Formatter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.example.mydemoservice.control.wechat.OfficialAccountsService;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class WebSocketService extends Service {

    String host;
    String port;
    String username;
    String password;
    boolean token;
    WebSocketClientUtil client;
    Intent rc_intent = new Intent();
    Thread connect_thread;
    WebSocketService.MainReceiver m_receiver;
    private WebSocketServiceBinder wbbinder = new WebSocketServiceBinder();

    class WebSocketServiceBinder extends Binder{
        public WebSocketService getService(){
            return  WebSocketService.this;
        }
    }


    public class ReconnectThread implements Runnable {
        public void run() {
            Log.i("WebSocketService:reconnect_thread","reconnect_thread start");
            Log.i("WebSocketService:reconnect_thread", "retry:" + host + ":" + port);
            rc_intent.putExtra("WebSocketServiceState","reconnect");
            sendBroadcast(rc_intent);
            long startTime;
            startTime = System.currentTimeMillis();
            authWebsocket("reconnect");
            while (true) {
                if (client != null && client.isClosed()) {
                    try {
                        long nowtime = System.currentTimeMillis();
                        if((nowtime - startTime) > 86400000){
                            authWebsocket("reconnect");
                            startTime = System.currentTimeMillis();
                        }
                        client.reconnectBlocking();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else{
                    rc_intent.putExtra("WebSocketServiceState","reconnect success");
                    sendBroadcast(rc_intent);
                    break;
                }
            }
            Log.i("WebSocketService:reconnect_thread","reconnect_thread finish");
        }
    }

    @Override
    public void onCreate() {
        Log.i("WebSocketService:onCreate","onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        m_receiver = new MainReceiver();
        host = intent.getStringExtra("connect_host");
        port = intent.getStringExtra("connect_port");
        username = intent.getStringExtra("connect_username");
        password = intent.getStringExtra("connect_password");
        token = false;
        Log.i("WebSocketService:onStartCommand",host);
        Log.i("WebSocketService:onStartCommand",port);
        Log.i("WebSocketService:onStartCommand",username);
        Log.i("WebSocketService:onStartCommand",password);
        rc_intent.setAction("MRecevier");
        NotificationChannel channel = new NotificationChannel("1", "WebSocketService", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(getApplicationContext(), "1").build();
            startForeground(1001, notification);
        }

        connect_thread = new Thread() {
            @Override
            public void run() {
                Log.i("WebSocketService:connect_thread","connect_thread start");
                try {
                    authWebsocket("connect");
                    if(token){
                        client.connectBlocking();
                    }
                }catch (InterruptedException ex){
                    Log.i("WebSocketService:connect_thread",ex.toString());
                }
            }
        };

        initClient();
        return super.onStartCommand(intent, flags, startId);
    }

    public void initClient() {
        String url = "ws://" + host + ":" + port + "/ws/app?username=" + username;
        Log.i("WebSocketService:onStartCommand",url);
        URI uri = URI.create(url);
        client = new WebSocketClientUtil(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i("WebSocketService:onOpen","isOpen:" + String.valueOf(client.isOpen()));
                rc_intent.putExtra("WebSocketServiceState","connected");
                sendBroadcast(rc_intent);
                if(client.isOpen()){
                    if (checkUpdate()){
                        rc_intent.putExtra("WebSocketServiceState","check_updating");
                        sendBroadcast(rc_intent);
                        updateApk();
                    }
                    sendDeviceData();
                }
                super.onOpen(handshakedata);
            }

            @Override
            public void onError(Exception ex) {
                Log.i("WebSocketService:onError",ex.toString());
                if(ex.toString().indexOf("Connection refused") != -1){
                    if(connect_thread.isAlive()){
                        connect_thread.interrupt();
                    }
                }else{
                    rc_intent.putExtra("WebSocketServiceState","connect_error");
                    sendBroadcast(rc_intent);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("WebSocketClientUtil:onClose","code:"+String.valueOf(code)+"reason:"+reason);
                cloaseAllApp();
                if(code != 1000){
                    ReconnectThread mr=new ReconnectThread();
                    Thread th=new Thread(mr);
                    th.start();
                }else{
                    rc_intent.putExtra("WebSocketServiceState","connect_close");
                    sendBroadcast(rc_intent);
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
                        case "start_hook":
                            closeApp(package_name);
                            controlHook(package_name,true);
                            openApp(package_name);
                            break;
                        case "stop_hook":
                            closeApp(package_name);
                            controlHook(package_name,false);
                            break;
                        case "update_config":
                            closeApp(package_name);
                            updateJsConfig(package_name,data);
                            copyJsConfig(package_name);
                            openApp(package_name);
                            break;
                        case "on_close":
                            rc_intent.putExtra("WebSocketServiceState","connect_close");
                            sendBroadcast(rc_intent);
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
        client.setConnectionLostTimeout(500);

        connect_thread.interrupt();
        connect_thread.start();
    }

    private void sendDeviceData(){
        JSONObject data = new JSONObject();
        try {
            data.put("local_ip", getLocalIp());
            data.put("android_id", getAndroidId());
            data.put("os_version",getOsVersion());
            data.put("sdk_verison",getSdkVersion());
            data.put("brand_info",getBrandInfo());
            data.put("app_version",getAppVersion());
            data.put("package_info",getPackageInfo());
            data.put("memory_info",getMemoryInfo());
            client.send(data.toString());
        } catch (JSONException ex) {
            client.send(data.toString());
        }
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
    }

    private boolean updateJsConfig(String package_name,String text){
        String basedir = "/data/system/xsettings/mydemo/jscfg/";
        File config_dir = new File(basedir + package_name);
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

        byte[] b_context = Base64.getDecoder().decode(text);
        for(int i=0;i<b_context.length;i++){
            if(b_context[i] < 0 ){
                b_context[i] += 256;
            }
        }
        try {
            File config_js = new File(basedir + package_name + "/base_config.js");
            FileOutputStream file = new FileOutputStream(config_js);
            file.write(b_context);
            file.flush();
            file.close();
            Os.chmod(config_js.getAbsolutePath(), 0777);
            Log.i("WebSocketService:updateJsConfig","创建文件成功");
            return true;
        }catch (IOException|ErrnoException ex){
            Log.i("WebSocketService:updateJsConfig","创建文件失败:"+ex.toString());
            return false;
        }
    }

    private boolean copyJsConfig(String package_name){
        String basedir = "/data/system/xsettings/mydemo/jscfg/";
        try {
            Log.i("WebSocketService:copyJsConfig",package_name);
            //读取模板脚本
            File base_file = new File(basedir + package_name + "/base_config.js");
            FileInputStream base_config_js = new FileInputStream(base_file);
            byte[] buffer = new byte[base_config_js.available()];
            base_config_js.read(buffer);
            String result = new String(buffer).replace("{host}",host).replace("{port}",port).replace("{android_id}",getAndroidId()).replace("{package_name}",package_name);
            //copy
            File config_js = new File(basedir + package_name + "/config.js");
            config_js.setWritable(true);
            config_js.setExecutable(true);
            config_js.setReadable(true);
            FileOutputStream file = new FileOutputStream(config_js);
            file.write(result.getBytes());
            file.flush();
            file.close();
            Os.chmod(config_js.getAbsolutePath(), 0777);
            return true;
        } catch (ErrnoException|IOException ex){
            Log.i("WebSocketService:copyJsConfig",package_name + ":" + ex.toString());
        }
        return false;
    }

    private void cloaseAllApp(){
        Log.i("WebSocketService:cloaseAllApp","start");
        try{
            JSONArray pakcage_info = getPackageInfo().getJSONArray("package_info");
            for(int i =0;i< pakcage_info.length();i++){
                JSONObject json_ = pakcage_info.getJSONObject(i);
                if(json_.getBoolean("is_start_hook")){
                    String package_name = json_.getString("package_name");
                    closeApp(package_name);
                }
            }
        }catch (JSONException ex){
            Log.i("WebSocketService:cloaseAllApp",ex.toString());
        }
    }

    private void openAllApp(){
        Log.i("WebSocketService:openAllApp","start");
        try{
            JSONArray pakcage_info = getPackageInfo().getJSONArray("package_info");
            for(int i =0;i< pakcage_info.length();i++){
                JSONObject json_ = pakcage_info.getJSONObject(i);
                if(json_.getBoolean("is_start_hook")){
                    String package_name = json_.getString("package_name");
                    openApp(package_name);
                }
            }
        }catch (JSONException ex){
            Log.i("WebSocketService:openAllApp",ex.toString());
        }
    }

    private JSONObject getPackageInfo(){
        JSONObject data = new JSONObject();
        JSONArray data_array = new JSONArray();
        List<PackageInfo> pakcage_info = getPackageManager().getInstalledPackages(0);
        for(int i=0;i<pakcage_info.size();i++){
            if(!isSystemPakcage(pakcage_info.get(i).packageName)) {
                JSONObject app_info = new JSONObject();
                try {
                    app_info.put("package_name", pakcage_info.get(i).packageName);
                    app_info.put("is_system_package", isSystemPakcage(pakcage_info.get(i).packageName));
                    app_info.put("app_name", getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pakcage_info.get(i).packageName, PackageManager.GET_META_DATA)).toString());
                    app_info.put("version", pakcage_info.get(i).versionName);
                    app_info.put("is_start_hook", isStartHook(pakcage_info.get(i).packageName));
                    app_info.put("has_js_config", isHasJSConfig(pakcage_info.get(i).packageName));
                    data_array.put(app_info);
                } catch (JSONException ex) {
                    continue;
                } catch (PackageManager.NameNotFoundException ex) {
                    continue;
                }
                Log.i("getPackageInfo", app_info.toString());
            }
        }
        try {
            data.put("package_info", data_array);
        } catch (JSONException ex){}
        return  data;
    }

    public void updateApk(){

        String uri = "http://" + host + ":" + port +"/file";
        String path = "/storage/emulated/0/Download/com.example.mydemoservice_update.apk";
        try {
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            //获取到文件的大小
            InputStream is = conn.getInputStream();
            File file = new File(path);
            FileOutputStream fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            bis.close();
            is.close();
            Uri contentUri = FileProvider.getUriForFile(WebSocketService.this, "com.example.mydemoservice", file);
            Intent intent = new Intent(Intent.ACTION_VIEW,contentUri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            startActivity(intent);
        }catch (IOException ex){
            Log.i("update_apk",ex.toString());
        };

    }

    public double getNewVersion(){
        return  0.0;
    }

    private String getAndroidId(){
        return Settings.Secure.getString(this.getContentResolver(),Settings.Secure.ANDROID_ID);
    }

    private String getLocalIp () {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
        return ipAddress;
    }

    private String getOsVersion(){
        return Build.VERSION.RELEASE;
    }

    private String getSdkVersion(){
        return Integer.valueOf(android.os.Build.VERSION.SDK).toString();
    }

    private String getBrandInfo(){
        return Build.DEVICE;
    }

    public double getAppVersion(){
        try {
            PackageManager packageManager = getPackageManager();
            //getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
            String versio_name = packInfo.versionName;
            Log.i("WebSocketService：getAppVersion", packInfo.versionName);
            return Double.valueOf(versio_name);
        }catch (PackageManager.NameNotFoundException ex){
            Log.i("WebSocketService：getAppVersion", ex.toString());
            return 1.0;
        }
    }

    private JSONObject getMemoryInfo(){
        JSONObject data = new JSONObject();
        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        try{
            data.put("avail_mem",memInfo.availMem / 1024/1024);
            data.put("total_mem",memInfo.totalMem/1024/1024);
        }catch (JSONException ex){
            Log.i("WebSocketService:getMemoryInfo",ex.toString());
        }

        return data;
    }

    private boolean isStartHook(String package_name) {
        String path = "/data/system/xsettings/mydemo/persisit/"+package_name+"/persist_mydemo";
        File file = new File(path);
        return file.exists();
    }

    private boolean isHasJSConfig(String package_name) {
        String path = "/data/system/xsettings/mydemo/jscfg/"+package_name+"/base_config.js";
        File file = new File(path);
        return file.exists();
    }

    private boolean isSystemPakcage(String package_name){
        try {
            PackageInfo a = getPackageManager().getPackageInfo(package_name,0);
            if ((a.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0){
                return false;
            }
        }catch (PackageManager.NameNotFoundException ex){
            Log.i("WebSocketService:isSystemPakcage",ex.toString());
        }
        return true;
    }

    public void authWebsocket(String for_which){
        switch (for_which){
            case "connect":
                rc_intent.putExtra("WebSocketServiceState","auth_fail");
                break;
            case "reconnect":
                rc_intent.putExtra("WebSocketServiceState","refresh_token_fail");
                break;
        }
        String url = "http://" + host + ":" + port +"/ws/auth?username=" + username + "&password=" + password;
        Log.i("WebSocketService:authWebsocket","url:"+url);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).get().build();
        Call call = client.newCall(request);
        try{
            Response response = call.execute();
            String str_response = response.body().string();
            Log.i("WebSocketService:authWebsocket","response.body():"+ str_response);
            if(str_response == ""){
                token = false;
                sendBroadcast(rc_intent);
            }else{
                token = true;
            }
            Log.i("WebSocketService:authWebsocket","token:"+ token);

        }catch (IOException ex){
            token = false;
            sendBroadcast(rc_intent);
            Log.i("WebSocketService:authWebsocket",ex.toString());

        }
    }

    public boolean checkUpdate() {
        if (getNewVersion() > getAppVersion()) {
            return true;
        }else{
            return false;
        }

    }

    public void openApp(String package_name){
        Intent intent = getPackageManager().getLaunchIntentForPackage(package_name);
        if (intent != null) {
            if (!getPackageName().equals(package_name)) {
                startActivity(intent);
                Log.i("ControlBase:openApp","open "+package_name+" success");
            }
        }
    }

    public void closeApp(String package_name){
        try {
            ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(am, package_name);
            Log.i("ControlBase:closeApp","close "+package_name+" success");
        } catch (IllegalArgumentException|ReflectiveOperationException ex){
            Log.i("ControlBase:closeApp","close "+package_name+" fail:"+ex.toString());
        }
    }


    class MainReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String app_status = intent.getStringExtra("app_status");
            if(client != null && client.isOpen()){
                try{
                    JSONObject data = new JSONObject();
                    data.put("app_status",app_status);
                    client.send(data.toString());
                }catch (JSONException ex){
                    Log.i("WebSocketService.MainReceiver",ex.toString());
                }
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return wbbinder;
    }

}
