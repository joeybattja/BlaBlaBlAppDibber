package me.dibber.blablablapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class PageFragment extends Fragment {
	
	private WebView mWebView;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate( R.layout.fragment_page, container, false);
		int id = getArguments().getInt(PostDetailFragment.ARG_ID);
		mWebView = (WebView) rootView.findViewById(R.id.webView1);
		mWebView.loadUrl(getPageURL(id));
		return rootView;
	}
	
	private String getPageURL(int id) {
		if (id < 1 || id > 2) {
			return "http://www.blablablog.nl/about/";
		}
		switch(id) {
		case 1:
			return "http://www.blablablog.nl/about/";
		case 2:
			return "http://www.blablablog.nl/ebooks/";
		default:
			return "http://www.blablablog.nl/about/";
		}
	}

}
