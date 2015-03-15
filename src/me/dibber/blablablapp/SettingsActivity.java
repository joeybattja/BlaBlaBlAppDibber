package me.dibber.blablablapp;

import java.util.Properties;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        
        Preference npref = findPreference("pref_max_post_stored");
        npref.setDefaultValue( getMaxPostStored());
	}
	
	public static int getMaxPostStored() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalState.getContext());
		if ( prefs.getBoolean("pref_use_storage", true) ) {
	        Properties p = AssetsPropertyReader.getProperties(GlobalState.getContext());
	        int defaultMax = Integer.parseInt(p.getProperty("MAX_NUMBER_OF_POSTS","100"));
			return prefs.getInt("pref_max_post_stored", defaultMax);
		} else {
			return 0;
		}
	}
}
