package com.example.mydemoservice;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.net.URI;


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
        setContentView(R.layout.activity_login);
        m_receiver = new MainReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("MRecevier");
        registerReceiver(m_receiver,filter);
        info_intent = new Intent(MainActivity.this, InfoActivity.class);
        wb_intent =  new Intent(MainActivity.this, WebSocketService.class);
        sp = getSharedPreferences("uri",MODE_PRIVATE);
        connect_host =  sp.getString("connect_host",null);
        connect_port =  sp.getString("connect_port",null);
        is_remember = findViewById(R.id.remember);
        connect = findViewById(R.id.connect);
        if(Boolean.valueOf(connect_host) && Boolean.valueOf(connect_port)){
            wb_intent.putExtra("connect_host",connect_host);
            wb_intent.putExtra("connect_port",connect_port);
            startForegroundService(wb_intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
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
            boolean is_connected = intent.getBooleanExtra("is_connected",false);
            Log.i("is_connected",String.valueOf(is_connected));
            if(is_connected){
                if(is_remember.isChecked()){
                    SharedPreferences.Editor edit = sp.edit();
                    edit.putString("connect_host",host.getText().toString());
                    edit.putString("connect_port",port.getText().toString());
                    edit.commit();
                    Toast.makeText(MainActivity.this,"remember connect info",Toast.LENGTH_SHORT).show();
                }
                startActivityForResult(info_intent,1);
            }else{
                stopService(wb_intent);
            }
        }
    }


}