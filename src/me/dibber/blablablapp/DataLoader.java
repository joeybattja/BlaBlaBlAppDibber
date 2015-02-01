package me.dibber.blablablapp;

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

import me.dibber.blablablapp.Post.Attachment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class DataLoader {
	
	public final static int JSON = 0;
	public final static int XML = 1;
	
	
	public DataLoader() {
		posts = ( (GlobalState) GlobalState.getContext() ).getPosts();
		state = State.IDLE;
		setStreamType(JSON);
	}
	
	/**
	 * Sets the data stream type for this DataLoader.
	 * Must call this method before prepareAsync() in order for the target stream type to become effective thereafter.
	 * @param streamType can be either Dataloader.JSON (0) or DataLoader.XML (1)
	 * @throws IllegalArgumentException if streamType is incorrect.
	 */
	public void setStreamType(int streamType) throws IllegalArgumentException {
		if (streamType < DataLoader.JSON || streamType > DataLoader.XML) {
			throw new IllegalArgumentException("Stream type " + streamType + " is incorrect. Possible feedtypes are: Dataloader.JSON (0) or DataLoader.XML (1)"); 
		} else {
			datatype = streamType;
		}
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
	
	public void prepareAsync() {
		if (state != State.INITIALIZED) {
			throw new IllegalStateException("Dataloader not yet initialized. Call setDataSource() first.");
		}
		state = State.BUSY;
		switch (datatype) {
		case DataLoader.JSON:
			parseJSON();
			break;
		case DataLoader.XML:
			parseXML();
			break;
		default:
			break;
		}
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
	private int datatype;
	private PostCollection posts;	
	private DataLoaderListener dll;
	private JSONArray postsdisk;
	private boolean internetConnection;
	
	private static String FILE_LOCATION = "Postdata";
	
	State state;
	private enum State {
		IDLE, INITIALIZED, BUSY, DONE
	}
	
	private void done() {
		state = State.DONE;
		dll.onDataLoaderDone(internetConnection);
	}
	 
    // *****************  implementation for JSON  *******************
	
	private void parseJSON() {

		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				postsdisk = new JSONArray();
				internetConnection = true;
				
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
						JSONreceived(sb.toString());
					} catch (IOException e) {
						Log.w("Error trying to read file from disk", e.toString());
					}
				}
				
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
					JSONreceived(data.toString());
				} catch (IOException e) {
					Log.w("Error trying to setup connections", e.toString());
					
					internetConnection = false;
				}
				
				done();
				try {
					JSONObject writeObj = new JSONObject();
					writeObj.put("status", "OK");
					writeObj.put("count", postsdisk.length());
					writeObj.put("posts", postsdisk);
					
					FileOutputStream os = c.openFileOutput(FILE_LOCATION, Context.MODE_PRIVATE);
					os.write(writeObj.toString().getBytes());
					os.close();
					
				} catch (JSONException e) {
					Log.w("Error trying to write data in internal storage", e.toString());
				} catch (FileNotFoundException e) {
					Log.w("Error trying to write data in internal storage", e.toString());
					e.printStackTrace();
				} catch (IOException e) {
					Log.w("Error trying to write data in internal storage", e.toString());
					e.printStackTrace();
				}
			}
		}
		);
		t.start();
	}
	
	/**
	 * Cleans up all posts from disk which are no longer in the PostCollection
	 * @param The PostCollection with all current posts
	 */
	public static void cleanUpPostsOnDisk(final PostCollection pc) {
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				JSONArray postsdsk = new JSONArray();
				
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
						JSONObject obj = new JSONObject(sb.toString());
						JSONArray jArr = obj.getJSONArray("posts");
						ArrayList<Integer> intList = new ArrayList<Integer>();  
						intList.addAll(pc.getAllPosts());
						for (int j = 0; j < jArr.length(); j++) {
							int index = intList.indexOf(jArr.getJSONObject(j).getInt("id"));
							if (index  >= 0) {
								postsdsk.put(jArr.getJSONObject(j));
								intList.remove(index);
							};
						}
						JSONObject writeObj = new JSONObject();
						writeObj.put("status", "OK");
						writeObj.put("count", postsdsk.length());
						writeObj.put("posts", postsdsk);
						
						FileOutputStream os = c.openFileOutput(FILE_LOCATION, Context.MODE_PRIVATE);
						os.write(writeObj.toString().getBytes());
						os.close();
						
						
					} catch (IOException | JSONException e) {
						Log.w("Error trying to read file from disk", e.toString());
					}
				}

				
			}
		});
		t.start();
	}
		
	
	private void JSONreceived(String JSONobject) {

		try {
			JSONObject obj = new JSONObject(JSONobject);
			if (obj == null || !getStringFromJSON(obj,"status").equals("OK")) {
				Log.w("error parsing the JSON object", "Status is not 'OK'");
				return;
			}
			JSONcreatePosts(obj.getJSONArray("posts"));
		} catch (JSONException e) {
			Log.w("error parsing the JSON object", e.toString());
		}
	}
	
	
	private void JSONcreatePosts(JSONArray postdata) throws JSONException {

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
			p.author = getStringFromJSON(postobj.getJSONObject("author"),"name");
			} catch (JSONException e) {
				p.author = null;
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
			
			// comments, can be empty:
			p.comments = new ArrayList<Post.Comment>();
			try {
				JSONArray cmnts = postobj.getJSONArray("comments");
				for (int k = 0; k < cmnts.length(); k++) {
					Post.Comment c = new Post.Comment();
					try {
					c.id = Integer.parseInt(getStringFromJSON(cmnts.getJSONObject(k),"id"));
					} catch (NumberFormatException e) {
						c.id = 0;
						Log.d("Error parsing id of a comment " + k + " of post " + p.id + " " + p.title, e.toString());
					}
					c.author = getStringFromJSON(cmnts.getJSONObject(k),"author");
					c.content = getStringFromJSON(cmnts.getJSONObject(k),"content");
					try {
						SimpleDateFormat  df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
							c.date = df.parse(getStringFromJSON(cmnts.getJSONObject(k),"date"));
					} catch (ParseException e) {
							Log.d("Error parsing date from JSON comment " + k + " of post " + p.id + " " + p.title, e.toString());
					}
				}
			} catch (JSONException e) {
				// no "comments" object in JSON 
			}
			if ("open".equals(getStringFromJSON(postobj,"comment_status"))) {
				p.commentstatus = true;
			} else {
				p.commentstatus = false;
			}
			posts.addPost(p);
			postsdisk.put(postobj);
		}
	}

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

	private String getStringFromJSON(JSONObject JSONObj, String name) {
		try {
			return JSONObj.getString(name);
		} catch (JSONException e) {
			return null;
		}
	}

    // *****************  implementation for XML  *******************

	private void parseXML() { }
	// TODO : make XML Parser. 
	
	public interface DataLoaderListener {
		
		void onDataLoaderDone(boolean internetConnection);
		
	}
	
}
