package me.dibber.blablablapp.activities;

import java.util.HashMap;
import java.util.List;

import me.dibber.blablablapp.R;
import me.dibber.blablablapp.activities.HomeActivity.ContentFrameType;
import me.dibber.blablablapp.core.AppConfig;
import me.dibber.blablablapp.core.GlobalState;
import me.dibber.blablablapp.core.PostCollection;
import me.dibber.blablablapp.core.PostCollection.DrawableType;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;

public class PostOverviewFragment extends Fragment {
	
	public static final String ARG_ID = "last_id";
	public static final String PREF_POSTDATA = "Postdata"; 
	public static final String PREF_MOST_RECENT_POST = "most recent post";
	
	private PostCollection posts;
	private List<Integer> postsIds;
	private GridView mGridView;
	private PostOverviewAdapter mAdapter;
	
	private int posToSynch;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate( R.layout.fragment_post_overview, container, false);
		posts = ((GlobalState) GlobalState.getContext() ).getPosts();
		postsIds = posts.getAllPosts();
		if (postsIds.size() > 0) {
			SharedPreferences prefs = getActivity().getSharedPreferences(PREF_POSTDATA,Context.MODE_PRIVATE);
			prefs.edit().putInt(PREF_MOST_RECENT_POST, postsIds.get(0)).commit();
		}
		mGridView = (GridView)rootView.findViewById(R.id.gridview_post_overview);
		mAdapter = new PostOverviewAdapter(getActivity(), R.layout.item_post_overview, postsIds);
		
		mGridView.setAdapter(mAdapter);
		int lastId = getArguments().getInt(ARG_ID);
		if (lastId != 0) {
			int p = postsIds.indexOf(lastId);
			if (p > 0) {
				mGridView.setSelection(p);
			}
		}
		
		int lastSynchId = ((GlobalState)GlobalState.getContext()).getOldestSynchedPost();
		posToSynch = postsIds.indexOf(lastSynchId);
		
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				((HomeActivity) getActivity()).replaceContentFrame(ContentFrameType.POST, postsIds.get(position));
			}
		}); 
		
		
		mGridView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				
				// refreshing posts when reaching the bottom of the view
				if(firstVisibleItem + visibleItemCount >= totalItemCount) {
					if ( postsIds.size() > 0 ) {
						((HomeActivity) getActivity()).getMorePosts(postsIds.get(postsIds.size() - 1),totalItemCount);
					} else {
						((HomeActivity) getActivity()).getMorePosts(0,0);
					}
				}
				
				if (posToSynch != -1 && firstVisibleItem + visibleItemCount > posToSynch) {
					((HomeActivity) getActivity()).getMorePosts(postsIds.get(posToSynch),posToSynch);
				}
			}
		});
		
		
		return rootView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		invalidatePostOverview();
	}

	public boolean invalidatePostOverview() {
		if (postsIds == null || posts == null || mAdapter == null) {
			return false;
		}
		postsIds = posts.getAllPosts();
		if (postsIds.size() > 0) {
			SharedPreferences prefs = getActivity().getSharedPreferences(PREF_POSTDATA,Context.MODE_PRIVATE);
			int curRecentPost = prefs.getInt(PREF_MOST_RECENT_POST, 0);
			int newRecentPost = postsIds.get(0);
			if (curRecentPost == 0 || posts.getItemDate(curRecentPost) == null || 
					posts.getItemDate(newRecentPost).after(posts.getItemDate(curRecentPost))) {
				prefs.edit().putInt(PREF_MOST_RECENT_POST, newRecentPost).commit();
			}
		}
		int lastSynchId = ((GlobalState)GlobalState.getContext()).getOldestSynchedPost();
		posToSynch = postsIds.indexOf(lastSynchId);

		mAdapter.clear();
		mAdapter.addAll(postsIds);
		mAdapter.notifyDataSetChanged();
		return true;
	}
	
	public int getLastPosition() {
		if (mGridView == null || postsIds == null) {
			return 0;
		}		
		int firstPos = mGridView.getFirstVisiblePosition();		
		if (firstPos >= 0 && firstPos < postsIds.size()) {
			return postsIds.get(firstPos);
		} else { 
			return 0;
		}
	}
	
	private class PostOverviewAdapter extends ArrayAdapter<Integer> {
		
		private Activity activity;
		private String youTubeApiKey;
		private HashMap<View,YouTubeThumbnailLoader> loaders;

		private PostOverviewAdapter(Activity context, int resourceId, List<Integer> list) {
			super(context, resourceId, list);
			activity = context;
			loaders = ((GlobalState)GlobalState.getContext()).getYouTubeAdapter().getYouTubeThumbnailLoaderList();
	        youTubeApiKey = AppConfig.getYouTubeAPIKey();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			TextView mTitleView;
			TextView mMetaView;
			ImageView mFavoIconView;
			TextView mContEllipsView;
			ImageView mImageView;
			YouTubeThumbnailView mYouTubeView;
			String videoID;
			
			int postId;
			boolean isLandscape;
			
			LayoutInflater inflater = activity.getLayoutInflater();
			postId = postsIds.get(position);
			isLandscape = getActivity().getResources().getBoolean(R.bool.isLandscape);
			
			videoID = posts.getItemYouTubeVideoID(postId);
			View listItem = null;
			ViewHolder vh;
			
			// I don't trust the adapter for always giving me the correct view in the convertView (and for good reason).
			// Checking if the convertView is indeed the same view, same postId and orientation will ensure this.
			if (convertView != null) {
				if (((ViewHolder)convertView.getTag()).postId == postId && ((ViewHolder)convertView.getTag()).isLandscape == isLandscape) {
					listItem = convertView;
				}
			}
			
			if (listItem == null) {
				
				listItem = inflater.inflate(R.layout.item_post_overview, parent, false);
				
				vh = new ViewHolder();
				
				listItem.setTag(vh);
				
				mTitleView = (TextView) listItem.findViewById(R.id.list_title);
				mMetaView = (TextView) listItem.findViewById(R.id.list_meta);
				mFavoIconView = (ImageView) listItem.findViewById(R.id.list_favoIcon);
				mContEllipsView= (TextView) listItem.findViewById(R.id.list_short_content);
				mImageView = (ImageView) listItem.findViewById(R.id.list_image);
				mYouTubeView = (YouTubeThumbnailView) listItem.findViewById(R.id.list_youtube);
				
				vh.postId = postId;
				vh.isLandscape = getActivity().getResources().getBoolean(R.bool.isLandscape);
				vh.mTitleView = mTitleView;
				vh.mMetaView = mMetaView;
				vh.mFavoIconView = mFavoIconView;
				vh.mContEllipsView = mContEllipsView;
				vh.mImageView = mImageView;
				vh.mYouTubeView = mYouTubeView;
				vh.videoID = videoID;
				
			} else {
				vh = (ViewHolder) listItem.getTag();
				postId = vh.postId;
				mTitleView = vh.mTitleView;
				mMetaView= vh.mMetaView;
				mFavoIconView = vh.mFavoIconView;
				mContEllipsView = vh.mContEllipsView;
				mImageView = vh.mImageView;
				mYouTubeView = vh.mYouTubeView;
				videoID = vh.videoID;
			}
			
			if (mTitleView != null) {
				mTitleView.setMaxLines(2);
				mTitleView.setEllipsize(TruncateAt.END);
				mTitleView.setTypeface(null, Typeface.BOLD);
				mTitleView.setText(posts.getItemTitle(postId));
			}
			if (mMetaView != null) {
				mMetaView.setMaxLines(1);
				mMetaView.setEllipsize(TruncateAt.END);
				mMetaView.setTypeface(null, Typeface.ITALIC);
				mMetaView.setText(posts.getItemMeta(postId));
			}
			if (mFavoIconView != null) {
				if (posts.itemIsFavorite(postId)) {
					mFavoIconView.setImageResource(R.drawable.ic_action_favo);
				} else {
					mFavoIconView.setImageResource(R.drawable.ic_action_no_favo);
				}
				final int pid = postId;
				mFavoIconView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (posts.itemIsFavorite(pid)) {
							posts.setItemFavorite(pid, false);
							((ImageView) v).setImageResource(R.drawable.ic_action_no_favo);
						} else {
							posts.setItemFavorite(pid, true);
							((ImageView) v).setImageResource(R.drawable.ic_action_favo);
						}
					}
				});
			}
			if (mContEllipsView != null) {
				mContEllipsView.setMaxLines(4);
				mContEllipsView.setEllipsize(TruncateAt.END);
				mContEllipsView.setText(posts.getItemContentReplaceBreaks(postId));	
				mContEllipsView.setTypeface(null, Typeface.NORMAL);
				if (getResources().getBoolean(R.bool.isLandscape)) {
					final TextView tempContentView = mContEllipsView;
					final int tempPostId = postId;
					
					mContEllipsView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
						
						@Override
						public void onLayoutChange(View v, int left, int top, int right,
								int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
							int maxLines = (int) (tempContentView.getHeight() / tempContentView.getLineHeight()) ;
							tempContentView.setMaxLines(maxLines);
							tempContentView.setEllipsize(TruncateAt.END);
							tempContentView.setText(posts.getItemContentReplaceBreaks(tempPostId));	
							tempContentView.setTypeface(null, Typeface.NORMAL);
							tempContentView.removeOnLayoutChangeListener(this);
						}
					});
				}
			}
			if (mImageView != null) {
				PostCollection.setImage(mImageView, DrawableType.LIST_IMAGE, postId);
			}
			if (mYouTubeView != null) {
				mYouTubeView.setVisibility(View.GONE);
			}
			
			if (videoID != null) {
				if (mYouTubeView != null) {
					YouTubeThumbnailLoader loader = loaders.get(mYouTubeView);
					if (loader == null && mYouTubeView.getTag() == null) {
						mYouTubeView.setTag(vh);
						mYouTubeView.initialize(youTubeApiKey, ((GlobalState)GlobalState.getContext()).getYouTubeAdapter());
					} else if (loader != null) {
						loader.setVideo(videoID);
					}
				}
			}
			return listItem;
		}
	}
	
	public class ViewHolder {
		public TextView mTitleView;
		public TextView mMetaView;
		public TextView mContEllipsView;
		public ImageView mImageView;
		public ImageView mFavoIconView;
		public YouTubeThumbnailView mYouTubeView;
		public String videoID;
		public int postId;
		public boolean isLandscape;
		}
	
}
