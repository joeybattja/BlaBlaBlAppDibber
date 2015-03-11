package me.dibber.blablablapp;

import java.util.List;
import java.util.Properties;

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
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
		public void onPageScrollStateChanged(int position) { }

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

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
		private ImageView mFavoView;
		private FrameLayout mYouTubeFrame;
		private PostYouTubeFragment mYouTubeFragment;
		private FrameLayout mCommentFrame;
		private CommentsFragment mCommentsFragment;
		private String videoId;
		
		private TextView mTitleView;
		private TextView mMetaView;
		private TextView mContentView;
		private int postId;
		
		private int marginWidth;
		private int marginHeight;
		
		
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			postId = getArguments().getInt(ARG_ID);
			posts = ((GlobalState) GlobalState.getContext() ).getPosts();
			videoId = posts.getItemYouTubeVideoID(postId);
			
			View rootView = inflater.inflate(R.layout.fragment_post_details, container, false);
			
			mTitleView = (TextView) rootView.findViewById(R.id.postTitle);
			mMetaView = (TextView) rootView.findViewById(R.id.postMeta);
			mFavoView = (ImageView) rootView.findViewById(R.id.postFavoIcon);
			mImageView = (ImageView) rootView.findViewById(R.id.postImageView);
			mContentView = (TextView) rootView.findViewById(R.id.postContent);
			mYouTubeFrame = (FrameLayout) rootView.findViewById(R.id.postYouTubeFrame);
			mCommentFrame = (FrameLayout) rootView.findViewById(R.id.postCommentFrame);
			
			if (mTitleView != null) {
				mTitleView.setTypeface(null, Typeface.BOLD);
				mTitleView.setText(posts.getItemTitle(postId));
			}
			if (mMetaView != null) {
				mMetaView.setTypeface(null, Typeface.ITALIC);
				mMetaView.setText(posts.getItemMeta(postId));
			}
			
			if (mFavoView != null) {
				if (posts.itemIsFavorite(postId)) {
					mFavoView.setImageResource(R.drawable.ic_action_favo);
				} else {
					mFavoView.setImageResource(R.drawable.ic_action_no_favo);
				}
				mFavoView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (posts.itemIsFavorite(postId)) {
							posts.setItemFavorite(postId, false);
							mFavoView.setImageResource(R.drawable.ic_action_no_favo);
						} else {
							posts.setItemFavorite(postId, true);
							mFavoView.setImageResource(R.drawable.ic_action_favo);
						}
					}
				});
			}
			
			if (mContentView != null) {
				mContentView.setText(posts.getItemContent(postId));
			}
			
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
			if (mYouTubeFrame != null) {
				if (videoId == null) {
					mYouTubeFrame.setVisibility(View.GONE);
				} else {
					mYouTubeFragment = PostYouTubeFragment.newInstance(videoId);
					getChildFragmentManager().beginTransaction().replace(R.id.postYouTubeFrame, mYouTubeFragment).commit();
				}
			}
			
			if (mCommentFrame != null) {
				if ( ((GlobalState)GlobalState.getContext()).optionReadComments() && posts.itemCommentsOpen(postId)) {
					mCommentsFragment = new CommentsFragment();
					Bundle args = new Bundle();
					args.putInt(ARG_ID, postId);
					mCommentsFragment.setArguments(args);
					getChildFragmentManager().beginTransaction().replace(R.id.postCommentFrame, mCommentsFragment).commit();
				} else {
					mCommentFrame.setVisibility(View.GONE);
				}
			}
			return rootView;
		}
		
		public void updateTextMargins() {
			
			if (mImageView != null) {
				
				mImageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					 @SuppressLint("NewApi")
					 @SuppressWarnings("deprecation")
					 @Override
					  public void onGlobalLayout() {
						 int w = mImageView.getMeasuredWidth();
						 int h = mImageView.getMeasuredHeight();
						 if (posts.countImages(postId) > 1) {
							 double maxRatio = posts.maxImageRatio(postId);
							 if (maxRatio != 0) {
								 h = (int) (w * maxRatio);
							 }
						 }
						 ViewGroup.LayoutParams params = mImageView.getLayoutParams();
						 int width = params.width;
						 						 
						 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
							 mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);							 
						 } else {
							 mImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						 }
						 
						 if ((GlobalState.getContext().getResources().getBoolean(R.bool.isLandscape))) {
							 mImageView.setLayoutParams(new RelativeLayout.LayoutParams(width, h));
							 onUpdateTextMarginsDone(w,h);
						 } else {
							 mImageView.setLayoutParams(new LinearLayout.LayoutParams(width, h));

						 }
					 }
				});
			} 
		}
		
		public void onUpdateTextMarginsDone(int imgWidth, int imgHeight) {
			
			if (imgWidth == marginWidth && imgHeight == marginHeight) {
				return;
			}
			marginWidth = imgWidth;
			marginHeight = imgHeight;

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
					if (linesCount <= lines) {
						spanS.setSpan(new ImageMarginSpan(lines, marginWidth), 0, spanS.length(), 0);
						
					} else {
						
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
		
		/**
		 * Sets the YouTubePlayer to fullscreen or back depending on the parameter.
		 * If the PostFragment does not have a YouTubePlayer active or if this call will have no effect (e.g. it is already fullscreen) 
		 * this will return false 
		 * @param fullscreen true to set the player fullscreen, false otherwise
		 * @return true if it changes the player to or from fullscreen, false otherwise; also false if the PostFragment does not have an active YouTubePlayer.
		 */
		public boolean setYouTubeFullscreen(boolean fullscreen) {
			if (mYouTubeFragment != null && mYouTubeFragment.youTubePlayer != null) {
				if (mYouTubeFragment.isFullscreen != fullscreen) {
					mYouTubeFragment.youTubePlayer.setFullscreen(fullscreen);
					return true;
				}
			}
			return false;
		}
		
	}
	
	public static class PostYouTubeFragment extends YouTubePlayerSupportFragment {
		
		private static String youTubeApiKey;
		public static String VIDEO_ID = "YouTube ID";
	    public YouTubePlayer youTubePlayer;
	    public boolean isFullscreen;
	    private static String youTubeVideo;

	    public static PostYouTubeFragment newInstance(String videoID) {

	    	PostYouTubeFragment youTubeFragment = new PostYouTubeFragment();

	        Bundle bundle = new Bundle();
	        bundle.putString(VIDEO_ID, videoID);
	        youTubeFragment.setArguments(bundle);
	        youTubeVideo = videoID;
	        
	        Properties p = AssetsPropertyReader.getProperties(GlobalState.getContext());
	        youTubeApiKey = p.getProperty("YOUTUBE_API_KEY");
	        
	        youTubeFragment.init();
	        
	        return youTubeFragment;
	    }

	    private void init() {

	        initialize(youTubeApiKey, new YouTubePlayer.OnInitializedListener() {

	            @Override
	            public void onInitializationFailure(Provider provider, YouTubeInitializationResult error) { }

	            @Override
	            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
	            	youTubePlayer = player;
	                
	                youTubePlayer.setFullscreenControlFlags(0);
	                youTubePlayer.setOnFullscreenListener(new YouTubePlayer.OnFullscreenListener() {
						
						@Override
						public void onFullscreen(boolean isFullscr) {
							youTubePlayer.setFullscreen(isFullscr);
							isFullscreen = isFullscr;
						}
					});
	                if (!wasRestored) {
	                	String currentVideo = ((GlobalState)GlobalState.getContext()).getCurrentYouTubeVideo();
	                	if (currentVideo != null && currentVideo.equals(youTubeVideo)) {
	            			youTubePlayer.loadVideo(youTubeVideo, ((GlobalState)GlobalState.getContext()).getYouTubeCurrentTimeMilis());
	                	} else {
		                	youTubePlayer.cueVideo(youTubeVideo);
	                	}
	                }
	            }
	        });
	    }

		@Override
		public void onStop() {
			if (youTubePlayer != null && youTubePlayer.isPlaying()) {
				((GlobalState)GlobalState.getContext()).setCurrentYouTubeVideoTime(youTubeVideo, youTubePlayer.getCurrentTimeMillis());
			}
			super.onStop();
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
