package me.dibber.blablablapp.activities;

import java.net.MalformedURLException;

import me.dibber.blablablapp.R;
import me.dibber.blablablapp.core.AppConfig;
import me.dibber.blablablapp.core.DataLoader;
import me.dibber.blablablapp.core.GlobalState;
import me.dibber.blablablapp.core.PostCollection;
import me.dibber.blablablapp.core.AppConfig.Function;
import me.dibber.blablablapp.core.DataLoader.DataLoaderListener;
import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class Notifications {
	
	public static final int NOTIFICATION_ID_NEW_POSTS = 1;
	public static final int NOTIFICATION_ID_MESSAGES = 2;
	
	public static void removeAllNotifications(Context context) {
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
	}
	
	public static class NotificationBroadcastReceiver extends WakefulBroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!((GlobalState)GlobalState.getContext()).optionNotifications()) {
				return;
			}

			// Explicitly specify that GcmIntentService will handle the intent.
	        ComponentName comp = new ComponentName(context.getPackageName(),NotificationIntentService.class.getName());
	        // Start the service, keeping the device awake while it is launching.
	        startWakefulService(context, (intent.setComponent(comp)));
	        setResultCode(Activity.RESULT_OK);
		}
	}
	
	public static class NotificationIntentService extends IntentService {
		
		private Integer mostRecentPost; 
		
		public NotificationIntentService() {
	        super("NotificationIntentService");
	    }

		@Override
		protected void onHandleIntent(Intent intent) {
			if (!((GlobalState)GlobalState.getContext()).optionNotifications()) {
				notifyDone(intent);
				return;
			}
			
			Bundle extras = intent.getExtras();
			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
			String messageType = gcm.getMessageType(intent);
			
			if (!extras.isEmpty() && GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				String title = extras.getString("title", "");
				String content = extras.getString("content","");
				if (!title.isEmpty()) {
					postMessage(title, content);
				}
				checkNewPost(intent);
			} else {
				notifyDone(intent);
			}
		}
		
		private void postMessage(String title, String content) {
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_notifications_messages", true)) {
				PendingIntent notifyIntent =  PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), 0);
	
				NotificationCompat.Builder mNotifBuilder = new NotificationCompat.Builder(this)
				.setAutoCancel(true)
				.setOnlyAlertOnce(true)
				.setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_LIGHTS|Notification.DEFAULT_VIBRATE)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(title)
				.setContentText(content)								
				.setContentIntent(notifyIntent);
				
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.notify(NOTIFICATION_ID_MESSAGES, mNotifBuilder.build());
			}
		}
		
		
		private void checkNewPost(final Intent intent) {
			if (((GlobalState)GlobalState.getContext()).getCurrentHomeActivity() != null) {
				notifyDone(intent);
				return;
			}
			if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_notifications_new_posts", true) ){
				notifyDone(intent);
				return;
			}
			SharedPreferences prefs = ((GlobalState)GlobalState.getContext()).getSharedPreferences(PostOverviewFragment.PREF_POSTDATA,Context.MODE_PRIVATE);
			mostRecentPost = prefs.getInt(PostOverviewFragment.PREF_MOST_RECENT_POST, 0);
			
			DataLoader dl = new DataLoader();
			dl.setDataLoaderListener(new DataLoaderListener() {
				
				@Override
				public void onDataLoaderDiskDone(boolean success) {	}
				
				@Override
				public void onDataLoaderOnlineDone(boolean success) {
					Context c = GlobalState.getContext();
					PostCollection newPc = ((GlobalState)c).getPosts();
					if (mostRecentPost == null) {
						return;
					}
					int index = newPc.getAllPosts().indexOf(mostRecentPost);
					if (index < 1) {
						return;
					}
					PendingIntent notifyIntent;
					String title;
					String content;
				
					if (index == 1) {
						int postId = newPc.getAllPosts().get(0); 
						notifyIntent =  PendingIntent.getActivity(c, 0, new Intent(c, StartActivity.class), 0);
						title = c.getResources().getString(R.string.app_name) + ": " + newPc.getItemTitle(postId);
						content = newPc.getItemContentReplaceBreaks(postId).toString();
					} else {
						notifyIntent =  PendingIntent.getActivity(c, 0, new Intent(c, StartActivity.class), 0);
						title = c.getResources().getString(R.string.app_name);
						content = c.getResources().getString(R.string.notify_new_posts, index);
					}
					NotificationCompat.Builder mNotifBuilder = new NotificationCompat.Builder(c)
					.setAutoCancel(true)
					.setOnlyAlertOnce(true)
					.setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_LIGHTS|Notification.DEFAULT_VIBRATE)
					.setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle(title)
					.setContentText(content)								
					.setContentIntent(notifyIntent);
					
					NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					mNotificationManager.notify(NOTIFICATION_ID_NEW_POSTS, mNotifBuilder.build());
					notifyDone(intent);
				}
			});
			dl.isInSynchWithExistingPosts(true);
			try {
				dl.setDataSource(AppConfig.getURLPath(Function.GET_RECENT_POSTS));
			} catch (MalformedURLException e) {
				Log.d("Path incorrect", e.toString());
			}
			dl.prepareAsync();
		}
		
		private void notifyDone(Intent intent) {
			NotificationBroadcastReceiver.completeWakefulIntent(intent);
		}
		
	}	

}
