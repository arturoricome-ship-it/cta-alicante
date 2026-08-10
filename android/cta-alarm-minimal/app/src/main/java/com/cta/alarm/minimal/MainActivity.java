package com.cta.alarm.minimal;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final String PREFS="cta_alarm_prefs";
    public static final String KEY_CONFIGURED="alarm_configured";
    public static final String KEY_MAX_VOLUME="alarm_max_volume";
    public static final String KEY_PROGRESSIVE="alarm_progressive";
    public static final String KEY_VIBRATE="alarm_vibrate";

    private static final String KEY_AT="pending_at", KEY_LABEL="pending_label", KEY_EXACT="exact_asked", KEY_FSI="fsi_asked";
    private static final int REQ_NOTIFICATIONS=41;

    private SharedPreferences prefs;
    private LinearLayout root;
    private TextView status;
    private boolean showingConfig=false;
    private boolean configForPendingAlarm=false;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        root=baseRoot();
        setContentView(root);
        handleIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent i){
        super.onNewIntent(i);
        setIntent(i);
        handleIntent(i);
    }

    @Override protected void onResume(){
        super.onResume();
        if(showingConfig)return;
        long at=prefs.getLong(KEY_AT,0L);
        if(at>System.currentTimeMillis()&&prefs.getBoolean(KEY_CONFIGURED,false)&&status!=null){
            status.postDelayed(()->attemptSchedule(false),300);
        }
    }

    private void handleIntent(Intent i){
        Uri d=i==null?null:i.getData();
        if(d==null||!"ctaalarm3".equalsIgnoreCase(d.getScheme())){
            toast("Las alarmas se crean desde CTA");
            finish();
            return;
        }

        String host=d.getHost();
        if("set".equalsIgnoreCase(host)){
            long at=parseLong(d.getQueryParameter("at"));
            String label=d.getQueryParameter("label");
            if(label==null||label.trim().isEmpty())label="Turno CTA";
            if(at<=System.currentTimeMillis()){
                toast("La hora de la alarma ya ha pasado");
                finish();
                return;
            }
            prefs.edit().putLong(KEY_AT,at).putString(KEY_LABEL,label).putBoolean(KEY_EXACT,false).putBoolean(KEY_FSI,false).apply();
            if(!prefs.getBoolean(KEY_CONFIGURED,false)) showConfig(true);
            else showSchedulingAndAttempt();
            return;
        }

        if("config".equalsIgnoreCase(host)){
            showConfig(false);
            return;
        }

        toast("Enlace de alarma no reconocido");
        finish();
    }

    private void showConfig(boolean forPendingAlarm){
        showingConfig=true;
        configForPendingAlarm=forPendingAlarm;
        root.removeAllViews();

        TextView title=text("Configura tu alarma CTA",24,Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title,wrap());

        TextView desc=text("Esta configuración se guarda para las siguientes alarmas.",15,Color.rgb(190,203,219));
        desc.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dlp=wrap(); dlp.topMargin=dp(10); dlp.bottomMargin=dp(24);
        root.addView(desc,dlp);

        int saved=prefs.getInt(KEY_MAX_VOLUME,40);
        TextView volLabel=text("Volumen máximo: "+saved+"%",18,Color.WHITE);
        root.addView(volLabel,wrap());

        SeekBar volume=new SeekBar(this);
        volume.setMax(90);
        volume.setProgress(Math.max(0,Math.min(90,saved-10)));
        LinearLayout.LayoutParams vlp=new LinearLayout.LayoutParams(-1,-2); vlp.topMargin=dp(8); vlp.bottomMargin=dp(10);
        root.addView(volume,vlp);

        Switch progressive=new Switch(this);
        progressive.setText("Subida progresiva de volumen");
        progressive.setTextColor(Color.WHITE);
        progressive.setTextSize(16);
        progressive.setChecked(prefs.getBoolean(KEY_PROGRESSIVE,true));
        LinearLayout.LayoutParams plp=wrap(); plp.topMargin=dp(12);
        root.addView(progressive,plp);

        Switch vibrate=new Switch(this);
        vibrate.setText("Vibración");
        vibrate.setTextColor(Color.WHITE);
        vibrate.setTextSize(16);
        vibrate.setChecked(prefs.getBoolean(KEY_VIBRATE,true));
        LinearLayout.LayoutParams vibLp=wrap(); vibLp.topMargin=dp(10);
        root.addView(vibrate,vibLp);

        TextView hint=text("El volumen máximo se aplicará aunque hayas dejado el volumen de alarmas del móvil más alto o más bajo. Al detener CTA, se restaura el volumen anterior.",13,Color.rgb(165,180,198));
        LinearLayout.LayoutParams hlp=wrap(); hlp.topMargin=dp(18); hlp.bottomMargin=dp(8);
        root.addView(hint,hlp);

        Button preview=button("▶ PROBAR VOLUMEN MÁXIMO",Color.rgb(42,91,145));
        root.addView(preview,new LinearLayout.LayoutParams(-1,dp(54)));

        Button save=button(forPendingAlarm?"GUARDAR Y CREAR ALARMA":"GUARDAR CONFIGURACIÓN",Color.rgb(218,186,77));
        save.setTextColor(Color.rgb(20,32,45));
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(58)); slp.topMargin=dp(14);
        root.addView(save,slp);

        Button cancel=button("CANCELAR",Color.rgb(55,65,78));
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,dp(52)); clp.topMargin=dp(10);
        root.addView(cancel,clp);

        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean fromUser){
                int pct=roundedPercent(p+10);
                volLabel.setText("Volumen máximo: "+pct+"%");
            }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });

        preview.setOnClickListener(v->previewVolume(roundedPercent(volume.getProgress()+10)));
        save.setOnClickListener(v->{
            int pct=roundedPercent(volume.getProgress()+10);
            prefs.edit()
                    .putBoolean(KEY_CONFIGURED,true)
                    .putInt(KEY_MAX_VOLUME,pct)
                    .putBoolean(KEY_PROGRESSIVE,progressive.isChecked())
                    .putBoolean(KEY_VIBRATE,vibrate.isChecked())
                    .apply();
            showingConfig=false;
            if(configForPendingAlarm)showSchedulingAndAttempt();
            else{toast("Configuración guardada");finishAndRemoveTask();}
        });
        cancel.setOnClickListener(v->{
            if(configForPendingAlarm)clearPending();
            finishAndRemoveTask();
        });
    }

    private void showSchedulingAndAttempt(){
        showingConfig=false;
        root.removeAllViews();
        status=text("Creando alarma…",16,Color.rgb(225,230,238));
        status.setGravity(Gravity.CENTER);
        root.addView(status,wrap());
        attemptSchedule(false);
    }

    private void showFullScreenPermissionRequired(){
        showingConfig=false;
        root.removeAllViews();

        TextView title=text("Permiso necesario",24,Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title,wrap());

        status=text("Activa “Alertas a pantalla completa” para CTA Alarma. Es lo que permite encender la pantalla bloqueada y mostrar DETENER / POSPONER cuando suene.",15,Color.rgb(225,230,238));
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stlp=wrap(); stlp.topMargin=dp(16); stlp.bottomMargin=dp(24);
        root.addView(status,stlp);

        Button allow=button("ACTIVAR PANTALLA COMPLETA",Color.rgb(218,186,77));
        allow.setTextColor(Color.rgb(20,32,45));
        root.addView(allow,new LinearLayout.LayoutParams(-1,dp(58)));
        allow.setOnClickListener(v->openFullScreenSettings());

        Button cancel=button("CANCELAR ALARMA",Color.rgb(55,65,78));
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,dp(52)); clp.topMargin=dp(12);
        root.addView(cancel,clp);
        cancel.setOnClickListener(v->{clearPending();finishAndRemoveTask();});
    }

    private void openFullScreenSettings(){
        try{
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())));
        }catch(Exception e){
            try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}
        }
    }

    private void previewVolume(int pct){
        final AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
        if(am==null)return;
        final int original=am.getStreamVolume(AudioManager.STREAM_ALARM);
        int max=am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        int target=Math.max(1,Math.round(max*(pct/100f)));
        try{am.setStreamVolume(AudioManager.STREAM_ALARM,target,0);}catch(Exception ignored){}
        Uri uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if(uri==null)uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final Ringtone r=RingtoneManager.getRingtone(this,uri);
        if(r==null){try{am.setStreamVolume(AudioManager.STREAM_ALARM,original,0);}catch(Exception ignored){}return;}
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            r.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        }
        try{r.play();}catch(Exception ignored){}
        new Handler().postDelayed(()->{
            try{r.stop();}catch(Exception ignored){}
            try{am.setStreamVolume(AudioManager.STREAM_ALARM,original,0);}catch(Exception ignored){}
        },2200);
    }

    private void attemptSchedule(boolean force){
        long at=prefs.getLong(KEY_AT,0L);
        String label=prefs.getString(KEY_LABEL,"Turno CTA");
        if(at<=System.currentTimeMillis()){clearPending();return;}

        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
        if(am==null){fail("Android no ofrece alarmas exactas");return;}

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&!am.canScheduleExactAlarms()){
            boolean asked=prefs.getBoolean(KEY_EXACT,false);
            status("Permite “Alarmas y recordatorios” una sola vez");
            if(force||!asked){
                prefs.edit().putBoolean(KEY_EXACT,true).apply();
                try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}
                catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}
            }
            return;
        }

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            status("Permite las notificaciones una sola vez");
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);
            return;
        }

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            boolean allowed=nm!=null&&nm.canUseFullScreenIntent();
            if(!allowed){
                boolean asked=prefs.getBoolean(KEY_FSI,false);
                showFullScreenPermissionRequired();
                if(force||!asked){
                    prefs.edit().putBoolean(KEY_FSI,true).apply();
                    openFullScreenSettings();
                }
                return;
            }
            prefs.edit().putBoolean(KEY_FSI,false).apply();
        }

        try{
            AlarmScheduler.schedule(this,at,label);
            clearPending();
            finishAndRemoveTask();
        }catch(Exception e){fail("No se pudo crear la alarma");}
    }

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){
        super.onRequestPermissionsResult(r,p,g);
        if(r==REQ_NOTIFICATIONS){
            if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)attemptSchedule(false);
            else fail("Sin notificaciones no puede funcionar la alarma CTA");
        }
    }

    private LinearLayout baseRoot(){
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setGravity(Gravity.CENTER);
        r.setPadding(dp(24),dp(28),dp(24),dp(28));
        r.setBackgroundColor(Color.rgb(10,20,33));
        return r;
    }

    private Button button(String s,int bg){Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setTextColor(Color.WHITE);b.setBackgroundColor(bg);return b;}
    private TextView text(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);return v;}
    private LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(-1,-2);}
    private int roundedPercent(int p){int r=Math.round(p/5f)*5;return Math.max(10,Math.min(100,r));}
    private void status(String s){if(status!=null)status.setText(s);}
    private void clearPending(){prefs.edit().remove(KEY_AT).remove(KEY_LABEL).apply();}
    private void fail(String s){toast(s);finishAndRemoveTask();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private long parseLong(String s){try{return Long.parseLong(s==null?"0":s);}catch(Exception e){return 0L;}}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
