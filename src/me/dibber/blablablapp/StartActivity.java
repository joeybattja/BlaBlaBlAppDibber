package me.dibber.blablablapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.Properties;

import me.dibber.blablablapp.DataLoader.PostType;
import me.dibber.blablablapp.PostCollection.DrawableType;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

public class StartActivity extends Activity implements DataLoader.DataLoaderListener {
	
	private static String VERSION_NUMBER = "Version number";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		checkVersion();
		setContentView(R.layout.activity_start);
		ImageView v = (ImageView)findViewById(R.id.imageViewLogo);
		PostCollection.setImage(v, DrawableType.LOGO_COLOR, 0);
		Properties p = AssetsPropertyReader.getProperties(this);
		String URL = p.getProperty("URL") + p.getProperty("GET_RECENT_POSTS") + p.getProperty("NUMBER_OF_POSTS");
		startDataLoader(PostType.POST, URL);
	}

	@Override
	public void onDataLoaderDone(boolean internetConnection) {
		if (!internetConnection) {
			this.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					Toast.makeText(StartActivity.this, R.string.no_connection, Toast.LENGTH_LONG).show();
				}
			});
		}
		
		Intent intent = new Intent(this, HomeActivity.class);
		startActivity(intent);
		this.finish();
	}
	
	private void startDataLoader(PostType type, String source) {
		DataLoader dl = new DataLoader(type);
		dl.setDataLoaderListener(this);
		dl.setStreamType(DataLoader.JSON);
		try {
			dl.setDataSource(source);
		} catch (MalformedURLException e) {
			Log.d("Path incorrect", e.toString());
		}
		dl.prepareAsync();
	}
	
	private void checkVersion() {
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
}
