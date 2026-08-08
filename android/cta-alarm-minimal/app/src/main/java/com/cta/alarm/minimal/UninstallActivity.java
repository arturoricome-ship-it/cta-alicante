package com.cta.alarm.minimal;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class UninstallActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        try{
            Intent i=new Intent(Intent.ACTION_DELETE, Uri.parse("package:"+getPackageName()));
            startActivity(i);
        }catch(Exception ignored){}
        finish();
    }
}
