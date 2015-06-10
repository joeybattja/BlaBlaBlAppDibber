package me.dibber.blablablapp.core;

import java.util.ArrayList;
import java.util.Date;

public class Post {
	
	public int id;
	public Date date;
	public String url; 
	public String title;
	public String content;
	public String authorname; 
	public int authorid;
	public ArrayList<Attachment> attachments;
	public ArrayList<Comment> comments; 
	public PodCast podcast;
	public int commentcount;
	public boolean commentstatus; 
	public char favorite; // 'Y' = favorite; 'N' = not favorite; 
	// Why not use a boolean for favorite? Because I cannot make a difference between explicit false (turned off), and implicit false (never turned on)
	public boolean inSynch;
	
	public static class Attachment {
		public int id; 
		public String mimeType;
		public String url;
	}
	
	public static class Comment { //not yet used in new_api
		public int id;
		public String author;		
		public Date date;
		public String content;
		public int parent;
	}
	
	public static class PodCast {
		public String title;
		public String link;
		public String duration;
		public String audioUrl;
		public Date date;
	}
	
}
