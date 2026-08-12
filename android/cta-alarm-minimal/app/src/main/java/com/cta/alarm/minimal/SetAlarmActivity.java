package com.cta.alarm.minimal;

import android.app.Activity;
import android.os.Bundle;

/**
 * Conservada únicamente para compatibilidad del código de prueba.
 * CTA Alarma v11 vuelve a programar mediante MainActivity, el flujo fiable.
 */
public class SetAlarmActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        finish();
    }
}
