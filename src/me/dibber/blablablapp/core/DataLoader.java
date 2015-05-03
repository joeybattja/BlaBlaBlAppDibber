package me.dibber.blablablapp.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import me.dibber.blablablapp.core.Post.Attachment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class DataLoader {
	
	public DataLoader() {
		posts = ( (GlobalState) GlobalState.getContext() ).getPosts();
		state = State.IDLE;
	}
	
	/**
	 * Sets the data source (URL) to use.
	 * @param path the URL to use.
	 * Must call this method before prepareAsync() in order for the target path to become effective thereafter.
	 * @throws MalformedURLException if path could not be parsed as a URL. 
	 */
	public void setDataSource(String path) throws MalformedURLException {
		url = new URL(path);
		state = State.INITIALIZED;
	}
	
	public void isInSynchWithExistingPosts(boolean synched) {
		isInSynchWithExistingPosts = synched;
	}
	
	public void prepareAsync() {
		if (state != State.INITIALIZED) {
			throw new IllegalStateException("Dataloader not yet initialized. Call setDataSource() first.");
		}
		state = State.BUSY;
		parseJSON();
	}
	
	public void setDataLoaderListener(DataLoaderListener dataLoaderListener) {
		dll = dataLoaderListener;
	}
	
	public void reset() {
		state = State.IDLE;
		url = null;
	}
	

    // *****************  private implementation  *******************
	
	private URL url;
	private PostCollection posts;	
	private DataLoaderListener dll;
	private boolean isInSynchWithExistingPosts;
	
	public static String FILE_LOCATION = "Postdata";
	
	State state;
	private enum State {
		IDLE, INITIALIZED, BUSY, DONE
	}
	
	private void done() {
		state = State.DONE;
	}
	
	private void parseJSON() {

		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// first read from disk 
				final Context c = GlobalState.getContext();
				File file = c.getFileStreamPath(FILE_LOCATION);
				boolean fileExists = true;
				if (file == null || !file.exists())
					fileExists = false;
				if (fileExists) {
					try {
						FileInputStream fin = new FileInputStream(file);
						BufferedReader bin = new BufferedReader(new InputStreamReader(fin));
						StringBuilder sb = new StringBuilder();
						String s;
						while ( (s = bin.readLine()) != null) {
							sb.append(s);
						}
						bin.close();
						JSONreceived(sb.toString(), false);
					} catch (IOException e) {
						Log.w("Error trying to read file from disk", e.toString());
						dll.onDataLoaderDiskDone(false);
					}
				}
				dll.onDataLoaderDiskDone(true);
				
				// then read from network
				try {
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					InputStream in = connection.getInputStream();
					BufferedReader r = new BufferedReader(new InputStreamReader(in));
					StringBuilder data = new StringBuilder();
					String line;
					while ( (line = r.readLine()) != null) {
						data.append(line);
					}
					r.close();
					JSONreceived(data.toString(), true);
				} catch (IOException e) {
					Log.w("Error trying to setup connections", e.toString());
					dll.onDataLoaderOnlineDone(false);
				}
				dll.onDataLoaderOnlineDone(true);

				done();
				
				// write the current postcollection to disk
				writePostCollectionToDisk(posts);
				state = State.DONE;
				}
		});
		t.start();
	}
	
	public static void writePostCollectionToDisk(final PostCollection pc) {
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				JSONObject writeObj = new JSONObject();
				ArrayList<Integer> postIds = (ArrayList<Integer>) pc.getAllPosts();
				
				try {
					writeObj.put("status", "OK");
					writeObj.put("count", postIds.size());
					JSONArray postsArr = new JSONArray();
					for (int i = 0; i < postIds.size(); i++) {
						int postId = postIds.get(i);
						Post p = pc.getPost(postId);
						JSONObject postObj = new JSONObject();
						
						postObj.put("id", Integer.toString(postId));
						postObj.put("title", p.title);
						postObj.put("url", p.url);
						postObj.put("content", p.content);
						
						JSONObject authObj = new JSONObject();
						authObj.put("id", Integer.toString(p.id));
						authObj.put("name", p.authorname);
						postObj.put("author", authObj);
						
						if (p.date != null) {
							SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
							postObj.put("date", df.format(p.date));
						}
						
						JSONArray attArr = new JSONArray();
						for (Attachment att : p.attachments) {
							JSONObject attObj = new JSONObject();
							attObj.put("id", Integer.toString(att.id));
							attObj.put("url", att.url);
							attObj.put("mime_type", att.mimeType);
							attArr.put(attObj);
						}
						postObj.put("attachments", attArr);
						
						/*JSONArray commArr = new JSONArray();
						for (Comment cmnt : p.comments) {
							JSONObject cmntObj = new JSONObject();
							cmntObj.put("id", Integer.toString(cmnt.id));
							cmntObj.put("parent", Integer.toString(cmnt.parent));
							cmntObj.put("author", cmnt.author);
							cmntObj.put("content", cmnt.content);
							if (cmnt.date != null) {
								SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
								cmntObj.put("date", df.format(cmnt.date));
							}
							commArr.put(cmntObj);
						}
						postObj.put("comments", commArr);*/
						
						postObj.put("comment_status", p.commentstatus);
						postObj.put("comment_count", p.commentcount);
						if (p.favorite == 'Y') {
							postObj.put("favorite", "Y");
						} else {
							postObj.put("favorite", "N");
						}
						postsArr.put(postObj);
					}
					
					writeObj.put("posts", postsArr);
					
				} catch (JSONException e) {
					Log.e("error reading postcollection while creating JSON for disk", e.toString());
				}
				
				try {
					Context c = GlobalState.getContext();
					FileOutputStream os = c.openFileOutput(FILE_LOCATION, Context.MODE_PRIVATE);
					os.write(writeObj.toString().getBytes());
					os.close();
				} catch (FileNotFoundException e) {
					Log.e("Error trying to write data in internal storage", e.toString());
				} catch (IOException e) {
					Log.e("Error trying to write data in internal storage", e.toString());
				}
			}
		});
		t.start();
	}
	
	
	private void JSONreceived(String JSONobject, boolean synchedOnline) {

		try {
			JSONObject obj = new JSONObject(JSONobject);
			if (obj == null || !getStringFromJSON(obj,"status").equals("OK")) {
				Log.w("error parsing the JSON object", "Status is not 'OK'");
				return;
			}
			try {
				JSONArray postsArr = obj.getJSONArray("posts");
				JSONcreatePosts(postsArr, synchedOnline);
			} catch (JSONException errorPosts) {
				// no posts array available in object
			}
			try {
				JSONArray commentsArr = obj.getJSONArray("comments");
				int postIdParam = 0;
				String[] params = url.getQuery().split("&");
				for (String param : params) {
					if (param.split("=")[0].equals("postId")) {
						try {
							postIdParam = Integer.parseInt((param.split("=")[1]));
						} catch (NumberFormatException e) {
							throw new JSONException("no valid value given for postId");
						}
						break;
					}
				}
				Post post = posts.getPost(postIdParam);
				if (post != null) {
					JSONcreateComments(commentsArr, post);
				}
			} catch (JSONException errorComments) {
				// no comments array available in object
			}

			
		} catch (JSONException e) {
			Log.w("error parsing the JSON object", e.toString());
		}
	}
	
	
	private void JSONcreatePosts(JSONArray postdata, boolean synchedOnline) throws JSONException {

		for (int i = 0; i < postdata.length(); i++) {
			
			JSONObject postobj = postdata.getJSONObject(i);
			
			int id = Integer.parseInt(getStringFromJSON(postobj,"id") );
			Post p = posts.getPost(id);
			if (p == null) {
				p = new Post();
				p.id = id;
			}
			p.url = getStringFromJSON(postobj,"url");
			p.title = getStringFromJSON(postobj,"title");
			p.content = getStringFromJSON(postobj,"content");
			try {
			p.authorname = getStringFromJSON(postobj.getJSONObject("author"),"name");
			p.authorid = Integer.parseInt(getStringFromJSON(postobj.getJSONObject("author"),"id") );
			} catch (JSONException e) {
				p.authorname = null;
				p.authorid = 0;
			}
			try {
			SimpleDateFormat  df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
				p.date = df.parse(getStringFromJSON(postobj,"date"));
			} catch (ParseException e) {
				Log.d("error parsing date from JSON of post " + p.id + " " + p.title, e.toString());
			}
			
			// attachments, can be empty (images will not yet be downloaded):
			ArrayList<Post.Attachment> tempAttachments = new ArrayList<Post.Attachment>();
			try {
				JSONArray att = postobj.getJSONArray("attachments");
				for (int j = 0; j < att.length(); j++) {
					Post.Attachment a = new Post.Attachment();
					try {
						a.id = Integer.parseInt(getStringFromJSON(att.getJSONObject(j),"id"));
					} catch (NumberFormatException e) {
						a.id = 0;
						Log.d("Error parsing id of an attachment " + j + " of post " + p.id + " " + p.title, e.toString());
					}
					a.url = getStringFromJSON(att.getJSONObject(j),"url");
					a.mimeType = getStringFromJSON(att.getJSONObject(j),"mime_type");
					tempAttachments.add(a);
				}
			} catch (JSONException e) {
				// no "attachments" object in JSON 
			}
			if (p.attachments != null && !attachmentsEqual(tempAttachments, p.attachments)) {
				PostCollection.cleanUpAttachments(p.id, p.attachments);
			}
			p.attachments = tempAttachments;
			
			// try to read comments, will eventually be empty in the get_recent_posts
			try {
				JSONArray cmnts = postobj.getJSONArray("comments");
				JSONcreateComments(cmnts,p);
			} catch (JSONException e) {
				// no "comments" object in JSON
			}
			
			try {
				p.commentstatus = postobj.getBoolean("comment_status");
			} catch (JSONException e) {
				if ("open".equals(getStringFromJSON(postobj,"comment_status"))) {
					p.commentstatus = true;
				} else {
					p.commentstatus = false;
				}
			}
			try {
				p.commentcount = Integer.parseInt(getStringFromJSON(postobj,"comment_count"));
			} catch (NumberFormatException e) {
				Log.e("no commentcount", e.toString());
				p.commentcount = 0;
			}
			if (p.favorite != 'Y' && p.favorite != 'N') {
				if (getStringFromJSON(postobj, "favorite") != null && getStringFromJSON(postobj, "favorite").charAt(0) == 'Y') {
					p.favorite = 'Y';
				} else {
					p.favorite = 'N';
				}
			}
			if (synchedOnline && isInSynchWithExistingPosts) {
				int oldPost = ((GlobalState)GlobalState.getContext()).getOldestSynchedPost();
				if (posts.getItemDate(oldPost) == null || posts.getItemDate(oldPost).after(p.date)) {
					((GlobalState)GlobalState.getContext()).setOldestSynchedPost(p.id);
				}
			}
			posts.addPost(p);
		}
	}
	
	private void JSONcreateComments(JSONArray commentData, Post p) throws JSONException {
		p.comments = new ArrayList<Post.Comment>();
		for (int i = 0; i < commentData.length(); i++) {
			JSONObject commentobj = commentData.getJSONObject(i);
			Post.Comment c = new Post.Comment();
			try {
				c.id = Integer.parseInt(getStringFromJSON(commentobj, "id"));
			} catch (NumberFormatException e) {
				c.id = 0;
				Log.w("Error parsing id of a comment " + i + " of post " + p.id + " " + p.title, e.toString());
			}
			try {
				c.parent = Integer.parseInt(getStringFromJSON(commentobj,"parent"));
			} catch (NumberFormatException e) {
				c.parent = 0;
				Log.w("Error parsing parent of a comment " + i + " of post " + p.id + " " + p.title + ", with comment id " + c.id, e.toString());
			}
			c.author = getStringFromJSON(commentobj,"author");
			c.content = getStringFromJSON(commentobj,"content");
			try {
				SimpleDateFormat  df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
					c.date = df.parse(getStringFromJSON(commentobj,"date"));
			} catch (ParseException e) {
					Log.w("Error parsing date from JSON comment " + i + " of post " + p.id + " " + p.title, e.toString());
			}
			p.comments.add(c);
		}
	}

	/**
	 * Checks if two lists of attachments are similar.
	 * @param tempAttachments First ArrayList of attachments
	 * @param attachments Second ArrayList of attachments
	 * @return true if they are equal, false otherwise
	 */
	private boolean attachmentsEqual(ArrayList<Attachment> tempAttachments,
			ArrayList<Attachment> attachments) {
		if (attachments == null && tempAttachments == null) { 
			return true;
		}
		if (attachments == null || tempAttachments == null) {
			return false;
		}
		if (attachments.size() != tempAttachments.size()) {
			return false;
		}
		int countIds = 0;
		for (Attachment a : attachments) {
			countIds += a.id;
		} 
		int countTempIds = 0;
		for (Attachment b : tempAttachments) {
			countTempIds += b.id;
		} 
		return (countIds == countTempIds);
	}

	/**
	 * Utility method to parse a String from JSON object, or returns null if the String is not present.
	 * Since this method will not throw a JSONException, it can be used to prevent lots of try/catch clauses.
	 * @param JSONObj the JSONObject
	 * @param name the name of the String
	 * @return returns the String or null of not present
	 */
	private String getStringFromJSON(JSONObject JSONObj, String name) {
		try {
			return JSONObj.getString(name);
		} catch (JSONException e) {
			return null;
		}
	}
	
	public interface DataLoaderListener {
		
		void onDataLoaderDiskDone(boolean success);
		void onDataLoaderOnlineDone(boolean success);
		
	}
	
}
