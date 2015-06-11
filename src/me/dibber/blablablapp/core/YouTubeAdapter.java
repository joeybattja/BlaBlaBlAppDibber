package me.dibber.blablablapp.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import me.dibber.blablablapp.activities.PostOverviewFragment.ViewHolder;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailLoader.ErrorReason;
import com.google.android.youtube.player.YouTubeThumbnailView;

public class YouTubeAdapter implements YouTubeThumbnailView.OnInitializedListener, YouTubePlayer.OnInitializedListener {
	
	private HashMap<View,YouTubeThumbnailLoader> youTubeThumbnailLoaders;
	private HashSet<YouTubePlayer> youTubePlayers;
	private YouTubePlayer currentYouTubePlayer;
	private String currentYouTubeVideo;
	private int currentYouTubeTime;
	private boolean isFullScreen;
	
	public HashMap<View,YouTubeThumbnailLoader> getYouTubeThumbnailLoaderList() {
		if (youTubeThumbnailLoaders == null) {
			youTubeThumbnailLoaders = new HashMap<View,YouTubeThumbnailLoader>();
		}
		return youTubeThumbnailLoaders;
	}
	
	public HashSet<YouTubePlayer> getYouTubePlayersList() {
		if (youTubePlayers == null) {
			youTubePlayers = new HashSet<YouTubePlayer>();
		}
		return youTubePlayers;
	}
	
	
	
	public void releaseYoutubeLoaders() {
	    if (youTubeThumbnailLoaders != null) {
		    for (Entry<View, YouTubeThumbnailLoader> entry : youTubeThumbnailLoaders.entrySet() ) {
		    	entry.getValue().release();
		    }
		    youTubeThumbnailLoaders.clear();
	    }
	    if (youTubePlayers != null) {
		    for (YouTubePlayer pl: youTubePlayers) {
		    	if (pl != null) {
		    		pl.release();
		    	}
		    }
		    youTubePlayers.clear();
	    }
	}
	
	public boolean removeYouTubeFullscreen() {
		if (isFullScreen && currentYouTubePlayer != null) {
			currentYouTubePlayer.setFullscreen(false);
			return true;
		}
		return false;
	}

	

	@Override
	public void onInitializationFailure(YouTubeThumbnailView thumbnailView, YouTubeInitializationResult YouTubeInitializationResult) {	}

	@Override
	public void onInitializationSuccess(YouTubeThumbnailView thumbnailView,	YouTubeThumbnailLoader thumbnailLoader) {
		final ViewHolder vh = (ViewHolder) thumbnailView.getTag();
		if (vh == null) {
			return;
		}
		thumbnailLoader.setOnThumbnailLoadedListener(new YouTubeThumbnailLoader.OnThumbnailLoadedListener() {
			
			@Override
			public void onThumbnailLoaded(YouTubeThumbnailView thumbnail, String videoId) {
				ViewHolder vh = (ViewHolder) thumbnail.getTag();
				if (vh.mImageView != null) {
					int desiredWidth = vh.mImageView.getMeasuredWidth();
					int desiredHeight = vh.mImageView.getMeasuredHeight();
					vh.mImageView.setVisibility(View.GONE);
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(desiredWidth, desiredHeight);
					thumbnail.setLayoutParams(params);
				}
				thumbnail.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onThumbnailError(YouTubeThumbnailView a, ErrorReason reason) {}
		});
		getYouTubeThumbnailLoaderList().put(thumbnailView, thumbnailLoader);
		thumbnailLoader.setVideo(vh.videoID);
		
	}

	@Override
	public void onInitializationFailure(Provider provider, YouTubeInitializationResult error) { }

	@Override
	public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
		final String vidID = ((PostYouTubeFragment) provider).getArguments().getString(PostYouTubeFragment.VIDEO_ID);
		
		getYouTubePlayersList().add(player);
        final YouTubePlayer fPlayer = player;
        fPlayer.setFullscreenControlFlags(0);
        fPlayer.setOnFullscreenListener(new YouTubePlayer.OnFullscreenListener() {
			
			@Override
			public void onFullscreen(boolean isFullscr) {
				fPlayer.setFullscreen(isFullscr);
				currentYouTubePlayer = fPlayer;
				isFullScreen = isFullscr;
			}
		});
        fPlayer.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
			
			@Override
			public void onStopped() {
				currentYouTubeTime = fPlayer.getCurrentTimeMillis();				
			}
			
			@Override
			public void onSeekTo(int newPositionMillis) {
				currentYouTubePlayer = fPlayer;
				currentYouTubeVideo = vidID;
				currentYouTubeTime = newPositionMillis;

			}
			
			@Override
			public void onPlaying() { 
				currentYouTubePlayer = fPlayer;
				currentYouTubeVideo = vidID;
			}
			
			@Override
			public void onPaused() {
				currentYouTubeTime = fPlayer.getCurrentTimeMillis();
			}
			
			@Override
			public void onBuffering(boolean isBuffering) { }
		});
        if (!wasRestored) {
        	if (currentYouTubeVideo != null && currentYouTubeVideo.equals(vidID) && currentYouTubeTime != 0) {
        		fPlayer.loadVideo(vidID, currentYouTubeTime);
        	} else {
        		fPlayer.cueVideo(vidID,0);
        	}
        }
    }
	
	public static class PostYouTubeFragment extends YouTubePlayerSupportFragment {
		
		private static String youTubeApiKey;
		public static String VIDEO_ID = "YouTube ID";

	    public static PostYouTubeFragment newInstance(String videoID) {

	    	PostYouTubeFragment youTubeFragment = new PostYouTubeFragment();
	    	Bundle bundle = new Bundle();
	        bundle.putString(VIDEO_ID, videoID);
	    	youTubeFragment.setArguments(bundle);

	        youTubeApiKey = AppConfig.getYouTubeAPIKey();
	        
	        youTubeFragment.initialize(youTubeApiKey, (((GlobalState)GlobalState.getContext()).getYouTubeAdapter()));
	        
	        return youTubeFragment;
	    }

	}

}
