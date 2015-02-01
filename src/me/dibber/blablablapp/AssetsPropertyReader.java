package me.dibber.blablablapp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class AssetsPropertyReader {
	
    private static String PROPERTY_FILENAME = "blog.properties";
    
    public static Properties getProperties(Context context) {
    	Properties properties = new Properties();
    	try {
    		AssetManager assetManager = context.getAssets();
    		InputStream inputStream = assetManager.open(PROPERTY_FILENAME);
    		properties.load(inputStream);
    	} catch (IOException e) {
            Log.e("AssetsPropertyReader",e.toString());
    	}
    	return properties;
    }

}
