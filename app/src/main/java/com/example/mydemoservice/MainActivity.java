package com.example.mydemoservice;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

public class MainActivity extends AppCompatActivity {

    private Button btn2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);

        final EditText host = findViewById(R.id.host); //变量不可以在被更改
        final EditText port = findViewById(R.id.port);
        final CheckBox isremember = findViewById(R.id.remember);
        btn2 = findViewById(R.id.btn2); //btn2 与 button id为btn2属性关联了

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("MainActivity","内名内部类按钮被点击了");
                Log.i("MainActivity",host.getText().toString());
                Log.i("MainActivity",port.getText().toString());
                Log.i("MainActivity", String.valueOf(isremember.isChecked()));
            }
        });
    }
}