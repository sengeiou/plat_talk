package com.kylindev.totalk.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kylindev.totalk.R;
import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.InterpttService.LocalBinder;
import com.kylindev.pttlib.service.model.Channel;
import com.kylindev.totalk.utils.AppCommonUtil;
import com.kylindev.totalk.utils.AppSettings;

import java.util.ArrayList;

import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTED;


public class SearchChannel extends Activity implements ListView.OnItemClickListener, OnClickListener {
	/**
	 * The InterpttService instance that drives this activity's data.
	 */
	private InterpttService mService;
	// Create dialog
	private ProgressDialog mConnectDialog = null;
	private Intent mServiceIntent = null;

	private ListView myList;
	private SearchedChannelListAdapter myAdapter;
	private ImageView mIVLeave;
	private EditText mETKeyword;
	private ImageButton mIBSearch;

	private ProgressBar mPBSearch;

	private boolean mServiceBind = false;
	private Handler mSearchTimeoutHandler = new Handler();

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
		setContentView(R.layout.activity_search_channel);

		//可能是首次运行，也可能是用户重新launch
		//因此，先检查service是否在运行，如果是，则直接bind以获取mService实例；如果没有，则startService，再bind
		mServiceIntent = new Intent(this, InterpttService.class);
		initServiceConnection();
		mServiceBind = bindService(mServiceIntent, mServiceConnection, 0);

		//之前channelView相关的
		mIVLeave = (ImageView) findViewById(R.id.iv_search_leave);
		mIVLeave.setOnClickListener(this);
		mETKeyword = (EditText) findViewById(R.id.et_search_channel);
		mIBSearch = (ImageButton) findViewById(R.id.ib_search_channel);
		mIBSearch.setOnClickListener(this);

		mPBSearch = (ProgressBar) findViewById(R.id.pb_search_channel);

		// Get the UI views
		myList = (ListView) findViewById(R.id.lv_searched_chan);
		myList.setOnItemClickListener(this);
		myAdapter = new SearchedChannelListAdapter(mService);
		myList.setAdapter(myAdapter);

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//点击选择，开始连接
		final MyChan chan = myAdapter.getChan(position);
		if (chan == null)
			return;

		boolean joined = chan.joined;
		if (! joined) {
			//加入频道
			final int cId = chan.chanId;
			if (mService != null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(chan.chanName);
				LayoutInflater inflater = LayoutInflater.from(this);
				View layout = inflater.inflate(R.layout.search_and_join_channel, null);
				builder.setView(layout);
				final EditText etPwd = (EditText) layout.findViewById(R.id.et_search_and_join_channel_pwd);
				final EditText etComment = (EditText) layout.findViewById(R.id.et_search_and_join_channel_comment);

				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						final String pwd = etPwd.getText().toString();
						final String comment = etComment.getText().toString();
						if (mService != null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
							mService.joinChannel(cId, pwd, comment);
						}
					}
				});

				builder.setNegativeButton(R.string.cancel, null);
				builder.show();
			}
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

	////////////////////////////////
	private BaseServiceObserver serviceObserver = new BaseServiceObserver() {
		@Override
		public void onChannelSearched(int chanId, String chanName, boolean joined, boolean needPwd, int onlineCount, int memberCount) throws RemoteException {
			mPBSearch.setVisibility(View.GONE);

			MyChan c = new MyChan(chanId, chanName, joined, needPwd, onlineCount, memberCount);
			myAdapter.addChan(c);

			myAdapter.notifyDataSetChanged();
		}

		@Override
		public void onChannelAdded(Channel channel) throws RemoteException {
			//新加入了一个频道，检查是否在搜索到到频道列表里。如果在，则显示为已加入
			int count = myAdapter.getCount();
			for (int i=0; i<count; i++) {
				MyChan c = myAdapter.getChan(i);

				if (c.chanId == channel.id) {
					c.joined = true;
					myAdapter.setItem(i, c);
					myAdapter.notifyDataSetChanged();
					break;
				}
			}
		}
	};

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();

		if (id == R.id.iv_search_leave) {
			finish();
		} else if (id == R.id.ib_search_channel) {//先清除已有数据
			myAdapter.clear();
			myAdapter.notifyDataSetChanged();

			String key = mETKeyword.getText().toString();
			if (mService != null) {
				mPBSearch.setVisibility(View.VISIBLE);
				//设置超时
				mSearchTimeoutHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						mPBSearch.setVisibility(View.GONE);
					}
				}, 10000);    //10秒

				mService.searchChannel(key);
			}
		}
	}

	class SearchedChannelListAdapter extends BaseAdapter {
		private final InterpttService service;
		private ArrayList<MyChan> mChans;
		private LayoutInflater mInflator;

		public SearchedChannelListAdapter(final InterpttService service) {
			super();
			this.service = service;
			mChans = new ArrayList<MyChan>();
			mInflator = SearchChannel.this.getLayoutInflater();
		}

		public void addChan(MyChan chan) {
			if (!mChans.contains(chan)) {
				mChans.add(chan);
			}
		}

		public MyChan getChan(int position) {
			return mChans.get(position);
		}

		public void clear() {
			mChans.clear();
		}

		@Override
		public int getCount() {
			return mChans.size();
		}

		@Override
		public Object getItem(int i) {
			return mChans.get(i);
		}

		public void setItem(int i, MyChan chan) {
			mChans.set(i, chan);
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
				view = mInflator.inflate(R.layout.listitem_searched_chan, null);
				viewHolder = new ViewHolder();
				viewHolder.chanName = (TextView) view.findViewById(R.id.searched_chan_name);
				viewHolder.chanId = (TextView) view.findViewById(R.id.searched_chan_id);
				viewHolder.tvCount = (TextView) view.findViewById(R.id.searched_chan_count);
				viewHolder.ivJoined = (ImageView) view.findViewById(R.id.iv_searched_chan_joined);
				viewHolder.ivLock = (ImageView) view.findViewById(R.id.iv_searched_chan_needpwd);

				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			MyChan chan = mChans.get(i);
			viewHolder.chanName.setText(chan.chanName);
			final int chanId = chan.chanId;
			viewHolder.chanId.setText(getString(R.string.chanid, chanId));

			//在线人数
			if (chan.onlineCount >= 0) {
				viewHolder.tvCount.setText("(" + chan.onlineCount + "/" + chan.memberCount + ")");
			}

			//连接指示
			boolean j = chan.joined;
			viewHolder.ivJoined.setVisibility(j ? View.VISIBLE : View.INVISIBLE);

			//是否需要口令
			boolean l = chan.needPwd;
			viewHolder.ivLock.setVisibility(l ? View.VISIBLE : View.INVISIBLE);

			return view;
		}
	}

	static class ViewHolder {
		TextView chanId;
		TextView chanName;
		TextView tvCount;
		ImageView ivJoined;
		ImageView ivLock;
	}

	public class MyChan {
		int chanId;
		String chanName;
		boolean joined;
		boolean needPwd;
		int onlineCount;
		int memberCount;

		public MyChan(int _chanId, String _chanName, boolean _joined, boolean _needPwd, int _onlineCount, int _memberCount) {
			chanId = _chanId;
			chanName = _chanName;
			joined = _joined;
			needPwd = _needPwd;
			onlineCount = _onlineCount;
			memberCount = _memberCount;
		}
	}

}
