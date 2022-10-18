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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private Button connect;
    private  SharedPreferences sp;
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
        super.onCreate(savedInstanceState);
        Log.i("MainActivity","onCreate");
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

        getPackageInfo();

        //初始化service intent 以及 Reciver 以及 下一个Activity intent
        m_receiver = new MainReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("MRecevier");
        registerReceiver(m_receiver,filter);
        info_intent = new Intent(MainActivity.this, InfoActivity.class);
        wb_intent =  new Intent(MainActivity.this, WebSocketService.class);

        //初始化页面数据 以及存储
        sp = getSharedPreferences("uri",MODE_PRIVATE);
        is_remember = findViewById(R.id.remember);
        connect = findViewById(R.id.connect);
        connect_host =  sp.getString("connect_host",null);
        connect_port =  sp.getString("connect_port",null);
        if (isServiceRunning(getPackageName()+".WebSocketService")){
            Log.i("isServiceRunning","true");
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
        Log.i("MainActivity","onStart");
        wb_intent.putExtra("connect_host",connect_host);
        wb_intent.putExtra("connect_port",connect_port);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                host =findViewById(R.id.host);
                port =findViewById(R.id.port);
                connect_host = host.getText().toString();
                connect_port = port.getText().toString();
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
                Log.i("is_remember",String.valueOf(is_remember.isChecked()));
                if(is_remember.isChecked()){
                    SharedPreferences.Editor edit = sp.edit();
                    edit.putString("connect_host",host.getText().toString());
                    edit.putString("connect_port",port.getText().toString());
                    edit.commit();
                    Toast.makeText(MainActivity.this,"connect success",Toast.LENGTH_SHORT).show();
                }
                startActivityForResult(info_intent,1);
            }else{
                SharedPreferences.Editor edit = sp.edit();
                edit.putString("connect_host",null);
                edit.putString("connect_port",null);
                edit.commit();
                stopService(wb_intent);
                Toast.makeText(MainActivity.this,"check host or port",Toast.LENGTH_SHORT).show();
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
        ArrayList<HashMap<String, String>> items = new ArrayList();
        List<PackageInfo> packageInfo = pckMan.getInstalledPackages(0);
        for (PackageInfo pInfo : packageInfo) {
            HashMap<String, String> item = new HashMap<String, String>();
            item.put("packageName", pInfo.packageName);
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

    private boolean isServiceRunning(String ServicePackageName) {
        Log.i("ServicePackageName",ServicePackageName);
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ServicePackageName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}