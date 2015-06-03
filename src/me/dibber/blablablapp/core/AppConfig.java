package me.dibber.blablablapp.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class AppConfig {
	
	// properties
	private static final boolean isProd = true; 
	private static final boolean ALLOW_WRITE_COMMENTS = true;
	private static final int DEF_MAX_NUMBER_OF_POSTS = 100;
	private static final int DEF_NUMBER_OF_POSTS_PER_REQUEST = 20;
	
	// Blog API parameters:
	private static final String API_URL = isProd ? "http://www.blablablog.nl/" : "http://server.dibber.me/wordpress/"; 	

	private static final String API_PHP = "new_api.php";
	private static final String API_GET_RECENT_POSTS = "?function=get_recent_posts";
	private static final String API_GET_COMMENTS = "?function=get_comments";
	private static final String API_COUNT_PARAM = "&count=";
	private static final String API_AFTERID_PARAM = "&afterPostId=";
	private static final String API_URL_PARAM = "&url=";
	private static final String API_POSTID_PARAM = "&postId=";
	private static final String API_GET_SUPPORTED_VERSIONS = "?function=get_supported_versions";
	private static final String API_ADD_DEVICE = "?function=add_device&deviceId=";
	private static final String API_POST_COMMENT = "?function=post_comment";
	
	private static final String PODCAST_FEED_URL = "http://www.blablablog.nl/blablablog.xml"; 
	
	// API Keys:
	private static final String YOUTUBE_API_KEY = "AIzaSyD7xWiQl4I8KW987uZyns8qma0eWfCY_8c";
	private static final String GOOGLE_CLOUD_MESSAGING_SENDER_ID = "1024871846922";

	// oldest app version with same data structure (if update is done on older version, all files will be deleted after startup 
	private static final int OLDEST_SUPPORTED_VERSION = 14;
	
	public enum Function {GET_RECENT_POSTS,GET_COMMENTS,POST_COMMENT,GET_POST_BY_ID,GET_POST_BY_URL,
		GET_POSTS_AFTER,GET_SUPPORTED_VERSIONS,ADD_DEVICE,GET_PODCAST_POSTS};
	
	public static class APIURLBuilder {
		
		private Function function;
		private int count;
		private int postId;
		private String URL;
		private String deviceId;
		
		private String commentAuthor;
		private String commentEmail;
		private String commentContent;
		private int commentParent;
		
		public APIURLBuilder(Function function) {
			this.function = function;
			count = getDefaultNrPerRequest();
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
		
		public APIURLBuilder setURL(String URL) {
			this.URL = URL;
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
			case GET_POST_BY_URL:
				if (URL == null) {
					throw new IllegalArgumentException("url is missing; first call setURL()");
				}
				path = API_URL + API_PHP + API_GET_RECENT_POSTS + API_URL_PARAM + URL + API_COUNT_PARAM + count;
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
				break;
			case GET_PODCAST_POSTS:
				path = PODCAST_FEED_URL;
				break;
			default:
				break;
			}
			return path;
		}
	}
	
	public static int getDefaultNrPerRequest() {
		return DEF_NUMBER_OF_POSTS_PER_REQUEST;
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
	
	
	public static int getMaxPostStored() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalState.getContext());
		if ( prefs.getBoolean("pref_use_storage", true) ) {
			return prefs.getInt("pref_max_post_stored", DEF_MAX_NUMBER_OF_POSTS);
		} else {
			return 0;
		}
	}
	
	public static boolean allowWriteComments() {
		return ALLOW_WRITE_COMMENTS;
	}
    
}
