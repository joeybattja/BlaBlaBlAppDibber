package me.dibber.blablablapp;

import java.util.ArrayList;
import java.util.Date;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class Post {
	
	public int id;
	public Date date;
	public String url; 
	public String title;
	public String content;
	public String author; //not yet used in new_api
	public ArrayList<Attachment> attachments;
	public ArrayList<Comment> comments; //not yet used in new_api
	public boolean commentstatus; //not yet used in new_api
	public boolean favorite;
	
	public static class Attachment {
		public int id; 
		public String mimeType;
		public Drawable image;
		public Bitmap thumbnail;
		public String url;
	}
	
	public static class Comment { //not yet used in new_api
		public int id;
		public String author;		
		public Date date;
		public String content;
	}
}
