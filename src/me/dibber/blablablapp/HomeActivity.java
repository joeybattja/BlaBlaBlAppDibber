package me.dibber.blablablapp;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import me.dibber.blablablapp.DataLoader.DataLoaderListener;
import me.dibber.blablablapp.Pages.PageType;
import me.dibber.blablablapp.PostDetailFragment.PostFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeThumbnailLoader;

public class HomeActivity extends ActionBarActivity implements DataLoaderListener {
	
	private final static String TAG_FRAGMENT_CONTENT = "TAG_FR_C";
	private final static String CURRENT_TYPE = "CUR_TYPE";
	private final static String CURRENT_PAGE = "CUR_PAGE";
	private final static String CURRENT_POST = "CUR_POST";
	
	private int currentPost;
	private int currentPage;
	private ContentFrameType currentType;
	
	private MenuItem searchItem;
	private MenuItem refresh;
	private MenuItem share;
	
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    public enum ContentFrameType {PAGE,POST}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
			
			/** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, Pages.getPageTitles() ));
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				currentPage = position;
				replaceContentFrame(ContentFrameType.PAGE, position);
			}
		});
        if (savedInstanceState != null) {
	        currentPost = savedInstanceState.getInt(CURRENT_POST);
	    	currentPage = savedInstanceState.getInt(CURRENT_PAGE);
	        currentType = (ContentFrameType) savedInstanceState.getSerializable(CURRENT_TYPE);
        } else {
        	currentType = ContentFrameType.PAGE;
        }
        invalidateContentFrame();
        if (((GlobalState)GlobalState.getContext()).getPosts().getAllPosts().size() == 0) {
        	refreshPosts();
        }
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		((GlobalState)GlobalState.getContext()).setCurrentHomeActivity(this);
		if (!((GlobalState)GlobalState.getContext()).isRefreshing() && refresh != null && refresh.getActionView() != null) {
			refresh.setActionView(null);
		}
	}
	
	@Override
	protected void onPause() {
		clearHomeActivityReference();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		clearHomeActivityReference();
		releaseYoutubeLoaders();
		super.onDestroy();
	}
	
	private void clearHomeActivityReference() {
		HomeActivity ha = ((GlobalState)GlobalState.getContext()).getCurrentHomeActivity();
		if (ha != null && ha.equals(this)) {
			((GlobalState)GlobalState.getContext()).setCurrentHomeActivity(null);
		}
	}
	
	private void releaseYoutubeLoaders() {
	    HashMap<View,YouTubeThumbnailLoader> loaders = ((GlobalState)GlobalState.getContext()).getYouTubeThumbnailLoaderList();
	    for (Entry<View, YouTubeThumbnailLoader> entry : loaders.entrySet() ) {
	    	entry.getValue().release();
	    }
	    loaders.clear();
	}
	
	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

	@Override
	public void onBackPressed() {
		// So, when pressing the back button : 
		// First, save the current state, since that's probably interesting for the next frame
		saveLastPosition();
		// Close the drawer if open, else...
		if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
			mDrawerLayout.closeDrawer(mDrawerList);
			return;
		}
		// If currently the details of a post are being shown...
		Fragment fragment = (Fragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_CONTENT);
		if (fragment instanceof PostDetailFragment) {
			// close the YouTubePlayer's fullscreen in case it was fullscreen, else...
			if (getCurrentPostFragment().setYouTubeFullscreen(false)){
				return;
			}
			// Go back to the Home screen, else...
			replaceContentFrame(ContentFrameType.PAGE, currentPage);
			return;
		}
		// If currently a Webpage is shown
		if (fragment instanceof WebPageFragment) {
			replaceContentFrame(ContentFrameType.PAGE, 0);
			return;
		}
		// collapse the search view, else...
		if (searchItem != null && MenuItemCompat.isActionViewExpanded(searchItem)) {
			MenuItemCompat.collapseActionView(searchItem);
			return;
		}
		// close the application.
		exitApplication();
	}
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	super.onSaveInstanceState(savedInstanceState);
    	saveLastPosition();
    	savedInstanceState.putInt(CURRENT_POST, currentPost);
    	savedInstanceState.putInt(CURRENT_PAGE, currentPage);
    	savedInstanceState.putSerializable(CURRENT_TYPE, currentType);
    }
    
	private void saveLastPosition() {
    	Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_CONTENT);
    	if (frag instanceof PostDetailFragment) {
    		currentPost = ((PostDetailFragment) frag).getViewPagerCurrentItem();
    	} else if (frag instanceof PostOverviewFragment) {
    		currentPost = ((PostOverviewFragment)frag).getLastPosition();
		}
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        // Associate searchable configuration with the SearchView
        searchItem = menu.findItem(R.id.search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
			
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}
			
			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				if (searchView.isEnabled()) {
					((GlobalState)GlobalState.getContext()).search(null);
				}
				return true;
			}
		});
        String currentSearchQuery = ((GlobalState)GlobalState.getContext()).getSearchQuery();
        if (currentSearchQuery != null) {
        	searchItem.expandActionView();
        	searchView.setQuery(currentSearchQuery, false);
        	searchView.clearFocus();
        }
        
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String text) {
				((GlobalState)GlobalState.getContext()).search(text); 
				replaceContentFrame(ContentFrameType.PAGE, currentPage);
				searchView.clearFocus();
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String text) {
				((GlobalState)GlobalState.getContext()).search(text); 
				replaceContentFrame(ContentFrameType.PAGE, currentPage);
				return true;
			}
		});  
        
        refresh = menu.findItem(R.id.refresh);
        if (((GlobalState)GlobalState.getContext()).isRefreshing()) {
        	refresh.setActionView(R.layout.actionbar_indeterminate_progress);
        }
        share = menu.findItem(R.id.share);
        share.setEnabled(false);
        share.setVisible(false);
        
        if (currentType == ContentFrameType.POST) {
        	simpleOptionsMenu(true);
        }

        return true;
    }
    
    public void simpleOptionsMenu(boolean simple) {
    	if (searchItem != null) {
    		searchItem.setEnabled(!simple);
    		searchItem.setVisible(!simple);
    	}
    	if (refresh != null) {
	    	refresh.setEnabled(!simple);
	    	refresh.setVisible(!simple);
    	}
    	if (share != null) {
    		share.setEnabled(simple);
    		share.setVisible(simple);
    	}
    	if (simple) {
    		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    		mDrawerToggle.setDrawerIndicatorEnabled(false);
    		getSupportActionBar().setHomeAsUpIndicator(getV7DrawerToggleDelegate().getThemeUpIndicator());
    	} else {
    		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    		mDrawerToggle.setDrawerIndicatorEnabled(true);
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
        	return true;
        }
        
        switch (item.getItemId()) {
        case android.R.id.home:
        	if (mDrawerToggle.isDrawerIndicatorEnabled()) {
        		return mDrawerToggle.onOptionsItemSelected(item);
        	} else {
        		onBackPressed();
        		return true;
        	}
        case R.id.refresh:
        	refreshPosts();
    		return true;
        case R.id.share:
        	sharePost();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void invalidateContentFrame() {
    	if (currentType == null) {
    		currentType = ContentFrameType.PAGE;
        	replaceContentFrame(ContentFrameType.PAGE, currentPage);
    	} else if (currentType == ContentFrameType.POST) {
    		replaceContentFrame(ContentFrameType.POST, currentPost);
        } else {
        	replaceContentFrame(ContentFrameType.PAGE, currentPage);
        }
    }
	
	public void replaceContentFrame(ContentFrameType type, int id) {
		Fragment fragment = null;
		currentType = type;
		
		switch (type) {
		case PAGE:
			currentPage = id;
			mDrawerList.setItemChecked(id, true);
		    setTitle(Pages.getPageTitles()[id]);
		    
			switch (Pages.getPageType(currentPage)) {
			case POSTS:
			case FAVORITES:
				simpleOptionsMenu(false);
				((GlobalState)GlobalState.getContext()).showOnlyFavorites(Pages.getPageType(currentPage) == Pages.PageType.FAVORITES);
				fragment = new PostOverviewFragment();
				Bundle argsO = new Bundle();
				if (currentPost != 0) {
					argsO.putInt(PostOverviewFragment.ARG_ID, currentPost);
				} else {
					argsO.putInt(PostOverviewFragment.ARG_ID, 0);
				}
				fragment.setArguments(argsO);
				break;
			case WEBPAGE:
				simpleOptionsMenu(true);
				fragment = new WebPageFragment();
				Bundle argsW = new Bundle();
				argsW.putString(WebPageFragment.ARG_URL, Pages.getPageURL(id));
				fragment.setArguments(argsW);
				break;
			}
			
			break;
		case POST:
			simpleOptionsMenu(true);
			currentPost = id;
			fragment = new PostDetailFragment();
			Bundle argsD = new Bundle();
			argsD.putInt(PostDetailFragment.ARG_ID, currentPost);
			fragment.setArguments(argsD);
			setTitle(PostCollection.getPostCollection().getItemTitle(currentPost));
			break;
		}
		if (fragment != null) {
			FragmentManager fragmentManager = getSupportFragmentManager();
		    fragmentManager.beginTransaction()
		                   .replace(R.id.content_frame, fragment, TAG_FRAGMENT_CONTENT)
		                   .addToBackStack(null)
		                   .commit();
		}
	    mDrawerLayout.closeDrawer(mDrawerList);
	}
	
	public Fragment getCurrentFragment() {
		return (Fragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_CONTENT);
	}
	
	/**
	 * Returns the PostFragment related to the postId, if this PostFragment exists. 
	 * If the PostFragment does not exist, either because the content frame is currently not showing a PostFragment or
	 * because the adapter is showing other posts, this will return null	
	 * @param postId The id of the post of the requested PostFragment
	 * @return the PostFragment object or null.
	 */
	public PostFragment getPostFragment(int postId) {
		if (getCurrentFragment() instanceof PostDetailFragment) {
			return (PostFragment) ((PostDetailFragment)getCurrentFragment()).getViewPagerItem(postId);
		} 
		return null;
	}
	
	/**
	 * Returns the PostFragment which is currently shown, or null if the content frame is currently not showing post details.
	 * @return the current PostFragment object or null
	 */
	public PostFragment getCurrentPostFragment() {
		Fragment f = getCurrentFragment();
		if (f instanceof PostDetailFragment) {
			int currentitemId = ((PostDetailFragment)f).getViewPagerCurrentItem();
			return (PostFragment) ((PostDetailFragment)f).getViewPagerItem(currentitemId);
		} 
		return null;
	}
	
	public void refreshPosts() {
		if ( ((GlobalState)GlobalState.getContext()).isRefreshing()  ) {
			return;
		}
		((GlobalState)GlobalState.getContext()).refresh(true);
		DataLoader dl = new DataLoader();
		dl.setDataLoaderListener(this);
		dl.isInSynchWithExistingPosts(true);
		try {
			Properties p = AssetsPropertyReader.getProperties(this);
			String URL = p.getProperty("URL") + p.getProperty("APIPHP") + p.getProperty("GET_RECENT_POSTS") + "&count=" + p.getProperty("NUMBER_OF_POSTS_PER_REQUEST");
			dl.setDataSource(URL);
		} catch (MalformedURLException e) {
			Log.d("Path incorrect", e.toString());
		}
		// Need to check if the MenuItem is already available, since this method is called in onCreate and onCreateOptionsMenu is called after onCreate has ended.
		if (refresh != null) {
			refresh.setActionView(R.layout.actionbar_indeterminate_progress);
		}
		dl.prepareAsync();
	}
	
	public void sharePost() {
		String url;
		String title;
		Fragment fragment = (Fragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_CONTENT);
		if (fragment instanceof PostDetailFragment) {
			currentPost = ((PostDetailFragment) fragment).getViewPagerCurrentItem();
			url = ((GlobalState)GlobalState.getContext()).getPosts().getItemUrl(currentPost);
			title = ((GlobalState)GlobalState.getContext()).getPosts().getItemTitle(currentPost).toString();
		} else if (fragment instanceof WebPageFragment) {
			url = ((WebPageFragment)fragment).getURL();
			title = Pages.getPageTitle(currentPage);
		} else {
			return;
		}
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name) + ": " + title);
		intent.putExtra(Intent.EXTRA_TEXT, title + "\n" + url);
		startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_title)));
	}
	
	public void getMorePosts(int lastPostId) {
		if (Pages.getPageType(currentPage) == PageType.FAVORITES) { // nothing to refresh if showing favorites 
			return;
		}
		saveLastPosition();
		if ( ((GlobalState)GlobalState.getContext()).isRefreshing()  ) {
			return;
		}
		((GlobalState)GlobalState.getContext()).refresh(true);
		DataLoader dl = new DataLoader();
		dl.setDataLoaderListener(this);
		dl.isInSynchWithExistingPosts(true);
		try {
			Properties p = AssetsPropertyReader.getProperties(this);
			String URL = p.getProperty("URL") + p.getProperty("APIPHP") + p.getProperty("GET_RECENT_POSTS") + "&count=" + p.getProperty("NUMBER_OF_POSTS_PER_REQUEST","20") 
					+ "&afterPostId=" + lastPostId;
			dl.setDataSource(URL);
		} catch (MalformedURLException e) {
			Log.d("Path incorrect", e.toString());
		}
		// Need to check if the MenuItem is already available, since this method is called in onCreate and onCreateOptionsMenu is called after onCreate has ended.
		if (refresh != null) {
			refresh.setActionView(R.layout.actionbar_indeterminate_progress);
		}
		dl.prepareAsync();
	}
	
	@Override
	public void onDataLoaderDiskDone(boolean success) {
		if (!success) {
			return;
		}
		final HomeActivity homeA = ((GlobalState)GlobalState.getContext()).getCurrentHomeActivity();
		
		if (homeA == null) {
			return;
		}
		homeA.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				saveLastPosition();
				invalidateContentFrame();
			}
		});
	}

	@Override
	public void onDataLoaderOnlineDone(boolean success) {
		((GlobalState)GlobalState.getContext()).refresh(false);
		
		final HomeActivity homeA = ((GlobalState)GlobalState.getContext()).getCurrentHomeActivity();
		
		if (homeA == null) {
			return;
		}
		
		if (!success) {
			homeA.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					Toast.makeText(HomeActivity.this, R.string.no_connection, Toast.LENGTH_LONG).show();
				}
			});
		}
		homeA.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				homeA.refresh.setActionView(null);
				saveLastPosition();
				invalidateContentFrame();
			}
		});
	}
	
	private void exitApplication() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
	    intent.addCategory(Intent.CATEGORY_HOME);
	    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startActivity(intent);
	    
	    // clean up the post collection (incl disk) to contain no more than the set max. 
	    Properties p = AssetsPropertyReader.getProperties(this);
	    int max = Integer.parseInt(p.getProperty("MAX_NUMBER_OF_POSTS"));
	    PostCollection.cleanUpPostCollection(max);
	    ((GlobalState)GlobalState.getContext()).setOldestSynchedPost(0);
	    
	    this.finish();
	}
}


