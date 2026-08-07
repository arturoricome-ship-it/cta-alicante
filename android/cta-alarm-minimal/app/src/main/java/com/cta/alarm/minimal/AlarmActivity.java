package com.cta.alarm.minimal;

import android.app.Activity;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;

public class AlarmActivity extends Activity {
    private long originalAt; private String label; private int notificationId;
    @Override protected void onCreate(Bundle b){super.onCreate(b);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O_MR1){setShowWhenLocked(true);setTurnScreenOn(true);}else{
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);}
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        originalAt=getIntent().getLongExtra(AlarmScheduler.EXTRA_AT,System.currentTimeMillis());
        label=getIntent().getStringExtra(AlarmScheduler.EXTRA_LABEL); if(label==null||label.trim().isEmpty())label="Turno CTA";
        notificationId=getIntent().getIntExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID,AlarmReceiver.notificationId(originalAt));
        setContentView(buildView());}
    private LinearLayout buildView(){int pad=dp(24);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(pad,pad,pad,pad);root.setBackgroundColor(Color.rgb(10,20,33));
        TextView title=text("CTA · DESPERTADOR",20,Color.rgb(218,186,77));title.setGravity(Gravity.CENTER);root.addView(title,wrap());
        TextView time=text(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()),54,Color.WHITE);time.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tlp=wrap();tlp.topMargin=dp(24);root.addView(time,tlp);
        TextView msg=text(label,18,Color.rgb(225,230,238));msg.setGravity(Gravity.CENTER);LinearLayout.LayoutParams mlp=wrap();mlp.topMargin=dp(12);mlp.bottomMargin=dp(34);root.addView(msg,mlp);
        Button stop=new Button(this);stop.setText("DETENER ALARMA");stop.setTextSize(17);stop.setTextColor(Color.WHITE);stop.setBackgroundColor(Color.rgb(190,45,45));stop.setOnClickListener(v->stopAlarm());root.addView(stop,new LinearLayout.LayoutParams(-1,dp(58)));
        Button snooze=new Button(this);snooze.setText("POSPONER 5 MIN");snooze.setTextSize(15);snooze.setTextColor(Color.WHITE);snooze.setBackgroundColor(Color.rgb(42,91,145));LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(54));slp.topMargin=dp(14);root.addView(snooze,slp);snooze.setOnClickListener(v->snooze());return root;}
    private void stopAlarm(){cancel();finishAndRemoveTask();}
    private void snooze(){cancel();try{AlarmScheduler.schedule(this,System.currentTimeMillis()+300000L,label+" · Pospuesta 5 min");}catch(Exception ignored){}finishAndRemoveTask();}
    private void cancel(){NotificationManager m=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(m!=null)m.cancel(notificationId);}
    @Override public void onBackPressed(){}
    private TextView text(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);return v;}
    private LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(-1,-2);} private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
