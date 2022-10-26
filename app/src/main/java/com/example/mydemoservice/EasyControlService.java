package com.example.mydemoservice;
import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EasyControlService extends AccessibilityService {

    private String package_name;
    private Intent rc_intent = new Intent();

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("EasyControlService:"+package_name,"onStartCommand");
        NotificationChannel channel = new NotificationChannel("1", "EasyControlService", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(getApplicationContext(), "1").build();
            startForeground(1001, notification);
        }
        package_name = intent.getStringExtra("package_name");
        Log.i("EasyControlService:onStartCommand",package_name+":start");
        ArrayList<String> data = readFile(package_name);
        Log.i("EasyControlService:data",data.toString());
        for(int i=0;i<data.size();i++){
            actionBytext(data.get(i));
        }
        rc_intent.setAction("SRecevier");
        rc_intent.putExtra(package_name + "_is_finish",true);
        sendBroadcast(rc_intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i("EasyControlService:"+package_name,"onDestroy");
        super.onDestroy();
    }

    private void openOtherApp(String packageName){
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            if (!getPackageName().equals(packageName)) {
                startActivity(intent);
                Log.i("EasyControlService:openOtherApp","open "+packageName+" success");
            }
        }
    }

    public void closeOtherApp(String packageName){
        try {
            ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(am, packageName);
            Log.i("EasyControlService:closeOtherApp","close "+packageName+" success");
        } catch (IllegalArgumentException|ReflectiveOperationException ex){
            Log.i("EasyControlService:closeOtherApp","close "+packageName+" fail:"+ex.toString());
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void onInterrupt() {
    }

    public ArrayList<String> readFile(String package_name){
        ArrayList<String> lines = new ArrayList<>();
        try {
            String path = "/data/system/xsettings/mydemo/eccfg/" + package_name + "/config.cfg";
            FileInputStream inputStream = new FileInputStream(path);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                lines.add(str);
            }
            inputStream.close();
            bufferedReader.close();
        }catch (IOException ex){
            Log.i("EasyControlService:readFile:"+package_name,ex.toString());
        }
        return  lines;
    }

    public void actionBytext(String text) {
        String[] action = text.split(" ");
        Log.i("EasyControlService:actionBytext",action.toString());
        try {
            switch (action[0]) {
                case "am_start":
                    openOtherApp(action[1]);
                    break;
                case "sleep":
                    Thread.sleep(Integer.valueOf(action[1]).intValue());
                    break;
                case "click_by_id":
                    clickViewByID(action[1]);
                    break;
                case "click_by_text":
                    clickViewByText(action[1]);
                    break;
                case "back":
                    performBackClick();
                    break;
                case "swipe_backward":
                    performScrollBackward();
                    break;
                case "swipe_forward":
                    performScrollForward();
                    break;
                default:
                    break;
            }
        }catch (InterruptedException ex){}
    }

    public void performViewClick(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return;
        }
        while (nodeInfo != null) {
            if (nodeInfo.isClickable()) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
            nodeInfo = nodeInfo.getParent();
        }
    }

    /**
     * 模拟返回操作
     */
    public void performBackClick() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    /**
     * 模拟下滑操作
     */
    public void performScrollBackward() {
        performGlobalAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    /**
     * 模拟上滑操作
     */
    public void performScrollForward() {
        performGlobalAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);

    }

    /**
     * 点击对应文本的一个view，前提是这个view能够点击，即 clickable == true，
     *
     * @param text 要查找的文本
     */
    public void clickViewByText(String text) {
        AccessibilityNodeInfo accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {
            return;
        }
        List<AccessibilityNodeInfo> nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText(text);
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                if (nodeInfo != null) {
                    performViewClick(nodeInfo);
                    break;
                }
            }
        }
    }

    /**
     * 点击对应id的一个view，前提是这个view能够点击，即 clickable == true，
     *
     * @param id 要查找的id
     */
    public void clickViewByID(String id) {
        AccessibilityNodeInfo accessibilityNodeInfo = getRootInActiveWindow();
        if (accessibilityNodeInfo == null) {
            return;
        }
        List<AccessibilityNodeInfo> nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(id);
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
                if (nodeInfo != null) {
                    performViewClick(nodeInfo);
                    break;
                }
            }
        }
    }

}
