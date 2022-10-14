package com.example.mydemoservice;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button connect;
    private Button btnalert;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText host = findViewById(R.id.host); //变量不可以在被更改
        final EditText port = findViewById(R.id.port);
        final CheckBox is_remember = findViewById(R.id.remember);
        connect = findViewById(R.id.btn2); //btn2 与 button id为btn2属性关联了
        btnalert = findViewById(R.id.btn_alert);

        //绑定listview
        //ListView lv_demo = findViewById(R.id.show_log_view);
        //定义数据源
        //List<String> names = new ArrayList<>();
        //names.add("a");
        //names.add("b");
        //names.add("c");
        //单元格布局
        //SimpleAdapter simpleAdapter = new SimpleAdapter();

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("MainActivity",host.getText().toString());
                Log.i("MainActivity",port.getText().toString());
                Log.i("MainActivity", String.valueOf(is_remember.isChecked()));
                Toast.makeText(MainActivity.this,"host or port not correct",Toast.LENGTH_SHORT).show();
            }
        });

        btnalert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("使用对话框").setMessage("设置信息").setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.i("MainActivity","ok");
                    }
                }).setNegativeButton("no", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.i("MainActivity","no");
                    }
                });
                AlertDialog ad = builder.create();
                ad.show();
            }
        });


    }
}