package com.kylindev.totalk.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.InterpttService.LocalBinder;
import com.kylindev.totalk.R;
import com.kylindev.totalk.utils.AppSettings;

import java.util.ArrayList;


public class MemberChannel extends Activity implements OnClickListener {
	/**
	 * The InterpttService instance that drives this activity's data.
	 */
	private InterpttService mService;
	// Create dialog
	private ProgressDialog mConnectDialog = null;
	private Intent mServiceIntent = null;

	private ListView myList;
	private MemberListAdapter myAdapter;
	private ImageView mIVLeave;
	private TextView mTVTitle, mTVPage;
	private Button mBtnPrev, mBtnNext;
	private ProgressBar mPBSearch;

	private boolean mServiceBind = false;

	private int mChanId;
	private String mChanName;
    private int memberCount = 0;
	private int currentPageId = 0;

	/**
	 * Management of service connection state.
	 */
	private ServiceConnection mServiceConnection = null;

	private void initServiceConnection() {
		mServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				LocalBinder localBinder = (LocalBinder) service;
				mService = localBinder.getService();
				mService.registerObserver(serviceObserver);

				mService.queryMembers(mChanId, currentPageId);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				mService = null;
				mServiceConnection = null;
			}
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_member_channel);

		mChanId = getIntent().getExtras().getInt("ChanId");
        mChanName = getIntent().getExtras().getString("ChanName");
        memberCount = getIntent().getExtras().getInt("ChanMemberCount");
		//可能是首次运行，也可能是用户重新launch
		//因此，先检查service是否在运行，如果是，则直接bind以获取mService实例；如果没有，则startService，再bind
		mServiceIntent = new Intent(this, InterpttService.class);
		initServiceConnection();
		mServiceBind = bindService(mServiceIntent, mServiceConnection, 0);

		//之前channelView相关的
		mIVLeave = (ImageView) findViewById(R.id.iv_member_leave);
		mIVLeave.setOnClickListener(this);

		mTVTitle = (TextView) findViewById(R.id.tv_member_chan_title);
		mTVTitle.setText(mChanName);

		mBtnPrev = (Button) findViewById(R.id.btn_prev_page);
		mBtnNext = (Button) findViewById(R.id.btn_next_page);
		mBtnPrev.setOnClickListener(this);
		mBtnNext.setOnClickListener(this);


		mTVPage = (TextView) findViewById(R.id.tv_pageid);
		refreshShow();

		mPBSearch = (ProgressBar) findViewById(R.id.pb_get_member);

		// Get the UI views
		myList = (ListView) findViewById(R.id.lv_members);
		myAdapter = new MemberListAdapter(mService);
		myList.setAdapter(myAdapter);

	}

	private void refreshShow() {
		mTVPage.setText(String.valueOf(currentPageId + 1));

		mBtnPrev.setEnabled(true);
		mBtnNext.setEnabled(true);
		if (currentPageId == 0) {
			mBtnPrev.setEnabled(false);
		}
		if ((currentPageId * 100 + 100) >= memberCount) {
			mBtnNext.setEnabled(false);
		}
	}

	@Override
	protected void onDestroy() {
		if (mService != null) {
			mService.unregisterObserver(serviceObserver);
			if (mServiceConnection != null) {
				if (mServiceBind) {
					unbindService(mServiceConnection);
				}
				mServiceConnection = null;
			}

			mService = null;
		}

		super.onDestroy();
	}




	////////////////////////////////wocao add member
	private BaseServiceObserver serviceObserver = new BaseServiceObserver() {
		@Override
		public void onMembersGot(int cid, String members) throws RemoteException {
			mPBSearch.setVisibility(View.GONE);
			myAdapter.clear();

			if (members!=null & members.length()>0) {
				String[] strs = members.split(";", 0);
				if (strs.length > 0) {
					for (String str : strs) {
						String[] info = str.split(",", 0);
						if (info.length>1) {
							String uid = info[0];
							String nick = info[1];
							MyMember mem = new MyMember(uid, nick);
							myAdapter.addmember(mem);
						}
					}
				}
			}

			myAdapter.notifyDataSetChanged();
		}
	};

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();

		if (id == R.id.iv_member_leave) {
			finish();
		} else if (id == R.id.btn_prev_page) {
			mPBSearch.setVisibility(View.VISIBLE);
			myAdapter.clear();
			myAdapter.notifyDataSetChanged();

			currentPageId--;
			if (currentPageId < 0) {
				currentPageId = 0;
			}

			refreshShow();

			mService.queryMembers(mChanId, currentPageId);
		} else if (id == R.id.btn_next_page) {
			mPBSearch.setVisibility(View.VISIBLE);
			myAdapter.clear();
			myAdapter.notifyDataSetChanged();

			currentPageId++;

			refreshShow();

			mService.queryMembers(mChanId, currentPageId);
		}
	}

	class MemberListAdapter extends BaseAdapter {
		private final InterpttService service;
		private ArrayList<MyMember> mMembers;
		private LayoutInflater mInflator;

		public MemberListAdapter(final InterpttService service) {
			super();
			this.service = service;
			mMembers = new ArrayList<MyMember>();
			mInflator = MemberChannel.this.getLayoutInflater();
		}

		public void addmember(MyMember m) {
			if (!mMembers.contains(m)) {
				mMembers.add(m);
			}
		}

		public MyMember getMember(int position) {
			return mMembers.get(position);
		}

		public void clear() {
			mMembers.clear();
		}

		@Override
		public int getCount() {
			return mMembers.size();
		}

		@Override
		public Object getItem(int i) {
			return mMembers.get(i);
		}

		public void setItem(int i, MyMember chan) {
			mMembers.set(i, chan);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			ViewHolder viewHolder;
			// General ListView optimization code.
			if (view == null) {
				view = mInflator.inflate(R.layout.listitem_member, null);
				viewHolder = new ViewHolder();
				viewHolder.tvUid = (TextView) view.findViewById(R.id.tv_member_id);
				viewHolder.tvNick = (TextView) view.findViewById(R.id.tv_member_nick);

				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			MyMember m = mMembers.get(i);
			viewHolder.tvUid.setText(m.uid);
			viewHolder.tvNick.setText(m.nick);

			return view;
		}
	}

	static class ViewHolder {
		TextView tvUid;
		TextView tvNick;
	}

	public static class MyMember {
		String uid;
		String nick;

		public MyMember(String _uid, String _nick) {
			uid = _uid;
			nick = _nick;
		}
	}

}
