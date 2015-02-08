package me.dibber.blablablapp;

import java.util.List;

import me.dibber.blablablapp.PostCollection.DrawableType;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

public class PostDetailFragment extends Fragment {
	
	public static final String ARG_ID = "post_id";
	private int currentId;
	
	private List<Integer> postIds;
	private ViewPager mViewPager;
	private PostPagerAdapter mPagerAdapter;
	private static PostCollection posts;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate( R.layout.fragment_post_pager, container, false);
		posts = ((GlobalState) GlobalState.getContext() ).getPosts();
		postIds = posts.getAllPosts();
		
		mPagerAdapter = new PostPagerAdapter(getChildFragmentManager());
		mViewPager = (ViewPager) rootView.findViewById(R.id.post_pager);
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOnPageChangeListener(mPagerAdapter);
		currentId = getArguments().getInt(ARG_ID);
		if (postIds.indexOf(currentId) > 0) {
			mViewPager.setCurrentItem(postIds.indexOf(currentId));
		}
		return rootView;
	}
	
	public int getViewPagerCurrentItem() {
		return postIds.get(mViewPager.getCurrentItem());
	}
	
	public Fragment getViewPagerItem(int postId) {
		return mPagerAdapter.getActiveFragment(postId);
	}
	
	private class PostPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {
		
		private SparseArray<Fragment> activeFragments;

		public PostPagerAdapter(FragmentManager fm) {
			super(fm);
			activeFragments = new SparseArray<Fragment>();
		}
		
		@Override
		public Fragment getItem(int position) {
			PostFragment f = new PostFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_ID, postIds.get(position));
			f.setArguments(args);
			return f;
		}
		
		@Override
	    public Object instantiateItem(ViewGroup container, int position) {
	        Fragment f = (Fragment) super.instantiateItem(container, position);
	        activeFragments.put(postIds.get(position), f);
	        return f;
	    }

		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			activeFragments.remove(postIds.get(position));
			super.destroyItem(container, position, object);
		}
		

		@Override
		public int getCount() {
			if (postIds != null) {
				return postIds.size();
			}
			return 0;
		}
		
		@Override
		public CharSequence getPageTitle(int position) {
			if (postIds != null)
				return posts.getItemTitle(postIds.get(position));
			return null;
		}

		@Override
		public void onPageScrollStateChanged(int position) {
			
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			
		}

		@Override
		public void onPageSelected(int position) {
			((HomeActivity) getActivity()).setTitle( 
					posts.getItemTitle(postIds.get(position)));
		}
		
		public Fragment getActiveFragment(int postId) {
			return activeFragments.get(postId);
		}

	}
	
	public static class PostFragment extends Fragment {
		private ImageView mImageView;
		private FrameLayout mYouTubeFrame;
		private PostYouTubeFragment mYouTubeFragment;
		private String videoId;
		
		private TextView mTitleView;
		private TextView mMetaView;
		private TextView mContentView;
		private int postId;
		
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			postId = getArguments().getInt(ARG_ID);
			posts = ((GlobalState) GlobalState.getContext() ).getPosts();
			videoId = posts.getItemYouTubeVideoID(postId);
			
			View rootView = null;
			if (videoId != null) {
				rootView = inflater.inflate(R.layout.fragment_post_details_youtube, container, false);
			} else {
				rootView = inflater.inflate(R.layout.fragment_post_details, container, false);
			}
			
			mTitleView = (TextView) rootView.findViewById(R.id.postTitle);
			mMetaView = (TextView) rootView.findViewById(R.id.postMeta);
			mImageView = (ImageView) rootView.findViewById(R.id.postImageView);
			mContentView = (TextView) rootView.findViewById(R.id.postContent);
			mYouTubeFrame = (FrameLayout) rootView.findViewById(R.id.postYouTubeFrame);
			
			if (mYouTubeFrame != null) {
				mYouTubeFragment = PostYouTubeFragment.newInstance(videoId);
				getChildFragmentManager().beginTransaction().replace(R.id.postYouTubeFrame, mYouTubeFragment).commit();
				updateTextMargins();
			}
			
			mContentView.setText(posts.getItemContent(postId));
			
			if (mImageView != null) {
				PostCollection.setImage(mImageView, DrawableType.POST_IMAGE, postId, new PostCollection.SetImageListener() {
					
					@Override
					public void onImageSet() {
						updateTextMargins();
						
					}
				});
				mImageView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (posts.countImages(postId) == 0) {
							return;
						} else {
							Intent i = new Intent((GlobalState) GlobalState.getContext(), ViewImageFullScreenActivity.class);
							i.putExtra(ViewImageFullScreenActivity.POSTID, postId);
							startActivity(i);
						}
					}
				});
			}
			
			if (mTitleView != null) {
				mTitleView.setTypeface(null, Typeface.BOLD);
				mTitleView.setText(posts.getItemTitle(postId));
			}
			if (mMetaView != null) {
				mMetaView.setTypeface(null, Typeface.ITALIC);
				mMetaView.setText(posts.getItemMeta(postId));
			}
			
			
			return rootView;
		}
		
		public void updateTextMargins() {
			if (!(getResources().getBoolean(R.bool.isLandscape))) {
				return;
			}
			
			if (mImageView != null) {
				mImageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					 @SuppressLint("NewApi")
					 @SuppressWarnings("deprecation")
					 @Override
					  public void onGlobalLayout() {
						 int w = mImageView.getMeasuredWidth();
						 int h = mImageView.getMeasuredHeight();
						 
						 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
							 mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);							 
						 } else {
							 mImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						 }
						 
						 onUpdateTextMarginsDone(w,h);
					 }
				});
			} 
		}
		
		public void onUpdateTextMarginsDone(int imgWidth, int imgHeight) {
			
			
			DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
			float dp = 10f;
			int marginPixels = (int) (metrics.density * dp + 0.5f);
			
			imgWidth += marginPixels;
			imgHeight += marginPixels;
			
			if (mContentView == null) {
				return;
			}
			
			float textSize = mContentView.getPaint().getTextSize();
			final int lines = (int) ( (double)imgHeight / textSize);
			
			if (lines <= 2) {
				return;
			}
			
			final int marginWidth = imgWidth;
			
			final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mContentView.getLayoutParams();
	        params.setMargins(marginWidth, 0, 0, 0);
	        mContentView.setText(posts.getItemContent(postId) );
	        
	        mContentView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				
	        	@SuppressLint("NewApi")
				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {
					
	        		if (mContentView.getLayout() == null) {
	        			return;
	        		}
	        		
					int linesCount = mContentView.getLayout().getLineCount();
					SpannableString spanS =  new  SpannableString ( posts.getItemContent(postId) );
					Log.d("lines " + postId, "" + lines);
					if (linesCount <= lines) {
						spanS.setSpan(new ImageMarginSpan(lines, marginWidth), 0, spanS.length(), 0);
						
					} else {
						
						Log.d("linesCount " + postId, "" + linesCount);
						int breakpoint = mContentView.getLayout().getLineEnd(lines-2);
						Spannable s1 = new SpannableStringBuilder(spanS, 0, breakpoint);
					    s1.setSpan(new ImageMarginSpan(lines, marginWidth), 0, s1.length(), 0);
					    Spannable s2 = new SpannableStringBuilder(System.getProperty("line.separator"));
					    Spannable s3 = new SpannableStringBuilder(spanS, breakpoint, spanS.length());
					    s3.setSpan(new ImageMarginSpan(0, 0), 0, s3.length(), 0);
					    spanS = new  SpannableString (TextUtils.concat(s1, s2, s3));
					}
					params.setMargins(0, 0, 0, 0);
					mContentView.setText(spanS);
					
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
						mContentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);							 
					 } else {
						 mContentView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					 }
				}
			});
		}
	}
	
	public static class PostYouTubeFragment extends YouTubePlayerSupportFragment {
		
		private final static String YOUTUBE_API_KEY = "AIzaSyD7xWiQl4I8KW987uZyns8qma0eWfCY_8c";
		public static String VIDEO_ID = "YouTube ID";
	    private YouTubePlayer mPlayer;

	    public static PostYouTubeFragment newInstance(String videoID) {

	    	PostYouTubeFragment youTubeFragment = new PostYouTubeFragment();

	        Bundle bundle = new Bundle();
	        bundle.putString(VIDEO_ID, videoID);

	        youTubeFragment.setArguments(bundle);
	        youTubeFragment.init();

	        return youTubeFragment;
	    }

	    private void init() {

	        initialize(YOUTUBE_API_KEY, new YouTubePlayer.OnInitializedListener() {

	            @Override
	            public void onInitializationFailure(Provider provider, YouTubeInitializationResult error) { }

	            @Override
	            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
	                mPlayer = player;
	                if (!wasRestored) {
	                	mPlayer.cueVideo(getArguments().getString(VIDEO_ID));
	                }
	            }
	        });
	    }
	}
			
	static class ImageMarginSpan implements LeadingMarginSpan.LeadingMarginSpan2 {
		
		private int margin;
		private int lines;
		
		public ImageMarginSpan(int lines, int margin) {
			this.margin = margin;
			this.lines = lines;
		}

		@Override
		public void drawLeadingMargin(Canvas c, Paint p, int x,
				int dir, int top, int baseline, int bottom, CharSequence text,
				int start, int end, boolean first, Layout layout) {
		}

		@Override
		public int getLeadingMargin(boolean first) {
			if (first) {
				return margin;
			} else {
				return 0;
			}
		}

		@Override
		public int getLeadingMarginLineCount() {
			return lines;
		}
	}
}
