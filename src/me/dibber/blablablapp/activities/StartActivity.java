package me.dibber.blablablapp.activities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import me.dibber.blablablapp.core.AppConfig;
import me.dibber.blablablapp.core.DataLoader;
import me.dibber.blablablapp.core.GlobalState;
import me.dibber.blablablapp.core.Notifications;
import me.dibber.blablablapp.core.PostCollection;
import me.dibber.blablablapp.core.AppConfig.Function;
import me.dibber.blablablapp.core.PostCollection.DrawableType;
import me.dibber.blablablapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class StartActivity extends FragmentActivity  {
	
	public static final String PREF_GCM_REG_ID = "registration_id";
	public static final String PREF_APP_VERSION = "version_code";
	public static final int PLAY_SERVICES_ERROR_DIALOG = 9000;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_start);
		ImageView v = (ImageView)findViewById(R.id.imageViewLogo); 
		PostCollection.setImage(v, DrawableType.LOGO_COLOR, 0);
		Notifications.removeAllNotifications(this);

		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				checkVersionLocal();
				checkVersionOnline();
				checkGCMRegistrationId();
			}
		});
		t.start();
	}

	private void startHomeActivity () {
		Intent intent = new Intent(this, HomeActivity.class);
		startActivity(intent);
		this.finish();
	}
	
	private void exitApp() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
	    intent.addCategory(Intent.CATEGORY_HOME);
	    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startActivity(intent);	
	    this.finish();
	}
	
	/**
	* Checks the version locally.
	* 
	* After this check the currently used version is stored in the internal storage. 
	* Before it does so, the app will check the last stored version. If this is empty, or if the version is too old (older than OLDEST_SUPPORTED_VERSION) 
	* all internal storage is cleaned.
	* 
	* This can only be triggered after an update or when the app is newly installed. The reason behind this is that an updated version of the app might decide to 
	* store files in a different place in the internal storage. This method prevents the old data which might never be used anymore to stay behind in the internal storage.
	*/
	private void checkVersionLocal() {
		Context c = GlobalState.getContext();
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		int registeredVersion = prefs.getInt(PREF_APP_VERSION, 0);
		int currentVersion = 0;
		try {
			currentVersion = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
		    SharedPreferences.Editor editor = prefs.edit();
		    editor.putInt(PREF_APP_VERSION, currentVersion);
		    editor.commit();
		} catch (NameNotFoundException e) {
			// should never happen...
			Log.w("Error reading version code from package manager", e.toString());
		}
		if (registeredVersion == currentVersion) {
			// no update since last use. Check is done! 
			return;
		}
		
		// Clear the registration id for Google Cloud Messaging
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PREF_GCM_REG_ID, "");
	    editor.commit();
	    
	    // in case the registered version is older than the oldestVersionWithSameDataStructure, all data files should be cleared. 
	    // this is done in those cases that an update is done in the local data structure and prevents unused files on the device. 
		if (registeredVersion < AppConfig.oldestVersionWithSameDataStructure()) {
			clearApplicationFiles();
		}
	}
	
	/**
	 * Checks the version online. 
	 * 
	 * If the app cannot make internet connection an appropriate dialog is shown, but the user can continue offline. If it does make connection, 
	 * the app will check the minimum supported version as defined in the database online. If the currently used version is outdated, a dialog
	 * will prevent the user to continue.
	 */
	private void checkVersionOnline() {
		try {
			URL checkVersionURL = new URL(new AppConfig.APIURLBuilder(Function.GET_SUPPORTED_VERSIONS).create());
			HttpURLConnection connection = (HttpURLConnection) checkVersionURL.openConnection();
			InputStream in = connection.getInputStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			StringBuilder data = new StringBuilder();
			String line;
			while ( (line = r.readLine()) != null) {
				data.append(line);
			}
			r.close();
			JSONObject obj = new JSONObject(data.toString());
			if (obj == null || !obj.getString("status").equals("OK")) {
				throw new JSONException("Status is not 'OK'");
			}
			JSONArray jArray = obj.getJSONArray("supportedVersions");
			int minVersion = 0;
			for (int i = 0; i < jArray.length(); i++) {
				if (jArray.getJSONObject(i).getString("appType").equals("Android")) {
					minVersion = jArray.getJSONObject(i).getInt("minVersion");
					break;
				};
			}
			Context c = GlobalState.getContext();
			int versionNumber = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
			if (minVersion > versionNumber) {
				unsupportedVersionDialogFragment dialog1 = new unsupportedVersionDialogFragment();
				dialog1.show(getSupportFragmentManager(), "unsupportedVersionDialogFragment");
			} else {
				startHomeActivity();
			}
			
		} catch (IOException e) {
			Log.w("Error trying to setup connections", e.toString());
			noNetworkDialogFragment dialog2 = new noNetworkDialogFragment();
			dialog2.show(getSupportFragmentManager(), "noNetworkDialogFragment");
						
		} catch (JSONException e) {
			Log.w("Error, message received from get_supported_versions not a proper JSON object:", e.toString());
			startHomeActivity();
		} catch (NameNotFoundException e) {
			Log.w("Error reading version code from package manager", e.toString());
			startHomeActivity();
		}
	}
	
	/**
	 * Deletes all files, except for the file used by the dataLoader class.
	 * Generally this deletes all stored images. 
	 */
	private void clearApplicationFiles() {
        File filesDir = getFilesDir();
        if (filesDir.exists()) {
            String[] children = filesDir.list();
            for (String s : children) {
                if (!DataLoader.FILE_LOCATION.equals(s)) {
                    deleteDir(new File(filesDir, s));
                }
            }
        }
    }

	private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
	
	private void checkGCMRegistrationId() {
		if (!((GlobalState)GlobalState.getContext()).optionNotifications()) {
			return;
		}
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		String regId = prefs.getString(PREF_GCM_REG_ID, "");
		if (regId.isEmpty()) {
			registerGCMInBackGround();
		}
	}
	
	private void registerGCMInBackGround() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode == ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_ERROR_DIALOG);
				return;
			}
		} 
		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				try {
					Context c = GlobalState.getContext();
					GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(c);
					String regId = gcm.register(AppConfig.getGCMSenderId());
					
					SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString(PREF_GCM_REG_ID, regId);
					editor.commit();
					sendGCMRegistrationIdToServer(regId);
				} catch (IOException e) {
					Log.w("Error trying to register on Google Cloud Messaging", e.toString());
				}
				return null;
			}
		}.execute(null,null,null);
	}
	
	private void sendGCMRegistrationIdToServer(String regId) {
		try {
			URL url = new URL(new AppConfig.APIURLBuilder(Function.ADD_DEVICE).setDeviceId(regId).create());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			InputStream in = connection.getInputStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			StringBuilder data = new StringBuilder();
			String line;
			while ( (line = r.readLine()) != null) {
				data.append(line);
			}
			r.close();
			JSONObject obj = new JSONObject(data.toString());
			if ("OK".equals(obj.get("status"))) {
				Log.i("Registration done successfully", "REG ID = " + regId);
			} else if ("DUP".equals(obj.get("status"))) {
				Log.i("Registration not needed, duplicate already existed on server", "REG ID = " + regId);
			} else {
				Log.w("Error while registering following device to server: " + regId, obj.toString());
				SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
				prefs.edit().putString(PREF_GCM_REG_ID, "").commit();

			}
		} catch (IOException | JSONException e) {
			Log.w("Exception while registering following device to server: " + regId, e.toString());
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			prefs.edit().putString(PREF_GCM_REG_ID, "").commit();
		}
	}
	
	private class noNetworkDialogFragment extends DialogFragment {
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.no_connection_dialog_title)
				   .setMessage(R.string.no_connection_dialog_message)
				   .setIconAttribute(android.R.attr.alertDialogIcon)
				   .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							startHomeActivity();
						}
					})
					.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							exitApp();
						}
					});
			
			return builder.create();
		}
	}
	
	private class unsupportedVersionDialogFragment extends DialogFragment {
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.unsupported_version_dialog_message)
				   .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							exitApp();
						}
					});
			return builder.create();
		}
	}
	
}
