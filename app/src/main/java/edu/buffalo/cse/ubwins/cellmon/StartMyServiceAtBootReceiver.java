package edu.buffalo.cse.ubwins.cellmon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by AmmY on 12/10/16.
 */

public class StartMyServiceAtBootReceiver extends BroadcastReceiver {
    SharedPreferences preferences;
    @Override
    public void onReceive(Context context, Intent intent) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if(preferences.getBoolean("TRACKING"))
            Intent serviceIntent = new Intent(context, ForegroundService.class);
            serviceIntent.setAction("startforeground");
            context.startService(serviceIntent);
        }
    }
}
