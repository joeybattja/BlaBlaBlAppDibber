package me.dibber.blablablapp.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import me.dibber.blablablapp.R;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphRequest.GraphJSONObjectCallback;
import com.facebook.GraphResponse;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

public class Profile {
	
	public enum ProfileType {NOT_LOGGED_IN,MANUALLY_CREATED,FACEBOOK,TWITTER,GOOGLEPLUS};
	
	private static final String PREFERENCE_PROFILETYPE = "pref_profile_type";
	private static final String PREFERENCE_PROFILE_NAME = "pref_profile_name";
	private static final String PREFERENCE_PROFILE_EMAIL = "pref_profile_email";
	private static final String PROFILE_ICON_FILE = "profile_icon.jpg";
	
	private static final int PROFILE_PIC_PIXEL_SIZE = 200;
	
    // Facebook
	private static final String FACEBOOK_PERMISSIONS = "email";
    private CallbackManager mFacebookCallbackMan;
    private ProfileTracker mFacebookProfileTracker;
    
    // Google+
    public static final int GOOGLEPLUS_SIGNIN = 76;
    private GoogleApiClient mGoogleApiClient;
    private boolean mGoogleResolvingInProgress;
    private boolean mGoogleSignInClicked;
	
	private static Profile defaultProfile;
	
	private String prefix;
	
	private Drawable mIcon;
	private String mName;
	private String mEmail;
	private ProfileType mProfileType;
	private OnProfileChangedListener listener;	
	
	private Profile() { }
	
	public static Profile getDefaultProfile() {
		if (defaultProfile == null) {
			defaultProfile = new Profile();
			defaultProfile.prefix = "";
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalState.getContext());
			defaultProfile.mName = prefs.getString(defaultProfile.prefix + PREFERENCE_PROFILE_NAME, "");
			defaultProfile.mEmail = prefs.getString(defaultProfile.prefix + PREFERENCE_PROFILE_EMAIL, "");
			try {
				defaultProfile.mProfileType = ProfileType.valueOf(prefs.getString(defaultProfile.prefix + PREFERENCE_PROFILETYPE, "NOT_LOGGED_IN"));
			} catch (IllegalArgumentException e) {
				defaultProfile.mProfileType = ProfileType.NOT_LOGGED_IN;
			}
			defaultProfile.mIcon = null;
			File file = GlobalState.getContext().getFileStreamPath(defaultProfile.prefix + PROFILE_ICON_FILE);
			if (file != null && file.exists()) {
				try {
					FileInputStream in = new FileInputStream(file);
					defaultProfile.mIcon = Drawable.createFromStream(in, "src");
					in.close();
				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				}
			}
			switch (defaultProfile.mProfileType) {
			case FACEBOOK:
				defaultProfile.initFaceBook();
				break;
			case GOOGLEPLUS:
				defaultProfile.initGooglePlus();
				break;
			case MANUALLY_CREATED:
			case NOT_LOGGED_IN:
			case TWITTER:
			default:
				break;
			}
		}
		return defaultProfile;
	}
	
	public static Profile getProfileByTag(String tag) {
		Profile newProfile = new Profile();
		newProfile.prefix = tag;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalState.getContext());
		newProfile.mName = prefs.getString(newProfile.prefix + PREFERENCE_PROFILE_NAME, "");
		newProfile.mEmail = prefs.getString(newProfile.prefix + PREFERENCE_PROFILE_EMAIL, "");
		try {
			newProfile.mProfileType = ProfileType.valueOf(prefs.getString(newProfile.prefix + PREFERENCE_PROFILETYPE, "NOT_LOGGED_IN"));
		} catch (IllegalArgumentException e) {
			newProfile.mProfileType = ProfileType.NOT_LOGGED_IN;
		}
		newProfile.mIcon = null;
		File file = GlobalState.getContext().getFileStreamPath(newProfile.prefix + PROFILE_ICON_FILE);
		if (file != null && file.exists()) {
			try {
				FileInputStream in = new FileInputStream(file);
				newProfile.mIcon = Drawable.createFromStream(in, "src");
				in.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
			switch (newProfile.mProfileType) {
			case FACEBOOK:
				defaultProfile.initFaceBook();
				break;
			case GOOGLEPLUS:
				defaultProfile.initGooglePlus();
				break;
			case MANUALLY_CREATED:
			case NOT_LOGGED_IN:
			case TWITTER:
			default:
				break;
			}
		}
		return newProfile;
	}
	
	public String getName() {
		return mName;
	}
	
	public String getEmail() {
		return mEmail;
	}
	
	public Drawable getIcon() {
		if (mIcon == null) {
		switch (getProfileType()) {
			default:
				mIcon = GlobalState.getContext().getResources().getDrawable(R.drawable.ic_action_person); 
				mIcon.setColorFilter(GlobalState.getContext().getResources().getColor(R.color.regular_foreground), PorterDuff.Mode.SRC_ATOP);
				break;
			}
		}
		return mIcon;
	}
	
	public void setIcon(Drawable icon) {
		mIcon = icon;
		if (icon != null) {
			try {
				FileOutputStream out = GlobalState.getContext().openFileOutput(prefix + PROFILE_ICON_FILE, Context.MODE_PRIVATE);
				Bitmap bmp = ((BitmapDrawable) mIcon).getBitmap();
				bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
				out.close();
			} catch (IOException e) {
			}
		} else {
			File file = GlobalState.getContext().getFileStreamPath(prefix + PROFILE_ICON_FILE);
			file.delete();
		}
		commitProfileChange();
	}
	
	public boolean isLoggedIn() {
		return !(getProfileType() == ProfileType.NOT_LOGGED_IN);
	}
	
	public void close() {
		if (mFacebookProfileTracker != null) {
			mFacebookProfileTracker.stopTracking();
		}
	}
	
	private ProfileType getProfileType() {
		return mProfileType;
	}
	
	private void logout() {
		mName = "";
		mEmail = "";
		setIcon(null);
		mProfileType = ProfileType.NOT_LOGGED_IN;
		commitProfileChange();
	}
	
	private void loginManual(String username, String email) {
		mName = username;
		mEmail = email;
		mProfileType = ProfileType.MANUALLY_CREATED;
		commitProfileChange();
	}
	
	private void setProfilePicFromURL (final String path) {
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					URL url = new URL(path);
					InputStream in = url.openStream();
					setIcon(Drawable.createFromStream(in, "src"));
					in.close();
				} catch (FileNotFoundException e) {
					Log.e("FileNotFoundException on getting profile image", e.toString());
				} catch (IOException e1) {
					Log.e("IOException on getting profile image", e1.toString());
				}
				commitProfileChange();
			}
		});
		t.start();
		
		commitProfileChange();
	}

	private void initFaceBook() {
		FacebookSdk.sdkInitialize(GlobalState.getContext());
		mFacebookProfileTracker = new ProfileTracker() {

			@Override
			protected void onCurrentProfileChanged(com.facebook.Profile oldProfile,	com.facebook.Profile currentProfile) {
				com.facebook.Profile profile = com.facebook.Profile.getCurrentProfile();
				if (profile != null) {
					loginFaceBook(profile);
				}
			}
        };
        mFacebookProfileTracker.startTracking();

        mFacebookCallbackMan = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mFacebookCallbackMan, new FacebookCallback<LoginResult>() {
			
			@Override
			public void onSuccess(LoginResult result) {
				com.facebook.Profile profile = com.facebook.Profile.getCurrentProfile();
				if (profile != null) {
					loginFaceBook(profile);
				}
			}
			
			@Override
			public void onError(FacebookException error) {
				Log.e("FacebookException: ", error.toString());
			}
			
			@Override
			public void onCancel() { }
		});
	}
	
	private void loginFaceBook(com.facebook.Profile profile) {
		mName = profile.getName();
		mProfileType = ProfileType.FACEBOOK;
		final Uri profileImage = profile.getProfilePictureUri(PROFILE_PIC_PIXEL_SIZE, PROFILE_PIC_PIXEL_SIZE);
		setProfilePicFromURL(profileImage.toString());
		
		GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphJSONObjectCallback() {
			
			@Override
			public void onCompleted(JSONObject object, GraphResponse response) {
				if (response.getError() == null) {
					try {
						mEmail = object.getString("email");
						commitProfileChange();
					} catch (JSONException e) {
						Log.w("JSONException while trying to retrieve facebook email address: ", e.toString());
					}
				} else {
					Log.w("FacebookRequestError while trying to retrieve email address: ", response.getError().toString());
				}
			}
		});
		Bundle params = request.getParameters();
		params.putString("fields", "email");
		request.setParameters(params);
		request.executeAsync();
		commitProfileChange();
	}
	
	private void initGooglePlus() {
		mGoogleApiClient = new GoogleApiClient.Builder(GlobalState.getContext())
        .addConnectionCallbacks(new ConnectionCallbacks() {
			
			@Override
			public void onConnectionSuspended(int cause) {
				mGoogleApiClient.connect();
			}
			
			@Override
			public void onConnected(Bundle connectionHint) {
				mGoogleSignInClicked = false;
				loginGooglePlus();
			}
		})
        .addOnConnectionFailedListener(new OnConnectionFailedListener() {
			
			@Override
			public void onConnectionFailed(ConnectionResult result) {
				if (!mGoogleResolvingInProgress) {
					if (mGoogleSignInClicked && result.hasResolution()) {
						try {
							mGoogleResolvingInProgress = true;
							((GlobalState)GlobalState.getContext()).getCurrentHomeActivity()
								.startIntentSenderForResult(result.getResolution().getIntentSender(), GOOGLEPLUS_SIGNIN, null, 0,0,0);
						} catch (SendIntentException e) {
							mGoogleResolvingInProgress = false;
							mGoogleApiClient.connect();
						}
					}
				}
			}
		})
        .addApi(Plus.API)
        .addScope(Plus.SCOPE_PLUS_LOGIN)
        .build();
	}
	
	private void loginGooglePlus() {
		if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
			return;
		}
		if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
			Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
			mName = currentPerson.getDisplayName();
			mProfileType = ProfileType.GOOGLEPLUS;
			if (currentPerson.getImage().getUrl() != null) {
				String url = currentPerson.getImage().getUrl();
				for (int i = 0; i+3 < url.length(); i++) {
					if ((url.charAt(i) == '?' || url.charAt(i) == '&')&& url.charAt(i+1) == 's' && url.charAt(i+2) == 'z' && url.charAt(i+3) == '=') {
						int j = 4;
						while (i+j < url.length() && url.charAt(i+j) != '&') {
							j++;
						}
						String start = url.substring(0, i+4);
						String end = "";
						if (i+j < url.length()) {
							end = url.substring(i+j, url.length());
						}
						url = start + PROFILE_PIC_PIXEL_SIZE + end;
						break;
					}
				}
				setProfilePicFromURL(url);
			}
		}
		if (Plus.AccountApi.getAccountName(mGoogleApiClient) != null) {
			mEmail = Plus.AccountApi.getAccountName(mGoogleApiClient);
		} 
		commitProfileChange();
	}
	
	public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
		if (requestCode == GOOGLEPLUS_SIGNIN) {
			if (resultCode != Activity.RESULT_OK) {
				mGoogleSignInClicked = false;
			}
			mGoogleResolvingInProgress = false;
			
			if (mGoogleApiClient == null) {
				initGooglePlus();
			}
		    if (!mGoogleApiClient.isConnecting()) {
		    	mGoogleApiClient.reconnect();
		    }
		}
	}
	
	private void commitProfileChange() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalState.getContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(prefix + PREFERENCE_PROFILE_NAME, mName);
		editor.putString(prefix + PREFERENCE_PROFILE_EMAIL, mEmail);
		editor.putString(prefix + PREFERENCE_PROFILETYPE,mProfileType.toString());
		editor.commit();
		if (listener != null) {
			((GlobalState)GlobalState.getContext()).getCurrentHomeActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					listener.onProfileChanged();
				}
			});
		}
	}
	
	public void openDialog() {
		GlobalState globalState = (GlobalState) GlobalState.getContext();
		
		switch (mProfileType) {
		case FACEBOOK:
			ProfileDialog dialogFaceBook = new ProfileDialog();
			dialogFaceBook.show(globalState.getCurrentHomeActivity().getSupportFragmentManager(), "facebookDialog");
			break;
		case GOOGLEPLUS:
			ProfileDialog dialogGooglePlus = new ProfileDialog();
			dialogGooglePlus.show(globalState.getCurrentHomeActivity().getSupportFragmentManager(), "dialogGooglePlus");
			break;
		case MANUALLY_CREATED:
			ManuallyCreatedProfileDialog dialogManual = new ManuallyCreatedProfileDialog();
			dialogManual.show(globalState.getCurrentHomeActivity().getSupportFragmentManager(), "manuallyCreatedProfileDialog");
			break;
		case NOT_LOGGED_IN:
			LoginDialog dialogLogin = new LoginDialog();
			dialogLogin.show(globalState.getCurrentHomeActivity().getSupportFragmentManager(), "loginDialog");
			break;
		case TWITTER:
			break;
		default:
			break;
		}
	}
	
	public void setOnProfileChangedListener(OnProfileChangedListener onProfileChangedListener) {
		listener = onProfileChangedListener;
	}
	
	public interface OnProfileChangedListener {
		public void onProfileChanged();
	}

	@SuppressLint("InflateParams")
	public static class LoginDialog extends DialogFragment {
		
		AlertDialog d;
		
		EditText userName;
		EditText email;
		TextView extraText;
		String tag;
		
		ImageView faceBookLogin;
		ImageView googlePlusLogin;
		ImageView linkedInLogin;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dialog_profile_login, null);
			
			userName = (EditText) view.findViewById(R.id.login_username);
			email = (EditText) view.findViewById(R.id.login_email);
			extraText = (TextView) view.findViewById(R.id.login_signinManually);
			
			d = builder.setView(view)
				   .setTitle(R.string.login_dialog)
				   .setPositiveButton(R.string.signin, null)
				   .setNegativeButton(R.string.cancel, null)
				   .create();
			
			userName.setHintTextColor(getResources().getColor(R.color.regular_foreground_hint));
			userName.setTextColor(getResources().getColor(R.color.regular_foreground_a));
			email.setHintTextColor(getResources().getColor(R.color.regular_foreground_hint));
			email.setTextColor(getResources().getColor(R.color.regular_foreground_a));
			extraText.setTextColor(getResources().getColor(R.color.regular_foreground_a));
			extraText.setTypeface(null, Typeface.ITALIC);
			
			userName.setText(getProfile().getName());
			email.setText(getProfile().getEmail());
			extraText.setText(R.string.login_signinManually);
			
			faceBookLogin = (ImageView) view.findViewById(R.id.login_facebook);
			faceBookLogin.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getProfile().initFaceBook();
					LoginManager loginMan = LoginManager.getInstance();
					loginMan.logInWithReadPermissions(LoginDialog.this, Arrays.asList(FACEBOOK_PERMISSIONS));
				}
			});
			
			googlePlusLogin = (ImageView) view.findViewById(R.id.login_googleplus);
			googlePlusLogin.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					getProfile().initGooglePlus();
					getProfile().mGoogleSignInClicked = true;
					getProfile().mGoogleApiClient.connect();
					d.dismiss();
				}
			});
			
			d.setOnShowListener(new DialogInterface.OnShowListener() {
				
				@Override
				public void onShow(DialogInterface dialog) {
					
					Button positiveButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
					positiveButton.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							boolean inputError = false;
							
							if (TextUtils.isEmpty(userName.getText().toString()) ) {
								userName.setError(getResources().getString(R.string.is_required));
								inputError = true;
							}
							if (TextUtils.isEmpty(email.getText().toString()) ) {
								email.setError(getResources().getString(R.string.is_required));
								inputError = true;
							} else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.getText()).matches()) {
								email.setError(getResources().getString(R.string.invalid_email));
								inputError = true;
							}
							if (!inputError) {
								getProfile().loginManual(userName.getText().toString(), email.getText().toString());
								d.dismiss();
							}
						}
					});
				}
			});
			return d;
		}
		
		private Profile getProfile() {
			if (tag == null) {
				return getDefaultProfile();
			} else {
				return getProfileByTag(tag);
			}
		}
		
		public void setTag(String tag) {
			this.tag = tag;
		}
		
		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
			if (getProfile().mFacebookCallbackMan != null) {
				getProfile().mFacebookCallbackMan.onActivityResult(requestCode, resultCode, data);
			}
			d.dismiss();
		}
		
	}
	
	@SuppressLint("InflateParams")
	public static class ProfileDialog extends DialogFragment {
		
		TextView userName;
		TextView email;
		ImageView profilePic;
		String tag;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dialog_profile_profile, null);
			userName = (TextView) view.findViewById(R.id.profile_profile_username);
			email = (TextView) view.findViewById(R.id.profile_profile_email);
			profilePic = (ImageView) view.findViewById(R.id.profile_profile_profilePic);
			
			int iconId = 0;
			switch (getProfile().getProfileType()) {
			case FACEBOOK:
				iconId = R.drawable.facebook_icon_125x125;
				break;
			case GOOGLEPLUS:
				iconId = R.drawable.googleplus_icon_125x125;
				break;
			case MANUALLY_CREATED:
			case NOT_LOGGED_IN:
			case TWITTER:
			default:
				iconId = R.drawable.googleplus_icon_125x125;
				break;
			}
			Bitmap btm = ((BitmapDrawable) getResources().getDrawable(iconId)).getBitmap();
			DisplayMetrics metrics = getResources().getDisplayMetrics();
			int px = (int) (32 * (metrics.densityDpi / 160f));
			Drawable scaledIcon = new BitmapDrawable(getResources(),Bitmap.createScaledBitmap(btm, px, px, true));
			
			final AlertDialog d = builder.setView(view)
				   .setTitle(R.string.profile)
				   .setIcon(scaledIcon)
				   .setPositiveButton(R.string.signout, null)
				   .setNegativeButton(R.string.cancel, null)
				   .create();
			
			userName.setTextColor(getResources().getColor(R.color.regular_foreground_a));
			userName.setTypeface(null, Typeface.BOLD);
			email.setTextColor(getResources().getColor(R.color.regular_foreground_a));
			
			userName.setText(getProfile().getName());
			email.setText(getProfile().getEmail());
			profilePic.setAdjustViewBounds(true);
			profilePic.setImageDrawable(getProfile().getIcon());
			
			d.setOnShowListener(new DialogInterface.OnShowListener() {
				
				@Override
				public void onShow(DialogInterface dialog) {
					
					Button signoutButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
					signoutButton.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							switch (getProfile().getProfileType()) {
							case FACEBOOK:
								LoginManager loginMan = LoginManager.getInstance();
								loginMan.logOut();
								getProfile().logout();
								d.dismiss();
								break;
							case GOOGLEPLUS:
								if (getProfile().mGoogleApiClient != null) {
									if (getProfile().mGoogleApiClient.isConnected()) {
										Plus.AccountApi.clearDefaultAccount(getProfile().mGoogleApiClient);
										getProfile().mGoogleApiClient.disconnect();
									}
								}
								getProfile().logout();
								d.dismiss();
								break;
							case MANUALLY_CREATED:
								break;
							case NOT_LOGGED_IN:
								break;
							case TWITTER:
								break;
							default:
								break;
							
							}
							
						}
					});
					
				}
			});
			return d;
		}
		
		private Profile getProfile() {
			if (tag == null) {
				return getDefaultProfile();
			} else {
				return getProfileByTag(tag);
			}
		}
		
		public void setTag(String tag) {
			this.tag = tag;
		}
	}
	
	@SuppressLint("InflateParams")
	public static class ManuallyCreatedProfileDialog extends DialogFragment {
		
		EditText userName;
		EditText email;
		TextView extraText;
		ImageView profilePic;
		String tag;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dialog_profile_manually_created, null);
			userName = (EditText) view.findViewById(R.id.manuallyCreated_username);
			email = (EditText) view.findViewById(R.id.manuallyCreated_email);
			extraText = (TextView) view.findViewById(R.id.manuallyCreated_extraText);
			profilePic = (ImageView) view.findViewById(R.id.manuallyCreated_profilePic);
			
			final AlertDialog d = builder.setView(view)
				   .setTitle(R.string.profile)
				   .setPositiveButton(R.string.ok, null)
				   .setNeutralButton(R.string.signout, null)
				   .setNegativeButton(R.string.cancel, null)
				   .create();
			
			userName.setHintTextColor(getResources().getColor(R.color.regular_foreground_hint));
			userName.setTextColor(getResources().getColor(R.color.regular_foreground_a));
			email.setHintTextColor(getResources().getColor(R.color.regular_foreground_hint));
			email.setTextColor(getResources().getColor(R.color.regular_foreground_a));
			extraText.setTextColor(getResources().getColor(R.color.regular_foreground_a));
			extraText.setTypeface(null, Typeface.ITALIC);
			
			userName.setText(getProfile().getName());
			email.setText(getProfile().getEmail());
			extraText.setText(R.string.manuallyCreated_extraText);
			profilePic.setAdjustViewBounds(true);
			profilePic.setImageDrawable(getProfile().getIcon());
			profilePic.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					selectImage();
				}
			});
			
			d.setOnShowListener(new DialogInterface.OnShowListener() {
				
				@Override
				public void onShow(DialogInterface dialog) {
					
					Button positiveButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
					positiveButton.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							boolean inputError = false;
							
							if (TextUtils.isEmpty(userName.getText().toString()) ) {
								userName.setError(getResources().getString(R.string.is_required));
								inputError = true;
							}
							if (TextUtils.isEmpty(email.getText().toString()) ) {
								email.setError(getResources().getString(R.string.is_required));
								inputError = true;
							} else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.getText()).matches()) {
								email.setError(getResources().getString(R.string.invalid_email));
								inputError = true;
							}
							if (!inputError) {
								getProfile().loginManual(userName.getText().toString(), email.getText().toString());
								getProfile().setIcon(profilePic.getDrawable());
								d.dismiss();
							}
						}
					});
					
					Button signoutButton = d.getButton(AlertDialog.BUTTON_NEUTRAL);
					signoutButton.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							getProfile().logout();		
							d.dismiss();
						}
					});
					
				}
			});
			return d;
		}
		
		private Profile getProfile() {
			if (tag == null) {
				return getDefaultProfile();
			} else {
				return getProfileByTag(tag);
			}
		}
		
		public void setTag(String tag) {
			this.tag = tag;
		}
		
		private static final int REQ_CODE_SELECT_CROPPED_IMAGE = 11;
		private static final String TEMP_PHOTO_FILE = "temp_profile_icon.jpg";  
		
		private void selectImage() {
		    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		    photoPickerIntent.setType("image/*");
		    photoPickerIntent.putExtra("crop", "true");
		    photoPickerIntent.putExtra("aspectX", 1);
		    photoPickerIntent.putExtra("aspectY", 1);
		    photoPickerIntent.putExtra("outputX", PROFILE_PIC_PIXEL_SIZE);
			photoPickerIntent.putExtra("outputY", PROFILE_PIC_PIXEL_SIZE);
			photoPickerIntent.putExtra("noFaceDetection", true);
			photoPickerIntent.putExtra("scale", true);
			photoPickerIntent.putExtra("return-data", false);
		    photoPickerIntent.putExtra(MediaStore.EXTRA_OUTPUT, getTempUri());
		    photoPickerIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		    startActivityForResult(photoPickerIntent, REQ_CODE_SELECT_CROPPED_IMAGE);
		}
		
		
		private Uri getTempUri() {
		    return Uri.fromFile(getTempFile());
		}
		
		private File getTempFile() {
			
		    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
		    	File f = new File(Environment.getExternalStorageDirectory(),TEMP_PHOTO_FILE);
		    	try {
		    		f.createNewFile();
		    	} catch (IOException e) {
		    		Log.e("IOException on getTempFile", e.toString());
		    	}
		    	return f;
		    } else {
		    	return null;
		    }
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			switch (requestCode) {
			
			case REQ_CODE_SELECT_CROPPED_IMAGE:
				if (resultCode == Activity.RESULT_OK) {
					if (data!=null) {
						File tempFile = getTempFile();
						Drawable d = null;
						try {
							FileInputStream filein = new FileInputStream(tempFile);
							if (filein.available() > 0) {
								d = Drawable.createFromStream(filein, "src");
							} else {
								Uri selectedImageURI = data.getData();
								if (selectedImageURI != null) {
									InputStream in = getActivity().getContentResolver().openInputStream(selectedImageURI);
									d = Drawable.createFromStream(in, "src");
									in.close();
								} else {
									Log.w("error while trying to select a cropped image:", "cannot retrieve image");
								}
							}
							filein.close();
							
							
						} catch (FileNotFoundException e) {
							Log.e("FileNotFoundException on cropping image", e.toString());
						} catch (IOException e1) {
							Log.e("IOException on cropping image", e1.toString());
						} finally {
							tempFile.delete();
						}
						if (d != null) {
							if (d.getIntrinsicHeight() != d.getIntrinsicWidth()) {
								Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
								d = new BitmapDrawable(getResources(),Bitmap.createScaledBitmap(bitmap, PROFILE_PIC_PIXEL_SIZE, PROFILE_PIC_PIXEL_SIZE, true));
							}
							profilePic.setImageDrawable(d);
						}
						
					}
				}
				break;
			}
		}
		
		
	}


}
