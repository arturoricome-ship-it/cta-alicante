package com.cta.alarm.minimal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public class AlarmService extends Service {
    public static final String ACTION_START="com.cta.alarm.minimal.START_ALARM";
    public static final String ACTION_STOP="com.cta.alarm.minimal.STOP_ALARM";
    private static final String CHANNEL_ID="cta_alarm_engine_v2";

    private MediaPlayer player;
    private AudioManager audio;
    private Vibrator vibrator;
    private final Handler handler=new Handler();
    private Runnable rampRunnable;
    private PowerManager.WakeLock wakeLock;
    private int originalAlarmVolume=-1;
    private int notificationId=24001;
    private boolean restored=false;

    @Override public void onCreate(){
        super.onCreate();
        ensureChannel();
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){
            stopEngine();
            stopSelf();
            return START_NOT_STICKY;
        }

        long at=intent==null?System.currentTimeMillis():intent.getLongExtra(AlarmScheduler.EXTRA_AT,System.currentTimeMillis());
        String label=intent==null?"Turno CTA":intent.getStringExtra(AlarmScheduler.EXTRA_LABEL);
        if(label==null||label.trim().isEmpty())label="Turno CTA";
        notificationId=AlarmReceiver.notificationId(at);

        Intent screen=new Intent(this,AlarmActivity.class)
                .putExtra(AlarmScheduler.EXTRA_AT,at)
                .putExtra(AlarmScheduler.EXTRA_LABEL,label)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent full=PendingIntent.getActivity(this,notificationId,screen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Notification n=new Notification.Builder(this,CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("CTA · Hora de levantarse")
                .setContentText(label)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(full)
                .setFullScreenIntent(full,true)
                .build();

        startForeground(notificationId,n);
        if(player==null)startEngine();
        return START_STICKY;
    }

    private void startEngine(){
        SharedPreferences prefs=getSharedPreferences(MainActivity.PREFS,MODE_PRIVATE);
        int pct=prefs.getInt(MainActivity.KEY_MAX_VOLUME,40);
        boolean progressive=prefs.getBoolean(MainActivity.KEY_PROGRESSIVE,true);
        boolean vibrate=prefs.getBoolean(MainActivity.KEY_VIBRATE,true);

        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        if(audio!=null){
            originalAlarmVolume=audio.getStreamVolume(AudioManager.STREAM_ALARM);
            int max=audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int target=Math.max(1,Math.round(max*(pct/100f)));
            try{audio.setStreamVolume(AudioManager.STREAM_ALARM,target,0);}catch(Exception ignored){}
        }

        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        if(pm!=null){
            try{wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CTA:AlarmWake");wakeLock.acquire(10*60*1000L);}catch(Exception ignored){}
        }

        Uri uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if(uri==null)uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        try{
            player=new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setDataSource(this,uri);
            player.setLooping(true);
            player.prepare();
            player.setVolume(progressive?0.12f:1f,progressive?0.12f:1f);
            player.start();
        }catch(Exception e){
            try{if(player!=null)player.release();}catch(Exception ignored){}
            player=null;
        }

        if(progressive&&player!=null){
            final long started=System.currentTimeMillis();
            rampRunnable=new Runnable(){
                @Override public void run(){
                    if(player==null)return;
                    float p=Math.min(1f,(System.currentTimeMillis()-started)/30000f);
                    float v=0.12f+0.88f*(float)Math.pow(p,1.35);
                    try{player.setVolume(v,v);}catch(Exception ignored){}
                    if(p<1f)handler.postDelayed(this,500);
                }
            };
            handler.post(rampRunnable);
        }

        if(vibrate){
            try{
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                    VibratorManager vm=(VibratorManager)getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                    if(vm!=null)vibrator=vm.getDefaultVibrator();
                }else vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
                if(vibrator!=null&&vibrator.hasVibrator()){
                    long[] pattern={0,700,250,700,250,1000,450};
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)vibrator.vibrate(VibrationEffect.createWaveform(pattern,0));
                    else vibrator.vibrate(pattern,0);
                }
            }catch(Exception ignored){}
        }
    }

    private void stopEngine(){
        if(rampRunnable!=null)handler.removeCallbacks(rampRunnable);
        try{if(player!=null){player.stop();player.release();}}catch(Exception ignored){}
        player=null;
        try{if(vibrator!=null)vibrator.cancel();}catch(Exception ignored){}
        restoreVolume();
        try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)stopForeground(STOP_FOREGROUND_REMOVE);else stopForeground(true);
    }

    private void restoreVolume(){
        if(restored)return;
        restored=true;
        if(audio!=null&&originalAlarmVolume>=0){
            try{audio.setStreamVolume(AudioManager.STREAM_ALARM,originalAlarmVolume,0);}catch(Exception ignored){}
        }
    }

    private void ensureChannel(){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm==null)return;
        NotificationChannel ch=new NotificationChannel(CHANNEL_ID,"Alarmas CTA",NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Motor de alarmas CTA");
        ch.setSound(null,null);
        ch.enableVibration(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    @Override public void onDestroy(){
        stopEngine();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent){return null;}
}
