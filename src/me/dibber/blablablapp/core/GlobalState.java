package me.dibber.blablablapp.core;

import me.dibber.blablablapp.activities.HomeActivity;
import android.app.Application;
import android.content.Context;

public class GlobalState extends Application {
	
	PostCollection posts;
	private static Context context;
	private String searchQuery;
	private boolean refreshing;
	private HomeActivity homeActivity;
	private int oldestSynchedPost;
	
	private YouTubeAdapter youTubeAdapter;
	
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
	
	public YouTubeAdapter getYouTubeAdapter() {
		if (youTubeAdapter == null) {
			youTubeAdapter = new YouTubeAdapter();
		}
		return youTubeAdapter;
	}

}
