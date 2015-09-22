package com.jinn.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.iid.InstanceID;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.pubnub.api.*;
import android.graphics.Bitmap;
import android.net.Uri;


public class PubnubPushNotification extends CordovaPlugin {
  private final String TAG = "PubnubPushPlugin";

    private static final String REGISTER_GCM = "register";
    private static final String UNREGISTER_GCM = "unregister";

    //public static final String JS_CALLBACK_KEY = "JS_CALLBACK";
    public static final String SENDER_ID_KEY = "SENDER_ID";
    //public static final String LAST_PUSH_KEY = "LAST_PUSH";

    public static final String SENT_TOKEN_KEY = "SENT_TOKEN_TO_SERVER";
    public static final String REFRESH_TOKEN_KEY = "REFRESH_TOKEN";
    public static final String GCM_TOKEN_KEY = "GCM_TOKEN";

    public static final String REG_COMPLETE_BROADCAST_KEY = "REGISTRATION_COMPLETE";
    public static final String MSG_RECEIVED_BROADCAST_KEY = "MESSAGE_RECEIVED";
    public static final String NOT_DELETED_BROADCAST_KEY = "NOTIFICATION_DELETED";
    public static final String AMOUNT_OF_MESSAGES = "AMOUNT_OF_MESSAGES";
    public static final String NOTIFICATION_MESSAGES = "NOTIFICATION_MESSAGES";

    public static final String CHANNEL = "CHANNEL";
    public static final String PUBKEY = "PUBKEY";
    public static final String SUBKEY = "SUBKEY";

    private static boolean foreground = false;

    private CallbackContext callback = null;

    private String senderId;
    //private String jsCallback;
    private String channel;
    private String pubKey;
    private String subKey;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        foreground = true;
        //SharedPreferences sharedPreferences =
                //PreferenceManager.getDefaultSharedPreferences(this.cordova.getActivity());
        //jsCallback = sharedPreferences.getString(JS_CALLBACK_KEY, null);

        if (mRegistrationBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(REG_COMPLETE_BROADCAST_KEY));
        }
        if (mMessageReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).registerReceiver(mMessageReceiver,
                    new IntentFilter(MSG_RECEIVED_BROADCAST_KEY));
        }
        if (mNotificationDeleteBroadcastReceiver !=null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).registerReceiver(mNotificationDeleteBroadcastReceiver,
                    new IntentFilter(NOT_DELETED_BROADCAST_KEY));
        }
    }

    @Override
    public boolean execute(String action, final JSONArray args,
                           final CallbackContext callbackContext) throws JSONException {
        try {
            callback = callbackContext;
            if (REGISTER_GCM.equals(action)) {
                senderId = args.optJSONObject(0).optString("senderId", null);
                if (senderId == null) {
                    callbackContext.error("You need to provide a Sender ID, please check: https://developers.google.com/cloud-messaging/android/client?configured=true for more information.");
                    return false;
                }
                /*jsCallback = args.optJSONObject(0).optString("jsCallback", null);
                if (jsCallback == null) {
                    callbackContext.error("Please provide a jsCallback to fully support notifications");
                    return false;
                }*/
                channel = args.optJSONObject(0).optString("channel", null);
                if (channel == null) {
                    callbackContext.error("Please provide a pubnub channel");
                    return false;
                }
                pubKey = args.optJSONObject(0).optString("pubKey", null);
                subKey = args.optJSONObject(0).optString("subKey", null);
                if (pubKey == null || subKey == null) {
                    callbackContext.error("Please provide correct pubnub keys");
                    return false;
                }
                Log.d("push-notification", "REG1");

                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences sharedPreferences =
                                PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
                        final SharedPreferences.Editor edit = sharedPreferences.edit();
                        edit.putString(SENDER_ID_KEY, senderId).apply();
                        Log.d("push-notification", "REG2");

                        //if (checkPlayServices()) {
                            Log.d("push-notification", "REG3");

                            // Start IntentService to register this application with GCM.
                            Intent intent = new Intent(cordova.getActivity(), RegistrationIntentService.class);
                            intent.putExtra(SENDER_ID_KEY, senderId);
                            intent.putExtra(CHANNEL, channel);
                            intent.putExtra(PUBKEY, pubKey);
                            intent.putExtra(SUBKEY, subKey);
                            cordova.getActivity().startService(intent);
                        //}
                    }
                });
                return true;
            } else if (UNREGISTER_GCM.equals(action)) {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        deleteSharedPreferences();
                        unregisterGCM();
                    }
                });
                return true;
            } else if ("onNotification".equals(action)) {
                try{
                    JSONArray response = new JSONArray();
                    response.put(new JSONObject().put("Type", "OK"));
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, response);
                    pluginResult.setKeepCallback(true);
                    callback.sendPluginResult(pluginResult);
                    return true;
                }catch (Exception e){
                    Log.d("push-notification", "onReceive ERROR");
                    Log.d("push-notification", e.toString());
                    return false;
                }
            } else {
                callbackContext.error("Action not Recognized.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
            return false;
        }
    }

    private void unregisterGCM() {
        InstanceID instanceID = InstanceID.getInstance(cordova.getActivity());
        Log.d("push-notification", "UNREG1");
        try {
            Pubnub pubnub = new Pubnub(
                pubKey,
                subKey
            );
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
            String token = sharedPreferences.getString(PubnubPushNotification.GCM_TOKEN_KEY, null);
            pubnub.removeAllPushNotificationsForDeviceRegistrationId(token,new Callback() {
                @Override
                public void successCallback(String channel, final Object message) {
                  Log.d("push-notification", "successRM");
                }

                @Override
                public void errorCallback(String channel, PubnubError pubnubError) {
                  Log.d("push-notification", "ErrorRM");
                }
            });
        }catch (Exception e) {
            e.printStackTrace();
            //callbackContext.error(e.getMessage());
            //return false;
        }
        Log.d("push-notification", "UNREG2");
        try {
            instanceID.deleteInstanceID();
            callback.success("Successfully unregistered from GCM");
        } catch (IOException e) {
            e.printStackTrace();
            callback.error("Unable to unregister from GCM: " + e.getLocalizedMessage());
        }
    }

    private void deleteSharedPreferences() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove(SENDER_ID_KEY)
            .remove(SENT_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .remove(GCM_TOKEN_KEY).apply();
    }

    @Override
    public Object onMessage(String id, Object data) {
        Log.d("push-notification", "onMessage");
        /*if (id.equals("onPageFinished")) {
            //This here is to catch throw the notification once the app has been down.
            //TODO: Maybe there is a better place to do this ? or another way to do this ?
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

            String lastPush = sharedPreferences.getString(LAST_PUSH_KEY, null);
            if (lastPush != null) {
                sendPushToJavascript(lastPush);
            }
        }*/
        return super.onMessage(id, data);
    }

    private void unregisterBroadcastReceivers() {
        if (mRegistrationBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).unregisterReceiver(mRegistrationBroadcastReceiver);
        }
        if (mMessageReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).unregisterReceiver(mMessageReceiver);
        }
        if (mNotificationDeleteBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).unregisterReceiver(mNotificationDeleteBroadcastReceiver);
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        foreground = false;
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        foreground = true;
    }

    @Override
    public void onDestroy() {
        unregisterBroadcastReceivers();
        foreground = false;
        super.onDestroy();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("push-notification", "onReceive");
            //sendPushToJavascript(intent.getStringExtra("data"));
            try{
                JSONArray response = new JSONArray();
                response.put(new JSONObject().put("Type", "PushMessage"));
                response.put(new JSONObject().put("data", intent.getStringExtra("data")));
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, response);
                pluginResult.setKeepCallback(true);
                callback.sendPluginResult(pluginResult);
            }catch (Exception e){
              Log.d("push-notification", "onReceive ERROR");
              Log.d("push-notification", e.toString());
            }
        }
    };

    private BroadcastReceiver mNotificationDeleteBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("push-notification", "onReceive DELETEEEEE");

        }
    };

    private BroadcastReceiver mRegistrationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context);
            boolean sentToken = sharedPreferences
                    .getBoolean(SENT_TOKEN_KEY, false);
            boolean shouldRefreshToken = sharedPreferences
                    .getBoolean(REFRESH_TOKEN_KEY, false);
            if (sentToken || shouldRefreshToken) {
                try {
                    final JSONObject jsonObject = new JSONObject();
                    jsonObject.put("gcm", sharedPreferences.getString(GCM_TOKEN_KEY, null));
                    callback.success(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.error("Error while sending token");
                }
            } else {
                callback.error("Error while getting token");
            }
        }
    };

    public static boolean isInForeground() {
      return foreground;
    }

    /*public static Bitmap getLargeImage() {
        //String icon = options.optString("icon", "icon");
        Bitmap bmp;

        try{
            Log.d("push-notification", "getLargeImage");
            Uri uri = Uri.parse(options.optString("iconUri"));
            bmp = assets.getIconFromUri(uri);
        } catch (Exception e){
            //bmp = assets.getIconFromDrawable(icon);
            Log.d("push-notification", "Error getLargeImage");

        }

        return bmp;
    }*/

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(cordova.getActivity());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GooglePlayServicesUtil.getErrorDialog(resultCode, cordova.getActivity(), 9000).show();
                    }
                });
            } else {
                Log.i(TAG, "This device is not supported.");
                cordova.getActivity().finish();
            }
            return false;
        }
        return true;
    }

}
