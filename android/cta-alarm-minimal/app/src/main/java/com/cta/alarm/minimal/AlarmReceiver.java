package com.cta.alarm.minimal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        long at=intent.getLongExtra(AlarmScheduler.EXTRA_AT,System.currentTimeMillis());
        String label=intent.getStringExtra(AlarmScheduler.EXTRA_LABEL);
        if(label==null||label.trim().isEmpty())label="Turno CTA";

        Intent service=new Intent(context,AlarmService.class)
                .setAction(AlarmService.ACTION_START)
                .putExtra(AlarmScheduler.EXTRA_AT,at)
                .putExtra(AlarmScheduler.EXTRA_LABEL,label);
        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)context.startForegroundService(service);
            else context.startService(service);
        }catch(Exception ignored){}
    }

    public static int notificationId(long value){
        long mixed=value^(value>>>32);
        return 10000+(int)(mixed&0x3fffffff);
    }
}
