package me.dibber.blablablapp.core;

import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.view.View;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;

public class PodCastPlayer extends MediaPlayer implements MediaPlayerControl, OnPreparedListener {
	
	private static PodCastPlayer sPodCastPlayer;
	
	private MediaController mController;
	private String AudioURL;
	private boolean isPrepared;
	
	private PodCastPlayer() { 
		this.setOnPreparedListener(this);
	}
	
	private void init(View view, String audioURL) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		if (mController != null) {
			mController.hide();
		}
		reset();
		isPrepared = false;
		sPodCastPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		sPodCastPlayer.setDataSource(audioURL);
		this.AudioURL = audioURL;
		sPodCastPlayer.setMediaController(view);
		sPodCastPlayer.prepareAsync();
	}
	
	private void setMediaController(View view) {
		mController = new MediaController(view.getContext());
		mController.setMediaPlayer(this);
		mController.setAnchorView(view);
		view.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (isPrepared) {
					if (mController.isShowing()) {
						mController.hide();
					} else {
						mController.show();
					}
				}
			}
		});
		mController.setEnabled(true);
	}
	
	public static void playPodcast(View view, String audioURL) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		if (sPodCastPlayer == null) {
			sPodCastPlayer = new PodCastPlayer();
		}
		if (sPodCastPlayer.AudioURL != null && sPodCastPlayer.AudioURL.equals(audioURL)) {
			return;
		}
		sPodCastPlayer.init(view, audioURL);
	}
	
	public static void done() {
		if (sPodCastPlayer != null) {
			sPodCastPlayer.release();
			sPodCastPlayer = null;
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		isPrepared = true;
		mController.show();
	}

	@Override
	public int getBufferPercentage() {
		if (getCurrentPosition() == 0) 
			return 0;
		return (getCurrentPosition()*100)/getDuration();
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}
	

}
