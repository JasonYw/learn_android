package com.example.mydemoservice;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScheduleService extends Service {

    private Thread build_thread;
    private Intent ec_intent;
    private String package_name;
    ScheduleService.MainReceiver m_receiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("ScheduleService:onStartCommand","onStartCommand");
        NotificationChannel channel = new NotificationChannel("1", "ScheduleService", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(getApplicationContext(), "1").build();
            startForeground(1001, notification);
        }
        m_receiver = new MainReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("SRecevier");
        registerReceiver(m_receiver,filter);
        ec_intent = new Intent(ScheduleService.this,EasyControlService.class);
        build_thread = new Thread() {
            @Override
            public void run() {
                Log.i("ScheduleService:onStartCommand","build_thread:start");
                while (true){
                    ArrayList<String> package_list = listPackage();
                    for(int i =0;i<package_list.size();i++){
                        package_name = package_list.get(i);
                        ec_intent.putExtra("package_name",package_name);
                        while (isServiceRunning(getPackageName()+".EasyControlService")){};
                        Log.i("ScheduleService:startService",package_name);
                        startService(ec_intent);
                    }
                }
            }
        };
        build_thread.setName("build_thread");
        build_thread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    private ArrayList<String> listPackage(){
        List<PackageInfo> pakcage_info = getPackageManager().getInstalledPackages(0);
        ArrayList<String> data = new ArrayList<>();
        for(int i=0;i<pakcage_info.size();i++){
            String package_name = pakcage_info.get(i).packageName;
            if(!isSystemPakcage(package_name)) {
                if(isStartHook(package_name) && isHasEasyControlConfig(package_name) && isHasJSConfig(package_name)) {
                    data.add(package_name);
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
        String path = "/data/system/xsettings/mydemo/eccfg/"+package_name+"/config.cfg";
        return new File(path).exists();
    }

    private boolean isServiceRunning(String ServicePackageName) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ServicePackageName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    class MainReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean is_finished = intent.getBooleanExtra(package_name+"_is_finish",false);
            Log.i("ScheduleService:MainReceiver",String.valueOf(is_finished));
            if(is_finished){
                stopService(ec_intent);
            }
        }
    }
}
