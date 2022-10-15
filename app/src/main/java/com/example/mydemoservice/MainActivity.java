package com.example.mydemoservice;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;


public class MainActivity extends AppCompatActivity {

    private Button connect;
    private MeyDemoWebSocketClient client;
    private  SharedPreferences sp;
    private Intent intent;
    private  EditText host;
    private  EditText port;
    private  CheckBox is_remember;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        host =findViewById(R.id.host);
        port =findViewById(R.id.port);
        is_remember = findViewById(R.id.remember);
        connect = findViewById(R.id.connect);
        sp = getSharedPreferences("uri",MODE_PRIVATE);
        intent = new Intent();
        intent.setClass(MainActivity.this,MyDemoShowLogActivity.class);
        String connect_host =  sp.getString("connect_host",null);
        String connect_port =  sp.getString("connect_port",null);
        host.setText(connect_host);
        port.setText(connect_port);

    }


    @Override
    protected void onStart() {
        super.onStart();
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                URI connect_uri = URI.create("ws://"+host.getText().toString()+":"+port.getText().toString());
                if(is_remember.isChecked()){

                    SharedPreferences.Editor edit = sp.edit();
                    edit.putString("connect_host",host.getText().toString());
                    edit.putString("connect_port",port.getText().toString());
                    edit.commit();
                    Toast.makeText(MainActivity.this,"remember connect info",Toast.LENGTH_SHORT).show();
                }
                startActivityForResult(intent,1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}