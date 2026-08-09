package com.cta.alarm.minimal;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
    private static final String SHIFT_SEP="|||CTA_SHIFT|||";
    private long originalAt;
    private String rawLabel;
    private String label;
    private String shiftReminder="";

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O_MR1){setShowWhenLocked(true);setTurnScreenOn(true);}
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        originalAt=getIntent().getLongExtra(AlarmScheduler.EXTRA_AT,System.currentTimeMillis());
        rawLabel=getIntent().getStringExtra(AlarmScheduler.EXTRA_LABEL);
        if(rawLabel==null||rawLabel.trim().isEmpty())rawLabel="Turno CTA";
        parseLabel();
        setContentView(buildView());
    }

    private void parseLabel(){
        int p=rawLabel.indexOf(SHIFT_SEP);
        if(p>=0){
            label=rawLabel.substring(0,p).trim();
            shiftReminder=rawLabel.substring(p+SHIFT_SEP.length()).trim();
        }else label=rawLabel.trim();
        if(label.isEmpty())label="Turno CTA";
    }

    private LinearLayout buildView(){
        int pad=dp(24);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(pad,pad,pad,pad);
        root.setBackgroundColor(Color.rgb(10,20,33));

        TextView title=text("CTA · DESPERTADOR",20,Color.rgb(218,186,77));
        title.setGravity(Gravity.CENTER); root.addView(title,wrap());

        TextView time=text(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()),54,Color.WHITE);
        time.setGravity(Gravity.CENTER); LinearLayout.LayoutParams tlp=wrap(); tlp.topMargin=dp(24); root.addView(time,tlp);

        TextView msg=text(label,18,Color.rgb(225,230,238));
        msg.setGravity(Gravity.CENTER); LinearLayout.LayoutParams mlp=wrap(); mlp.topMargin=dp(12); mlp.bottomMargin=dp(34); root.addView(msg,mlp);

        Button stop=new Button(this); stop.setText("DETENER ALARMA"); stop.setTextSize(17); stop.setTextColor(Color.WHITE); stop.setBackgroundColor(Color.rgb(190,45,45));
        stop.setOnClickListener(v->stopAlarm()); root.addView(stop,new LinearLayout.LayoutParams(-1,dp(58)));

        Button snooze=new Button(this); snooze.setText("POSPONER 5 MIN"); snooze.setTextSize(15); snooze.setTextColor(Color.WHITE); snooze.setBackgroundColor(Color.rgb(42,91,145));
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(54)); slp.topMargin=dp(14); root.addView(snooze,slp); snooze.setOnClickListener(v->snooze());

        if(!shiftReminder.isEmpty()){
            LinearLayout card=new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(18),dp(15),dp(18),dp(15));
            GradientDrawable bg=new GradientDrawable();
            bg.setColor(Color.rgb(15,31,48));
            bg.setCornerRadius(dp(16));
            bg.setStroke(dp(1),Color.rgb(48,70,92));
            card.setBackground(bg);

            TextView reminderTitle=text("TU TURNO DE HOY",12,Color.rgb(218,186,77));
            reminderTitle.setGravity(Gravity.CENTER);
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP)reminderTitle.setLetterSpacing(.08f);
            card.addView(reminderTitle,wrap());

            String pretty=shiftReminder.replace(" · ","\n");
            TextView reminder=text(pretty,21,Color.WHITE);
            reminder.setGravity(Gravity.CENTER);
            reminder.setLineSpacing(0,1.08f);
            LinearLayout.LayoutParams rlp=wrap(); rlp.topMargin=dp(7); card.addView(reminder,rlp);

            TextView hint=text("Comprueba el horario antes de salir",12,Color.rgb(145,164,184));
            hint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams hlp=wrap(); hlp.topMargin=dp(7); card.addView(hint,hlp);

            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);
            cp.topMargin=dp(24);
            root.addView(card,cp);
        }
        return root;
    }

    private void stopAlarm(){
        sendStop();
        finishAndRemoveTask();
    }

    private void snooze(){
        sendStop();
        try{AlarmScheduler.schedule(this,System.currentTimeMillis()+300000L,rawLabel);}catch(Exception ignored){}
        finishAndRemoveTask();
    }

    private void sendStop(){
        try{startService(new Intent(this,AlarmService.class).setAction(AlarmService.ACTION_STOP));}catch(Exception ignored){}
    }

    @Override public void onBackPressed(){}
    private TextView text(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);return v;}
    private LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(-1,-2);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
