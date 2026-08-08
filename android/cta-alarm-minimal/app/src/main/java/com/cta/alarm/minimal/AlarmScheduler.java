package com.cta.alarm.minimal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class AlarmScheduler {
    public static final String EXTRA_AT = "cta_alarm_at";
    public static final String EXTRA_LABEL = "cta_alarm_label";
    private AlarmScheduler() {}

    public static void schedule(Context context, long triggerAtMillis, String label) {
        AlarmManager alarmManager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(alarmManager==null) throw new IllegalStateException("AlarmManager no disponible");
        int requestCode=requestCode(triggerAtMillis);
        Intent fireIntent=fireIntent(context,triggerAtMillis,label);
        PendingIntent firePending=PendingIntent.getBroadcast(context,requestCode,fireIntent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent showIntent=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent showPending=PendingIntent.getActivity(context,requestCode+1,showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis,showPending),firePending);
    }

    public static void cancel(Context context,long triggerAtMillis){
        AlarmManager alarmManager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(alarmManager==null)return;
        int requestCode=requestCode(triggerAtMillis);
        Intent fireIntent=fireIntent(context,triggerAtMillis,null);
        PendingIntent firePending=PendingIntent.getBroadcast(context,requestCode,fireIntent,
                PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
        if(firePending!=null){
            try{alarmManager.cancel(firePending);}catch(Exception ignored){}
            try{firePending.cancel();}catch(Exception ignored){}
        }
    }

    private static Intent fireIntent(Context context,long at,String label){
        Intent i=new Intent(context,AlarmReceiver.class)
                .setAction("com.cta.alarm.minimal.FIRE")
                .putExtra(EXTRA_AT,at);
        if(label!=null)i.putExtra(EXTRA_LABEL,label);
        return i;
    }

    private static int requestCode(long value){long mixed=value^(value>>>32);return (int)(mixed&0x7fffffff);}
}
