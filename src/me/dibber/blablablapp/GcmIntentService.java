package me.dibber.blablablapp;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService {
	
	public static final int NOTIFICATION_ID = 1;
	
	public GcmIntentService() {
        super("GcmIntentService");
    }

	@Override
	protected void onHandleIntent(Intent intent) {
		if (!((GlobalState)GlobalState.getContext()).optionNotifications()) {
			GcmBroadcastReceiver.completeWakefulIntent(intent);
			return;
		}
		
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);
		
		if (!extras.isEmpty()) {
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				Log.i("GCM Send error: ", extras.toString());
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				Log.i("GCM Message deleted: ", extras.toString());
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				
				String title = extras.getString("title", "");
				String contentText = extras.getString("content", "");
				
				PendingIntent notifyIntent =  PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), 0);

				NotificationCompat.Builder mNotifBuilder = new NotificationCompat.Builder(this)
				.setAutoCancel(true)
				.setOnlyAlertOnce(true)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(title)
				.setContentText(contentText)								
				.setContentIntent(notifyIntent);
				
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.notify(NOTIFICATION_ID, mNotifBuilder.build());
			}
			
			
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);

	}

}
