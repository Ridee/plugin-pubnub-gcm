package com.jinn.plugins;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.InstanceIDListenerService;

import com.jinn.plugins.RegistrationIntentService;


public class MyInstanceIDListenerService extends InstanceIDListenerService {

    private static final String TAG = "MyInstanceIDLS";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String senderId = sharedPreferences.getString(PubnubPushNotification.SENDER_ID_KEY, null);

        if (senderId != null) {
            sharedPreferences.edit().putBoolean(PubnubPushNotification.REFRESH_TOKEN_KEY, true).apply();

            // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
            Intent intent = new Intent(this, RegistrationIntentService.class);
            intent.putExtra(PubnubPushNotification.SENDER_ID_KEY, senderId);
            startService(intent);
            return;
        }
    }
    // [END refresh_token]
}
