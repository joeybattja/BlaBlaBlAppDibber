package me.dibber.blablablapp.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import me.dibber.blablablapp.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class Profile {
	
	public enum ProfileType {NOT_LOGGED_IN,MANUALLY_CREATED,FACEBOOK,TWITTER,GOOGLEPLUS};
	
	private static final String PREFERENCE_PROFILETYPE = "pref_profile_type";
	private static final String PREFERENCE_PROFILE_NAME = "pref_profile_name";
	private static final String PREFERENCE_PROFILE_EMAIL = "pref_profile_email";
		private static final String PROFILE_ICON_FILE = "profile_icon.jpg";
	
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
	
	private ProfileType getProfileType() {
		return mProfileType;
	}
	
	private void logout() {
		mName = "";
		mEmail = "";
		mIcon = null;
		mProfileType = ProfileType.NOT_LOGGED_IN;
		commitProfileChange();
	}
	
	private void loginManual(String username, String email) {
		mName = username;
		mEmail = email;
		mProfileType = ProfileType.MANUALLY_CREATED;
		commitProfileChange();
	}
	
	private void commitProfileChange() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalState.getContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(prefix + PREFERENCE_PROFILE_NAME, mName);
		editor.putString(prefix + PREFERENCE_PROFILE_EMAIL, mEmail);
		editor.putString(prefix + PREFERENCE_PROFILETYPE,mProfileType.toString());
		editor.commit();
		if (listener != null) {
			listener.onProfileChanged();
		}
	}
	
	public void openDialog(FragmentManager fragmentmanager) {
		switch (mProfileType) {
		case FACEBOOK:
			break;
		case GOOGLEPLUS:
			break;
		case MANUALLY_CREATED:
			ManuallyCreatedProfileDialog dialogManual = new ManuallyCreatedProfileDialog();
			dialogManual.show(fragmentmanager, "loginDialog");
			break;
		case NOT_LOGGED_IN:
			LoginDialog dialogLogin = new LoginDialog();
			dialogLogin.show(fragmentmanager, "loginDialog");
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
		
		EditText userName;
		EditText email;
		TextView extraText;
		String tag;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dialog_profile_login, null);
			userName = (EditText) view.findViewById(R.id.login_username);
			email = (EditText) view.findViewById(R.id.login_email);
			extraText = (TextView) view.findViewById(R.id.login_signinManually);
			final AlertDialog d = builder.setView(view)
				   .setTitle(R.string.login_dialog)
				   .setPositiveButton(R.string.signin, null)
				   .setNegativeButton(R.string.cancel, null)
				   .create();
			userName.setText(getProfile().getName());
			email.setText(getProfile().getEmail());
			extraText.setTypeface(null, Typeface.ITALIC);
			extraText.setText(R.string.login_signinManually);
			
			
			d.setOnShowListener(new DialogInterface.OnShowListener() {
				
				@Override
				public void onShow(DialogInterface dialog) {
					
					Button positiveButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
					positiveButton.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							boolean inputError = false;
							
							if (TextUtils.isEmpty(userName.getText().toString()) ) {
								userName.setError("this cannot be empty");
								inputError = true;
							}
							if (TextUtils.isEmpty(email.getText().toString()) ) {
								email.setError("this cannot be empty");
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
			userName.setText(getProfile().getName());
			email.setText(getProfile().getEmail());
			extraText.setTypeface(null, Typeface.ITALIC);
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
								userName.setError("this cannot be empty");
								inputError = true;
							}
							if (TextUtils.isEmpty(email.getText().toString()) ) {
								email.setError("this cannot be empty");
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
		
		private static final int REQ_CODE_PICK_IMAGE = 11;
		private static final String TEMP_PHOTO_FILE = "temp_profile_icon.jpg";  
		
		private void selectImage() {
		    Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
		    photoPickerIntent.setType("image/*");
		    photoPickerIntent.putExtra("crop", "true");
		    photoPickerIntent.putExtra("aspectX", 1);  
		    photoPickerIntent.putExtra("aspectY", 1);  
		    photoPickerIntent.putExtra("outputX", 96);  
			photoPickerIntent.putExtra("outputY", 96);  
			photoPickerIntent.putExtra("noFaceDetection", true);
			photoPickerIntent.putExtra("scale", true);
			photoPickerIntent.putExtra("return-data", true);                                  
		    photoPickerIntent.putExtra(MediaStore.EXTRA_OUTPUT, getTempUri());
		    photoPickerIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		    startActivityForResult(photoPickerIntent, REQ_CODE_PICK_IMAGE);
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
		    	}
		    	return f;
		    } else {
		    	return null;
		    }
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			switch (requestCode) {
			
			case REQ_CODE_PICK_IMAGE:
				if (resultCode == Activity.RESULT_OK) {
					if (data!=null) {
						File tempFile = getTempFile();
						try {
							FileInputStream in = new FileInputStream(tempFile);
							Drawable d = Drawable.createFromStream(in, "src");
							profilePic.setImageDrawable(d);
							in.close();
						} catch (FileNotFoundException e) {
						} catch (IOException e) {
						} finally {
							tempFile.delete();
						}
					}
				}
				break;
			}
		}
		
		
	}


}
