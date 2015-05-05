package me.dibber.blablablapp.activities;

import me.dibber.blablablapp.R;
import me.dibber.blablablapp.core.AppConfig;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.widget.FrameLayout;

public class SettingsActivity extends ActionBarActivity {
	
	private final static int FRAME_ID = 9135;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    getSupportActionBar().setTitle(R.string.settings);
	    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    getSupportActionBar().setHomeAsUpIndicator(getV7DrawerToggleDelegate().getThemeUpIndicator());
	    FrameLayout frame = new FrameLayout(this);
	    setContentView(frame);
	    frame.setId(FRAME_ID);	    	    
	    getFragmentManager().beginTransaction()
	            .replace(FRAME_ID, new SettingsFragment()).commit();
	 }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case android.R.id.home:
    	    this.finish();
    		return true;
        default:
            return super.onOptionsItemSelected(item);
		}
	}
	
	private class SettingsFragment extends PreferenceFragment {
	
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.settings);
	        
	        Preference npref = findPreference("pref_max_post_stored");
	        npref.setDefaultValue( AppConfig.getMaxPostStored());
	        npref.setSummary(AppConfig.getMaxPostStored() + "");
		    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		    prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
				
				@Override
				public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
						String key) {
					if (key.equals("pref_max_post_stored") || key.equals("pref_use_storage")) {
						Preference npref = findPreference("pref_max_post_stored");
						npref.setSummary(AppConfig.getMaxPostStored() + "");
					}
					
				}
			});
		}
	}

}
