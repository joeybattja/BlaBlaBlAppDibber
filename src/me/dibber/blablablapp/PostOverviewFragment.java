package me.dibber.blablablapp;

import java.util.HashMap;
import java.util.List;

import me.dibber.blablablapp.HomeActivity.ContentFrameType;
import me.dibber.blablablapp.PostCollection.DrawableType;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;
import com.google.android.youtube.player.YouTubeThumbnailView.OnInitializedListener;

public class PostOverviewFragment extends Fragment {
	
	public static final String ARG_ID = "last_id";
	
	private PostCollection posts;
	private List<Integer> postsIds;
	private GridView mGridView;
	private PostOverviewAdapter adapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate( R.layout.fragment_post_overview, container, false);
		posts = ((GlobalState) GlobalState.getContext() ).getPosts();
		postsIds = posts.getAllPosts();
		
		mGridView = (GridView)rootView.findViewById(R.id.gridview_post_overview);
		adapter = new PostOverviewAdapter(getActivity(), R.layout.item_post_overview, postsIds);
		
		mGridView.setAdapter(adapter);
		int lastId = getArguments().getInt(ARG_ID);
		if (lastId != 0) {
			int p = postsIds.indexOf(lastId);
			if (p > 0) {
				mGridView.setSelection(p);
			}
		}
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
				if(firstVisibleItem + visibleItemCount >= totalItemCount) {
					if ( postsIds.size() > 0 ) {
						((HomeActivity) getActivity()).getMorePosts(postsIds.get(postsIds.size() - 1));
					}
				}
			}
		});
		
		
		return rootView;
	}
	
	public int getLastPosition() {
		if (mGridView == null || postsIds == null)
			return 0;
		return postsIds.get(mGridView.getFirstVisiblePosition());
	}
	
	private class PostOverviewAdapter extends ArrayAdapter<Integer> implements OnInitializedListener {
		
		private Activity activity;
		private final static String YOUTUBE_API_KEY = "AIzaSyD7xWiQl4I8KW987uZyns8qma0eWfCY_8c";
		HashMap<View,YouTubeThumbnailLoader> loaders;

		private PostOverviewAdapter(Activity context, int resourceId, List<Integer> list) {
			super(context, resourceId, list);
			activity = context;
			loaders = ((GlobalState)GlobalState.getContext()).getYouTubeThumbnailLoaderList();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			TextView mTitleView;
			TextView mMetaView;
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
			
				if (videoID == null) {
					listItem = inflater.inflate(R.layout.item_post_overview, parent, false);
				} else {
					listItem = inflater.inflate(R.layout.item_post_overview_youtube, parent, false);
				}
				
				vh = new ViewHolder();
				
				listItem.setTag(vh);
				
				mTitleView = (TextView) listItem.findViewById(R.id.list_title);
				mMetaView = (TextView) listItem.findViewById(R.id.list_meta);
				mContEllipsView= (TextView) listItem.findViewById(R.id.list_short_content);
				mImageView = (ImageView) listItem.findViewById(R.id.list_image);
				mYouTubeView = (YouTubeThumbnailView) listItem.findViewById(R.id.list_youtube);
				
				vh.postId = postId;
				vh.isLandscape = getActivity().getResources().getBoolean(R.bool.isLandscape);
				vh.mTitleView = mTitleView;
				vh.mMetaView = mMetaView;
				vh.mContEllipsView = mContEllipsView;
				vh.mImageView = mImageView;
				vh.mYouTubeView = mYouTubeView;
				vh.videoID = videoID;
			} else {
				vh = (ViewHolder) listItem.getTag();
				postId = vh.postId;
				mTitleView = vh.mTitleView;
				mMetaView= vh.mMetaView;
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
			if (mContEllipsView != null) {
				mContEllipsView.setMaxLines(4);
				mContEllipsView.setEllipsize(TruncateAt.END);
				mContEllipsView.setText(posts.getItemContentReplaceBreaks(postId));	
				mContEllipsView.setTypeface(null, Typeface.NORMAL);
			}
			if (mImageView != null) {
				PostCollection.setImage(mImageView, DrawableType.LIST_IMAGE, postId);
			}
			if (mYouTubeView != null) {
				YouTubeThumbnailLoader loader = loaders.get(mYouTubeView);
				if (loader == null && mYouTubeView.getTag() == null) {
					mYouTubeView.setTag(videoID);
					mYouTubeView.initialize(YOUTUBE_API_KEY, this);
				} else if (loader != null) {
					loader.setVideo(videoID);
					updateLayoutYouTubeThumbnail(mYouTubeView);
				}
			}
			
			return listItem;
		}
		
		private class ViewHolder {
			public TextView mTitleView;
			public TextView mMetaView;
			public TextView mContEllipsView;
			public ImageView mImageView;
			public YouTubeThumbnailView mYouTubeView;
			public String videoID;
			public int postId;
			public boolean isLandscape;
			}

		@Override
		public void onInitializationFailure(YouTubeThumbnailView thumbnailView,
				YouTubeInitializationResult error) {
		}

		@Override
		public void onInitializationSuccess(YouTubeThumbnailView thumbnailView,
				YouTubeThumbnailLoader thumbnailLoader) {
			String id = (String) thumbnailView.getTag();
			loaders.put(thumbnailView, thumbnailLoader);
			thumbnailLoader.setVideo(id);
			updateLayoutYouTubeThumbnail(thumbnailView);
		}
	}
	
	
	// Method to make sure the YouTubeThumbnailView is adjusted to a 4*3 aspect ratio.
	// Should be called every time a YouTubeThumbnailView is created. Else it won't fit nicely in the overview screen.
	static void updateLayoutYouTubeThumbnail(final YouTubeThumbnailView youTubeView) {
		if (youTubeView == null) {
			return;
		}
		youTubeView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			 @SuppressLint("NewApi")
			 @SuppressWarnings("deprecation")
			 @Override
			  public void onGlobalLayout() {
				 int w = youTubeView.getMeasuredWidth();
				 int h = (int) (w * 0.75);
				 
				 youTubeView.setMinimumHeight(h);
				 
				 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
					 youTubeView.getViewTreeObserver().removeOnGlobalLayoutListener(this);							 
				 } else {
					 youTubeView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				 }
			 }
		});
	}
}
