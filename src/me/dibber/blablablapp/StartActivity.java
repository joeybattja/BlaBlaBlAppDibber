package me.dibber.blablablapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import me.dibber.blablablapp.PostCollection.DrawableType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ImageView;

public class StartActivity extends FragmentActivity  {
	
	private static String VERSION_NUMBER = "Version number";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_start);
		ImageView v = (ImageView)findViewById(R.id.imageViewLogo);
		PostCollection.setImage(v, DrawableType.LOGO_COLOR, 0);
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				checkVersionLocal();
				checkVersionOnline();
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
		int versionNumber = 0;
		Context c = GlobalState.getContext();
		File f = c.getFileStreamPath(VERSION_NUMBER);
		boolean fileExists = true;
		if (f == null || !f.exists())
			fileExists = false;
		if (!fileExists) {
			clearApplicationData();
		} else {
			try {
				BufferedReader  in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				String version = in.readLine();
				in.close();
				try {
					versionNumber = Integer.parseInt(version);
				} catch (NumberFormatException e2) {
					Log.w("Incorrect version number", e2.toString());
				}
			} catch (FileNotFoundException e) {
				Log.w("Cannot open internal storage to check version number", e.toString());
			} catch (IOException e1) {
				Log.w("Error reading version number", e1.toString());
			}
			Properties p = AssetsPropertyReader.getProperties(this);
			String OldestSupportedVersionCode = p.getProperty("OLDEST_SUPPORTED_VERSION");
			int oldversionCode = 0;
			try {
				oldversionCode = Integer.parseInt(OldestSupportedVersionCode);
			} catch (NumberFormatException e) {
				Log.e("Error in property OLDEST_SUPPORTED_VERSION: invalid number", e.toString());
			}
			if (versionNumber < oldversionCode) {
				clearApplicationData();
			}
		}
		try {
			versionNumber = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			Log.w("Error reading version code from package manager", e.toString());
		}
		try {
			OutputStreamWriter out = new OutputStreamWriter(c.openFileOutput(VERSION_NUMBER, Context.MODE_PRIVATE));
			out.write(versionNumber + "");
			out.close();
		} catch (IOException e) {
			Log.w("Error writing file version number", e.toString());
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
			Properties p = AssetsPropertyReader.getProperties(this);
			URL checkVersionURL = new URL(p.getProperty("URL") + p.getProperty("APIPHP") + p.getProperty("GET_SUPPORTED_VERSIONS"));
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
	
	private void clearApplicationData() {
        File cache = getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                if (!s.equals("lib")) {
                    deleteDir(new File(appDir, s));
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
