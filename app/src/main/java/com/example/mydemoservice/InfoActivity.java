package com.example.mydemoservice;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {

    Button disconnect;
    Intent intent;
    WebSocketClientUtil client;
    WebSocketService.WebSocketClientBinder binder;
    WebSocketService webSocketService;



    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (WebSocketService.WebSocketClientBinder) service;
            webSocketService =binder.getService();
            client = webSocketService.client;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        disconnect = findViewById(R.id.disconnect);
        bindService();
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //断开web_socket
                //关闭页面
                setResult(2,intent);
                finish();
            }
        });
    }


    public void bindService() {
        intent = new Intent(InfoActivity.this,WebSocketService.class);
        bindService(intent,serviceConnection,BIND_AUTO_CREATE);
    }

    public void onBackPressed(){
        Toast.makeText(InfoActivity.this,"please disconnect to back login activity",Toast.LENGTH_SHORT).show();
    }
}
