package com.sheepoox.tablo.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Tomáš Černý
 * Project Zupl, 2017
 */

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
		// Create a notifications based on intent "paramaters"
        SubsService.notifyUser(3, context, intent.getStringExtra("cls"), intent.getStringExtra("msg_short"), intent.getStringArrayExtra("msgs"));
    }
}
