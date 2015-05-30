package me.dibber.blablablapp.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class AppConfig {
	
	// Blog API parameters:
	private static boolean isProd = true; 
	private static final String API_URL = isProd ? "http://www.blablablog.nl/" : "http://server.dibber.me/wordpress/"; 	

	private static final String API_PHP = "new_api.php";
	private static final String API_GET_RECENT_POSTS = "?function=get_recent_posts";
	private static final String API_GET_COMMENTS = "?function=get_comments";
	private static final String API_COUNT_PARAM = "&count=";
	private static final String API_AFTERID_PARAM = "&afterPostId=";
	private static final String API_POSTID_PARAM = "&postId=";
	private static final String API_GET_SUPPORTED_VERSIONS = "?function=get_supported_versions";
	private static final String API_ADD_DEVICE = "?function=add_device&deviceId=";
	private static final String API_POST_COMMENT = "?function=post_comment";
	
	// API Keys:
	private static final String YOUTUBE_API_KEY = "AIzaSyD7xWiQl4I8KW987uZyns8qma0eWfCY_8c";
	private static final String GOOGLE_CLOUD_MESSAGING_SENDER_ID = "1024871846922";

	// oldest app version with same data structure (if update is done on older version, all files will be deleted after startup 
	private static final int OLDEST_SUPPORTED_VERSION = 14;
	
	
	// name of properties file
    private static String PROPERTY_FILENAME = "blog.properties";
	
	public enum Function {GET_RECENT_POSTS,GET_COMMENTS,POST_COMMENT,GET_POST_BY_ID,GET_POSTS_AFTER,GET_SUPPORTED_VERSIONS,ADD_DEVICE};
	
	public static class APIURLBuilder {
		
		private Function function;
		private int count;
		private int postId;
		private String deviceId;
		
		private String commentAuthor;
		private String commentEmail;
		private String commentContent;
		private int commentParent;
		
		public APIURLBuilder(Function function) {
			this.function = function;
			String nr = getProperties(GlobalState.getContext()).getProperty("NUMBER_OF_POSTS_PER_REQUEST","20");
			try {
				count = Integer.parseInt(nr);
			} catch (NumberFormatException e) {
				Log.w("error parsing property to int: NUMBER_OF_POSTS_PER_REQUEST", e.toString());
				count=20;
			}
		}
		
		public APIURLBuilder setCount(int count) {
			if (count < 0) {
				count = 0;
			}
			this.count = count;
			return this;
		}
		
		public APIURLBuilder setPostId(int postId) {
			this.postId = postId;
			return this;
		}
		
		public APIURLBuilder setDeviceId(String deviceId) {
			this.deviceId = deviceId;
			return this;
		}
		
		public APIURLBuilder setComment(String author, String email, String comment) {
			return setComment(0,author,email,comment);
		}
		
		public APIURLBuilder setComment(int parent, String author, String email, String comment) {
			commentParent = parent;
			commentAuthor = author;
			commentEmail = email;
			commentContent = comment;
			return this;
		}
		
		public String create() {
			String path = null;
			switch (function) {
			case ADD_DEVICE:
				if (deviceId == null) {
					throw new IllegalArgumentException("DeviceId is missing; first call setDeviceId()");
				}
				path = API_URL + API_PHP + API_ADD_DEVICE + deviceId;
				break;
			case GET_COMMENTS:
				if (postId == 0) {
					throw new IllegalArgumentException("postId is missing; first call setPostId()");
				}
				path = API_URL + API_PHP + API_GET_COMMENTS + API_POSTID_PARAM + postId;
				break;
			case GET_POSTS_AFTER:
				if (postId == 0) {
					throw new IllegalArgumentException("postId is missing; first call setPostId()");
				}
				path = API_URL + API_PHP + API_GET_RECENT_POSTS + API_COUNT_PARAM + count + API_AFTERID_PARAM + postId;
				break;
			case GET_POST_BY_ID:
				if (postId == 0) {
					throw new IllegalArgumentException("postId is missing; first call setPostId()");
				}
				path = API_URL + API_PHP + API_GET_RECENT_POSTS + API_POSTID_PARAM + postId + API_COUNT_PARAM + count;
				break;
			case GET_RECENT_POSTS:
				path = API_URL + API_PHP + API_GET_RECENT_POSTS + API_COUNT_PARAM + count;
				break;
			case GET_SUPPORTED_VERSIONS:
				path = API_URL + API_PHP + API_GET_SUPPORTED_VERSIONS;
				break;
			case POST_COMMENT:
				if (commentAuthor == null || commentEmail == null || commentContent == null) {
					throw new IllegalArgumentException("comment is missing; first call setComment()");
				}
				try {
					path = API_URL + API_PHP + API_POST_COMMENT + "&postId=" + postId + "&commentAuthor=" + URLEncoder.encode(commentAuthor, "UTF-8") +
							"&commentAuthorEmail=" + URLEncoder.encode(commentEmail, "UTF-8") + "&commentContent=" + URLEncoder.encode(commentContent, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					Log.w("Error while trying to encode URL for post comment", e.toString());
					path = API_URL + API_PHP + API_POST_COMMENT + "&postId=" + postId + "&commentAuthor=" + commentAuthor + 
							"&commentAuthorEmail=" + commentEmail + "&commentContent=" + commentContent;
					path = path.replace(" ", "%20").replace("\n","%0A");
				}
				if (commentParent != 0) {
					path = path + "&commentParent=" + commentParent;
				}
			default:
				break;
			}
			return path;
		}
	}
	/*
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
		case GET_POST_BY_ID:
		case GET_COMMENTS:
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
		case GET_COMMENTS:
			path = API_URL + API_PHP + API_GET_COMMENTS + API_POSTID_PARAM + param;
			break;
		case GET_POST_BY_ID:
			if (param != null) {
				path = API_URL + API_PHP + API_GET_RECENT_POSTS + API_POSTID_PARAM + param + API_COUNT_PARAM + count;
			} else {
				path = API_URL + API_PHP + API_GET_RECENT_POSTS + API_COUNT_PARAM + count;
			}
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
	
	public static String getURLPathPostComment(int postId, String author, String email, String comment) {
		String path; 
		
		try {
			path = API_URL + API_PHP + API_POST_COMMENT + "&postId=" + postId + "&commentAuthor=" + URLEncoder.encode(author, "UTF-8") +
					"&commentAuthorEmail=" + URLEncoder.encode(email, "UTF-8") + "&commentContent=" + URLEncoder.encode(comment, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.w("Error while trying to encode URL for post comment", e.toString());
			path = API_URL + API_PHP + API_POST_COMMENT + "&postId=" + postId + "&commentAuthor=" + author + 
					"&commentAuthorEmail=" + email + "&commentContent=" + comment;
			path = path.replace(" ", "%20").replace("\n","%0A");
		}
		return path;
	}*/
	
	public static String getYouTubeAPIKey() {
		return YOUTUBE_API_KEY;
	}
	
	public static String getGCMSenderId() {
		return GOOGLE_CLOUD_MESSAGING_SENDER_ID;
	}
	
	public static int oldestVersionWithSameDataStructure() {
		return OLDEST_SUPPORTED_VERSION;
	}
	
	
	public static int getMaxPostStored() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalState.getContext());
		if ( prefs.getBoolean("pref_use_storage", true) ) {
	        Properties p = AppConfig.getProperties(GlobalState.getContext());
	        int defaultMax = Integer.parseInt(p.getProperty("MAX_NUMBER_OF_POSTS","100"));
			return prefs.getInt("pref_max_post_stored", defaultMax);
		} else {
			return 0;
		}
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
