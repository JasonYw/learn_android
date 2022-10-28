package com.example.mydemoservice.control.wechat;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.mydemoservice.ControlBase;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class OfficialAccountsService extends ControlBase {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onCreate() {
        super.package_name =  "com.tencent.mm";
        super.service_name = "wechat_official_account";
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void start_control() {
        super.start_control();
        super.closeApp();
        int interval =  sp.getInt(service_name+"interval",300);
        Timer timer = new Timer();
        TimerTask timerTask =new TimerTask(){
            @Override
            public void run() {
                control_intent.putExtra("app_status",service_name+":running");
                sendBroadcast(control_intent);
                OfficialAccountsService.super.openApp();
            }
        };
        timer.schedule(timerTask,1,interval*1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
