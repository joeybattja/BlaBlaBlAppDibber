package me.dibber.blablablapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import me.dibber.blablablapp.Post.Attachment;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;

public class PostCollection {
			
	private static PostCollection postCollection;
	private final static String MIMETYPE_JPEG = "image/jpeg";
	private final static String MIMETYPE_PNG = "image/png";
	private final static String MIMETYPE_YOUTUBE = "video/youtube";
	private final static int THUMBNAIL_WIDTH = 240;
	private final static int THUMBNAIL_HEIGHT = 180;
	private static SparseArray<Handler> activeHandlers;
	
	private TreeMap<Integer,Post> posts;
	
	private PostCollection() {
		posts = new TreeMap<Integer,Post>();
	}
	
	public void addPost(Post post) {
		posts.put(post.id, post);
	}
	
	
	public void removePost(int postId) {
		cleanUpAttachments(postId, getAttachments(postId));
		posts.remove(postId);
	}
	
	public Post getPost(int postId) {
		return	posts.get(postId);
	}
	
	public List<Integer> getAllPosts() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (Integer i : posts.keySet()) {
			list.add(i);
		}
		Collections.sort(list, new Comparator<Integer>() {

			@Override
			public int compare(Integer lhs, Integer rhs) {
				
				return getItemDate(rhs).compareTo(getItemDate(lhs));
			}
		});
		return list;
	}
	
	public boolean itemCommentsOpen(int postId) {
		if (posts.get(postId) == null) {
			return false;
		}
		return posts.get(postId).commentstatus;
	}
	
	public ArrayList<Integer> getItemComments(final int postId) {
		ArrayList<Integer> comments = new ArrayList<Integer>();
		if (posts.get(postId) == null) { 
			return comments;
		}
		if (posts.get(postId).comments == null) {
			return comments;
		}
		for (Post.Comment c : posts.get(postId).comments) {
			if (c.id != 0) {
				comments.add(c.id);
			}
		}
		Collections.sort(comments, new Comparator<Integer>() {

			@Override
			public int compare(Integer lhs, Integer rhs) {
				if (getCommentParent(postId, lhs) == 0 && getCommentParent(postId, rhs) == 0) {
					return getCommentDate(postId, lhs).compareTo(getCommentDate(postId, rhs));
				} else if (getCommentParent(postId, lhs) == 0) {
					int rootParent = getCommentParent(postId, rhs);
					while (rootParent != 0) {
						rhs = rootParent;
						rootParent = getCommentParent(postId, rhs);
					}
					return getCommentDate(postId, lhs).compareTo(getCommentDate(postId, rhs));					
				} else if (getCommentParent(postId, rhs) == 0) {
					int rootParent = getCommentParent(postId, lhs);
					while (rootParent != 0) {
						lhs = rootParent;
						rootParent = getCommentParent(postId, lhs);
					}
					return getCommentDate(postId, lhs).compareTo(getCommentDate(postId, rhs));
				} else {
					int orlhs = lhs;
					int orrhs = rhs;
					int rootParentrhs = getCommentParent(postId, rhs);
					int rootParentlhs = getCommentParent(postId, lhs);
					int crossing = 0;
					int endlessLoopCheck = 0;
					do {
						endlessLoopCheck++;
						if (endlessLoopCheck > 10000) {
							Log.e("POSTCOLLECTION ENDLESS LOOP!!", "breaking now...");
							break;
						}
						lhs = orlhs;
						rhs = orrhs;
						while (rootParentrhs != crossing) {
							rhs = rootParentrhs;
							rootParentrhs = getCommentParent(postId, rhs);
						}
						while (rootParentlhs != crossing) {
							lhs = rootParentlhs;
							rootParentlhs = getCommentParent(postId, lhs);
						}
					} while (rhs == lhs); 
					return getCommentDate(postId, lhs).compareTo(getCommentDate(postId, rhs));
				}
			}
		});

		return comments;
	}
	
	public String getCommentAuthor(int postId, int commentId) {
		if (posts.get(postId) == null) {
			return " ";
		} 
		if (posts.get(postId).comments == null) {
			return null;
		}
		for (Post.Comment c : posts.get(postId).comments) {
			if (c.id == commentId) {
				if (c.author == null) { 
					return " ";
				} else {
					return c.author;
				}
			} 
		} 
		return " ";
	}
	
	public CharSequence getCommentContent(int postId, int commentId) {
		if (posts.get(postId) == null) {
			return " ";
		} 
		if (posts.get(postId).comments == null) {
			return null;
		}
		for (Post.Comment c : posts.get(postId).comments) {
			if (c.id == commentId) {
				if (c.content == null) { 
					return " ";
				} else {
					return trimTrailingWhitespace(Html.fromHtml(c.content.trim()));
				}
			} 
		} 
		return " ";
	}
	
	public String getCommentDateAsString(int postId, int commentId) {
		if (posts.get(postId) == null) {
			return " ";
		} 
		if (posts.get(postId).comments == null) {
			return " ";
		} 
		Date d = null;
		for (Post.Comment c : posts.get(postId).comments) {
			if (c.id == commentId) {
				d = c.date;
			}
		} 		
		if (d != null) {
			DateFormat df = SimpleDateFormat.getDateTimeInstance();
			return df.format(d);
		} else {
			return " ";
		}
	}
	
	public Date getCommentDate(int postId, int commentId) {
		if (posts.get(postId) == null) {
			return null;
		} 
		if (posts.get(postId).comments == null) {
			return null;
		} 
		for (Post.Comment c : posts.get(postId).comments) {
			if (c.id == commentId) {
				return c.date;
			}
		} 
		return null;
	}
	
	public int getCommentParent(int postId, int commentId) {
		if (posts.get(postId) == null) {
			return 0;
		} 
		if (posts.get(postId).comments == null) {
			return 0;
		} 
		for (Post.Comment c : posts.get(postId).comments) {
			if (c.id == commentId) {
				return c.parent;
			}
		} 
		return 0;
	}
	
	public CharSequence getItemTitle(int postId) {
		if (posts.get(postId) == null) {
			return " ";
		} 
		String t = posts.get(postId).title;
		if (t != null) {
			return Html.fromHtml(t);
		} else {
			return " ";
		}
	}
	
	public String getItemDateAsString(int postId) {
		if (posts.get(postId) == null) {
			return " ";
		} 
		Date d = posts.get(postId).date;
		
		if (d != null) {
			DateFormat df = SimpleDateFormat.getDateInstance();
			return df.format(d);
		} else {
			return " ";
		}
	}
	
	public Date getItemDate(int postId) {
		if (posts.get(postId) == null) {
			return null;
		} 
		Date d = posts.get(postId).date;
		
		if (d != null) {
			return d;
		} else {
			return null;
		}
	}
	
	public String getItemAuthor(int postId) {
		if (posts.get(postId) == null) {
			return " ";
		} 
		String a = posts.get(postId).authorname;
		if (a != null) {
			return a;
		} else {
			return " ";
		}
	}
	
	public int getItemAuthorId(int postId) {
		if (posts.get(postId) == null) {
			return -1;
		} 
		return posts.get(postId).id;
	}
	
	public String getItemMeta(int postId) {
		return (getItemDateAsString(postId) + " " + GlobalState.getContext().getString(R.string.author_name, getItemAuthor(postId)));
	}

	public CharSequence getItemContent(int postId) {
		if (posts.get(postId) == null) {
			return " ";
		} 
		String c = posts.get(postId).content.trim();
		if (c != null) {
			CharSequence cs = Html.fromHtml(c.replaceAll("<img.+?>", ""));
			return trimTrailingWhitespace(cs);
		} else {
			return " ";
		}
	}
	
	public CharSequence getItemContentReplaceBreaks(int postId) {
		if (posts.get(postId) == null) {
			return " ";
		} 
		String c = posts.get(postId).content.trim();
		if (c != null) {
			CharSequence cs = Html.fromHtml(c.replaceAll("<img.+?>", "").replaceAll("<br.+?>", " "));
			return trimTrailingWhitespace(cs);
		} else {
			return " ";
		}
	}
	
	public String getItemUrl(int postId) {
		if (posts.get(postId) == null) {
			return " ";
		}
		String u = posts.get(postId).url;
		if (u != null) {
			return u;
		} else {
			return " ";
		}
	}
	
	public boolean itemIsFavorite(int postId) {
		if (posts.get(postId) == null) {
			return false;
		}
		return posts.get(postId).favorite == 'Y';
	}
	
	public void setItemFavorite(int postId, boolean favorite) {
		if (posts.get(postId) == null) {
			return;
		}
		if (favorite) {
			posts.get(postId).favorite = 'Y';
		} else {
			posts.get(postId).favorite = 'N';
		}
	}
	
	public String getItemYouTubeVideoID(int postId) {
		if (posts.get(postId) == null) {
			return " ";
		}
		String videoURL = null;
		ArrayList<Post.Attachment> att = posts.get(postId).attachments;
		for (int i = 0; i < att.size(); i++) {
			if (att.get(i).mimeType.equals(MIMETYPE_YOUTUBE)) {
				videoURL = att.get(i).url;
				break;
			}
		}
		if (videoURL == null) {
			return null;
		}
		int cut = 0;
		for (int i = 0; i < videoURL.length(); i++) {
			if (videoURL.charAt(i) == '/') {
				cut = i;
			}
		}
		String videoID = (String) videoURL.subSequence(cut+1, videoURL.length());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < videoID.length(); i++) {
			if (videoID.charAt(i) == '?') {
				break;
			}
			sb.append(videoID.charAt(i));
		}
		return sb.toString();
	}
	
	public int countImages(int postId) {
		if (posts.get(postId) == null) {
			return 0;
		} 
		int count = 0;
		for (Post.Attachment a : posts.get(postId).attachments) {
			if (a.mimeType.equals(MIMETYPE_JPEG) || a.mimeType.equals(MIMETYPE_PNG)) {
				count++;
			}
		}
		return count;
	}
	
	public int countComments(int postId) {
		if (posts.get(postId) == null) {
			return 0;
		}
		ArrayList<Post.Comment> comments = posts.get(postId).comments;
		if (comments == null) {
			return 0;
		}
		return comments.size();
	}
	
	private ArrayList<Post.Attachment> getAttachments(int postId) {
		if (posts.get(postId) == null) {
			return null;
		} 
		return posts.get(postId).attachments;
	}
	
	private ArrayList<Drawable> getImages(int postId) {
		if (posts.get(postId) == null) {
			return null;
		} 
		ArrayList<Drawable> images = new ArrayList<Drawable>();
		ArrayList<Post.Attachment> att = posts.get(postId).attachments;
		for (int i = 0; i < att.size(); i++) {
			if (att.get(i).mimeType.equals(MIMETYPE_JPEG) || att.get(i).mimeType.equals(MIMETYPE_PNG)) {
				images.add(att.get(i).image);
			}
		}
		return images;
	}
	
	private Bitmap getThumbnail(int postId) {
		if (posts.get(postId) == null) {
			return null;
		} 
		return posts.get(postId).attachments.get(0).thumbnail;		
	}
	
	
	
	// ---------------- static and utility stuff, mostly for the picture things... ----------------
	
	public static PostCollection getPostCollection() {
		if (postCollection == null) {
			postCollection = new PostCollection();
		}
		return postCollection;
	}
	
	public static PostCollection getFilteredPostCollection(String[] query, boolean filterFromFavorites) {
		PostCollection sourcePc;
		if (filterFromFavorites) {
			sourcePc = getFavoritesPostCollection();
		} else {
			sourcePc = getPostCollection();
		}
		PostCollection filteredPostCollection = new PostCollection();
		for (Post p : sourcePc.posts.values()) {
			boolean addToPC = true;
			for (String arg : query) {
				if (!(p.title.toLowerCase().contains(arg.toLowerCase()) || p.content.toLowerCase().contains(arg.toLowerCase())) ) {
					addToPC = false;
					break;
				}
			} 
			if (addToPC) {
				filteredPostCollection.addPost(p);
			}
		}
		return filteredPostCollection;
	}
	
	public static PostCollection getFavoritesPostCollection() {
		if (postCollection == null) {
			postCollection = new PostCollection();
		}
		PostCollection favoritesPostCollection = new PostCollection();
		for (Post p : postCollection.posts.values()) {
			if (p.favorite == 'Y') {
				favoritesPostCollection.addPost(p);
			}
		}
		return favoritesPostCollection;
	}
	
	private static CharSequence trimTrailingWhitespace(CharSequence source) {
	    if(source == null)
	        return "";
	    int i = source.length();
	    // loop back to the first non-whitespace character
	    while(--i >= 0 && Character.isWhitespace(source.charAt(i))) {
	    }
	    return source.subSequence(0, i+1);
	}
	
	public enum DrawableType {LOGO_COLOR, LOGO_GREYSCALE, POST_IMAGE, LIST_IMAGE, FULLSCREEN_IMAGE};
		
	
	public static void setImage(ImageView v, DrawableType type) {
		if (type == DrawableType.POST_IMAGE || type == DrawableType.LIST_IMAGE || type == DrawableType.FULLSCREEN_IMAGE)
			return;
		setImage(v,type,-1,-1,null);
	}
	
	public static void setImage(ImageView v, DrawableType type, SetImageListener listener) {
		if (type == DrawableType.POST_IMAGE || type == DrawableType.LIST_IMAGE || type == DrawableType.FULLSCREEN_IMAGE)
			return;
		setImage(v,type,-1,-1,listener);
	}
	
	public static void setImage(ImageView v, DrawableType type, int postId) {
		setImage(v,type,postId,-1,null);
	}
	
	public static void setImage(ImageView v, DrawableType type, int postId, SetImageListener listener) {
		setImage(v,type,postId,-1,listener);
	}
	
	public static void setImage(ImageView v, DrawableType type, int postId, int position) {
		setImage(v,type,postId,position,null);
	}
	
	/**
	 * Utility method to set a Drawable to an ImageView and fitting it to screen.
	 * @param v The ImageView in with the Drawable needs to be set.
	 * @param type The type of image you need to set, can be set to:
	 * 		DrawableType.LOGO_COLOR for the logo in color
	 * 		DrawableType.LOGO_GREYSCALE for the logo in greyscale
	 * 		DrawableType.POST_IMAGE for the image belonging to a specific post in a detail screen, the image will keep its current ratio 
	 * 			(if the post does not have an image the greyscale logo will be used instead).
	 * 		DrawableType.LIST_IMAGE for the image belonging to a specific post in a list, the image will be scaled to fit
	 * 			(if the post does not have an image the greyscale logo will be used instead).
	 * @param postId The id of the post. Ignored for LOGO types.
	 * @param position The position image which should be used in case of multiple images per post. 
	 * 		Number as in an array of pictures (so first picture is 0, last picture is numberofpictures -1).
	 * 		If set to an invalid number (e.g. -1) the images will all be set with a delay between images. 
	 * 		Ignored when only 1 image is present for the post or for LOGO types.
	 */
	public static void setImage(ImageView v, DrawableType type, int postId, int position, SetImageListener listener) {
		if (v == null || type == null)
			return;
		
		PostCollection pc = getPostCollection();
		
		switch (type) {
		case LOGO_COLOR:
			setImage(v, ((GlobalState) GlobalState.getContext()).getResources().getDrawable(R.drawable.logo_color), 0, 0, listener);
			break;
		case LOGO_GREYSCALE:
			setImage(v, ((GlobalState) GlobalState.getContext()).getResources().getDrawable(R.drawable.logo_grey), 0, 0, listener);
			break;
		case POST_IMAGE:
			if (pc.countImages(postId) != 0) {
				if (pc.countImages(postId) > 1) {
					ArrayList<Drawable> images = pc.getImages(postId);
		
					if (position < 0 || position > images.size() ) {
						setMultipleImages(v, images, postId, listener);
					} else {
						setImage(v, images.get(position), postId, position, listener);
					}
				} else {
					setImage(v, pc.getImages(postId).get(0), postId, 0, listener);
				}
			} 
			break;
		case LIST_IMAGE:
			if (pc.countImages(postId) == 0) {
				setThumbnail(v, decodeThumbnailFromResources(GlobalState.getContext().getResources(), R.drawable.logo_grey), postId, listener);
			} else {
				setThumbnail(v, pc.getThumbnail(postId), postId, listener);
			} 
			break;
		case FULLSCREEN_IMAGE:
			if (pc.countImages(postId) != 0) {
				if (pc.countImages(postId) > 1) {
					ArrayList<Drawable> images = pc.getImages(postId);
					
					if (position < 0 || position > images.size() ) {
						setMultipleImages(v, images, postId, listener);
					} else {
						setImage(v, images.get(position), postId, position, listener);
					}
				} else {
					setImage(v, pc.getImages(postId).get(0), postId, 0, listener);
				}
			}
		default:
			break;
		}
	}
	
	private static void retrieveImages(final int postId, final boolean fullsized, final ImagesRetrievalListener listener) {
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Context c = GlobalState.getContext();
				PostCollection pc = getPostCollection();
				ArrayList<Post.Attachment> att = pc.getAttachments(postId); 
				for (Post.Attachment a : att) {
					if (a.mimeType.equals(MIMETYPE_JPEG) || a.mimeType.equals(MIMETYPE_PNG)) {
						
						try {
						
						boolean fileExists = true;
						File file = c.getFileStreamPath("" + postId + a.id);
						if (file == null || !file.exists())
							fileExists = false;
						if (fileExists) {
							try {
								if (fullsized) {
									FileInputStream fis = c.openFileInput("" + postId + a.id);
									a.image = Drawable.createFromStream(fis, "src");
									fis.close();
								} else {
									a.thumbnail = decodeThumbnailFromFile(c, "" + postId + a.id);
								}
							} catch (IOException e1) {
								Log.w("error retrieving file from internal storage", e1.toString());
							}
						} else {
							try {
								URL url = new URL(a.url); 
								InputStream is = url.openStream();
								Bitmap b = ((BitmapDrawable) Drawable.createFromStream(is, "src")).getBitmap();
								ByteArrayOutputStream stream = new ByteArrayOutputStream();
								if (a.mimeType.equals(MIMETYPE_JPEG)){
									b.compress(Bitmap.CompressFormat.JPEG, 100, stream);
								} else {
									b.compress(Bitmap.CompressFormat.PNG, 100, stream);
								}
								FileOutputStream fos = c.openFileOutput("" + postId + a.id, Context.MODE_PRIVATE);
								fos.write(stream.toByteArray());
								fos.close();
								
							} catch (IOException e2) {
								Log.w("error retrieving image from web or saving image in internal storage", e2.toString());
							}
							try {
								if (fullsized) {
									FileInputStream fis = c.openFileInput("" + postId + a.id);
									a.image = Drawable.createFromStream(fis, "src");
									fis.close();
								} else {
									a.thumbnail = decodeThumbnailFromFile(c, "" + postId + a.id);
								}
							} catch (IOException e3) {
								Log.w("error retrieving image from internal storage", e3.toString());
							}
						} 
					} catch (Exception e) {
						Log.e("Unexpected error for post " + postId + "/" + a.id + " titled: " + pc.getItemTitle(postId), e.toString());
					}
				} 
					
				}
				listener.onImagesRetrieved();
			}
		});
		t.start(); 
	}

	private static void setImage(final ImageView v, final Drawable d, final int postId, final int position, final SetImageListener listener) { 
		
		if (d == null) {
			retrieveImages(postId, true, new ImagesRetrievalListener() {
				
				@Override
				public void onImagesRetrieved() {
					setImage(v, getPostCollection().getImages(postId).get(position), postId, position, listener);
				}
			});
			return;
		}
		
		final Activity a = (Activity)v.getContext();
		a.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
								
				v.setAdjustViewBounds(true);
				v.setImageDrawable(d);
				if (listener != null) {
					listener.onImageSet();
				}
			}
		});
	}
	
	private static void setThumbnail(final ImageView v, final Bitmap b, final int postId, final SetImageListener listener) {
		
		if (b == null) {
			setThumbnail(v, decodeThumbnailFromResources(GlobalState.getContext().getResources(), R.drawable.logo_grey), postId, listener);
			retrieveImages(postId, false, new ImagesRetrievalListener() {
				
				@Override
				public void onImagesRetrieved() {
					setThumbnail(v, getPostCollection().getThumbnail(postId), postId, listener);
				}
			});
			return;
		}
		
		final Activity a = (Activity)v.getContext();
		a.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				
				v.setAdjustViewBounds(true);
				v.setImageBitmap(b);
				if (listener != null) {
					listener.onImageSet();
				}
			}
		});
	}

	private static void setMultipleImages(final ImageView v, final ArrayList<Drawable> images, final int postId, final SetImageListener listener) {
		
		if (activeHandlers == null) {
			activeHandlers = new SparseArray<Handler>();
		}
		String prefix;
		if (v.getContext().getResources().getBoolean(R.bool.isLandscape)) {
			prefix = "2";
		} else {
			prefix = "1";
		}
		final int uniqueId = Integer.valueOf(prefix + postId);
		if (activeHandlers.get(uniqueId) != null) {
			activeHandlers.get(uniqueId).removeCallbacksAndMessages(null);
		}
		final Handler handler = new Handler(); 
		activeHandlers.put(uniqueId, handler);
		Runnable runnable = new Runnable() {
			int j = 0;
			@Override
			public void run() {
				setImage(v, images.get(j), postId, j, listener);
				j++;
				if (j > images.size() - 1) {
					j = 0;
				}
				Activity a = (Activity) v.getContext();
				if (a instanceof HomeActivity && 
						((HomeActivity)a).getPostFragment(postId) != null) {
					handler.postDelayed(this, 2000);
				} else {
					activeHandlers.remove(uniqueId);
				}
			}
		};
		handler.post(runnable);
		
	}
	
	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) {
	
	        final int heightRatio = Math.round((float) height / (float) reqHeight);
	        final int widthRatio = Math.round((float) width / (float) reqWidth);
	
	        inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
	    }
	
	    return inSampleSize;
	}
	
	private static Bitmap decodeThumbnailFromFile(Context c, String filePath) throws IOException {
		FileInputStream fis = c.openFileInput(filePath);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
	    BitmapFactory.decodeStream(fis, null, options);
	    options.inSampleSize = calculateInSampleSize(options, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
	    options.inJustDecodeBounds = false;
		fis.close();
		
		fis = c.openFileInput(filePath);
	    Bitmap bm = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(fis, null, options), THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, false);
	    fis.close();
	    return bm;
	}
	
	private static Bitmap decodeThumbnailFromResources(Resources res, int id) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, id);
		options.inSampleSize = calculateInSampleSize(options, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
	    options.inJustDecodeBounds = false;
	    return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, id, options), THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, false);
	}
	
	public static void cleanUpPostCollection(int max) {
		PostCollection pc = getPostCollection();
		if (pc.getAllPosts().size() <= max) {
			DataLoader.writePostCollectionToDisk(pc);
			return;
		}
		for (int i = max; i < pc.getAllPosts().size(); ) {
			int postId = pc.getAllPosts().get(i);
			if (pc.itemIsFavorite(postId)) {
				i++;
			} else {
				pc.removePost(postId);
			}
		}
		DataLoader.writePostCollectionToDisk(pc);
	}
	
	public static void cleanUpAttachments(int postId, ArrayList<Attachment> attachments) {
		Context c = GlobalState.getContext();
		for (Post.Attachment a : attachments) {
			if (a.mimeType.equals(MIMETYPE_JPEG) || a.mimeType.equals(MIMETYPE_PNG)) {
				boolean fileExists = true;
				File file = c.getFileStreamPath("" + postId + a.id);
				if (file == null || !file.exists())
					fileExists = false;
				if (fileExists) {
					boolean done = file.delete();
					if (!done) {
						Log.w("Error deleting file from internal storage", "" + a.id + ":" + a.url);
					}
				}
			}
		}
	}
	
	public interface SetImageListener {
		void onImageSet();
	}
	
	private interface ImagesRetrievalListener {
		void onImagesRetrieved();
	}
}
