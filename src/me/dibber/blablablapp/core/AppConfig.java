package me.dibber.blablablapp.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class AppConfig {
	
	// Blog API parameters:
	private static final String API_URL = "http://www.blablablog.nl/";
	private static final String API_PHP = "new_api.php";
	private static final String API_GET_RECENT_POSTS = "?function=get_recent_posts";
	private static final String API_COUNT_PARAM = "&count=";
	private static final String API_AFTERID_PARAM = "&afterPostId=";
	private static final String API_GET_SUPPORTED_VERSIONS = "?function=get_supported_versions";
	private static final String API_ADD_DEVICE = "?function=add_device&deviceId=";
	
	// API Keys:
	private static final String YOUTUBE_API_KEY = "AIzaSyD7xWiQl4I8KW987uZyns8qma0eWfCY_8c";
	private static final String GOOGLE_CLOUD_MESSAGING_SENDER_ID = "1024871846922";

	// oldest app version with same data structure (if update is done on older version, all files will be deleted after startup 
	private static final int OLDEST_SUPPORTED_VERSION = 3;
	
	
	// name of properties file
    private static String PROPERTY_FILENAME = "blog.properties";
	
	public enum Function {GET_RECENT_POSTS,GET_POSTS_AFTER,GET_SUPPORTED_VERSIONS,ADD_DEVICE};
	
	public static String getURLPath(Function function) {
		return getURLPath(function,null);
	}
	
	public static String getURLPath(Function function, String param) {
		int count = 0;
		switch (function) {
		case ADD_DEVICE:
		case GET_SUPPORTED_VERSIONS:
			break;
		case GET_POSTS_AFTER:
		case GET_RECENT_POSTS:
			String nr = getProperties(GlobalState.getContext()).getProperty("NUMBER_OF_POSTS_PER_REQUEST","20");
			try {
				count = Integer.parseInt(nr);
			} catch (NumberFormatException e) {
				Log.w("error parsing property to int: NUMBER_OF_POSTS_PER_REQUEST", e.toString());
				count=20;
			}
			break;
		}
		return getURLPath(function,param,count);
	}
	
	public static String getURLPath(Function function, int count) {
		return getURLPath(function,null,count);
	}
	
	public static String getURLPath(Function function, String param, int count) {
		String path = null;
		switch (function) {
		case GET_RECENT_POSTS:
			path = API_URL + API_PHP + API_GET_RECENT_POSTS + API_COUNT_PARAM + count;
			break;
		case GET_POSTS_AFTER:
			if (param != null) {
				path = API_URL + API_PHP + API_GET_RECENT_POSTS + API_COUNT_PARAM + count + API_AFTERID_PARAM + param;
			} else {
				path = API_URL + API_PHP + API_GET_RECENT_POSTS + API_COUNT_PARAM + count;
			}
			break;
		case GET_SUPPORTED_VERSIONS:
			path = API_URL + API_PHP + API_GET_SUPPORTED_VERSIONS;
			break;
		case ADD_DEVICE:
			path = API_URL + API_PHP + API_ADD_DEVICE + param;
			break;
		}
		return path;
	}
	
	public static String getYouTubeAPIKey() {
		return YOUTUBE_API_KEY;
	}
	
	public static String getGCMSenderId() {
		return GOOGLE_CLOUD_MESSAGING_SENDER_ID;
	}
	
	public static int oldestVersionWithSameDataStructure() {
		return OLDEST_SUPPORTED_VERSION;
	}
	
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
