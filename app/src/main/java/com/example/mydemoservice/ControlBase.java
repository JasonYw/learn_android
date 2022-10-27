package com.example.mydemoservice;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.lang.reflect.Method;
import java.util.List;

public class ControlBase extends AccessibilityService {


    public Intent control_intent = new Intent();
    public String class_name = this.getClass().getName();
    public String package_name;
    private String service_name;
    public SharedPreferences sp;
    public   SharedPreferences.Editor editor;


    @Override
    public void onCreate() {
        control_intent.setAction("control_intent");
        sp = getSharedPreferences("uri",MODE_PRIVATE);
        editor = sp.edit();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        closeApp();
        control_intent.putExtra("app_status",service_name+":destroy");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(class_name ,":onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    public void closeApp(){
        try {
            ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(am, package_name);
            Log.i("ControlBase:closeApp","close "+package_name+" success");
        } catch (IllegalArgumentException|ReflectiveOperationException ex){
            Log.i("ControlBase:closeApp","close "+package_name+" fail:"+ex.toString());
        }
    }

    public void openApp(){
        Intent intent = getPackageManager().getLaunchIntentForPackage(package_name);
        if (intent != null) {
            if (!getPackageName().equals(package_name)) {
                startActivity(intent);
                Log.i("ControlBase:openApp","open "+package_name+" success");
            }
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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {}

    @Override
    public void onInterrupt() {}


}
