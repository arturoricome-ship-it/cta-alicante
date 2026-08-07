package com.cta.alarm.minimal;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private static final String PREFS="cta_alarm_prefs";
    private static final String KEY_AT="pending_at",KEY_LABEL="pending_label",KEY_EXACT="exact_asked",KEY_FSI="fsi_asked";
    private static final int REQ_NOTIFICATIONS=41;
    private SharedPreferences prefs; private TextView status;
    @Override protected void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences(PREFS,MODE_PRIVATE);setContentView(buildView());handleIntent(getIntent());}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    @Override protected void onResume(){super.onResume();long at=prefs==null?0L:prefs.getLong(KEY_AT,0L);if(at>System.currentTimeMillis()&&status!=null)status.postDelayed(()->attemptSchedule(false),300);}
    private void handleIntent(Intent i){Uri d=i==null?null:i.getData();if(d!=null&&"ctaalarm2".equalsIgnoreCase(d.getScheme())&&"set".equalsIgnoreCase(d.getHost())){
            long at=parseLong(d.getQueryParameter("at"));String label=d.getQueryParameter("label");if(label==null||label.trim().isEmpty())label="Turno CTA";
            if(at<=System.currentTimeMillis()){toast("La hora de la alarma ya ha pasado");finish();return;}
            prefs.edit().putLong(KEY_AT,at).putString(KEY_LABEL,label).putBoolean(KEY_EXACT,false).putBoolean(KEY_FSI,false).apply();attemptSchedule(false);
        }else{toast("Las alarmas se crean desde CTA");finish();}}
    private void attemptSchedule(boolean force){long at=prefs.getLong(KEY_AT,0L);String label=prefs.getString(KEY_LABEL,"Turno CTA");if(at<=System.currentTimeMillis()){clearPending();return;}
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(am==null){fail("Android no ofrece alarmas exactas");return;}
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&!am.canScheduleExactAlarms()){
            boolean asked=prefs.getBoolean(KEY_EXACT,false);status("Permite “Alarmas y recordatorios” una sola vez");
            if(force||!asked){prefs.edit().putBoolean(KEY_EXACT,true).apply();try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}return;}
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){status("Permite las notificaciones una sola vez");requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);return;}
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.UPSIDE_DOWN_CAKE){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);boolean allowed=nm!=null&&nm.canUseFullScreenIntent();boolean asked=prefs.getBoolean(KEY_FSI,false);if(!allowed&&(force||!asked)){
                prefs.edit().putBoolean(KEY_FSI,true).apply();status("Permite la alarma a pantalla completa una sola vez");try{startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())));return;}catch(Exception ignored){}}}
        try{AlarmScheduler.schedule(this,at,label);clearPending();String hh=DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(at));toast("⏰ Alarma "+hh+" creada");finishAndRemoveTask();}
        catch(Exception e){fail("No se pudo crear la alarma");}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_NOTIFICATIONS){if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)attemptSchedule(false);else fail("Sin notificaciones no puede sonar la alarma CTA");}}
    private void clearPending(){prefs.edit().remove(KEY_AT).remove(KEY_LABEL).apply();}
    private LinearLayout buildView(){LinearLayout root=new LinearLayout(this);root.setGravity(Gravity.CENTER);root.setPadding(dp(28),dp(28),dp(28),dp(28));root.setBackgroundColor(Color.rgb(10,20,33));status=new TextView(this);status.setGravity(Gravity.CENTER);status.setTextColor(Color.rgb(225,230,238));status.setTextSize(16);status.setText("Creando alarma…");root.addView(status,new LinearLayout.LayoutParams(-1,-2));return root;}
    private void status(String s){if(status!=null)status.setText(s);}
    private void fail(String s){toast(s);finishAndRemoveTask();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private long parseLong(String s){try{return Long.parseLong(s==null?"0":s);}catch(Exception e){return 0L;}}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
