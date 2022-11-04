package com.example.mydemoservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {

    Button disconnect;
    Button connect;
    Intent wbintent;
    InfoActivity.WebSocketServiceReceiver wb_receiver;
    Utils utils;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Log.i("InfoActivity","onCreate");
        wb_receiver = new InfoActivity.WebSocketServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("WebSocketServiceReceiver");
        registerReceiver(wb_receiver,filter);
        wbintent = new Intent(InfoActivity.this,WebSocketService.class);

        //初始化button
        disconnect = findViewById(R.id.disconnect);
        connect = findViewById(R.id.connect);

        //初始化工具类
        utils = new Utils(null,null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("InfoActivity","onStart");
        if(!utils.isServiceRunning(getPackageName()+".WebSocketService")){
            startActivity(new Intent(InfoActivity.this,MainActivity.class));
        }
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("InfoActivity:onClick","disconnect");
                stopService(wbintent);
                startActivity(new Intent(InfoActivity.this,MainActivity.class));
            }
        });
    }

    class WebSocketServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String WebSocketServiceState = intent.getStringExtra("WebSocketServiceState");
            Log.i("InfoActivity:onReceive","WebSocketServiceState:"+WebSocketServiceState);
            switch (WebSocketServiceState){
                case "reconnect":
                    startActivity(new Intent(InfoActivity.this,MainActivity.class));
                    break;
                case "connect_close":
                    stopService(wbintent);
                    startActivity(new Intent(InfoActivity.this,MainActivity.class));
                    break;
                default:
                    break;
            }
        }
    }

    public void onBackPressed(){}

    @Override
    protected void onDestroy() {
        Log.i("InfoActivity","onDestroy");
        super.onDestroy();
    }
}
