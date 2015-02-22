package me.dibber.blablablapp;

public class Pages {
	
	public static enum PageType {POSTS,WEBPAGE,FAVORITES};
	
	// ---------------- HERE YOU CAN CHANGE THE MENU. IT IS POSSIBLE TO SIMPLY ADD WEBPAGES ----------------- 
	
	private static Page[] pages = new Page[] {
			new Page(GlobalState.getContext().getResources().getString(R.string.Home), 			
					PageType.POSTS, null),
			new Page(GlobalState.getContext().getResources().getString(R.string.Favorites),		
					PageType.FAVORITES,	null),
			new Page(GlobalState.getContext().getResources().getString(R.string.About_Theo), 
					PageType.WEBPAGE, "http://www.blablablog.nl/about/"),
			new Page(GlobalState.getContext().getResources().getString(R.string.eBook_store), 
					PageType.WEBPAGE, "http://www.blablablog.nl/ebooks/")
			};
	
	private static class Page {
		String mTitle;
		PageType mType;
		String mURL;
		
		public Page(String title, PageType type, String url) {
			mTitle = title;
			mType = type;
			mURL = url;
		}
	}
	
	public static int countPages() {
		return pages.length;
	}
	
	public static String[] getPageTitles() {
		String[] titles = new String[countPages()];
		for (int i = 0; i < pages.length; i++) {
			titles[i] = pages[i].mTitle;
		}
		return titles;
	}
	
	public static String getPageTitle(int id) {
		return pages[id].mTitle;
	}
	
	public static PageType getPageType(int id) {
		return pages[id].mType;
	}
	
	public static String getPageURL(int id) {
		return pages[id].mURL;
	}
}	
	
