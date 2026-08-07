package com.cta.alarm.minimal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID="cta_alarm_clock_minimal";
    public static final String EXTRA_NOTIFICATION_ID="cta_notification_id";
    @Override public void onReceive(Context context,Intent intent){
        long at=intent.getLongExtra(AlarmScheduler.EXTRA_AT,System.currentTimeMillis());
        String label=intent.getStringExtra(AlarmScheduler.EXTRA_LABEL);
        if(label==null||label.trim().isEmpty())label="Turno CTA";
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager==null)return; ensureChannel(manager);
        int id=notificationId(at);
        Intent screen=new Intent(context,AlarmActivity.class)
                .putExtra(AlarmScheduler.EXTRA_AT,at).putExtra(AlarmScheduler.EXTRA_LABEL,label)
                .putExtra(EXTRA_NOTIFICATION_ID,id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent full=PendingIntent.getActivity(context,id,screen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(context,CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("CTA · Hora de levantarse")
                .setContentText(label).setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC).setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true).setAutoCancel(false).setContentIntent(full).setFullScreenIntent(full,true).build();
        n.flags|=Notification.FLAG_INSISTENT|Notification.FLAG_NO_CLEAR;
        try{manager.notify(id,n);}catch(SecurityException ignored){}
    }
    private void ensureChannel(NotificationManager manager){
        Uri sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        AudioAttributes audio=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        NotificationChannel ch=new NotificationChannel(CHANNEL_ID,"Alarmas CTA",NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Alarmas creadas desde CTA"); ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0,700,400,700,400,900}); ch.setSound(sound,audio);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); manager.createNotificationChannel(ch);
    }
    public static int notificationId(long value){long mixed=value^(value>>>32);return 10000+(int)(mixed&0x3fffffff);}
}
