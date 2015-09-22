package com.jinn.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.util.Log;

public class MyBroadcastsReceiver extends BroadcastReceiver {
    private static final String TAG = "MyBroadcastsReceiver";

    @Override
    public void onReceive (Context context, Intent intent) {
        Log.d("push-notification", "DELETE BROADCAST");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().remove(PubnubPushNotification.AMOUNT_OF_MESSAGES).apply();
        sharedPreferences.edit().remove(PubnubPushNotification.NOTIFICATION_MESSAGES).apply();
    }

}
