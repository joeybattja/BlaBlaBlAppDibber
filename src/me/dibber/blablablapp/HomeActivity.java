package me.dibber.blablablapp;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import me.dibber.blablablapp.DataLoader.DataLoaderListener;
import me.dibber.blablablapp.PostDetailFragment.PostFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeThumbnailLoader;

public class HomeActivity extends ActionBarActivity implements DataLoaderListener {
	
	private final static String TAG_FRAGMENT_CONTENT = "TAG_FR_C";
	private final static String CURRENT_TYPE = "CUR_TYPE";
	private final static String CURRENT_ID = "CUR_ID";
	private final static String CURRENT_POST = "CUR_POST";
	
	private int currentId;
	private int currentPost;
	private ContentFrameType currentType;
	
	private MenuItem menuItem;
	private MenuItem refresh;
	
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private CharSequence[] pageTitles;
    public enum ContentFrameType {PAGE,POST}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		pageTitles = new CharSequence[] {"Home","Over Theo","eBook winkel"};
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
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, pageTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        
        try {
        	currentId = savedInstanceState.getInt(CURRENT_ID);
        	currentPost = savedInstanceState.getInt(CURRENT_POST);
            currentType = (ContentFrameType) savedInstanceState.getSerializable(CURRENT_TYPE);
            replaceContentFrame(currentType, currentId);
        } catch (NullPointerException e) {
        	replaceContentFrame(ContentFrameType.PAGE, 0);
        }
        refreshPosts();
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
	public void onBackPressed() {
		saveLastPosition();
		Fragment fragment = (Fragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_CONTENT);
		if (fragment instanceof PostDetailFragment) {
			replaceContentFrame(ContentFrameType.PAGE, 0);
		} else {
			
			if (menuItem != null && MenuItemCompat.isActionViewExpanded(menuItem)) {
				MenuItemCompat.collapseActionView(menuItem);
				return;
			}
			exitApplication();
		}		
	}
	
	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	super.onSaveInstanceState(savedInstanceState);
    	saveLastPosition();
    	savedInstanceState.putInt(CURRENT_ID, currentId);
    	savedInstanceState.putInt(CURRENT_POST, currentPost);
    	savedInstanceState.putSerializable(CURRENT_TYPE, currentType);
    }
    
	private void saveLastPosition() {
    	Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_CONTENT);
    	if (frag instanceof PostDetailFragment) {
    		currentPost = ((PostDetailFragment) frag).getViewPagerCurrentItem();
    		currentId = currentPost;
    	} else if (frag instanceof PostOverviewFragment) {
    		currentPost = ((PostOverviewFragment)frag).getLastPosition();
		}
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        // Associate searchable configuration with the SearchView
        menuItem = menu.findItem(R.id.search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        MenuItemCompat.setOnActionExpandListener(menuItem, new MenuItemCompat.OnActionExpandListener() {
			
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}
			
			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				((GlobalState)GlobalState.getContext()).search(null);
				return true;
			}
		});
        String currentSearchQuery = ((GlobalState)GlobalState.getContext()).getSearchQuery();
        if (currentSearchQuery == null) {
        } else {
        	menuItem.expandActionView();
        	searchView.setQuery(currentSearchQuery, false);
        	searchView.clearFocus();
        }
        
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String text) {
				((GlobalState)GlobalState.getContext()).search(text); 
				replaceContentFrame(ContentFrameType.PAGE, 0);
				searchView.clearFocus();
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String text) {
				((GlobalState)GlobalState.getContext()).search(text); 
				replaceContentFrame(ContentFrameType.PAGE, 0);
				return true;
			}
		});  
        
        refresh = menu.findItem(R.id.refresh);
        if (((GlobalState)GlobalState.getContext()).isRefreshing()) {
        	refresh.setActionView(R.layout.actionbar_indeterminate_progress);
        }

        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
          return true;
        }
        
        switch (item.getItemId()) {
        case R.id.refresh:
        	refreshPosts();
    		return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
	    @Override
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	    	replaceContentFrame(ContentFrameType.PAGE, position);
	    }
	}

	public void replaceContentFrame(ContentFrameType type, int id) {
		Fragment fragment = null;
		currentType = type;
		currentId = id;
		
		switch (type) {
		case PAGE:
			if (id == 0) {
				fragment = new PostOverviewFragment();
				Bundle args = new Bundle();
				if (currentPost != 0) {
					args.putInt(PostOverviewFragment.ARG_ID, currentPost);
				} else {
					args.putInt(PostOverviewFragment.ARG_ID, 0);
				}
				fragment.setArguments(args);
			} else { 
				fragment = new PageFragment();
				Bundle args = new Bundle();
				args.putInt(PostDetailFragment.ARG_ID, id);
				fragment.setArguments(args);
			}
			mDrawerList.setItemChecked(id, true);
		    setTitle(pageTitles[id]);
			break;
		case POST:
			currentPost = id;
			fragment = new PostDetailFragment();
			Bundle args = new Bundle();
			args.putInt(PostDetailFragment.ARG_ID, currentPost);
			fragment.setArguments(args);
			setTitle(PostCollection.getPostCollection().getItemTitle(currentPost));
			break;
		default:
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
	
	public PostFragment getCurrentPostFragment(int postId) {
		if (getCurrentFragment() instanceof PostDetailFragment) {
			return (PostFragment) ((PostDetailFragment)getCurrentFragment()).getViewPagerItem(postId);
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
		dl.setStreamType(DataLoader.JSON);
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
	
	public void getMorePosts(int lastPostId) {
		saveLastPosition();
		if ( ((GlobalState)GlobalState.getContext()).isRefreshing()  ) {
			return;
		}
		((GlobalState)GlobalState.getContext()).refresh(true);
		DataLoader dl = new DataLoader();
		dl.setDataLoaderListener(this);
		dl.setStreamType(DataLoader.JSON);
		try {
			Properties p = AssetsPropertyReader.getProperties(this);
			String URL = p.getProperty("URL") + p.getProperty("APIPHP") + p.getProperty("GET_RECENT_POSTS") + "&count=" + p.getProperty("NUMBER_OF_POSTS_PER_REQUEST") 
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
	public void onDataLoaderDone(boolean internetConnection) {
		((GlobalState)GlobalState.getContext()).refresh(false);
		
		final HomeActivity homeA = ((GlobalState)GlobalState.getContext()).getCurrentHomeActivity();
		
		if (homeA == null) {
			return;
		}
		
		if (!internetConnection) {
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
				try {
					homeA.replaceContentFrame(currentType, currentId);
		        } catch (NullPointerException e) {
		        	homeA.replaceContentFrame(ContentFrameType.PAGE, 0);
		        }
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
	    
	    this.finish();
	}
}


