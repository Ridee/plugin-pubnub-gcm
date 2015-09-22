package com.jinn.plugins;

import android.app.Activity;

import android.content.Context;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    /*
     * this activity will be started if the user touches a notification that we own.
     * We send it's data off to the push plugin for processing.
     * If needed, we boot up the main activity to kickstart the application.
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle json = getIntent().getExtras();
        Log.d("push-notification", "MainActivity");
        if (json != null) {
            Log.d("push-notification", "APP OPENED WITH DATA");

            String notificationJSON = json.getString("json");

            Intent intent = new Intent(PubnubPushNotification.MSG_RECEIVED_BROADCAST_KEY);
            intent.putExtra("data", notificationJSON);

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            sharedPreferences.edit().remove(PubnubPushNotification.AMOUNT_OF_MESSAGES).apply();
            sharedPreferences.edit().remove(PubnubPushNotification.NOTIFICATION_MESSAGES).apply();
        }

        finish();

        forceMainActivityReload();
    }

    private void forceMainActivityReload() {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        startActivity(launchIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}
