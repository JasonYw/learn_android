package com.example.mydemoservice;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {

    Button disconnect;
    Intent intent;
    Intent wbintent;
    private WebSocketService.WebSocketServiceBinder wbbinder;

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            wbbinder = (WebSocketService.WebSocketServiceBinder)iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i("InfoActivity","onServiceDisconnected");
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        wbintent = new Intent(InfoActivity.this,WebSocketService.class);
        bindService(wbintent,conn, BIND_AUTO_CREATE);
        disconnect = findViewById(R.id.disconnect);
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wbbinder.getService().client.close();
                unbindService(conn);
                setResult(2,intent);
                finish();
            }
        });
    }



    public void onBackPressed(){
        Toast.makeText(InfoActivity.this,"please disconnect to back login activity",Toast.LENGTH_SHORT).show();
    }
}
