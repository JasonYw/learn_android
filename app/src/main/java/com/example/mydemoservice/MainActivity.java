package com.example.mydemoservice;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;


import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private Button connect;
    private  SharedPreferences sp;
    private  SharedPreferences.Editor editor;
    private  EditText host;
    private  EditText port;
    private  CheckBox is_remember;
    MainReceiver m_receiver;
    String connect_host;
    String connect_port;
    Intent wb_intent;
    Intent info_intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("MainActivity","onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //获取权限
        String[] PERMISSIONS = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        int PERMISSION_CODE = 123;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            if(checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,android.os.Process.myPid(),android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(PERMISSIONS,PERMISSION_CODE);
            }
        }


        //初始化service intent 以及 Reciver 以及 下一个Activity intent
        m_receiver = new MainReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("MRecevier");
        registerReceiver(m_receiver,filter);
        info_intent = new Intent(MainActivity.this, InfoActivity.class);
        wb_intent =  new Intent(MainActivity.this, WebSocketService.class);


        //初始化页面数据 以及存储
        sp = getSharedPreferences("uri",MODE_PRIVATE);
        editor = sp.edit();
        is_remember = findViewById(R.id.remember);
        host =findViewById(R.id.host);
        port =findViewById(R.id.port);
        connect = findViewById(R.id.connect);

        //从存储中获取数据
        connect_host =  sp.getString("connect_host",null);
        connect_port =  sp.getString("connect_port",null);
        if (isServiceRunning(getPackageName()+".WebSocketService")){
            Log.i("MainActivity:onCreate","startActivity");
            startActivity(info_intent);
        }
    }

    @Override
    protected void onStart() {
        Log.i("MainActivity","onStart");

        super.onStart();
        //初始化init
        wb_intent.putExtra("connect_host",connect_host);
        wb_intent.putExtra("connect_port",connect_port);

        //初始化表单数据
        if(connect_host != null && connect_host != null) {
            host.setText(connect_host);
            port.setText(connect_port);
            is_remember.setChecked(true);
        }

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect.setEnabled(false);
                connect.setText("connecting...");
                stopService(wb_intent);
                connect_host = host.getText().toString();
                connect_port = port.getText().toString();
                Log.i("MainActivity:connect_host",connect_host);
                Log.i("MainActivity:connect_port",connect_port);
                wb_intent.putExtra("connect_host",connect_host);
                wb_intent.putExtra("connect_port",connect_port);
                startForegroundService(wb_intent);
            }
        });
    }

    class MainReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String WebSocketServiceState = intent.getStringExtra("WebSocketServiceState");
            Log.i("MainActivity:onReceive","WebSocketServiceState:"+WebSocketServiceState);
            switch (WebSocketServiceState){
                case "connected":
                    if(is_remember.isChecked()){
                        editor.putString("connect_host",host.getText().toString());
                        editor.putString("connect_port",port.getText().toString());
                        editor.commit();
                        Log.i("MainActivity:onReceive","edit commit");
                        connect.setText("checking update");
                        boolean check_update = intent.getBooleanExtra("check_update",false);
                        if(check_update){
                            connect.setText("updating");
                        }
                    }
                    connect.setEnabled(true);
                    connect.setText("connect");
                    startActivity(info_intent);
                    break;
                case "reconnect":
                    connect.setEnabled(false);
                    connect.setText(WebSocketServiceState);
                    break;
                case "reconnect_finish":
                    connect.setEnabled(true);
                    connect.setText("connect");
                    startActivity(info_intent);
                    break;
                case "connect_close":
                    connect.setEnabled(true);
                    connect.setText("connect");
                    break;
                default:
                    break;
            }

        }
    }

    private boolean isServiceRunning(String ServicePackageName) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ServicePackageName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    public void onBackPressed(){}



}