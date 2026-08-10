package com.cta.alarm.minimal;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

public class UninstallActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        try{
            Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+getPackageName()));
            startActivity(i);
        }catch(Exception e){
            try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}
        }
        finish();
    }
}
