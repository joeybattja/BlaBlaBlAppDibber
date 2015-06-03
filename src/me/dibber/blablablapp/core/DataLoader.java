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
import java.util.Date;
import java.util.Locale;

import me.dibber.blablablapp.core.AppConfig.Function;
import me.dibber.blablablapp.core.Post.Attachment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

public class DataLoader {

	public DataLoader() {
		posts = ((GlobalState) GlobalState.getContext()).getPosts();
		state = State.IDLE;
	}

	/**
	 * Sets the data source (URL) to use.
	 * 
	 * @param path
	 *            the URL to use. Must call this method before prepareAsync() in
	 *            order for the target path to become effective thereafter.
	 * @throws MalformedURLException
	 *             if path could not be parsed as a URL.
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
			throw new IllegalStateException(
					"Dataloader not yet initialized. Call setDataSource() first.");
		}
		state = State.BUSY;
		if (isPodcast) {
			parsePodcastXML();
		} else {
			parseJSON();
		}
	}

	public void setDataLoaderListener(DataLoaderListener dataLoaderListener) {
		dll = dataLoaderListener;
	}

	public void reset() {
		state = State.IDLE;
		url = null;
	}

	// ***************** private implementation *******************

	private URL url;
	private PostCollection posts;
	private DataLoaderListener dll;
	private boolean isInSynchWithExistingPosts;

	public static String FILE_LOCATION = "Postdata";

	State state;

	private enum State {
		IDLE, INITIALIZED, BUSY, DONE
	}

	private void done(boolean noErrors) {
		if (dll != null) {
			dll.onDataLoaderOnlineDone(noErrors);
		}
		state = State.DONE;
		// write the current postcollection to disk
		writePostCollectionToDisk(posts);
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
						BufferedReader bin = new BufferedReader(
								new InputStreamReader(fin));
						StringBuilder sb = new StringBuilder();
						String s;
						while ((s = bin.readLine()) != null) {
							sb.append(s);
						}
						bin.close();
						JSONreceived(sb.toString(), false);
						if (dll != null) {
							dll.onDataLoaderDiskDone(true);
						}
					} catch (IOException e) {
						Log.w("Error trying to read file from disk",
								e.toString());
						if (dll != null) {
							dll.onDataLoaderDiskDone(false);
						}
					}
				}

				// then read from network
				try {
					HttpURLConnection connection = (HttpURLConnection) url
							.openConnection();
					InputStream in = connection.getInputStream();
					BufferedReader r = new BufferedReader(
							new InputStreamReader(in));
					StringBuilder data = new StringBuilder();
					String line;
					while ((line = r.readLine()) != null) {
						data.append(line);
					}
					r.close();
					JSONreceived(data.toString(), true);
					done(true);
				} catch (IOException e) {
					Log.w("Error trying to setup connections", e.toString());
					done(false);
				}
			}
		});
		t.start();
	}

	public static void writePostCollectionToDisk(final PostCollection pc) {

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				JSONObject writeObj = new JSONObject();
				ArrayList<Integer> postIds = (ArrayList<Integer>) pc
						.getAllPosts();

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
							SimpleDateFormat df = new SimpleDateFormat(
									"yyyy-MM-dd HH:mm:ss", Locale.US);
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
					Log.e("error reading postcollection while creating JSON for disk",
							e.toString());
				}

				try {
					Context c = GlobalState.getContext();
					FileOutputStream os = c.openFileOutput(FILE_LOCATION,
							Context.MODE_PRIVATE);
					os.write(writeObj.toString().getBytes());
					os.close();
				} catch (FileNotFoundException e) {
					Log.e("Error trying to write data in internal storage",
							e.toString());
				} catch (IOException e) {
					Log.e("Error trying to write data in internal storage",
							e.toString());
				}
			}
		});
		t.start();
	}

	private void JSONreceived(String JSONobject, boolean synchedOnline) {

		try {
			JSONObject obj = new JSONObject(JSONobject);
			if (obj == null || !getStringFromJSON(obj, "status").equals("OK")) {
				String reason = getStringFromJSON(obj, "reason");
				Log.w("error parsing the JSON object",
						"Status is not 'OK', reason: "
								+ (reason == null ? "unknown" : reason));
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
							postIdParam = Integer
									.parseInt((param.split("=")[1]));
						} catch (NumberFormatException e) {
							throw new JSONException(
									"no valid value given for postId");
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

	private void JSONcreatePosts(JSONArray postdata, boolean synchedOnline)
			throws JSONException {

		for (int i = 0; i < postdata.length(); i++) {

			JSONObject postobj = postdata.getJSONObject(i);

			int id = Integer.parseInt(getStringFromJSON(postobj, "id"));
			Post p = posts.getPost(id);
			if (p == null) {
				p = new Post();
				p.id = id;
			}
			p.url = getStringFromJSON(postobj, "url");
			p.title = getStringFromJSON(postobj, "title");
			p.content = getStringFromJSON(postobj, "content");
			try {
				p.authorname = getStringFromJSON(
						postobj.getJSONObject("author"), "name");
				p.authorid = Integer.parseInt(getStringFromJSON(
						postobj.getJSONObject("author"), "id"));
			} catch (JSONException e) {
				p.authorname = null;
				p.authorid = 0;
			}
			try {
				SimpleDateFormat df = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss", Locale.US);
				p.date = df.parse(getStringFromJSON(postobj, "date"));
			} catch (ParseException e) {
				Log.d("error parsing date from JSON of post " + p.id + " "
						+ p.title, e.toString());
			}

			// attachments, can be empty (images will not yet be downloaded):
			ArrayList<Post.Attachment> tempAttachments = new ArrayList<Post.Attachment>();
			try {
				JSONArray att = postobj.getJSONArray("attachments");
				for (int j = 0; j < att.length(); j++) {
					Post.Attachment a = new Post.Attachment();
					try {
						a.id = Integer.parseInt(getStringFromJSON(
								att.getJSONObject(j), "id"));
					} catch (NumberFormatException e) {
						a.id = 0;
						Log.d("Error parsing id of an attachment " + j
								+ " of post " + p.id + " " + p.title,
								e.toString());
					}
					a.url = getStringFromJSON(att.getJSONObject(j), "url");
					a.mimeType = getStringFromJSON(att.getJSONObject(j), "mime_type");
					tempAttachments.add(a);
				}
			} catch (JSONException e) {
				// no "attachments" object in JSON
			}
			if (p.attachments != null
					&& !attachmentsEqual(tempAttachments, p.attachments)) {
				PostCollection.cleanUpAttachments(p.id, p.attachments);
			}
			p.attachments = tempAttachments;

			// try to read comments, will eventually be empty in the
			// get_recent_posts
			try {
				JSONArray cmnts = postobj.getJSONArray("comments");
				JSONcreateComments(cmnts, p);
			} catch (JSONException e) {
				// no "comments" object in JSON
			}

			try {
				p.commentstatus = postobj.getBoolean("comment_status");
			} catch (JSONException e) {
				if ("open".equals(getStringFromJSON(postobj, "comment_status"))) {
					p.commentstatus = true;
				} else {
					p.commentstatus = false;
				}
			}
			try {
				p.commentcount = Integer.parseInt(getStringFromJSON(postobj,
						"comment_count"));
			} catch (NumberFormatException e) {
				Log.e("no commentcount", e.toString());
				p.commentcount = 0;
			}
			if (p.favorite != 'Y' && p.favorite != 'N') {
				if (getStringFromJSON(postobj, "favorite") != null
						&& getStringFromJSON(postobj, "favorite").charAt(0) == 'Y') {
					p.favorite = 'Y';
				} else {
					p.favorite = 'N';
				}
			}
			if (synchedOnline && isInSynchWithExistingPosts) {
				int oldPost = ((GlobalState) GlobalState.getContext()).getOldestSynchedPost();
				if (posts.getItemDate(oldPost) == null || posts.getItemDate(oldPost).equals(new Date(0)) || posts.getItemDate(oldPost).after(p.date)) {
					((GlobalState) GlobalState.getContext()).setOldestSynchedPost(p.id);
				}
			}
			posts.addPost(p);
		}
	}

	private void JSONcreateComments(JSONArray commentData, Post p)
			throws JSONException {
		p.comments = new ArrayList<Post.Comment>();
		for (int i = 0; i < commentData.length(); i++) {
			JSONObject commentobj = commentData.getJSONObject(i);
			Post.Comment c = new Post.Comment();
			try {
				c.id = Integer.parseInt(getStringFromJSON(commentobj, "id"));
			} catch (NumberFormatException e) {
				c.id = 0;
				Log.w("Error parsing id of a comment " + i + " of post " + p.id
						+ " " + p.title, e.toString());
			}
			try {
				c.parent = Integer.parseInt(getStringFromJSON(commentobj,
						"parent"));
			} catch (NumberFormatException e) {
				c.parent = 0;
				Log.w("Error parsing parent of a comment " + i + " of post "
						+ p.id + " " + p.title + ", with comment id " + c.id,
						e.toString());
			}
			c.author = getStringFromJSON(commentobj, "author");
			c.content = getStringFromJSON(commentobj, "content");
			try {
				SimpleDateFormat df = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss", Locale.US);
				c.date = df.parse(getStringFromJSON(commentobj, "date"));
			} catch (ParseException e) {
				Log.w("Error parsing date from JSON comment " + i + " of post "
						+ p.id + " " + p.title, e.toString());
			}
			p.comments.add(c);
		}
		if (p.comments.size() > 0) {
			p.commentcount = p.comments.size();
		}
	}

	/**
	 * Checks if two lists of attachments are similar.
	 * 
	 * @param tempAttachments
	 *            First ArrayList of attachments
	 * @param attachments
	 *            Second ArrayList of attachments
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
	 * Utility method to parse a String from JSON object, or returns null if the
	 * String is not present. Since this method will not throw a JSONException,
	 * it can be used to prevent lots of try/catch clauses.
	 * 
	 * @param JSONObj
	 *            the JSONObject
	 * @param name
	 *            the name of the String
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

	// --------------- PODCAST ---------------------

	private XmlPullParser parser;
	private int firstIndex;
	private int lastIndex;
	private boolean isPodcast;
	
	public void setAsPodcastLoader(boolean isPodcast) {
		this.isPodcast = isPodcast;
	}
	
	public void setPodcastRange(int from, int to) {
		firstIndex = from;
		lastIndex = to;
	}

	private void parsePodcastXML() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				InputStream in = null;
				try {
					try {
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						in = connection.getInputStream();
						parser = Xml.newPullParser();
						parser.setInput(in, null);
						parser.nextTag();
						processXML();
						done(true);
					} catch (Exception e) {
						Log.w("Error trying to setup connection to Podcast",
								e.toString());
						done(false);
					} finally {
						in.close();
					}
				} catch (Exception e) {
					Log.w("Error while connecting to Podcast", e.toString());
					done(false);
				}

			}
		});
		t.start();
	}

	private void processXML() throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, null, "rss");
		int index = 0;
		while (parser.next() != XmlPullParser.END_DOCUMENT) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			if (parser.getName().equals("item")) {
				if (lastIndex == 0 || (index >= firstIndex && index <= lastIndex)) {
					Post.PodCast podcastData = readItem();
					Post post = retrievePostByURL(podcastData.link);
					if (post != null) {
						post.podcast = podcastData;
					} else {
						Log.w("No post found for podcast", "titled: " + podcastData.title);
					}
				} else {
					Post.PodCast podcastData = readItem();
					Post post = posts.getPostByURL(podcastData.link);
					if (post != null) {
						post.podcast = podcastData;
					}
				}
				index++;
			}
		}
	}

	private Post.PodCast readItem() throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, null, "item");
		Post.PodCast podcast = new Post.PodCast();
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			if (parser.getName().equals("title")) {
				parser.require(XmlPullParser.START_TAG, null, "title");
				podcast.title = readText();
				parser.require(XmlPullParser.END_TAG, null, "title");
				continue;
			}
			if (parser.getName().equals("link")) {
				parser.require(XmlPullParser.START_TAG, null, "link");
				podcast.link = readText();
				parser.require(XmlPullParser.END_TAG, null, "link");
				continue;
			}
			if (parser.getName().equals("enclosure")) {
				parser.require(XmlPullParser.START_TAG, null, "enclosure");
				podcast.audioUrl = parser.getAttributeValue(null, "url");
				parser.nextTag();
				parser.require(XmlPullParser.END_TAG, null, "enclosure");
				continue;
	        }
	        if (parser.getName().equals("pubDate")) {
	        	parser.require(XmlPullParser.START_TAG, null, "pubDate");
	        	
				try {
					SimpleDateFormat df = new SimpleDateFormat("ccc, dd MMM yyyy HH:mm:ss Z", Locale.US);
					podcast.date = df.parse(readText());
				} catch (ParseException e) {
					Log.e("error parsing date podcast XML for podcast titled " + podcast.title, e.toString());
				}
	        	parser.require(XmlPullParser.END_TAG, null, "pubDate");
	        	continue;
	        }
			if (parser.getName().equals("duration")) {
				parser.require(XmlPullParser.START_TAG, null, "duration");
				podcast.duration = readText();
				parser.require(XmlPullParser.END_TAG, null, "duration");
				continue;
			}
			skip();
		}
		return podcast;
	}

	private String readText() throws XmlPullParserException, IOException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	private void skip() throws XmlPullParserException, IOException {
		int depth = 1;
		while (depth != 0) {

			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

	private Post retrievePostByURL(String URL) {

		Post post = posts.getPostByURL(URL);
		if (post == null) {
			URL apiURL = null;
			try {
				apiURL = new URL(new AppConfig.APIURLBuilder(
						Function.GET_POST_BY_URL).setURL(URL).setCount(1)
						.create());
			} catch (MalformedURLException e) {
				Log.w("Path incorrect", e.toString());
			}
			try {
				HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
				InputStream in = connection.getInputStream();
				BufferedReader r = new BufferedReader(new InputStreamReader(in));
				StringBuilder data = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					data.append(line);
				}
				r.close();

				try {
					JSONObject js = new JSONObject(data.toString());
					if (js == null || !getStringFromJSON(js, "status").equals("OK")) {
						String reason = getStringFromJSON(js, "reason");
						Log.w("error parsing the JSON object",
								"Status is not 'OK', reason: "	+ (reason == null ? "unknown" : reason));
					}
					JSONArray array = js.getJSONArray("posts");
					JSONcreatePosts(array, false);
					int id = Integer.parseInt(getStringFromJSON(array.getJSONObject(0), "id"));
					post = posts.getPost(id);
				} catch (JSONException e) {
					Log.w("error parsing the JSON object", e.toString());
				}

			} catch (IOException e) {
				Log.w("Error trying to setup connections", e.toString());
			}
		}
		return post;
	}

}
