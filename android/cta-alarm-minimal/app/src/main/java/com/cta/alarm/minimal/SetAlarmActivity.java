package com.cta.alarm.minimal;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

/**
 * Entrada transparente para ctaalarm3://set.
 * Si la app ya está configurada y todos los permisos necesarios están concedidos,
 * programa la alarma sin mostrar ninguna interfaz. Si falta configuración o algún
 * permiso, delega en MainActivity para que el usuario pueda resolverlo.
 */
public class SetAlarmActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);

        Intent source=getIntent();
        Uri d=source==null?null:source.getData();
        if(d==null||!"ctaalarm3".equalsIgnoreCase(d.getScheme())||!"set".equalsIgnoreCase(d.getHost())){
            finish();
            return;
        }

        long at=parseLong(d.getQueryParameter("at"));
        String label=d.getQueryParameter("label");
        if(label==null||label.trim().isEmpty())label="Turno CTA";
        if(at<=System.currentTimeMillis()){
            finish();
            return;
        }

        SharedPreferences prefs=getSharedPreferences(MainActivity.PREFS,MODE_PRIVATE);
        boolean configured=prefs.getBoolean(MainActivity.KEY_CONFIGURED,false);
        if(configured&&hasAllRuntimeRequirements()){
            try{
                AlarmScheduler.schedule(this,at,label);
                prefs.edit().remove("pending_at").remove("pending_label").apply();
                finish();
                overridePendingTransition(0,0);
                return;
            }catch(Exception ignored){
                // Si por cualquier motivo no se puede programar silenciosamente,
                // MainActivity mostrará el flujo normal y su mensaje de error.
            }
        }

        Intent fallback=new Intent(this,MainActivity.class);
        fallback.setAction(Intent.ACTION_VIEW);
        fallback.setData(d);
        fallback.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(fallback);
        overridePendingTransition(0,0);
        finish();
        overridePendingTransition(0,0);
    }

    private boolean hasAllRuntimeRequirements(){
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
        if(am==null)return false;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&!am.canScheduleExactAlarms())return false;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if(nm==null||!nm.canUseFullScreenIntent())return false;
        }
        return true;
    }

    private long parseLong(String s){
        try{return Long.parseLong(s==null?"0":s);}catch(Exception e){return 0L;}
    }
}
