package me.dibber.blablablapp;

import java.util.List;

import me.dibber.blablablapp.HomeActivity.ContentFrameType;
import me.dibber.blablablapp.PostCollection.DrawableType;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

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
	
	private class PostOverviewAdapter extends ArrayAdapter<Integer> {
		
		private Activity activity;

		private PostOverviewAdapter(Activity context, int resourceId, List<Integer> list) {
			super(context, resourceId, list);
			activity = context;
		}
		
		@Override
		public View getView(int position, View container, ViewGroup parent) {
			
			LayoutInflater inflater = activity.getLayoutInflater();
			View listItem = inflater.inflate(R.layout.item_post_overview, null, true);
			//TODO : use View Holder pattern (use recycled view passed into this method as the second parameter) for smoother scrolling
			int postId = postsIds.get(position);
			
			TextView mTitleView = (TextView) listItem.findViewById(R.id.list_title);
			mTitleView.setMaxLines(2);
			mTitleView.setEllipsize(TruncateAt.END);
			mTitleView.setTypeface(null, Typeface.BOLD);
			mTitleView.setText(posts.getItemTitle(postId));
			
			TextView mMetaView = (TextView) listItem.findViewById(R.id.list_meta);
			mMetaView.setMaxLines(1);
			mMetaView.setEllipsize(TruncateAt.END);
			mMetaView.setTypeface(null, Typeface.ITALIC);
			mMetaView.setText(posts.getItemMeta(postId));
					
			TextView mContEllipsView= (TextView) listItem.findViewById(R.id.list_short_content);
			mContEllipsView.setMaxLines(4);
			mContEllipsView.setEllipsize(TruncateAt.END);
			mContEllipsView.setText(posts.getItemContentReplaceBreaks(postId));	
			mContEllipsView.setTypeface(null, Typeface.NORMAL);
			
			ImageView mImageView = (ImageView) listItem.findViewById(R.id.list_image);
			PostCollection.setImage(mImageView, DrawableType.LIST_IMAGE, postId);
			
			return listItem;
		}
	}
}
