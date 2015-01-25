package me.dibber.blablablapp;

import android.app.Application;
import android.content.Context;

public class GlobalState extends Application {
	
	PostCollection posts;
	PostCollection pages;
	private static Context context;
	private String searchQuery;
	private boolean refreshing;
	private HomeActivity homeActivity;
	
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
	
	public PostCollection getPages() {
		if (pages == null) {
			pages = PostCollection.getPageCollection();
		}
		return pages;
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
	
	public static Context getContext() {
		return context;
	}

}
