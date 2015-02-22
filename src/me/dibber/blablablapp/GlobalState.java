package me.dibber.blablablapp;

import java.util.HashMap;

import android.app.Application;
import android.content.Context;
import android.view.View;

import com.google.android.youtube.player.YouTubeThumbnailLoader;

public class GlobalState extends Application {
	
	PostCollection posts;
	private static Context context;
	private String searchQuery;
	private boolean showOnlyFavorites;
	private boolean refreshing;
	private HomeActivity homeActivity;
	
	@Override
	public void onCreate() {
		super.onCreate();
		context = getApplicationContext();
		refreshing = false;
	}
	
	// Needed to get the Context from anywhere within the application
	public static Context getContext() {
		return context;
	}
	
	public PostCollection getPosts() {
		if (searchQuery == null) {
			if (!showOnlyFavorites) {
				return PostCollection.getPostCollection();
			} else {
				return PostCollection.getFavoritesPostCollection();
			}
		} else {
			return PostCollection.getFilteredPostCollection(searchQuery.split(" "), showOnlyFavorites);
		}
	}
	
	public void showOnlyFavorites(boolean favorites) {
		showOnlyFavorites = favorites;
	}
	
	public void search(String query) {
		if (query != null) {
			query.trim();
			if ("".equals(query))
				query = null;
		}
		searchQuery = query;
	}
	
	public String getSearchQuery() {
		return searchQuery;
	}
	
	public boolean isRefreshing() {
		return refreshing;
	}
	
	public void refresh(boolean refresh) {
		refreshing = refresh;
	}

	public HomeActivity getCurrentHomeActivity() {
		return homeActivity;
	}

	public void setCurrentHomeActivity(HomeActivity homeActivity) {
		this.homeActivity = homeActivity;
	}
	
	
//--------------------------- YouTube support -----------------------------------
	
	private HashMap<View,YouTubeThumbnailLoader> youTubeThumbnailLoaders;
	private String currentYouTubeVideo;
	private int currentYouTubeTime;
	
	public HashMap<View,YouTubeThumbnailLoader> getYouTubeThumbnailLoaderList() {
		if (youTubeThumbnailLoaders == null) {
			youTubeThumbnailLoaders = new HashMap<View,YouTubeThumbnailLoader>();
		}
		return youTubeThumbnailLoaders;
	}
	
	public void setCurrentYouTubeVideoTime(String videoID, int currentTimeMillis) {
		currentYouTubeVideo = videoID;
		currentYouTubeTime = currentTimeMillis;
	}
	
	public int getYouTubeCurrentTimeMilis() {
		return currentYouTubeTime;
	}
	
	public String getCurrentYouTubeVideo() {
		return currentYouTubeVideo;
	}

}
