package me.dibber.blablablapp.activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import me.dibber.blablablapp.R;
import me.dibber.blablablapp.core.AppConfig;
import me.dibber.blablablapp.core.AppConfig.Function;
import me.dibber.blablablapp.core.DataLoader;
import me.dibber.blablablapp.core.DataLoader.DataLoaderListener;
import me.dibber.blablablapp.core.GlobalState;
import me.dibber.blablablapp.core.PostCollection;
import me.dibber.blablablapp.core.Profile;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CommentsFragment extends Fragment {
	
	private TextView mCountComments;
	private int nrOfComments;
	private LinearLayout mCommentsFrame;
	private TextView mLeaveReply;
	private LinearLayout mNewCommentForm;	
	private EditText mNewComment;
	private Button mSubmitComment;
	
	private int postId;
	PostCollection posts;
	private ArrayList<Integer> commentIds;
	
	volatile boolean canUpdateFragments;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		postId = getArguments().getInt(PostDetailFragment.ARG_ID);
		posts = ((GlobalState) GlobalState.getContext() ).getPosts();
		View rootView = inflater.inflate(R.layout.fragment_comments, container, false);
		
		mCountComments = (TextView) rootView.findViewById(R.id.comment_CountText);
		mCommentsFrame = (LinearLayout) rootView.findViewById(R.id.comment_Commentsframe);
		mLeaveReply = (TextView) rootView.findViewById(R.id.comment_LeaveReplyText);
		mNewCommentForm = (LinearLayout) rootView.findViewById(R.id.newComment_form);
		mNewComment = (EditText) rootView.findViewById(R.id.newComment_comment);
		mSubmitComment = (Button) rootView.findViewById(R.id.newComment_submitButton);
		
		synchronized (this) {
			canUpdateFragments = true;
		}
		
		invalidateComments();
		
		return rootView;
	}
	
	
	
	@Override
	public void onPause() {
		synchronized (this) {
			canUpdateFragments = false;
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		synchronized (this) {
			canUpdateFragments = true;
		}

	}

	private void displayComments() {
		if (postId <= 0) {
			return;
		}
		posts = ((GlobalState) GlobalState.getContext() ).getPosts();
		
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
				if (nrOfComments == commentCount) {
					return;
				}
				nrOfComments = commentCount;

				mCountComments.setVisibility(View.VISIBLE);
				mCommentsFrame.setVisibility(View.VISIBLE);
				mCommentsFrame.removeAllViews();
				mCountComments.setTypeface(null,Typeface.BOLD);
				mCountComments.setText(GlobalState.getContext().getResources().getQuantityString(R.plurals.nrOfComments, commentCount, commentCount));
				
				for (int i = 0; i < commentIds.size(); i++) {
					
					CommentItemFragment commentItemFrag = new CommentItemFragment();
					Bundle args = new Bundle();
					args.putInt(CommentItemFragment.ARG_POSTID, postId);
					args.putInt(CommentItemFragment.ARG_COMMENTID, commentIds.get(i));
					commentItemFrag.setCommentFragment(this);
					commentItemFrag.setArguments(args);
					
					// need to check if the activity is not destroyed before adding the comments.
					HomeActivity ha = ((GlobalState)GlobalState.getContext()).getCurrentHomeActivity();
					if (getActivity() != null && getActivity().equals(ha)) {
						getChildFragmentManager().beginTransaction().add(mCommentsFrame.getId(), commentItemFrag, postId + "pos" + i).commit();
					}
				}
			} 
		}
	}
	
	private void displayReplySection() {
		if (AppConfig.allowWriteComments()) {
			
			if (mLeaveReply != null) {
				mLeaveReply.setTypeface(null,Typeface.BOLD);
				mLeaveReply.setText(R.string.leave_a_reply);
			}
			if (mSubmitComment != null && mNewComment != null) {
				String text;	
				HomeActivity ha = getHomeActivity();
				if (ha != null) {
					Profile pr = ha.getProfile();
								
					if (pr.isLoggedIn()) {
						text = GlobalState.getContext().getResources().getString(R.string.Post_comment_as) + " " + pr.getName();
					} else {
						text = GlobalState.getContext().getResources().getString(R.string.Post_comment_as) + "...";
					}
				} else {
					text = GlobalState.getContext().getResources().getString(R.string.Post_comment_as) + "...";
				}
				mSubmitComment.setText(text);
				
				mSubmitComment.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						boolean error = false;
						HomeActivity ha = getHomeActivity();
						if (ha != null) {
							Profile pro = ha.getProfile();
							if (!pro.isLoggedIn()) {
								pro.openDialog();
								error = true;
							} else if (pro.getName().isEmpty()) {
								Toast.makeText(GlobalState.getContext(), R.string.post_error_no_name, Toast.LENGTH_LONG).show();
								error = true;
							} else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(pro.getEmail()).matches()) {
								Toast.makeText(GlobalState.getContext(), R.string.post_error_invalid_email, Toast.LENGTH_LONG).show();						
								error = true;
							} else if (mNewComment.getText().toString().trim().isEmpty()) {
								mNewComment.setError(GlobalState.getContext().getResources().getString(R.string.is_required));
								error = true;
							}
							if (!error) {
								postComment(postId,pro.getName(), pro.getEmail(), mNewComment.getText().toString().trim(), new OnCommentPostedListener() {
									
									@Override
									public void onCommentPosted(final boolean success) {
										HomeActivity ha = ((GlobalState)GlobalState.getContext()).getCurrentHomeActivity();
										if (ha != null) {
											ha.runOnUiThread(new Runnable() {
												
												@Override
												public void run() {
													if (!success) {
														Toast.makeText(GlobalState.getContext(), GlobalState.getContext().getResources().getString(R.string.post_error_exception), Toast.LENGTH_LONG).show();
													}
													mNewComment.setText("");
													invalidateComments();
												}
											});
										}
									}
								});
							}
						}
					}
				});
			}
		} else {
			if (mLeaveReply != null) { 
				mLeaveReply.setVisibility(View.GONE);
			}
			if (mNewCommentForm != null) {
				mNewCommentForm.setVisibility(View.GONE);
			}
		}
	}
	
	public void invalidateComments() {
		if (postId <= 0) {
			return;
		}
		posts = ((GlobalState) GlobalState.getContext() ).getPosts();
		
		DataLoader dl = new DataLoader();
		try {
			dl.setDataSource(new AppConfig.APIURLBuilder(Function.GET_COMMENTS).setPostId(postId).create());
		} catch (MalformedURLException e) {
			Log.w("Path incorrect", e.toString());
		}
		dl.setDataLoaderListener(new DataLoaderListener() {
			
			@Override
			public void onDataLoaderOnlineDone(boolean success) {
				HomeActivity ha = ((GlobalState)GlobalState.getContext()).getCurrentHomeActivity();
				if (ha != null) {
					ha.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							synchronized (this) {
								if (canUpdateFragments) {
									displayComments();
									displayReplySection();
								}
							}
						}
					});
				}
			}
			
			@Override
			public void onDataLoaderDiskDone(boolean success) {	}
		});
		dl.prepareAsync();
	}
	
	private static void postComment(int postId, String name, String email, String comment, final OnCommentPostedListener listener) {
		postComment(postId,name,email,comment,0,listener);
	}
	
	private static void postComment(int postId, String name, String email, String comment, int parent, final OnCommentPostedListener listener) {
		final String path = new AppConfig.APIURLBuilder(Function.POST_COMMENT).setPostId(postId).setComment(parent,name, email, comment).create(); 
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					URL url = new URL(path);
					InputStream in = url.openStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));
					JSONObject reply = new JSONObject(reader.readLine());
					if (!"OK".equals(reply.getString("status"))) {
						Log.w("error",reply.getString("reason"));
						listener.onCommentPosted(false);
					} else {
						listener.onCommentPosted(true);
					}
				} catch (IOException e) {
					Log.w("IOException while posting comment", e.toString());
					listener.onCommentPosted(false);

				} catch (JSONException e1) {
					Log.w("JSONException while posting comment", e1.toString());
					listener.onCommentPosted(false);
				}
			}
		});
		t.start();
	}
	
	private static HomeActivity getHomeActivity() {
		return ((GlobalState)GlobalState.getContext()).getCurrentHomeActivity();
	}
	
	public interface OnCommentPostedListener {
		void onCommentPosted(boolean success);
	}

	public static class CommentItemFragment extends Fragment {
		
		public final static String ARG_POSTID = "arg postid";
		public final static String ARG_COMMENTID = "arg commentid";
		
		private CommentsFragment commentFragment;
		
		private View mIndentView;
		private View mLine;
		private TextView mAuthorView;
		private TextView mParentName;
		private TextView mDateView;
		private TextView mContentView;
		
		private EditText mNewReply;
		private Button mSubmitReply;
		
		private LinearLayout mCommentBody;
		private LinearLayout mReplyComment;
		
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
			mParentName = (TextView) commentRootView.findViewById(R.id.item_comment_parentName);
			mDateView = (TextView) commentRootView.findViewById(R.id.item_comment_date);
			mContentView = (TextView) commentRootView.findViewById(R.id.item_comment_content);
			
			mCommentBody = (LinearLayout) commentRootView.findViewById(R.id.item_comment_body);
			mReplyComment = (LinearLayout) commentRootView.findViewById(R.id.item_comment_reply);
			mNewReply = (EditText) commentRootView.findViewById(R.id.item_comment_reply_content);
			mSubmitReply = (Button) commentRootView.findViewById(R.id.item_comment_reply_submitButton);

			mCommentBody.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (mReplyComment.getVisibility() == View.GONE) {
						mReplyComment.setVisibility(View.VISIBLE);
					} else {
						mReplyComment.setVisibility(View.GONE);
					}
				}
			});
			
			mAuthorView.setTypeface(null,Typeface.BOLD);
			mAuthorView.setText(psts.getCommentAuthor(pstId, cmntId));
			mDateView.setTypeface(null,Typeface.ITALIC);
			mDateView.setText(psts.getCommentDateAsString(pstId, cmntId));
			
			int parent = psts.getCommentParent(pstId, cmntId);
			if (parent != 0) {
				mParentName.setTypeface(null,Typeface.ITALIC);
				mParentName.setText(" @" + psts.getCommentAuthor(pstId,parent));
			}
			
			indent = 0;
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
			
			// If this is the last comment, don't show a line
			ArrayList<Integer> commentIds = psts.getItemComments(pstId);
			if (commentIds.size() == 0 || commentIds.get(commentIds.size()-1) == cmntId) {
				mLine.setVisibility(View.GONE);
			}
			
			mReplyComment.setVisibility(View.GONE);
			
			if (mNewReply != null && mSubmitReply != null) {
				String replyHint = GlobalState.getContext().getResources().getString(R.string.Reply_to) + " " + psts.getCommentAuthor(pstId, cmntId);
				mNewReply.setHint(replyHint);
				String replySubmit;
				HomeActivity ha = getHomeActivity();
				if (ha != null) {
					Profile pr = ha.getProfile();
								
					if (pr.isLoggedIn()) {
						replySubmit = GlobalState.getContext().getResources().getString(R.string.Post_reply_as) + " " + pr.getName();
					} else {
						replySubmit = GlobalState.getContext().getResources().getString(R.string.Post_reply_as) + "...";
					}
				} else {
					replySubmit = GlobalState.getContext().getResources().getString(R.string.Post_reply_as) + "...";
				}
				
				mSubmitReply.setText(replySubmit);
				
				mSubmitReply.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						boolean error = false;
						HomeActivity ha = getHomeActivity();
						if (ha != null) {
							Profile pro = ha.getProfile();
							if (!pro.isLoggedIn()) {
								pro.openDialog();
								error = true;
							} else if (pro.getName().isEmpty()) {
								Toast.makeText(GlobalState.getContext(), R.string.post_error_no_name, Toast.LENGTH_LONG).show();
								error = true;
							} else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(pro.getEmail()).matches()) {
								Toast.makeText(GlobalState.getContext(), R.string.post_error_invalid_email, Toast.LENGTH_LONG).show();						
								error = true;
							} else if (mNewReply.getText().toString().trim().isEmpty()) {
								mNewReply.setError(GlobalState.getContext().getResources().getString(R.string.is_required));
								error = true;
							}
							if (!error) {
								postComment(pstId,pro.getName(), pro.getEmail(), mNewReply.getText().toString().trim(), cmntId, new OnCommentPostedListener() {
									
									@Override
									public void onCommentPosted(final boolean success) {
										HomeActivity ha = ((GlobalState)GlobalState.getContext()).getCurrentHomeActivity();
										if (ha != null) {
											ha.runOnUiThread(new Runnable() {
												
												@Override
												public void run() {
													if (!success) {
														Toast.makeText(GlobalState.getContext(), GlobalState.getContext().getResources().getString(R.string.post_error_exception), Toast.LENGTH_LONG).show();
													}
													mNewReply.setText("");
													mReplyComment.setVisibility(View.GONE);
													if (commentFragment != null) {
														commentFragment.invalidateComments();
													}
												}
											});
										}
									}
								});
							}
						}
					}
				});
			}
			return commentRootView;
		}
		
		public void setCommentFragment(CommentsFragment parent) {
			commentFragment = parent;
		}
	}
	
}
