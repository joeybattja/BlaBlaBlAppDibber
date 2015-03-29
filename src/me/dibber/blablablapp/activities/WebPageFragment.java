package me.dibber.blablablapp.activities;

import me.dibber.blablablapp.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class WebPageFragment extends Fragment {
	
	private WebView mWebView;
	private ProgressBar mProgressBar;
	private String url;
	public final static String ARG_URL = "Web address"; 
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate( R.layout.fragment_page, container, false);
		url = getArguments().getString(ARG_URL);
		mWebView = (WebView) rootView.findViewById(R.id.page_webView);
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.page_progressbar);
		mWebView.setVisibility(View.GONE);
		mProgressBar.setVisibility(View.VISIBLE);
		mWebView.setWebViewClient(new WebViewClient() {
			
			@Override
			public void onPageFinished(WebView view, String url) {
				mProgressBar.setVisibility(View.GONE);
				mWebView.setVisibility(View.VISIBLE);
				super.onPageFinished(view, url);
			}
		});
		mWebView.loadUrl(url);
		return rootView;
	}
	
	public String getURL() {
		return url;
	}
}
