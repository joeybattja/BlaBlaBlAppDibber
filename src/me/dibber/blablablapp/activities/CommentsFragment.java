package me.dibber.blablablapp.activities;

import java.util.ArrayList;

import me.dibber.blablablapp.R;
import me.dibber.blablablapp.core.GlobalState;
import me.dibber.blablablapp.core.PostCollection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CommentsFragment extends Fragment {
	
	private TextView mCountComments;
	private LinearLayout mCommentsFrame;
	private TextView mLeaveReply;
	private LinearLayout mNewCommentForm;	
	private EditText mName;
	private EditText mEmail;
	private EditText mWebsite;
	private EditText mNewComment;
	private Button mSubmitComment;
	
	private int postId;
	PostCollection posts;
	private ArrayList<Integer> commentIds;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		postId = getArguments().getInt(PostDetailFragment.ARG_ID);
		posts = ((GlobalState) GlobalState.getContext() ).getPosts();
		View rootView = inflater.inflate(R.layout.fragment_comments, container, false);
		
		mCountComments = (TextView) rootView.findViewById(R.id.comment_CountText);
		mCommentsFrame = (LinearLayout) rootView.findViewById(R.id.comment_Commentsframe);
		mLeaveReply = (TextView) rootView.findViewById(R.id.comment_LeaveReplyText);
		mNewCommentForm = (LinearLayout) rootView.findViewById(R.id.newComment_form);
		mName = (EditText) rootView.findViewById(R.id.newComment_name);
		mEmail = (EditText) rootView.findViewById(R.id.newComment_email);
		mWebsite = (EditText) rootView.findViewById(R.id.newComment_website);
		mNewComment = (EditText) rootView.findViewById(R.id.newComment_comment);
		mSubmitComment = (Button) rootView.findViewById(R.id.newComment_submitButton);
		
		commentIds = posts.getItemComments(postId);
		int commentCount = posts.countComments(postId);
		if (commentIds.size() != commentCount) {
			Log.w("Warning, comment size is not equal:", "commentCount parameter is: " + commentCount + 
					", while there are " + commentIds.size() + " actual comments.");
		}
		
		if (mCountComments != null && mCommentsFrame != null) {
			if (commentCount == 0) {
				mCountComments.setVisibility(View.GONE);
				mCommentsFrame.setVisibility(View.GONE);
			} else {
				mCountComments.setTypeface(null,Typeface.BOLD);
				mCountComments.setText(getResources().getQuantityString(R.plurals.nrOfComments, commentCount, commentCount));
				
				for (int i = 0; i < commentIds.size(); i++) {
					FrameLayout frame = new FrameLayout(getActivity().getApplicationContext());
					frame.setId(commentIds.get(i));
					mCommentsFrame.addView(frame);
					CommentItemFragment commentItemFrag = new CommentItemFragment();
					Bundle args = new Bundle();
					args.putInt(CommentItemFragment.ARG_POSTID, postId);
					args.putInt(CommentItemFragment.ARG_COMMENTID, commentIds.get(i));
					commentItemFrag.setArguments(args);
					getChildFragmentManager().beginTransaction().add(commentIds.get(i), commentItemFrag).commit();
				}
			} 
		}
		if ( ((GlobalState)GlobalState.getContext()).optionWriteComments() ) {
			
			if (mLeaveReply != null) {
				mLeaveReply.setTypeface(null,Typeface.BOLD);
				mLeaveReply.setText(R.string.leave_a_reply);
			}
			if (mName != null && mEmail != null && mWebsite != null && mNewComment != null && mSubmitComment != null) {
				
			}
		} else {
			if (mLeaveReply != null) { 
				mLeaveReply.setVisibility(View.GONE);
			}
			if (mNewCommentForm != null) {
				mNewCommentForm.setVisibility(View.GONE);
			}
		}
		
		return rootView;
	}
	
	public static class CommentItemFragment extends Fragment {
		
		public final static String ARG_POSTID = "arg postid";
		public final static String ARG_COMMENTID = "arg commentid";
		
		private View mIndentView;
		private View mLine;
		private TextView mAuthorView;
		private TextView mDateView;
		private TextView mContentView;
		private PostCollection psts;
		
		private int pstId;
		private int cmntId;
		private int indent;
		
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View commentRootView = inflater.inflate(R.layout.item_comment, container, false);
			psts = ((GlobalState) GlobalState.getContext() ).getPosts();
			
			pstId = getArguments().getInt(ARG_POSTID);
			cmntId = getArguments().getInt(ARG_COMMENTID);
			
			mIndentView = commentRootView.findViewById(R.id.item_comment_indent);
			mLine = commentRootView.findViewById(R.id.item_comment_bottomline);
			mAuthorView = (TextView) commentRootView.findViewById(R.id.item_comment_author);
			mDateView = (TextView) commentRootView.findViewById(R.id.item_comment_date);
			mContentView = (TextView) commentRootView.findViewById(R.id.item_comment_content);
			
			mAuthorView.setTypeface(null,Typeface.BOLD);
			mAuthorView.setText(psts.getCommentAuthor(pstId, cmntId));
			mDateView.setTypeface(null,Typeface.ITALIC);
			mDateView.setText(psts.getCommentDateAsString(pstId, cmntId));
			
			indent = 0;
			int parent = psts.getCommentParent(pstId, cmntId);
			while (parent != 0) {
				indent++;
				parent = psts.getCommentParent(pstId, parent);
				if (indent == 10) {
					break;
				}
			}
			LinearLayout.LayoutParams indentParams = (LinearLayout.LayoutParams) mIndentView.getLayoutParams();
			indentParams.width = 0;
			indentParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
			indentParams.weight = indent;
			mIndentView.setLayoutParams(indentParams);
			
			mContentView.setText(psts.getCommentContent(pstId, cmntId));

			
			if (psts.getItemComments(pstId).get(psts.getItemComments(pstId).size() -1) == cmntId) {
				mLine.setVisibility(View.GONE);
			}
			
			return commentRootView;
		}
	}
}
