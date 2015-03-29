package me.dibber.blablablapp.activities;

import me.dibber.blablablapp.core.GlobalState;
import me.dibber.blablablapp.core.PostCollection;
import me.dibber.blablablapp.core.PostCollection.DrawableType;
import me.dibber.blablablapp.ext.TouchImageView;
import me.dibber.blablablapp.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ViewImageFullScreenActivity extends FragmentActivity {
	
	public static String POSTID = "postId";
	
	private static int post;
	
	private static PostCollection posts;
	private ViewPager mViewPager;
	private ImageFullScreenPagerAdapter mAdapter;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		post = i.getIntExtra(POSTID, 0);
		posts = ((GlobalState) GlobalState.getContext() ).getPosts();
		
		mViewPager = new ViewPager(this);
		mViewPager.setId(R.id.viewpager1);
		mAdapter = new ImageFullScreenPagerAdapter(getSupportFragmentManager());
		mViewPager.setAdapter(mAdapter);
		setContentView(mViewPager);
		
	}
	
	private static class ImageFullScreenPagerAdapter extends FragmentPagerAdapter {

		public ImageFullScreenPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			ImageFullScreenFragment fr = new ImageFullScreenFragment();
			Bundle args = new Bundle();
			args.putInt("position", i);
			fr.setArguments(args);
			return fr;
		}

		@Override
		public int getCount() {
			return posts.countImages(post);
		}
	}
	
	public static class ImageFullScreenFragment extends Fragment {
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.activity_view_image_full_screen, container, false);
			int position = getArguments().getInt("position");
			TouchImageView imgView = (TouchImageView)rootView.findViewById(R.id.img_fullscreen);
			PostCollection.setImage(imgView, DrawableType.FULLSCREEN_IMAGE, post, position); 
			
			return rootView;
		}
		
	}
}
