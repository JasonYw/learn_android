package com.example.mydemoservice;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.text.format.Formatter;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {



    public String host;
    public String port;
    public Context mcontext = MyApplication.getContext();
    private static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";
    private static final String KEY = "0123456789012345";
    private static final String IV = "0123456789012345";


    Utils(String connect_host,String connect_port){
        host = connect_host;
        port = connect_port;
    }

    public boolean controlHook(String package_name, Boolean on){
        File file = new File("/data/system/xsettings/mydemo/persisit/"+package_name+"/persist_mydemo");
        Log.i("Utils:controlHook","on:"+on.toString());
        if(on) {
            Boolean start = file.mkdirs();
            Log.i("Utils:controlHook",package_name +" start:"+start.toString());
            try {
                Os.chmod("/data/system/xsettings/mydemo/persisit/"+package_name, 0777);
                Os.chmod(file.getAbsolutePath(), 0777);
                Log.i("Utils:controlHook","chmod:"+file.getAbsolutePath()+" success");
                return true;
            }catch (ErrnoException ex){
                Log.i("Utils:controlHook","chmod:"+file.getAbsolutePath()+" error:"+ex.toString());
                return false;
            }
        }else{
            Boolean stop = file.delete();
            Log.i("Utils:controlHook",package_name +" stop:"+stop.toString());
            return false;
        }
    }

    public boolean updateJsConfig(String package_name,String text){
        String basedir = "/data/system/xsettings/mydemo/jscfg/";
        File config_dir = new File(basedir + package_name);
        if(!config_dir.exists()){
            if(!config_dir.mkdirs()){
                Log.i("Utils:updateJsConfig",package_name + " jsdir 创建失败");
                return  false;
            }
        }
        try {
            Os.chmod(config_dir.getAbsolutePath(), 0777);
            Log.i("Utils:updateJsConfig","chmod:"+config_dir.getAbsolutePath()+" success");
        }catch (ErrnoException ex){
            Log.i("Utils:updateJsConfig","chmod:"+config_dir.getAbsolutePath()+" error:"+ex.toString());
        }
        Log.i("Utils:updateJsConfig",package_name + " jsdir create success");
        byte[]  b_context = Base64.getDecoder().decode(text);
        for(int i=0;i<b_context.length;i++){
            if(b_context[i] < 0 ){
                b_context[i] += 256;
            }
        }
        byte[]  context = Base64.getDecoder().decode(new String(b_context));
        for(int i=0;i<context.length;i++){
            if(context[i] < 0 ){
                context[i] += 256;
            }
        }
        try {
            File config_js = new File(basedir + package_name + "/base_config.js");
            FileOutputStream file = new FileOutputStream(config_js);
            file.write(context);
            file.flush();
            file.close();
            Os.chmod(config_js.getAbsolutePath(), 0777);
            Log.i("Utils:updateJsConfig","创建文件成功");
            return true;
        }catch (IOException |ErrnoException ex){
            Log.i("Utils:updateJsConfig","创建文件失败:"+ex.toString());
            return false;
        }
    }

    public boolean copyJsConfig(String package_name){
        String basedir = "/data/system/xsettings/mydemo/jscfg/";
        try {
            Log.i("Utils:copyJsConfig",package_name);
            //读取模板脚本
            File base_file = new File(basedir + package_name + "/base_config.js");
            FileInputStream base_config_js = new FileInputStream(base_file);
            byte[] buffer = new byte[base_config_js.available()];
            base_config_js.read(buffer);
            String result = new String(buffer).replace("{host}",host).replace("{port}",port).replace("{android_id}",getAndroidId()).replace("{package_name}",package_name);
            //copy
            File config_js = new File(basedir + package_name + "/config.js");
            config_js.setWritable(true);
            config_js.setExecutable(true);
            config_js.setReadable(true);
            FileOutputStream file = new FileOutputStream(config_js);
            file.write(result.getBytes());
            file.flush();
            file.close();
            Os.chmod(config_js.getAbsolutePath(), 0777);
            return true;
        } catch (ErrnoException|IOException ex){
            Log.i("Utils:copyJsConfig",package_name + ":" + ex.toString());
        }
        return false;
    }



    public void copyAndUpdateAllConfig(){
        Log.i("Utils:copyAndUpdateAllConfig","start");
        try{
            JSONArray pakcage_info = getPackageInfo();
            for(int i =0;i< pakcage_info.length();i++){
                JSONObject json_ = pakcage_info.getJSONObject(i);
                if(json_.getBoolean("has_js_config")){
                    String package_name = json_.getString("package_name");
                    copyJsConfig(package_name);
                }
            }
        }catch (JSONException ex){
            Log.i("Utils:copyAndUpdateAllConfig",ex.toString());
        }
    }



    public JSONArray getPackageInfo(){
        JSONArray data_array = new JSONArray();
        List<PackageInfo> pakcage_info = mcontext.getPackageManager().getInstalledPackages(0);
        for(int i=0;i<pakcage_info.size();i++){
            if(!isSystemPakcage(pakcage_info.get(i).packageName)) {
                JSONObject app_info = new JSONObject();
                try {
                    app_info.put("package_name", pakcage_info.get(i).packageName);
                    app_info.put("is_system_package", isSystemPakcage(pakcage_info.get(i).packageName));
                    app_info.put("app_name", mcontext.getPackageManager().getApplicationLabel(mcontext.getPackageManager().getApplicationInfo(pakcage_info.get(i).packageName, PackageManager.GET_META_DATA)).toString());
                    app_info.put("version", pakcage_info.get(i).versionName);
                    app_info.put("is_start_hook", isStartHook(pakcage_info.get(i).packageName));
                    app_info.put("has_js_config", isHasJSConfig(pakcage_info.get(i).packageName));
                    data_array.put(app_info);
                } catch (JSONException ex) {
                    continue;
                } catch (PackageManager.NameNotFoundException ex) {
                    continue;
                }
            }
        }
        return  data_array;
    }

    public void openApp(String package_name){
        Intent intent = mcontext.getPackageManager().getLaunchIntentForPackage(package_name);
        if (intent != null) {
            if (!mcontext.getPackageName().equals(package_name)) {
                mcontext.startActivity(intent);
                Log.i("Utils:openApp","open "+package_name+" success");
            }
        }
    }

    public void closeApp(String package_name){
        try {
            ActivityManager am = (ActivityManager) mcontext.getSystemService(ACTIVITY_SERVICE);
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(am, package_name);
            Log.i("Utils:closeApp","close "+package_name+" success");
        } catch (IllegalArgumentException|ReflectiveOperationException ex){
            Log.i("Utils:closeApp","close "+package_name+" fail:"+ex.toString());
        }
    }

    public void cloaseAllApp(){
        Log.i("Utils:cloaseAllApp","start");
        try{
            JSONArray pakcage_info = getPackageInfo();
            for(int i =0;i< pakcage_info.length();i++){
                JSONObject json_ = pakcage_info.getJSONObject(i);
                if(json_.getBoolean("is_start_hook")){
                    String package_name = json_.getString("package_name");
                    closeApp(package_name);
                }
            }
        }catch (JSONException ex){
            Log.i("Utils:cloaseAllApp",ex.toString());
        }
    }

    public void openAllApp(){
        Log.i("Utils:openAllApp","start");
        try{
            JSONArray pakcage_info = getPackageInfo();
            for(int i =0;i< pakcage_info.length();i++){
                JSONObject json_ = pakcage_info.getJSONObject(i);
                if(json_.getBoolean("is_start_hook") && json_.getBoolean("has_js_config")){
                    String package_name = json_.getString("package_name");
                    openApp(package_name);
                }
            }
        }catch (JSONException ex){
            Log.i("Utils:openAllApp",ex.toString());
        }
    }

    public boolean isStartHook(String package_name) {
        String path = "/data/system/xsettings/mydemo/persisit/"+package_name+"/persist_mydemo";
        File file = new File(path);
        return file.exists();
    }

    public boolean isHasJSConfig(String package_name) {
        String path = "/data/system/xsettings/mydemo/jscfg/"+package_name+"/base_config.js";
        File file = new File(path);
        return file.exists();
    }

    public boolean isSystemPakcage(String package_name){
        if(package_name.equals("com.example.mydemoservice")){
            return true;
        }
        try {
            PackageInfo a = mcontext.getPackageManager().getPackageInfo(package_name,0);
            if ((a.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0){
                return false;
            }
        }catch (PackageManager.NameNotFoundException ex){
            Log.i("Utils:isSystemPakcage",ex.toString());
        }
        return true;
    }

    public double getNewVersion(){
        return  0.0;
    }

    public String getAndroidId(){
        return Settings.Secure.getString(mcontext.getContentResolver(),Settings.Secure.ANDROID_ID);
    }

    public String getLocalIp () {
        WifiManager wifiManager = (WifiManager) mcontext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
        return ipAddress;
    }

    public String getOsVersion(){
        return Build.VERSION.RELEASE;
    }

    public String getSdkVersion(){
        return Integer.valueOf(android.os.Build.VERSION.SDK).toString();
    }

    public String getBrandInfo(){
        return Build.DEVICE;
    }

    public JSONObject getMemoryInfo(){
        JSONObject data = new JSONObject();
        ActivityManager activityManager = (ActivityManager) mcontext.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        try{
            data.put("avail_mem",memInfo.availMem / 1024/1024);
            data.put("total_mem",memInfo.totalMem/1024/1024);
        }catch (JSONException ex){
            Log.i("Utils:getMemoryInfo",ex.toString());
        }

        return data;
    }

    public double getAppVersion(){
        try {
            PackageManager packageManager = mcontext.getPackageManager();
            //getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packInfo = packageManager.getPackageInfo(mcontext.getPackageName(), 0);
            String versio_name = packInfo.versionName;
            return Double.valueOf(versio_name);
        }catch (PackageManager.NameNotFoundException ex){
            Log.i("Utils：getAppVersion", ex.toString());
            return 1.0;
        }
    }

    public boolean isServiceRunning(String ServicePackageName) {
        ActivityManager manager = (ActivityManager) mcontext.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ServicePackageName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean checkUpdate() {
        if (getNewVersion() > getAppVersion()) {
            return true;
        }else{
            return false;
        }

    }

    public String decryptAes(String strCipherText) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), AES);
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] clearText = cipher.doFinal(strCipherText.getBytes(StandardCharsets.UTF_8),0,16);
            return clearText.toString();
        } catch (Exception e) {
            Log.i("Utils：decryptAes",e.toString());
        }
        return null;
    }

}
