package com.example.mydemoservice;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;

public class MyDemoShowLogActivity extends AppCompatActivity {

    Button disconnect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        disconnect = findViewById(R.id.disconnect);
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //断开web_socket
                //关闭页面
                Intent intent = new Intent();
                setResult(2,intent);
                finish();
            }
        });
    }


    public void onBackPressed(){
        Toast.makeText(MyDemoShowLogActivity.this,"please disconnect to back login activity",Toast.LENGTH_SHORT).show();
    }
}
