package me.dibber.blablablapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class WebPageFragment extends Fragment {
	
	private WebView mWebView;
	public final static String ARG_URL = "Web address"; 
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate( R.layout.fragment_page, container, false);
		String url = getArguments().getString(ARG_URL);
		mWebView = (WebView) rootView.findViewById(R.id.webView1);
		mWebView.loadUrl(url);
		return rootView;
	}
}
