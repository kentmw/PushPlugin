package com.plugin.gcm;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.util.Random;

import com.google.android.gcm.GCMBaseIntentService;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	private static final String TAG = "GCMIntentService";

	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try {
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		} catch( JSONException e) {
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

  @Override
  protected void onMessage(Context context, Intent intent) {
    Log.d(TAG, "onMessage - context: " + context);

    // Extract the payload from the message
    Bundle extras = intent.getExtras();
    if (extras != null) {
      // if we are in the foreground, just surface the payload, else post it to the statusbar
      if (PushPlugin.isInForeground()) {
        Log.d(TAG, "onMessage - pushing extras because we're in the foreground");
        extras.putBoolean("foreground", true);
        PushPlugin.sendExtras(extras);
      } else {
        extras.putBoolean("foreground", false);
        // Send a notification if there is a message
        if (extras.getString("message") != null && extras.getString("message").length() != 0) {
          Log.d(TAG, "onMessage - create notification because we're in the background");
          createNotification(context, extras);
          //PushPlugin.sendExtras(extras); uncomment for the ecb to fire even if the app is not in foreground
        }
      }
    }
	}

	public void createNotification(Context context, Bundle extras) {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(this);

		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

    int requestCode = new Random().nextInt();
    PendingIntent contentIntent = PendingIntent.getActivity(this, requestCode, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		//PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		int defaults = Notification.DEFAULT_ALL;

		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException e) {}
		}

		NotificationCompat.Builder mBuilder =
			new NotificationCompat.Builder(context)
				.setDefaults(defaults)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(extras.getString("title"))
				.setTicker(extras.getString("title"))
				.setContentIntent(contentIntent)
				.setAutoCancel(true);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mBuilder.setSmallIcon(context.getResources().getIdentifier("secondary_icon", "drawable", context.getPackageName()))
      setNotificationIconColor(extras.getString("color"), mBuilder);
    } else {
      mBuilder.setSmallIcon(context.getApplicationInfo().icon);
    }

		String message = extras.getString("message");
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("<missing message content>");
		}

		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}

		int notId = 0;

		try {
			notId = Integer.parseInt(extras.getString("notId"));
		}
		catch(NumberFormatException e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
		}
		catch(Exception e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
		}

		mNotificationManager.notify((String) appName, notId, mBuilder.build());
	}

	private static String getAppName(Context context)
	{
		CharSequence appName =
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());

		return (String)appName;
	}

	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

  private void setNotificationIconColor(String color, NotificationCompat.Builder mBuilder) {
    int iconColor = 0;
    if (color != null) {
      try {
          iconColor = Color.parseColor(color);
      } catch (IllegalArgumentException e) {
          Log.e(TAG, "couldn't parse color from android options");
      }
    }
    if (iconColor != 0) {
      // Not setting color because of error in PGB:
      /*
        [javac] Compiling 43 source files to /project/bin/classes
        [javac] /project/src/com/plugin/gcm/GCMIntentService.java:168: error: cannot find symbol
        [javac]       mBuilder.setColor(iconColor);
      */
      //mBuilder.setColor(iconColor);
    }
  }
}
