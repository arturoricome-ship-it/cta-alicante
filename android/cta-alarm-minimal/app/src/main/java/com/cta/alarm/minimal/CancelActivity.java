package com.cta.alarm.minimal;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class CancelActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        Uri d=getIntent()==null?null:getIntent().getData();
        long at=0L;
        try{at=Long.parseLong(d==null?"0":String.valueOf(d.getQueryParameter("at")));}catch(Exception ignored){}
        if(at>0L){
            AlarmScheduler.cancel(this,at);
            Toast.makeText(this,"Alarma CTA cancelada",Toast.LENGTH_SHORT).show();
        }
        finishAndRemoveTask();
    }
}
