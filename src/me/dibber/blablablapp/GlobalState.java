package me.dibber.blablablapp;

import java.util.HashMap;

import com.google.android.youtube.player.YouTubeThumbnailLoader;

import android.app.Application;
import android.content.Context;
import android.view.View;

public class GlobalState extends Application {
	
	PostCollection posts;
	private static Context context;
	private String searchQuery;
	private boolean refreshing;
	private HomeActivity homeActivity;
	private HashMap<View,YouTubeThumbnailLoader> youTubeThumbnailLoaders;
	
	@Override
	public void onCreate() {
		super.onCreate();
		context = getApplicationContext();
		searchQuery = null;
		refreshing = false;
	}
	
	public PostCollection getPosts() {
		if (searchQuery == null) {
			return PostCollection.getPostCollection();
		} else {
			return PostCollection.getFilteredPostCollection(searchQuery.split(" "));
		}
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
	
	public HashMap<View,YouTubeThumbnailLoader> getYouTubeThumbnailLoaderList() {
		if (youTubeThumbnailLoaders == null) {
			youTubeThumbnailLoaders = new HashMap<View,YouTubeThumbnailLoader>();
		}
		return youTubeThumbnailLoaders;
	}
	
	public static Context getContext() {
		return context;
	}

}
