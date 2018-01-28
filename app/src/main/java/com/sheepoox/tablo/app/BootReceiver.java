package com.sheepoox.tablo.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Tomáš Černý
 * Project Zupl, 2017
 */

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
		// Filter unwanted actions
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Set the periodic subs updating
            Intent sIntent = new Intent(context, SubsService.class);
            sIntent.setAction("UPDATE_SUBS");
            PendingIntent sPendingIntent = PendingIntent.getService(context, 0, sIntent, 0);
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_HOUR, sPendingIntent);
        }
    }

}
