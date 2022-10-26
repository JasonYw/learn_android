package com.example.mydemoservice;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

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
    Intent s_intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        s_intent = new Intent(MainActivity.this,ScheduleService.class);

        //开启控制服务
        startForegroundService(s_intent);

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
            startActivityForResult(info_intent,1);
        }
        if(Boolean.valueOf(connect_host) && Boolean.valueOf(connect_port)){
            wb_intent.putExtra("connect_host",connect_host);
            wb_intent.putExtra("connect_port",connect_port);
            startForegroundService(wb_intent);
        }
    }

    @Override
    protected void onStart() {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    class MainReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean is_connected = intent.getBooleanExtra("is_connect",false);
            if(is_connected){
                if(is_remember.isChecked()){
                    editor.putString("connect_host",host.getText().toString());
                    editor.putString("connect_port",port.getText().toString());
                    editor.commit();
                    Log.i("MainActivity:onReceive","edit commit");
                }
                startActivityForResult(info_intent,1);
            }else{
                stopService(wb_intent);
                Log.i("MainActivity:onReceive","stopService");
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


}