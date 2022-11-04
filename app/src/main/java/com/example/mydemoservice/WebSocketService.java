package com.example.mydemoservice;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.alibaba.fastjson.JSON;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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
    String url;
    boolean token;
    WebSocketClientUtil client;
    Intent broadcast_intent = new Intent();
    Thread connect_thread;
    Thread reconnect_thread;
    Utils utils;

    private WebSocketServiceBinder wbbinder = new WebSocketServiceBinder();

    class WebSocketServiceBinder extends Binder{
        public WebSocketService getService(){
            return  WebSocketService.this;
        }
    }

    public class ReconnectThread implements Runnable {
        public void run() {
            Log.i("WebSocketService:reconnect_thread", "retry:" + host + ":" + port);
            broadcast_intent.putExtra("WebSocketServiceState", "reconnect:" + url);
            sendBroadcast(broadcast_intent);
            long startTime;
            startTime = System.currentTimeMillis();
            authWebsocket("reconnect");
            while (true) {
                if (client == null){
                    Log.i("WebSocketService:reconnect_thread", "client == null");
                    initClient();
                    break;
                }
                if(client != null) {
                    if (client.isClosed()) {
                        try {
                            long nowtime = System.currentTimeMillis();
                            if ((nowtime - startTime) > 86400000) {
                                authWebsocket("reconnect");
                                startTime = System.currentTimeMillis();
                            }
                            client.reconnectBlocking();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        broadcast_intent.putExtra("WebSocketServiceState", "reconnect success");
                        sendBroadcast(broadcast_intent);
                        break;
                    }
                }
            }
            utils.openAllApp();
            Log.i("WebSocketService:reconnect_thread", "reconnect_thread finish");
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
        username = intent.getStringExtra("connect_username");
        password = intent.getStringExtra("connect_password");
        token = false;
        utils = new Utils(host,port);
        Log.i("WebSocketService:onStartCommand",host);
        Log.i("WebSocketService:onStartCommand",port);
        Log.i("WebSocketService:onStartCommand",username);
        Log.i("WebSocketService:onStartCommand",password);
        broadcast_intent.setAction("WebSocketServiceReceiver");
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
        TimerTask HeartBeatThread =new TimerTask(){
            @Override
            public void run() {
                if(client != null && client.isOpen()){
                    sendDeviceData();
                }
            }
        };
        Timer timer = new Timer();
        Log.i("WebSocketService:onStartCommand","start HeartBeatThread");
        timer.schedule(HeartBeatThread,1,120*1000);
        return super.onStartCommand(intent, flags, startId);
    }

    public void initClient() {
        url = "ws://" + host + ":" + port + "/ws/app?username=" + username;
        Log.i("WebSocketService:onStartCommand",url);
        URI uri = URI.create(url);
        client = new WebSocketClientUtil(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i("WebSocketService:onOpen","isOpen:" + String.valueOf(client.isOpen()));
                if(client.isOpen()){
                    utils.copyAndUpdateAllConfig();
                    if (utils.checkUpdate()){
                        broadcast_intent.putExtra("WebSocketServiceState","check_updating");
                        sendBroadcast(broadcast_intent);
                        updateApk();
                    }
                    sendDeviceData();
                }
                broadcast_intent.putExtra("WebSocketServiceState","connected");
                sendBroadcast(broadcast_intent);
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
                    broadcast_intent.putExtra("WebSocketServiceState","connect_error");
                    sendBroadcast(broadcast_intent);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("WebSocketClientUtil:onClose","code:"+String.valueOf(code)+" reason:"+reason);
                utils.cloaseAllApp();
                if(code != 1000){
                    if(reconnect_thread == null || !reconnect_thread.isAlive()){
                        Log.i("WebSocketService:onClose:reconnect_thread:isalive:","false");
                        ReconnectThread mr=new ReconnectThread();
                        reconnect_thread=new Thread(mr);
                        reconnect_thread.start();
                    }else{
                        Log.i("WebSocketService:onClose:reconnect_thread:isalive:","true");
                    }
                }else{
                    broadcast_intent.putExtra("WebSocketServiceState","connect_close");
                    sendBroadcast(broadcast_intent);
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
                            utils.closeApp(package_name);
                            utils.controlHook(package_name,true);
                            utils.openApp(package_name);
                            break;
                        case "stop_hook":
                            utils.closeApp(package_name);
                            utils.controlHook(package_name,false);
                            break;
                        case "update_config":
                            utils.closeApp(package_name);
                            utils.updateJsConfig(package_name,data);
                            utils.copyJsConfig(package_name);
                            break;
                        case "on_close":
                            broadcast_intent.putExtra("WebSocketServiceState","connect_close");
                            sendBroadcast(broadcast_intent);
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
            data.put("local_ip", utils.getLocalIp());
            data.put("android_id", utils.getAndroidId());
            data.put("os_version", utils.getOsVersion());
            data.put("sdk_version", utils.getSdkVersion());
            data.put("brand_info", utils.getBrandInfo());
            data.put("app_version", utils.getAppVersion());
            data.put("package_info", utils.getPackageInfo());
            data.put("memory_info", utils.getMemoryInfo());
            client.send(data.toString());
        } catch (JSONException ex) {
            client.send(data.toString());
        }
    }

    public void authWebsocket(String for_which){
        switch (for_which){
            case "connect":
                broadcast_intent.putExtra("WebSocketServiceState","auth_fail");
                break;
            case "reconnect":
                broadcast_intent.putExtra("WebSocketServiceState","refresh_token_fail");
                break;
        }
        String url = "http://" + host + ":" + port +"/appLogin?username=" + username + "&password=" + password;
        Log.i("WebSocketService:authWebsocket","url:"+url);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).get().build();
        Call call = client.newCall(request);
        try{
            Response response = call.execute();
            String str_response = response.body().string();
            com.alibaba.fastjson.JSONObject data = JSON.parseObject(str_response);
            Log.i("WebSocketService:authWebsocket","response.body():"+ data.toString());
            if(data.getString("status").equals("200")){
                token = true;
            }else{
                token = false;
                sendBroadcast(broadcast_intent);
            }
            Log.i("WebSocketService:authWebsocket","token:"+ token);

        }catch (IOException ex){
            token = false;
            sendBroadcast(broadcast_intent);
            Log.i("WebSocketService:authWebsocket",ex.toString());
        }
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



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return wbbinder;
    }

}
