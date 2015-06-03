package me.dibber.blablablapp.core;

import java.util.HashMap;

import me.dibber.blablablapp.activities.HomeActivity;
import android.app.Application;
import android.content.Context;
import android.view.View;

import com.google.android.youtube.player.YouTubeThumbnailLoader;

public class GlobalState extends Application {
	
	PostCollection posts;
	private static Context context;
	private String searchQuery;
	private boolean refreshing;
	private HomeActivity homeActivity;
	private int oldestSynchedPost;
	
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
		if (posts == null) {
			posts = PostCollection.getPostCollection();
		}
		return posts;
	}
	
	public void showOnlyFavorites(boolean favorites) {
		getPosts().showFavorite(favorites);
	}
	
	public void showPodcast(boolean podcast) {
		getPosts().showPodcast(podcast);
	}
	
	public void search(String query) {
		if (query != null) {
			query.trim();
			if ("".equals(query))
				query = null;
		}
		searchQuery = query;
		getPosts().setFilter(query == null ? null : searchQuery.split(" "));
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
	
	public int getOldestSynchedPost() {
		return oldestSynchedPost;
	}

	public void setOldestSynchedPost(int oldestSynchedPost) {
		this.oldestSynchedPost = oldestSynchedPost;
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
