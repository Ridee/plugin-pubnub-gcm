package com.jinn.plugins;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.NotificationManagerCompat;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.AssetManager;

import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import com.google.android.gms.gcm.GcmListenerService;

import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        JSONObject jsonObject = new JSONObject();
        final Set<String> keys = data.keySet();
        Log.d("push-notification", "Sending JSON:");
        for (String key : keys) {
            try {
                jsonObject.put(key, data.getString(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.d("push-notification", "Sending JSON:"+jsonObject.toString());
        if(PubnubPushNotification.isInForeground()){
            Log.d("push-notification", "JSON");
            Intent msg_received = new Intent(PubnubPushNotification.MSG_RECEIVED_BROADCAST_KEY);
            try{
                msg_received.putExtra("data", jsonObject.getString("json").toString());
            }catch(JSONException e){
                e.printStackTrace();
            }
            //msg_received.putExtra("data", jsonObject.toString());
            LocalBroadcastManager.getInstance(this).sendBroadcast(msg_received);
            //PubnubPushNotification.sendForegroundNotification(jsonObject);
        }else {
            sendNotification(jsonObject);
        }
    }
    // [END receive_message]

    /**
     * Create and show a notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(JSONObject message) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int numberOfMessages = sharedPreferences.getInt(PubnubPushNotification.AMOUNT_OF_MESSAGES, 0);
        Set<String> notificationMessages= sharedPreferences.getStringSet(PubnubPushNotification.NOTIFICATION_MESSAGES, null);

        numberOfMessages++;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("json",message.optString("json").toString());
        notificationIntent.putExtra("delete", false);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        int notifyID = 1;
        int color = 0xff25a5eb;
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.addLine(message.optString("message"));
        if(notificationMessages != null){
            Log.d("push-notification", "notificationMessages no null");
            Iterator<String> iterator = notificationMessages.iterator();
            while(iterator.hasNext()){
                Log.d("push-notification", "next");
                inboxStyle.addLine(iterator.next());
            }
        }else{
            notificationMessages = new HashSet<String>();
        }
        inboxStyle.setBigContentTitle(numberOfMessages + " new messages");
        Bitmap bt = null;
        int icon= getApplicationInfo().icon;
        int iconId =0;
        try{
            AssetManager assetManager = getAssets();
            InputStream istr = assetManager.open("www/img/icon.png");
            bt = BitmapFactory.decodeStream(istr);
            Log.d("push-notification", "largeImage" + icon);
            Resources resources = getResources();
            iconId = resources.getIdentifier("lamp", "drawable", getPackageName());
            Log.d("push-notification", "largeImage" + iconId + Uri.parse("file://android_asset/www/audio/ring.wav") + Uri.parse("android.resource://" + getPackageName() + "/raw/ring"));
        }catch(Exception e){
             Log.d("push-notification", e.toString());
            Log.d("push-notification", "errorlargeImage");
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(iconId)
                //.setLargeIcon(bt)
                .setContentTitle(numberOfMessages + " new messages")
                .setContentText(message.optString("message"))
                .setColor(color)
                .setAutoCancel(true)
                .setSound(Uri.parse("android.resource://" + getPackageName() + "/raw/ring"))
                //.setSound(Uri.parse("file://android_asset/www/audio/beep.wav"))
                //.setStyle(inboxStyle)
                .setContentIntent(contentIntent)
                .setDeleteIntent(PendingIntent.getBroadcast(this, 0, new Intent(this, MyBroadcastsReceiver.class), 0));
        notificationBuilder.setStyle(inboxStyle);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        //NotificationManager notificationManager =
          //      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notifyID, notificationBuilder.build());
        sharedPreferences.edit().putInt(PubnubPushNotification.AMOUNT_OF_MESSAGES, numberOfMessages).apply();
        notificationMessages.add(message.optString("message"));
        sharedPreferences.edit().putStringSet(PubnubPushNotification.NOTIFICATION_MESSAGES, notificationMessages).apply();
    }
}
