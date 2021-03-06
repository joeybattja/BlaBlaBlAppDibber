package me.dibber.blablablapp.activities;

import java.net.MalformedURLException;

import me.dibber.blablablapp.R;
import me.dibber.blablablapp.activities.PostDetailFragment.PostFragment;
import me.dibber.blablablapp.core.AppConfig;
import me.dibber.blablablapp.core.AppConfig.Function;
import me.dibber.blablablapp.core.DataLoader;
import me.dibber.blablablapp.core.DataLoader.DataLoaderListener;
import me.dibber.blablablapp.core.GlobalState;
import me.dibber.blablablapp.core.Pages;
import me.dibber.blablablapp.core.Pages.PageType;
import me.dibber.blablablapp.core.PodCastPlayer;
import me.dibber.blablablapp.core.PostCollection;
import me.dibber.blablablapp.core.Profile;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
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
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends ActionBarActivity implements DataLoaderListener {
	
	public final static String ARG_POST_ID = "ARG_POSTID";
	
	private final static String TAG_FRAGMENT_CONTENT = "TAG_FR_C";
	private final static String CURRENT_TYPE = "CUR_TYPE";
	private final static String CURRENT_PAGE = "CUR_PAGE";
	private final static String CURRENT_POST = "CUR_POST";
	
	private int currentPost;
	private int currentPage;
	private ContentFrameType currentType;
	private boolean intentChecked; 
	
	private MenuItem searchItem;
	private MenuItem refresh;
	private MenuItem share;
	
	private ProgressBar mProgressBar;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private LinearLayout mDrawerView;
    private LinearLayout mDrawerProfile;
    private Profile mCurrentProfile;
    public enum ContentFrameType {PAGE,POST}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setThisAsCurrentHomeActivity();
		setContentView(R.layout.activity_home);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		if (savedInstanceState != null) {
	        currentPost = savedInstanceState.getInt(CURRENT_POST);
	    	currentPage = savedInstanceState.getInt(CURRENT_PAGE);
	        currentType = (ContentFrameType) savedInstanceState.getSerializable(CURRENT_TYPE);
        }		
		
		mProgressBar = (ProgressBar) findViewById(R.id.home_progressbar);
		mProgressBar.setVisibility(View.GONE);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerView = (LinearLayout) findViewById(R.id.drawer_view);
		mDrawerList = (ListView) findViewById(R.id.drawer_list);
		mDrawerProfile = (LinearLayout) findViewById(R.id.drawer_profile);
		
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
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, Pages.getPageTitles() ));
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				currentPage = position;
				replaceContentFrame(ContentFrameType.PAGE, position);
			}
		});
        mDrawerProfile.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getProfile().openDialog();
			}
		});
        
        invalidateContentFrame();
        invalidateProfile();
        refreshPosts();
	}


	@Override
	protected void onResume() {
		super.onResume();
		setThisAsCurrentHomeActivity();
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
		((GlobalState)GlobalState.getContext()).getYouTubeAdapter().releaseYoutubeLoaders();
	    if (mCurrentProfile != null) {
	    	mCurrentProfile.close();
	    }
		super.onDestroy();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
		getProfile().onActivityResult(this, requestCode, responseCode, intent);
		super.onActivityResult(requestCode, responseCode, intent);
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
		if (mDrawerLayout.isDrawerOpen(mDrawerView)) {
			mDrawerLayout.closeDrawer(mDrawerView);
			return;
		}
		// If currently the details of a post are being shown...
		Fragment fragment = (Fragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_CONTENT);
		if (fragment instanceof PostDetailFragment) {
			// close the YouTubePlayer's fullscreen in case it was fullscreen, else...
			if (((GlobalState)GlobalState.getContext()).getYouTubeAdapter().removeYouTubeFullscreen()){
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
        
        refresh.getIcon().setColorFilter(getResources().getColor(R.color.actionbar_foreground), PorterDuff.Mode.SRC_ATOP);
        share.getIcon().setColorFilter(getResources().getColor(R.color.actionbar_foreground), PorterDuff.Mode.SRC_ATOP);
        searchItem.getIcon().setColorFilter(getResources().getColor(R.color.actionbar_foreground), PorterDuff.Mode.SRC_ATOP);

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
        case R.id.settings:
    		startActivity(new Intent(this, SettingsActivity.class));
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void invalidateContentFrame() {
    	if (mProgressBar != null) {
    		mProgressBar.setVisibility(View.GONE);
    	}    		
    	if (currentType == null) {
    		currentType = ContentFrameType.PAGE;
        	replaceContentFrame(ContentFrameType.PAGE, currentPage);
    	} else if (currentType == ContentFrameType.POST) {
    		replaceContentFrame(ContentFrameType.POST, currentPost);
        } else if (currentType == ContentFrameType.PAGE){
        	Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_CONTENT);
        	if (frag instanceof PostOverviewFragment && Pages.getPageType(currentPage) == PageType.POSTS) {
        		if (((PostOverviewFragment)frag).invalidatePostOverview()) {
        			return;
        		};
        	}
    		if (mProgressBar != null && ((GlobalState)GlobalState.getContext()).isRefreshing() 
    				&& frag instanceof PostOverviewFragment && ((PostOverviewFragment)frag).getLastPosition() == 0) {
    			mProgressBar.setVisibility(View.VISIBLE);
    		}
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
			case PODCAST:
				simpleOptionsMenu(false);
				((GlobalState)GlobalState.getContext()).showOnlyFavorites(Pages.getPageType(currentPage) == Pages.PageType.FAVORITES);
				((GlobalState)GlobalState.getContext()).showPodcast(Pages.getPageType(currentPage) == Pages.PageType.PODCAST);
				fragment = new PostOverviewFragment();
				Bundle argsO = new Bundle();
				argsO.putInt(PostOverviewFragment.ARG_ID, currentPost);
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
		default:
			break;
		}
		replaceFragment(fragment);
	    mDrawerLayout.closeDrawer(mDrawerView);
	}
	
	private synchronized void replaceFragment(Fragment fragment) {
		if (fragment != null && this.equals(((GlobalState)GlobalState.getContext()).getCurrentHomeActivity())) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction()
		                   .replace(R.id.content_frame, fragment, TAG_FRAGMENT_CONTENT)
		                   .addToBackStack(null)
		                   .commit();
		}
	}
	
	private synchronized void setThisAsCurrentHomeActivity() {
        ((GlobalState)GlobalState.getContext()).setCurrentHomeActivity(this);
	}
	
	private synchronized void clearHomeActivityReference() {
		HomeActivity ha = ((GlobalState)GlobalState.getContext()).getCurrentHomeActivity();
		if (ha != null && ha.equals(this)) {
			((GlobalState)GlobalState.getContext()).setCurrentHomeActivity(null);
		}
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
			if (currentitemId == 0) {
				return null;
			} else {
				return (PostFragment) ((PostDetailFragment)f).getViewPagerItem(currentitemId);
			}
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
			dl.setDataSource(new AppConfig.APIURLBuilder(Function.GET_RECENT_POSTS).create());
		} catch (MalformedURLException e) {
			Log.d("Path incorrect", e.toString());
		}
		// Need to check if the MenuItem is already available, since this method is called in onCreate and onCreateOptionsMenu is called after onCreate has ended.
		if (refresh != null) {
			refresh.setActionView(R.layout.actionbar_indeterminate_progress);
		}
		if (mProgressBar != null) {
			Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_CONTENT);
			if (frag instanceof PostOverviewFragment && ((PostOverviewFragment)frag).getLastPosition() == 0) {
				mProgressBar.setVisibility(View.VISIBLE);
			}
		}
		dl.prepareAsync();
		
		// Also get the first podcast results, to prevent long loading screen when opening podcast page
		DataLoader podCastdl = new DataLoader();
		podCastdl.setAsPodcastLoader(true);
		podCastdl.isInSynchWithExistingPosts(false);
		podCastdl.setPodcastRange(0, AppConfig.getDefaultNrPerRequest());
		try {
			podCastdl.setDataSource(new AppConfig.APIURLBuilder(Function.GET_PODCAST_POSTS).create());
		} catch (MalformedURLException e) {
			Log.w("Path for podcast incorrect", e.toString());
		}
		podCastdl.prepareAsync();
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
	
	public void getMorePosts(int lastPostId,int currentPosition) {
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
		if (Pages.getPageType(currentPage) == PageType.PODCAST) {
			dl.isInSynchWithExistingPosts(false);
			dl.setAsPodcastLoader(true);
			dl.setPodcastRange(currentPosition, currentPosition + AppConfig.getDefaultNrPerRequest());
			try {
	 			dl.setDataSource(new AppConfig.APIURLBuilder(Function.GET_PODCAST_POSTS).create());
			} catch (MalformedURLException e) {
				Log.w("Path incorrect", e.toString());
			}
		} else {
			dl.isInSynchWithExistingPosts(true);
			try {
				if (lastPostId != 0) {
					dl.setDataSource(new AppConfig.APIURLBuilder(Function.GET_POSTS_AFTER).setPostId(lastPostId).create());
				} else {
					dl.setDataSource(new AppConfig.APIURLBuilder(Function.GET_RECENT_POSTS).create());
				}
			} catch (MalformedURLException e) {
				Log.w("Path incorrect", e.toString());
			}
		}
		// Need to check if the MenuItem is already available, since this method is called in onCreate and onCreateOptionsMenu is called after onCreate has ended.
		if (refresh != null) {
			refresh.setActionView(R.layout.actionbar_indeterminate_progress);
		}
		if (mProgressBar != null) {
			Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_CONTENT);
			if (frag instanceof PostOverviewFragment && ((PostOverviewFragment)frag).getLastPosition() == 0) {
				mProgressBar.setVisibility(View.VISIBLE);
			}
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
				if (!intentChecked) {
					Bundle b = getIntent().getExtras();
					if (b != null) {
						int intentId = b.getInt(ARG_POST_ID);

						if (intentId != 0) {
							currentPost = intentId;
							currentType = ContentFrameType.POST;
						}
					}
					intentChecked = true;
				}
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
				if (homeA.refresh != null) {
					homeA.refresh.setActionView(null);
				}
				if (homeA.mProgressBar != null) {
					homeA.mProgressBar.setVisibility(View.GONE);
				}

				saveLastPosition();
				invalidateContentFrame();
			}
		});
	}
	
	public Profile getProfile() {
		if (mCurrentProfile == null) {
			mCurrentProfile = Profile.getDefaultProfile();
			mCurrentProfile.setOnProfileChangedListener(new Profile.OnProfileChangedListener() {
				@Override
				public void onProfileChanged() {
					invalidateProfile();
				}
			});
		}
		return mCurrentProfile;
	}
	
	private void invalidateProfile() {
		Profile profile = getProfile();
		ImageView mProfileImage = (ImageView) findViewById(R.id.drawer_profile_picture);
		mProfileImage.setImageDrawable( profile.getIcon());
		TextView mProfileName = (TextView) findViewById(R.id.drawer_profile_name);
		mProfileName.setTypeface(null, Typeface.BOLD);
		TextView mProfileEmail = (TextView) findViewById(R.id.drawer_profile_email);
		mProfileEmail.setTypeface(null, Typeface.ITALIC);

		if (profile.isLoggedIn()) {
			mProfileName.setText(profile.getName());
			mProfileEmail.setText(profile.getEmail());
		} else {
			mProfileName.setText(R.string.signin);
			mProfileEmail.setText("");
		}
		// need to invalidate PostFragment because of the submit button in the comments section
		if (getCurrentPostFragment() != null) {
			getCurrentPostFragment().invalidatePostFragment();
		}
		
	}
	
	private void exitApplication() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
	    intent.addCategory(Intent.CATEGORY_HOME);
	    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startActivity(intent);
	    
	    // clean up the post collection (incl disk) to contain no more than the set max. 
	    int max = AppConfig.getMaxPostStored();
	    PostCollection.cleanUpPostCollection(max);
	    ((GlobalState)GlobalState.getContext()).setOldestSynchedPost(0);
	    PodCastPlayer.done();
	    this.finish();
	}
}


