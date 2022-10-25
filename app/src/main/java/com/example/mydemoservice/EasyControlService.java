package com.example.mydemoservice;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
public class EasyControlService extends Service {

    private ReentrantLock reentrant_lock = new ReentrantLock();
    private Thread build_thread;
    private Thread control_thread;
    private ArrayList<Thread> thread_array = new ArrayList<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i("EasyControlService:onStartCommand","onCreate");
        super.onCreate();
        build_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    String[] package_list = listPackage();
                    for(int i =0;i<package_list.length;i++){
                        String package_name = package_list[i];
                        if(!isInThreadArray(thread_array,package_name)){
                            Thread temp = new Thread(new Runnable() {
                                @Override
                                public void run() {

                                }
                            });
                            temp.setName(package_name);
                            thread_array.add(temp);
                        }
                    }

                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("EasyControlService:onStartCommand","onStartCommand");
        NotificationChannel channel = new NotificationChannel("1", "EasyControlService", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(getApplicationContext(), "1").build();
            startForeground(1001, notification);
        }
        control_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    for(int i =0;i<thread_array.size();i++){
                        if(!thread_array.get(i).isAlive()){
                            thread_array.get(i).start();
                        }
                    }
                }

            }
        });
        control_thread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private String[] listPackage(){
        List<PackageInfo> pakcage_info = getPackageManager().getInstalledPackages(0);
        String[] data = {};
        for(int i=0;i<pakcage_info.size();i++){
            String package_name = pakcage_info.get(i).packageName;
            if(!isSystemPakcage(package_name)) {
                if(isStartHook(package_name) && isHasEasyControlConfig(package_name) && isHasJSConfig(package_name)) {
                    data[i] = package_name;
                }
            }
        }
        return data;
    }

    private boolean isSystemPakcage(String package_name){
        try {
            PackageInfo a = getPackageManager().getPackageInfo(package_name,0);
            if ((a.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0){
                return false;
            }
        }catch (PackageManager.NameNotFoundException ex){
        }
        return true;
    }

    private boolean isStartHook(String package_name) {
        String path = "/data/system/xsettings/mydemo/persisit/"+package_name+"/persist_mydemo";
        return new File(path).exists();
    }

    private boolean isHasJSConfig(String package_name) {
        String path = "/data/system/xsettings/mydemo/jscfg/"+package_name+"/config.js";
        return new File(path).exists();
    }

    private boolean isHasEasyControlConfig(String package_name) {
        String path = "/data/system/xsettings/mydemo/eccfg/"+package_name+"/config.js";
        return new File(path).exists();
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

    public boolean isInThreadArray(ArrayList<Thread> thread_list,String package_name){
        for(int i=0;i<thread_list.size();i++){
            if(thread_list.get(i).getName() == package_name){
                return true;
            }
        }
        return  false;
    }
}
