package com.example.mydemoservice;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
    Intent wbintent;
    InfoActivity.MainReceiver m_receiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        Log.i("InfoActivity","onCreate");
        m_receiver = new InfoActivity.MainReceiver();
        wbintent = new Intent(InfoActivity.this,WebSocketService.class);
        disconnect = findViewById(R.id.disconnect);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("InfoActivity","onStart");
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("InfoActivity:onClick","disconnect");
                stopService(wbintent);
                startActivity(new Intent(InfoActivity.this,MainActivity.class));
            }
        });
    }

    class MainReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("InfoActivity:onReceive","get msg");
            boolean is_connected = intent.getBooleanExtra("is_connect", false);
            if (!is_connected) {
                stopService(wbintent);
                startActivity(new Intent(InfoActivity.this,MainActivity.class));
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
