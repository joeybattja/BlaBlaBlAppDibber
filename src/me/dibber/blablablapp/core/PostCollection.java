package me.dibber.blablablapp.core;

import java.io.File;
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

import me.dibber.blablablapp.R;
import me.dibber.blablablapp.activities.HomeActivity;
import me.dibber.blablablapp.core.Post.Attachment;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

public class PostCollection {
			
	private static PostCollection postCollection;
	private final static String MIMETYPE_JPEG = "image/jpeg";
	private final static String MIMETYPE_PNG = "image/png";
	private final static String MIMETYPE_YOUTUBE = "video/youtube";
	private final static int THUMBNAIL_WIDTH = 240;
	private final static int THUMBNAIL_HEIGHT = 180;
	private static SparseArray<Handler> activeHandlers;
	
	private LruCache<String, Bitmap> mMemoryCache;
	private final static String THUMBNAIL = "thumb";
	private final static String IMAGE = "img";
	
	private boolean isPodcastCollection;
	private boolean isFavoriteCollection;
	private String[] filter;
	
	private TreeMap<Integer,Post> posts;
	
	private PostCollection() {
		posts = new TreeMap<Integer,Post>();
	}
	
	public void showFavorite(boolean showFavorite) {
		isFavoriteCollection = showFavorite;
	}
	
	public void showPodcast(boolean showPodcast) {
		isPodcastCollection = showPodcast;
	}
	
	public void setFilter(String[] filter) {
		this.filter = filter;
	}
	
	public synchronized void addPost(Post post) {
		posts.put(post.id, post);
	}
	
	
	public synchronized void removePost(int postId) {
		cleanUpAttachments(postId, getAttachments(postId));
		posts.remove(postId);
	}
	
	public Post getPost(int postId) {
		return	posts.get(postId);
	}
	
	public Post getPostByURL(String URL) {
		for (Post p : posts.values()) {
			if (p.url != null && p.url.equals(URL)) {
				return p;
			}
		}
		return null;
	}
	
	public synchronized List<Integer> getAllPosts() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		if (isFavoriteCollection) {
			for (Integer i : posts.keySet()) {
				if (itemIsFavorite(i)) {
					list.add(i);
				}
			}
		} else if (isPodcastCollection) {
			for (Integer i : posts.keySet()) {
				if (itemHasPodcast(i)) {
					list.add(i);
				}
			}
		} else {
			for (Integer i : posts.keySet()) {
				list.add(i);
			}
		}
		
		if (filter != null) {
			ArrayList<Integer> filteredList = new ArrayList<Integer>();
			for (int j : list) {
				boolean addToList = true;
				for (String arg : filter) {
					if (!(getPost(j).title.toLowerCase().contains(arg.toLowerCase()) || getPost(j).content.toLowerCase().contains(arg.toLowerCase())) ) {
						addToList = false;
						break;
					}
				} 
				if (addToList) {
					filteredList.add(j);
				}
			}
			list = filteredList;
		}
		
		Collections.sort(list, new Comparator<Integer>() {

			@Override
			public int compare(Integer lhs, Integer rhs) {
				
				if (isPodcastCollection) {
					return getItemPodcastDate(rhs).compareTo(getItemPodcastDate(lhs));
				} else {
					return getItemDate(rhs).compareTo(getItemDate(lhs));
				}
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
				int templhs = lhs;
				int temprhs = rhs;
				int rhsParent,lhsParent,root = 0;
				int endlessLoopCheck = 0;
				while (true) {
					endlessLoopCheck++;
					if (endlessLoopCheck > 10000) {
						Log.e("POSTCOLLECTION ENDLESS LOOP!!", "params: " + lhs + "," + rhs);
						break;
					}
					if (lhs.equals(rhs)) {
						// Shouldn't happen, unless there's some hickup showing the same comment twice.
						break;
					}
					templhs = lhs;
					temprhs = rhs;
					rhsParent = getCommentParent(postId, temprhs);
					lhsParent = getCommentParent(postId, templhs);
					while (rhsParent != root && temprhs != root && rhsParent != 0 && endlessLoopCheck < 10000) {
						endlessLoopCheck++;
						if (rhsParent == (int) lhs) {
							// If rhs is a child of lhs, lhs is earlier than rhs.  
							return -1;
						}
						temprhs = rhsParent;
						rhsParent = getCommentParent(postId, temprhs);
					}
					while (lhsParent != root && templhs != root && lhsParent != 0 && endlessLoopCheck < 10000) {
						endlessLoopCheck++;
						if (lhsParent == (int) rhs) {
							// If lhs is a child of rhs, lhs is later than rhs.  
							return 1;
						}
						templhs = lhsParent;
						lhsParent = getCommentParent(postId, templhs);
					}
					if (temprhs == templhs) {
						// both rhs and lhs are in the same trunk from the root, change the root and level lower:
						root = temprhs;
					} else {
						// rhs and lhs are both in different trunks from the root. Compare both trunks. 
						rhs = temprhs;
						lhs = templhs;
						break;
					}
				} 
				return getCommentDate(postId, lhs).compareTo(getCommentDate(postId, rhs));
			}
		});

		return comments;
	}
	
	public String getCommentAuthor(int postId, int commentId) {
		String author = "";
		if (posts.get(postId) != null && posts.get(postId).comments != null) {
			for (Post.Comment c : posts.get(postId).comments) {
				if (c.id == commentId) {
					if (c.author == null) { 
						author = "";
					} else {
						author = c.author;
					}
				} 
			}
		}
		if (author.isEmpty()) {
			author = GlobalState.getContext().getResources().getString(R.string.guest);
		}
		return author;
	}
	
	public CharSequence getCommentContent(int postId, int commentId) {
		if (posts.get(postId) == null) {
			return " ";
		} 
		if (posts.get(postId).comments == null) {
			return " ";
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
			return new Date(0);
		} 
		if (posts.get(postId).comments == null) {
			return new Date(0);
		} 
		for (Post.Comment c : posts.get(postId).comments) {
			if (c.id == commentId) {
				return c.date;
			}
		} 
		return new Date(0);
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
			return new Date(0);
		} 
		Date d = posts.get(postId).date;
		
		if (d != null) {
			return d;
		} else {
			return new Date(0);
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
	
	public String getItemPodcastAudioUrl(int postId) {
		if (posts.get(postId) == null) {
			return null;
		} else if (posts.get(postId).podcast == null) {
			return null;
		} else {
			return posts.get(postId).podcast.audioUrl;
		}
	}
	
	public Date getItemPodcastDate(int postId) {
		if (posts.get(postId) == null) {
			return new Date(0);
		}  else if (posts.get(postId).podcast == null) {
			return new Date(0);
		}
		Date d = posts.get(postId).podcast.date;
		if (d != null) {
			return d;
		} else {
			return new Date(0);
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
			return null;
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
		return posts.get(postId).commentcount;
	}
	
	public boolean itemHasPodcast(int postId) {
		if (posts.get(postId) == null) {
			return false;
		}
		return posts.get(postId).podcast != null;
	}
	
	private ArrayList<Post.Attachment> getAttachments(int postId) {
		if (posts.get(postId) == null) {
			return new ArrayList<Post.Attachment>();
		} 
		return posts.get(postId).attachments;
	}
	
	private ArrayList<Bitmap> getImages(int postId) {
		if (posts.get(postId) == null) {
			return new ArrayList<Bitmap>();
		} 
		ArrayList<Bitmap> images = new ArrayList<Bitmap>();
		ArrayList<Post.Attachment> att = posts.get(postId).attachments;
		for (int i = 0; i < att.size(); i++) {
			if ((att.get(i).mimeType.equals(MIMETYPE_JPEG) || att.get(i).mimeType.equals(MIMETYPE_PNG))) {
				images.add(getImagefromMemoryCache(postId,att.get(i).id, false));
			}
		}
		return images;
	}
	
	private void initMemoryCache() {
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 4;
		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	            // The cache size will be measured in kilobytes rather than
	            // number of items.
	            return bitmap.getByteCount() / 1024;
	        }
	    };
	}
	
	private Bitmap getImagefromMemoryCache(int postId, int attachmentId, boolean thumbnail) {
		if (postId == 0 || attachmentId == 0) {
			return null;
		}
		String imagepath = ""+postId+attachmentId;
		if (thumbnail) {
			imagepath = imagepath+THUMBNAIL;
		} else {
			imagepath = imagepath+IMAGE;
		}
		if (mMemoryCache == null) {
			initMemoryCache();
		}
		return mMemoryCache.get(imagepath);
	}
	
	private void addImageToMemoryCache(int postId, int attachmentId, Bitmap bitmap, boolean thumbnail) {
		if (postId == 0 || attachmentId == 0) {
			return;
		}
		String imagepath = ""+postId+attachmentId;
		if (thumbnail) {
			imagepath = imagepath+THUMBNAIL;
		} else {
			imagepath = imagepath+IMAGE;
		}
		if (mMemoryCache == null) {
			initMemoryCache();
		}
	    if (getImagefromMemoryCache(postId,attachmentId, thumbnail) == null) {
	        mMemoryCache.put(imagepath, bitmap);
	    }
	}
	
	/**
	 * Returns the maximum ratio as a double height/width of all images related to this post. 
	 * If this is called before all images are retrieved it will return 0
	 * @param postId
	 * @return the maximum ratio h/w as a double, or 0 if the images are not yet received.
	 */
	public double maxImageRatio(int postId) {
		if (posts.get(postId) == null) {
			return 0;
		}
		double maxRatio = 0;
		for (Bitmap d : getPostCollection().getImages(postId)) {
			if (d == null) {
				return 0;
			}
			double ratio = (double) d.getHeight() / d.getWidth();
			if (ratio > maxRatio) {
				maxRatio = ratio;
			}
		}
		return maxRatio;
	}
	
	public void getItemThumbnail(int postId, ImagesRetrievalListener imageRetrievalListener) {
		if (posts.get(postId) == null) {
			return;
		}
		retrieveImage(postId, 0, true, imageRetrievalListener);
	}
	
	private Bitmap getThumbnail(int postId) {
		if (posts.get(postId) == null || posts.get(postId).attachments.size() == 0) {
			return null;
		} 
		return getImagefromMemoryCache(postId,posts.get(postId).attachments.get(0).id, false);
	}
	
	// ---------------- static and utility stuff, mostly for the picture things... ----------------
	
	public static PostCollection getPostCollection() {
		if (postCollection == null) {
			postCollection = new PostCollection();
		}
		return postCollection;
	}
	
/*	private static PostCollection getFilteredPostCollection(String[] query, PostCollection sourceCollection ) {
		PostCollection filteredPostCollection = new PostCollection();
		for (Post p : sourceCollection.posts.values()) {
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
	
	private static PostCollection getFavoritesPostCollection() {
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
	
	private static PostCollection getPodCastPostCollection() {
		if (postCollection == null) {
			postCollection = new PostCollection();
		}
		PostCollection podcastCollection = new PostCollection();
		for (Post p : postCollection.posts.values()) {
			if (p.podcast != null) {
				podcastCollection.addPost(p);
			}
		}
		podcastCollection.isPodcastCollection = true;
		return podcastCollection;
	}*/
	
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
			setImage(v, BitmapFactory.decodeResource(v.getContext().getResources(),R.drawable.logo_color), 0, 0, listener);
			break;
		case LOGO_GREYSCALE:
			setImage(v, BitmapFactory.decodeResource(v.getContext().getResources(), R.drawable.logo_grey), 0, 0, listener);
			break;
		case POST_IMAGE:
			if (pc.countImages(postId) != 0) {
				if (pc.countImages(postId) > 1) {
					ArrayList<Bitmap> images = pc.getImages(postId);
		
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
					ArrayList<Bitmap> images = pc.getImages(postId);
					
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
	
	private static void retrieveImage(final int postId, final int position, final boolean thumbnail, final ImagesRetrievalListener listener) {
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Context c = GlobalState.getContext();
				PostCollection pc = getPostCollection();
				int i = 0;
				Post.Attachment img = null;
				for (Post.Attachment a : pc.getAttachments(postId)) {
					if (a.mimeType.equals(MIMETYPE_JPEG) || a.mimeType.equals(MIMETYPE_PNG)) {
						i++;
						if (i > position) {
							img = a;
							break;
						}
					}
				}
				if (img == null) {
					listener.onImageRetrievedFailed(new Error("Post does not have an image for given position"));
					return;
				}
				Bitmap bitmap = null;
				String filePath = "" + postId + img.id;
				if (thumbnail) {
					filePath = filePath+THUMBNAIL;
				} else {
					filePath = filePath+IMAGE;
				}
				
				// First try to get the bitmap from memory
				bitmap = pc.getImagefromMemoryCache(postId,img.id,thumbnail);
				if (bitmap != null) {
					listener.onImageRetrievedSuccess(bitmap);
					return;
				}
				
				// Secondly try to get the bitmap from disk
				try {
					bitmap = decodeImageFromDisk(c, filePath);
					if (bitmap != null) {
						pc.addImageToMemoryCache(postId,img.id, bitmap, thumbnail);
						listener.onImageRetrievedSuccess(bitmap);
						return;
					}
				} catch (IOException e1) {
					Log.w("error retrieving file from internal storage", e1.toString());
				}
				
				// Last, try to download the bitmap from the URL
				try {
					URL url = new URL(img.url); 
					bitmap = decodeImageFromURL(c, url, thumbnail); 
					if (bitmap != null) {
						storeImageOnDisk(c, bitmap, filePath, img.mimeType);
						pc.addImageToMemoryCache(postId,img.id, bitmap, thumbnail);
						listener.onImageRetrievedSuccess(bitmap);
						return;
					}
				} catch (IOException e2) {
					Log.w("error retrieving image from web or saving image in internal storage", e2.toString());
				}
				if (bitmap != null) {
					listener.onImageRetrievedSuccess(bitmap);
				} else {
					listener.onImageRetrievedFailed(new Error("Bitmap cannot be retrieved"));
				}
			}
		});
		t.start(); 
	}

	private static void setImage(final ImageView v, final Bitmap d, final int postId, final int position, final SetImageListener listener) { 
		
		if (d == null) {
			retrieveImage(postId, position, false, new ImagesRetrievalListener() {

				@Override
				public void onImageRetrievedSuccess(Bitmap bitmap) {
					setImage(v, bitmap, postId, position, listener);
				}

				@Override
				public void onImageRetrievedFailed(Error e) {
					Log.e("Error on retrieving image", e.toString());
				}
			});
			return;
		}
		
		final Activity a = (Activity)v.getContext();
		a.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
								
				v.setAdjustViewBounds(true);
				v.setImageBitmap(d);
				
				if (listener != null) {
					listener.onImageSet();
				}
			}
		});
	}
		
	private static void setThumbnail(final ImageView v, final Bitmap b, final int postId, final SetImageListener listener) {
		
		if (b == null) {
			setThumbnail(v, decodeThumbnailFromResources(GlobalState.getContext().getResources(), R.drawable.logo_grey), postId, listener);
			retrieveImage(postId, 0, true, new ImagesRetrievalListener() {

				@Override
				public void onImageRetrievedSuccess(Bitmap bitmap) {
					setThumbnail(v, bitmap, postId, listener);
				}

				@Override
				public void onImageRetrievedFailed(Error e) {
					Log.e("Error on retrieving image", e.toString());
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
			}
		});
	}

	private static void setMultipleImages(final ImageView v, final ArrayList<Bitmap> images, final int postId, final SetImageListener listener) {
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
		final Handler handler = new Handler(Looper.getMainLooper()); 
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
	
	private static Bitmap decodeImageFromURL(Context c, URL url, boolean thumbnail) throws IOException {
		try {
			InputStream is = url.openStream();
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
		    BitmapFactory.decodeStream(is, null, options);
		    
		    if (thumbnail) {
		    	options.inSampleSize = calculateInSampleSize(options, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
		    } else {
		    	WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
				Display display = wm.getDefaultDisplay();
				Point imageSize = new Point();
				display.getSize(imageSize);
				
				int maxSize = imageSize.x > imageSize.y ? imageSize.y : imageSize.x;
				int lowSize;
				double ratio;
				if (options.outHeight > options.outWidth) {
					ratio = (double) options.outWidth / options.outHeight;
					lowSize = (int) (maxSize * ratio);
					options.inSampleSize =  calculateInSampleSize(options, lowSize, maxSize);
				} else {
					ratio = (double) options.outHeight / options.outWidth;
					lowSize = (int) (maxSize * ratio);
					options.inSampleSize =  calculateInSampleSize(options, maxSize, lowSize);
				}
		    }
		    options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			options.inDither = true;
			is.close();
			is = url.openStream();
			Bitmap bitmap;
			if (thumbnail) {
				bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(is, null, options), THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, false);
			} else {
				bitmap = BitmapFactory.decodeStream(is, null, options);
			}
			is.close();
		    return bitmap;
		} catch (OutOfMemoryError e) {
			Log.e("OutOfMemoryError while decoding thumbnail from URL", e.toString());
			return null;
		}
	}
	
	private static Bitmap decodeImageFromDisk(Context c, String filePath) throws IOException {
		File file = c.getFileStreamPath(filePath);
		if (file == null || !file.exists()) {
			return null;
		}
		try {
			InputStream is = c.openFileInput(filePath);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			options.inDither = true;
			Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
			is.close();
			return bitmap;
		} catch (OutOfMemoryError e) {
			Log.e("OutOfMemoryError while decoding image from URL", e.toString());
			return null;
		}
	}
	
	private static void storeImageOnDisk(Context context, Bitmap bitmap, String filePath, String mimeType) throws IOException {
		try {
			FileOutputStream fos = context.openFileOutput(filePath, Context.MODE_PRIVATE);
			if (mimeType.equals(MIMETYPE_JPEG)){
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			} else if (mimeType.equals(MIMETYPE_PNG)) {
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			}
			fos.close();
		} catch (OutOfMemoryError e) {
			Log.e("OutOfMemoryError while storing image on disk", e.toString());
		}
	}
	
	private static Bitmap decodeThumbnailFromResources(Resources res, int id) {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeResource(res, id);
			options.inSampleSize = calculateInSampleSize(options, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
		    options.inJustDecodeBounds = false;
		    return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, id, options), THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, false);
		} catch (OutOfMemoryError e) {
			Log.e("OutOfMemoryError while decoding thumbnail from resources", e.toString());
			return null;
		}

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
	
	public interface ImagesRetrievalListener {
		void onImageRetrievedSuccess(Bitmap bitmap);
		void onImageRetrievedFailed(Error e);
	}
}
