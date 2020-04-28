package com.kylindev.totalk.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kylindev.pttlib.db.ChatMessageBean;
import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.model.Channel;
import com.kylindev.totalk.R;
import com.kylindev.totalk.app.BaseActivity;
import com.kylindev.totalk.app.ChannelActivity;
import com.kylindev.totalk.utils.AppSettings;
import com.kylindev.totalk.view.pulltorefreshview.PullToRefreshRecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.kylindev.pttlib.LibConstants.TABLE_NAME_CALL;
import static com.kylindev.pttlib.LibConstants.TABLE_NAME_HISTORY;

public class RecyclerViewChatActivity extends Activity implements ListView.OnItemClickListener {
    private PullToRefreshRecyclerView myList;
    private ChatRecyclerAdapter tbAdapter;
    private SendMessageHandler sendMessageHandler;

    //左侧频道列表
    private ListView channelList;
    private HistoryAdapter channelAdapter;
    private ArrayList<Channel> channels = new ArrayList<Channel>();

    private ImageView mIVBack;
    private ImageView mIVSetting, mIVDelete;

    private static int RECORDS_PER_PAGE = 50;    //聊天记录每页几条


    private InterpttService mService;// mChatDbManager;
    private List<ChatMessageBean> pagelist = new ArrayList<ChatMessageBean>();  //一页的
    private List<ChatMessageBean> tblist = new ArrayList<ChatMessageBean>();                   //所有load出来的
    private static final int LOAD_MORE = 0x02;
    private static final int RECEIVE_OK = 0x03;
    private static final int UPDATE_PERCENT = 0x04;
    private static final int REMOVE_ITEM = 0x05;
    private static final int PLAYBACK_REFRESH = 0x06;

    private Intent mServiceIntent = null;
    private boolean mServiceBind = false;

    private int currentChanId = 0;
    private AlertDialog mDeleteDialog = null;
    String srcview;
    /**
     * Management of service connection state.
     */
    private ServiceConnection mServiceConnection = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        findView();
        srcview = getIntent().getStringExtra("SOURCE");
        mServiceIntent = new Intent(this, InterpttService.class);
        initServiceConnection();
        mServiceBind = bindService(mServiceIntent, mServiceConnection, 0);
    }

    private void findView() {
        mIVBack = findViewById(R.id.iv_chat_back);
        mIVSetting = findViewById(R.id.iv_history_setting);
        mIVSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RecyclerViewChatActivity.this);
                LayoutInflater inflater = LayoutInflater.from(RecyclerViewChatActivity.this);
                View layout = inflater.inflate(R.layout.set_history_hour, null);
                final TextView tvHours = layout.findViewById(R.id.tv_hours);
                final SeekBar sb = layout.findViewById(R.id.sb_history_hours);

                //未按确定前改变当前的volume
                int nowHours = mService.getHistoryHours();
                tvHours.setText("" + nowHours);
                sb.setProgress(nowHours);
                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        tvHours.setText("" + progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                builder.setView(layout);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int iHours = sb.getProgress();
                        //按完确定键后，保存到sharedpreference中
                        if (mService != null) {
                            mService.setHistoryHours(iHours);
                        }
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);

                builder.show();
            }
        });

        mIVDelete= findViewById(R.id.iv_chat_delete);
        mIVBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //展示DialogFragment  删除历史记录选项
        mIVDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater=LayoutInflater.from(RecyclerViewChatActivity.this);
                View layout=inflater.inflate(R.layout.dialogfragment_deleteinfo,null);
                LinearLayout li_deleteAll= (LinearLayout) layout.findViewById(R.id.Li_delete_AllChannelInfo);
                LinearLayout li_delete= (LinearLayout) layout.findViewById(R.id.Li_delete_ChannelInfo);
                li_deleteAll.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(RecyclerViewChatActivity.this)
                                .setTitle(R.string.delete_current_channel_record)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if(currentChanId==-1) {
                                            mService.deleteChannelDB(TABLE_NAME_CALL, currentChanId);
                                        }else{
                                            mService.deleteChannelDB(TABLE_NAME_HISTORY, currentChanId);
                                        }
                                        sendMessageHandler.sendEmptyMessage(REMOVE_ITEM);
                                    }
                                })
                                .setNegativeButton(R.string.cancel,null)
                                .show();
                        if (mDeleteDialog != null) {
                            mDeleteDialog.dismiss();
                        }
                    }
                });
                li_delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new AlertDialog.Builder(RecyclerViewChatActivity.this)
                                .setTitle(R.string.delete_all_channel_records)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mService.deleteChannelAllDB(TABLE_NAME_HISTORY);
                                        sendMessageHandler.sendEmptyMessage(REMOVE_ITEM);
                                    }
                                })
                                .setNegativeButton(R.string.cancel,null)
                                .show();
                        if (mDeleteDialog != null) {
                            mDeleteDialog.dismiss();
                        }
                    }
                });

                final AlertDialog.Builder builder = new AlertDialog.Builder(RecyclerViewChatActivity.this)
                        .setTitle(R.string.delete_history_information)
                        .setView(layout);

                mDeleteDialog = builder.show();
            }
        });

        // Get the UI views
        channelList = (ListView) findViewById(R.id.lv_history);//频道列表
        channelList.setOnItemClickListener(this);

        myList = (PullToRefreshRecyclerView) findViewById(R.id.content_lv);//历史记录列表
    }

    private void initServiceConnection() {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                InterpttService.LocalBinder localBinder = (InterpttService.LocalBinder) service;
                mService = localBinder.getService();
                serviceReady();

                //
                init();

                //频道列表
                channelAdapter = new HistoryAdapter(mService);
                channelList.setAdapter(channelAdapter);//添加适配器

                //加载本地聊天记录
                int cid = mService.getHistoryChanId();
                if (cid == 0) {
                    if (mService.getCurrentChannel()!=null){
                        cid = mService.getCurrentChannel().id;
                    }
                }

                if (cid > 0) {
                    doClickChan(cid);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
                mServiceConnection = null;
            }
        };
    }

    @Override
    protected void onDestroy() {
        if (mService != null) {
            mService.unregisterObserver(serviceObserver);
        }

        tbAdapter.notifyDataSetChanged();
        myList.setAdapter(null);
        sendMessageHandler.removeCallbacksAndMessages(null);  //清空当前handler队列，避免内存泄露
        if (mService != null) {
            mService.stopPlayback();

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

    protected void init() {
        setTitle("RecyclerView");
        tbAdapter = new ChatRecyclerAdapter(this, tblist, mService);
        // custom own load-more-view and add it into ptrrv
//        PullLoadMoreView loadMoreView = new PullLoadMoreView(this, myList.getRecyclerView());
//        loadMoreView.setLoadmoreString(getString(R.string.loading));
//        loadMoreView.setLoadMorePadding(100);
//        myList.setLoadMoreFooter(loadMoreView);

        myList.setSwipeEnable(true);

        myList.setLayoutManager(new LinearLayoutManager(this));
        // set PagingableListener
        myList.setPagingableListener(new PullToRefreshRecyclerView.PagingableListener() {
            @Override
            public void onLoadMoreItems() {
//                Log.w("t", "=====more");
//                //do loadmore here
//                loadMoreRecords(currentChanId);
            }
        });

        // set OnRefreshListener
        myList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // do refresh here
                loadMoreRecords(currentChanId, false);
            }
        });

        // set loadmore String
//        myList.setLoadmoreString("loading");

        // set loadmore enable, onFinishLoading(can load more? , select before item)
//        myList.onFinishLoading(true, false);

        myList.setAdapter(tbAdapter);
        sendMessageHandler = new SendMessageHandler(this);
       // tbAdapter.notifyDataSetChanged();

        tbAdapter.setVoiceIsReadListener(new ChatRecyclerAdapter.VoiceIsRead() {

            @Override
            public void voiceOnClick(int position) {
                // TODO Auto-generated method stub
                for (int i = 0; i < tbAdapter.unReadPosition.size(); i++) {
                    if (tbAdapter.unReadPosition.get(i).equals(position + "")) {
                        tbAdapter.unReadPosition.remove(i);

                        break;
                    }
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void serviceReady() {
        //means InterpttService is connected
        mService.registerObserver(serviceObserver);
    }

    int moreRecords = 0;    //记录每次新增了多少条数据，用于列表显示到合适的位置
    //如果tablist有数据，则获取下一页；如果没数据，获取一页
    private void loadMoreRecords(int cid, boolean first) {
        if (first) {
            //说明要load新频道的数据，清空之前的
            tblist.clear();
            tbAdapter.clearData();
            //通知adapter，当前chanid
            tbAdapter.setCurrentChanId(cid);
            tbAdapter.notifyDataSetChanged();

            if (pagelist != null) {
                pagelist.clear();
            }
        }

        int nowItems = tblist.size();
        pagelist = mService.loadDBRecords(cid, nowItems, RECORDS_PER_PAGE);
        //如果有历史记录的话
        if (pagelist!=null && pagelist.size() != 0) {
            //Log.d("call_car", "pagelist.size:"+pagelist.size()+"cid="+cid);
            tblist.addAll(0, pagelist);
            moreRecords = pagelist.size();
        } else {
            moreRecords=0;
        }
        sendMessageHandler.sendEmptyMessage(LOAD_MORE);
    }

    private void clearRecords() {
        tblist.clear();
        tbAdapter.clearData();
        tbAdapter.notifyDataSetChanged();
    }

    static class SendMessageHandler extends Handler {
        WeakReference<RecyclerViewChatActivity> mActivity;

        SendMessageHandler(RecyclerViewChatActivity activity) {
            mActivity = new WeakReference<RecyclerViewChatActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            RecyclerViewChatActivity theActivity = mActivity.get();
            if (theActivity != null) {
                switch (msg.what) {
                    case LOAD_MORE:
                        //获取窗口显示条目数
                        int ItemLen= theActivity.myList.findLastVisibleItemPosition()-theActivity.myList.findFirstCompletelyVisibleItemPosition();
                        theActivity.tbAdapter.notifyDataSetChanged();
                        theActivity.myList.setOnRefreshComplete();
                        theActivity.myList.onFinishLoading(true, false);
                        break;
                    case RECEIVE_OK:
                        theActivity.tbAdapter.notifyItemInserted(theActivity.tblist.size() - 1);
                        //如果每次收到新消息就滚到到最后，则用户在浏览之前记录时，会不时被迫滚动。因此加判断
                        if (theActivity.myList.getRecyclerView().canScrollVertically(1)) {
                            //能向上滚动，说明不在底部
                        } else {
                            //在底部
                            int position = theActivity.tbAdapter.getItemCount() - 1 < 0 ? 0 : theActivity.tbAdapter.getItemCount() - 1;
                            theActivity.myList.smoothScrollToPosition(position);
                        }
                        break;
                    case UPDATE_PERCENT:
                        break;
                    case REMOVE_ITEM:
                        theActivity.clearRecords();
                        break;
                    case PLAYBACK_REFRESH:
                        theActivity.tbAdapter.notifyDataSetChanged();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private BaseServiceObserver serviceObserver = new BaseServiceObserver() {
        @Override
        public void onRecordFinished(ChatMessageBean cmb) {
            //可能录音的频道，不是当前显示的频道，所以过滤一下
            if (cmb == null) {
                return;
            }
            Integer voiceChanId = cmb.getCid();
            if(voiceChanId!=null) {
                if (voiceChanId.equals(currentChanId)) {
                    tblist.add(cmb);
                    sendMessageHandler.sendEmptyMessage(RECEIVE_OK);
                }
            }
        }

        @Override
        public void onPlaybackChanged(final int chanId, final int resId, final boolean start)  {
            if (start) {
                tbAdapter.startPlayVoice(chanId, resId);
            } else {
                tbAdapter.stopPlayVoice();
            }

            //刷新播放动画
            sendMessageHandler.sendEmptyMessage(PLAYBACK_REFRESH);
        }
    };

    //频道列表
    class HistoryAdapter extends BaseAdapter {
        private final InterpttService service;
        private ArrayList<MyChan> mChans;
        private LayoutInflater mInflator;

        public HistoryAdapter(final InterpttService service) {
            super();
            this.service = service;
            mChans = new ArrayList<MyChan>();
            mInflator = getLayoutInflater();
        }

        public void updateData() {
            mChans.clear();
            if (service == null) {
                return;
            }

            channels = service.getChannelList();
            service.getCallCount();
            if (channels != null && channels.size()>0) {
                for (Channel c:channels) {
                    MyChan mc = new MyChan(c.id, c.name);
                    mChans.add(mc);
                }
            }
            //增加历史电话列表
            if( service.getCallCount()>0 && srcview.equals("OPEN")) {
                MyChan mc = new MyChan(-1, "订单记录");
                mChans.add(mc);
            }


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
                view = mInflator.inflate(R.layout.listitem_history_chans, null);
                viewHolder = new ViewHolder();
                viewHolder.chanName = (TextView) view.findViewById(R.id.tv_history_chan_name);
                viewHolder.chanId = (TextView) view.findViewById(R.id.tv_history_chan_id);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            MyChan chan = mChans.get(i);
            viewHolder.chanName.setText(chan.chanName);   //频道名称
            final int cid = chan.chanId;
            viewHolder.chanId.setText(getString(R.string.chanid, cid)); //频道号

            if (cid == currentChanId) {
                view.setBackgroundResource(R.drawable.selector_current_channel);
            }
            else {
                view.setBackgroundResource(R.drawable.selector_other_channel);
            }

            return view;
        }
    }

    static class ViewHolder {
        TextView chanId;
        TextView chanName;
    }

    public class MyChan {
        int chanId;
        String chanName;

        public MyChan(int _chanId, String _chanName) {
            chanId = _chanId;
            chanName = _chanName;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //点击选择，开始连接
        final MyChan chan = channelAdapter.getChan(position);
        if (chan == null)
            return;
        doClickChan(chan.chanId);
    }

    private void doClickChan(int chanId) {
        currentChanId = chanId;

        moreRecords = 0;
        if (mService != null) {
            mService.setHistoryChanId(chanId);
        }
        //刷新当前频道高亮显示
        channelAdapter.updateData();
        channelAdapter.notifyDataSetChanged();
        loadMoreRecords(chanId, true);
    }
}
