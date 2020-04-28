package com.kylindev.totalk.app;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kylindev.pttlib.LibConstants;
import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.InterpttService.HandmicState;
import com.kylindev.pttlib.service.InterpttService.HeadsetState;
import com.kylindev.pttlib.service.InterpttService.MicState;
import com.kylindev.pttlib.service.model.Channel;
import com.kylindev.pttlib.service.model.Contact;
import com.kylindev.pttlib.service.model.PendingMember;
import com.kylindev.pttlib.service.model.User;
import com.kylindev.totalk.AppConstants;
import com.kylindev.totalk.R;
import com.kylindev.totalk.utils.AppCommonUtil;
import com.kylindev.totalk.utils.AppSettings;
import com.kylindev.totalk.view.ActionItem;
import com.kylindev.totalk.view.CircleImageView;
import com.kylindev.totalk.view.InterpttNestedAdapter;
import com.kylindev.totalk.view.InterpttNestedListView;
import com.kylindev.totalk.view.TitlePopup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.kylindev.pttlib.service.InterpttService.ConnState;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTED;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTING;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_DISCONNECTED;

public class ChannelActivity extends BaseActivity implements OnClickListener,InterpttNestedListView.OnNestedChildClickListener, InterpttNestedListView.OnNestedChildLongClickListener, InterpttNestedListView.OnNestedGroupClickListener, InterpttNestedListView.OnNestedGroupLongClickListener  {
    private Handler mHandler = new Handler();    //用于其他线程更新UI

    private boolean appWantQuit = false;

    private AlertDialog mDialog = null;
    private AlertDialog mJoinChannelDialog = null;

    private boolean pubChecked = false;    //纪录创建频道时，是否公开的选框
    private boolean needapplyChecked = false;    //纪录创建频道时，是否需要审核

    private ApplyListAdapter applyAdapter;
    private ListView mLVPendingMember;

    private InterpttNestedListView mLVChannel;
    private ChannelListAdapter channelAdapter;

    private LinearLayout mLLTips;

    @Override
    protected void serviceConnected() {
        mService.registerObserver(serviceObserver); //baseactivity里注册observer，这里也要注册
        mService.enableBleButtonPtt(true);

        //此时有可能是服务器断开状态，且用户点击app图标执行到这里的。因此，应先检查service的状态是否是CONNECTION_STATE_CONNECTED
        //是的话，才能执行setupChannelList等操作。否则，无需setup，只需等待自动重连后，自动setup
        if (mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
            setupList();
        }

        refreshNewContactApply();
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_channel;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setIsMap(false);
        mIVBarLeft.setImageResource(R.drawable.ic_leave);
        mIVBarLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService != null && mService.getConnectionState() == CONNECTION_STATE_CONNECTED && mService.getCurrentChannel() != null && mService.getCurrentChannel().id != 0) {
                    //正常连接，且不在守候频道，应离开当前频道，进入守候频道
                    mService.enterChannel(0);
                }
            }
        });
        mIVBarLeftInner.setImageResource(R.drawable.ic_contact);
        mIVBarLeftInner.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ChannelActivity.this, ContactActivity.class);
                startActivity(i);
            }
        });


        //wocao add channel
        mIVBarRightInner.setImageResource(R.drawable.add);
        mIVBarRightInner.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //add pindao
            }
        });

        mIVBarRightInner.setImageResource(R.drawable.ic_add);
        mIVBarRightInner.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final TitlePopup addPopup = new TitlePopup(ChannelActivity.this, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                addPopup.addAction(new ActionItem(ChannelActivity.this, getString(R.string.join_channel), R.drawable.join_channel));
                addPopup.addAction(new ActionItem(ChannelActivity.this, getString(R.string.search_channel), R.drawable.ic_search));
                addPopup.addAction(new ActionItem(ChannelActivity.this, getString(R.string.create_channel), R.drawable.create_channel));

                addPopup.setItemOnClickListener(new TitlePopup.OnItemOnClickListener() {
                    @Override
                    public void onItemClick(ActionItem item, int position) {
                        switch (position) {
                            case 0:
                                joinChannel();
                                break;
                            case 1:
                                searchChannel();
                                break;
                            case 2:
                                createChannel();
                                break;
                            default:
                                break;
                        }
                    }
                });
                addPopup.show(v);
            }
        });
        mIVBarRightInner.setVisibility(View.INVISIBLE);
        mIVBarRight.setImageResource(R.drawable.ic_menu_white);
        mIVBarRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final TitlePopup optionPopup = new TitlePopup(ChannelActivity.this, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                optionPopup.addAction(new ActionItem(ChannelActivity.this, getString(R.string.account), R.drawable.account));
                optionPopup.addAction(new ActionItem(ChannelActivity.this, getString(R.string.settings), R.drawable.setting));
                optionPopup.addAction(new ActionItem(ChannelActivity.this, getString(R.string.accessory), R.drawable.accessory));
                optionPopup.addAction(new ActionItem(ChannelActivity.this, getString(R.string.help), R.drawable.tips));
                optionPopup.addAction(new ActionItem(ChannelActivity.this, getString(R.string.quit), R.drawable.ic_quit));

                optionPopup.setItemOnClickListener(new TitlePopup.OnItemOnClickListener() {
                    @Override
                    public void onItemClick(ActionItem item, int position) {
                        switch (position) {
                            case 0:
                                accountDlg();
                                break;
                            case 1:
                                settings();
                                break;
                            case 2:
                                handMicActivity();
                                break;
                            case 3:
                                help();
                                break;
                            case 4:
                                quit();
                                break;
                            default:
                                break;
                        }
                    }
                });
                optionPopup.show(v);
            }
        });

        mTVBarTitle.setText(R.string.app_name);

        // Get the UI views
        //invitation
        mLVPendingMember = (ListView) findViewById(R.id.lv_pending_member);
        mLVPendingMember.setOnItemClickListener(mApplyItemClickListener);

        mLVChannel = (InterpttNestedListView) findViewById(R.id.channelUsers);
        mLVChannel.setOnChildClickListener(this);
        mLVChannel.setOnChildLongClickListener(this);
        mLVChannel.setOnGroupClickListener(this);
        mLVChannel.setOnGroupLongClickListener(this);

        mLLTips = (LinearLayout) findViewById(R.id.ll_tips);
        TextView tvCreateChannel = (TextView) findViewById(R.id.tv_create_channel);
        tvCreateChannel.setOnClickListener(this);
        TextView tvJoinChannel = (TextView) findViewById(R.id.tv_join_channel);
        tvJoinChannel.setOnClickListener(this);

        getPermissions();
    }

    @Override
    public void onResume() {
        if (mService != null) {
            mService.activityShowing(true);
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mService != null) {
            mService.unregisterObserver(serviceObserver);
        }

        super.onDestroy();
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			boolean selecting = mService!=null && mService.isSelectingContact();
			if (selecting) {
				mService.cancelSelect();
				return true;
			}
		}

        return super.onKeyDown(keyCode, event);
    }

    ////////////////////////////////
    private BaseServiceObserver serviceObserver = new BaseServiceObserver() {
        public void onConnectionStateChanged(ConnState state) throws RemoteException {
            switch (state) {
                case CONNECTION_STATE_CONNECTING:
                    break;
                case CONNECTION_STATE_SYNCHRONIZING:
                    break;
                case CONNECTION_STATE_CONNECTED:
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setupList();
                        }
                    });
                    break;
                case CONNECTION_STATE_DISCONNECTED:
                    if (appWantQuit) {
                        bye();
                    } else {
                        //setnull不能省，否则断开后继续显示adapter的getview，其中getcurrentuser等得到null，崩溃
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setListAdapter(null);
                            }
                        });
                    }
                    break;
            }
        }

        @Override
        public void onPermissionDenied(String reason, int denyType) throws RemoteException {
        }

        @Override
        public void onCurrentChannelChanged() throws RemoteException {
            if (mService == null) {
                return;
            }
            if (mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
                handleCurrentChannelChanged();
            }
        }

        @Override
        public void onChannelAdded(Channel channel) throws RemoteException {
            handleChannelAdded(channel);
        }

        @Override
        public void onChannelRemoved(Channel channel) throws RemoteException {
            handleChannelRemoved(channel);
        }

        //某个频道信息有变化
        @Override
        public void onChannelUpdated(Channel channel) throws RemoteException {
            handleChannelUpdated(channel);
        }

        @Override
        public void onUserUpdated(User user) throws RemoteException {
            updateUser(user);
        }

        @Override
        public void onUserTalkingChanged(User user, boolean talk) throws RemoteException {
            updateChannelList();    //如果只刷新user，则频道上的talker指示不能刷新
        }

        @Override
        public void onLocalUserTalkingChanged(User user, boolean talk) throws RemoteException {
            //刷新频道红点
            updateChannelList();    //如果只刷新user，则频道上的talker指示不能刷新
        }


        @Override
        public void onNewVolumeData(short volume) throws RemoteException {

        }

        @Override
        public void onMicStateChanged(MicState s) throws RemoteException {

        }

        @Override
        public void onHeadsetStateChanged(HeadsetState s) throws RemoteException {

        }

        @Override
        public void onScoStateChanged(int s) throws RemoteException {

        }

        @Override
        public void onTargetHandmicStateChanged(BluetoothDevice device, HandmicState s) throws RemoteException {

        }

        @Override
        public void onTalkingTimerTick(int seconds) throws RemoteException {
        }

        @Override
        public void onTalkingTimerCanceled() throws RemoteException {
        }

        @Override
        public void onUserAdded(final User u) throws RemoteException {
            updateChannelList();
        }

        @Override
        public void onUserRemoved(final User user) throws RemoteException {
            updateChannelList();
        }

        @Override
        public void onLeDeviceScanStarted(boolean start) throws RemoteException {

        }

        @Override
        public void onShowToast(final String str) throws RemoteException {
            AppCommonUtil.showToast(ChannelActivity.this, str);
        }

        @Override
        public void onInvited(final Channel chan) throws RemoteException {
            //mayNotifyInvitation();
        }

        @Override
        public void onPendingMemberChanged() throws RemoteException {
            updateChannelList();
        }

        @Override
        public void onApplyContactReceived(boolean add, Contact contact) throws RemoteException {
            refreshNewContactApply();
        }

        @Override
        public void onPendingContactChanged() throws RemoteException {
            refreshNewContactApply();
        }

        @Override
        public void onSynced() throws RemoteException {
            updateChannelList();
        }

        @Override
        public void onVoiceToggleChanged(boolean on) throws RemoteException {

        }

        @Override
        public void onListenChanged(boolean listen) throws RemoteException {
            updateChannelList();
            if (listen) {
                AppCommonUtil.showToast(ChannelActivity.this, R.string.listen_ok);
            }
		}

		@Override
		public void onApplyOrderResult(int uid, int cid, String phone, boolean success) throws RemoteException {
		}

		@Override
        public void onPcmRecordFinished(short[] data, int length) throws RemoteException {
        }

        @Override
        public void onBleButtonDown(boolean down) throws RemoteException {

        }
    };

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        int id = v.getId();

        if (id == R.id.ll_count) {
            setAccount();
        } else if (id == R.id.tv_create_channel) {
            createChannel();
        } else if (id == R.id.tv_join_channel) {
            joinChannel();
        }

        super.onClick(v);
    }

    private void setAccount() {
        if (mService == null || mService.getConnectionState() != CONNECTION_STATE_CONNECTED) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.set_account, null);

        builder.setTitle(R.string.set_count);
        builder.setView(layout);

        final TextView tvNick = (TextView) layout.findViewById(R.id.tv_change_nick);
        tvNick.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
                changeNick();
            }
        });

//		final TextView tvAvatar = (TextView) layout.findViewById(R.id.tv_change_avatar);
//		tvAvatar.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				mDialog.dismiss();
//				setAvatar();
//			}
//		});

        final TextView tvPwd = (TextView) layout.findViewById(R.id.tv_change_password);
        tvPwd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
                changePwd();
            }
        });

        mDialog = builder.show();
    }

    private void refreshNewContactApply() {
        if (mService == null) {
            return;
        }

        Map<Integer, Contact> pendingContactMap = mService.getPendingContacts();
        if (pendingContactMap != null && pendingContactMap.size() > 0) {
            //联系人图标高亮
            mIVBarLeftInner.setImageResource(R.drawable.ic_contact_new_apply);
        }
        else {
            mIVBarLeftInner.setImageResource(R.drawable.ic_contact);
        }
    }

    private void changeNick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.change_nick, null);

        builder.setTitle(R.string.change_nick);
        builder.setView(layout);
        final EditText etNick = (EditText) layout.findViewById(R.id.et_change_nick);
        if (mService != null && mService.getCurrentUser() != null)
            etNick.setText(mService.getCurrentUser().nick);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                final String nick = etNick.getText().toString();
                if (!AppCommonUtil.validNick(nick)) {
                    AppCommonUtil.showToast(ChannelActivity.this, R.string.nick_bad_format);
                } else {
                    if (mService != null && mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
                        mService.changeNick(nick);
                    }
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    private void changePwd() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);//, android.R.style.Theme_Holo_Light_Dialog);

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.change_password, null);

        builder.setTitle(R.string.change_pwd);
        builder.setView(layout);
        final EditText etPwd0 = (EditText) layout.findViewById(R.id.et_change_pwd0);
        final EditText etPwd1 = (EditText) layout.findViewById(R.id.et_change_pwd1);
        final EditText etPwd2 = (EditText) layout.findViewById(R.id.et_change_pwd2);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                final String pwd0 = etPwd0.getText().toString();
                final String pwd1 = etPwd1.getText().toString();
                final String pwd2 = etPwd2.getText().toString();
                //临时解决方案，只在本地检查密码 TODO：服务器检查
                final String storedPwd = AppSettings.getInstance(ChannelActivity.this).getPassword();
                if (!pwd0.equals(storedPwd)) {
                    AppCommonUtil.showToast(ChannelActivity.this, R.string.wrong_old_pwd);
                } else if (!pwd1.equals(pwd2)) {
                    AppCommonUtil.showToast(ChannelActivity.this, R.string.different_pwd);
                } else if (!AppCommonUtil.validPwd(pwd1)) {
                    AppCommonUtil.showToast(ChannelActivity.this, R.string.password_bad_format);
                } else {
                    if (mService != null && mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
                        try {
                            mService.changePassword(pwd1);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            AppCommonUtil.showToast(ChannelActivity.this, "操作失败，请重试");
                            return;
                        }
                    }
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    public void createChannel() {
        if (mService.getConnectionState() == CONNECTION_STATE_DISCONNECTED) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.create_channel, null);
        builder.setView(layout);
        final EditText etChannel = (EditText) layout.findViewById(R.id.et_channel_name);
        final EditText etPwd1 = (EditText) layout.findViewById(R.id.et_channel_pwd1);
        final ImageView ivPub = (ImageView) layout.findViewById(R.id.iv_searchable_chan);
        final ImageView ivApply = (ImageView) layout.findViewById(R.id.iv_need_apply);
        pubChecked = false;
        final LinearLayout llPub = (LinearLayout) layout.findViewById(R.id.ll_searchable_chan);
        llPub.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pubChecked = !pubChecked;
                ivPub.setImageResource(pubChecked ? R.drawable.checkbox_on : R.drawable.checkbox_off);
            }
        });
        needapplyChecked = false;
        final LinearLayout llApply = (LinearLayout) layout.findViewById(R.id.ll_need_apply);
        llApply.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                needapplyChecked = !needapplyChecked;
                ivApply.setImageResource(needapplyChecked ? R.drawable.checkbox_on : R.drawable.checkbox_off);
            }
        });

        builder.setTitle(R.string.create_channel);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                final String str = etChannel.getText().toString();
                final String pwd1 = etPwd1.getText().toString();

                if (!AppCommonUtil.validChannelName(str)) {
                    AppCommonUtil.showToast(ChannelActivity.this, R.string.channel_name_bad_format);
                } else if (etPwd1.length() != 0 && !AppCommonUtil.validChannelPwd(pwd1)) {
                    AppCommonUtil.showToast(ChannelActivity.this, R.string.channel_pwd_bad_format);
                } else {
                    //此时有两种情况，要么是合法的口令，要么为空，表示频道无需口令
                    if (mService != null && mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
                        mService.createChannel(str, pwd1, null, pubChecked, needapplyChecked,false);
                    }
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    public void joinChannel() {
        if (mService.getConnectionState() == CONNECTION_STATE_DISCONNECTED) {
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.join_channel);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.join_channel, null);
        builder.setView(layout);
        final EditText etChannel = (EditText) layout.findViewById(R.id.et_join_channel_id);
        final EditText etPwd = (EditText) layout.findViewById(R.id.et_join_channel_pwd);
        final EditText etComment = (EditText) layout.findViewById(R.id.et_join_channel_comment);
        final Button btnJoinCustomChan = (Button) layout.findViewById(R.id.btn_join_custom_chan);

        btnJoinCustomChan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String str = etChannel.getText().toString();
                final String pwd = etPwd.getText().toString();
                final String comment = etComment.getText().toString();
                if (!AppCommonUtil.validChannelId(str)) {
                    AppCommonUtil.showToast(ChannelActivity.this, R.string.channel_id_bad_format);
                } else if (etPwd.length() != 0 && !AppCommonUtil.validChannelPwd(pwd)) {
                    AppCommonUtil.showToast(ChannelActivity.this, R.string.channel_pwd_bad_format);
                } else {
                    if (mService != null && mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
                        try {
                            int id = Integer.parseInt(str);
                            mService.joinChannel(id, pwd, comment);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (mJoinChannelDialog != null) {
                    mJoinChannelDialog.dismiss();
                }
            }
        });
        builder.setNegativeButton(R.string.close, null);

        mJoinChannelDialog = builder.show();
    }

    private void searchChannel() {
        Intent i = new Intent(ChannelActivity.this, SearchChannel.class);
        startActivity(i);
    }

    private void accountDlg() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.account, null);

        builder.setTitle(R.string.account);
        builder.setView(view);

        final LinearLayout llCount = (LinearLayout) view.findViewById(R.id.ll_count);
        llCount.setOnClickListener(this);

        ImageView ivAvatar = (ImageView) view.findViewById(R.id.iv_my_avatar);
        final TextView tvId = (TextView) view.findViewById(R.id.tv_my_id);
        final TextView tvNick = (TextView) view.findViewById(R.id.tv_my_nick);

        //填充账号内容
            User me = mService.getCurrentUser();
            if (me != null) {
                if (me.avatar == null) {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                } else {
                    Bitmap bm = BitmapFactory.decodeByteArray(me.avatar, 0, me.avatar.length);
                    ivAvatar.setImageBitmap(bm);
                }

                tvId.setText(getString(R.string.myid, me.iId));
                tvNick.setText(getString(R.string.mynick, me.nick));
            }

        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    private void rebootCallSystem() {
       mService.rebootCallSystem();
    }

    private void aboutCallSystem() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.callsystem_about, null);

        LinearLayout llgotoMore = (LinearLayout) layout.findViewById(R.id.ll_goto_more);
        llgotoMore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://www.bilibili.com/video/av31981812/");
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                intent.setData(uri);
                startActivity(intent);
            }
        });

        builder.setTitle(R.string.app_name);
        builder.setView(layout);
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    private void settings() {
        if (mService == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.settings, null);

        builder.setTitle(R.string.settings);
        builder.setView(view);

        final ImageView ivFloat = (ImageView) view.findViewById(R.id.iv_float_btn);
        boolean check = mService.getFloatWindow();
        ivFloat.setImageResource(check ? R.drawable.checkbox_on : R.drawable.checkbox_off);

        final LinearLayout llFloatBtn = (LinearLayout) view.findViewById(R.id.ll_float_btn);
        //悬浮窗
        llFloatBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean oldChecked = mService.getFloatWindow();
                boolean now = !oldChecked;
                mService.setFloatWindow(now);
                ivFloat.setImageResource(now ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                if (now) {
                    if (!mService.checkAlertWindowsPermission(mService)) {
                        AppCommonUtil.showToast(ChannelActivity.this, R.string.confirm_float_permission);
                    }
                }
            }
        });

        final ImageView ivAutoLaunch = (ImageView) view.findViewById(R.id.iv_auto_launch);
        boolean c = AppSettings.getInstance(this).getAutoLaunch();
        ivAutoLaunch.setImageResource(c ? R.drawable.checkbox_on : R.drawable.checkbox_off);

        final LinearLayout llAutoLaunch = (LinearLayout) view.findViewById(R.id.ll_auto_launch);
        llAutoLaunch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean old = AppSettings.getInstance(ChannelActivity.this).getAutoLaunch();
                boolean n = !old;
                AppSettings.getInstance(ChannelActivity.this).setAutoLaunch(n);
                ivAutoLaunch.setImageResource(n ? R.drawable.checkbox_on : R.drawable.checkbox_off);
            }
        });

        //强力在线
        final ImageView ivStrongOnline = (ImageView) view.findViewById(R.id.iv_strong_online);
        boolean so = mService.getStrongOnline();
        ivStrongOnline.setImageResource(so ? R.drawable.checkbox_on : R.drawable.checkbox_off);

        final LinearLayout llStrongOnline = (LinearLayout) view.findViewById(R.id.ll_strong_online);
        llStrongOnline.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean old = mService.getStrongOnline();
                boolean n = !old;
                mService.setStrongOnline(n);
                ivStrongOnline.setImageResource(n ? R.drawable.checkbox_on : R.drawable.checkbox_off);
            }
        });
        final ImageView ivSrongHelp = (ImageView) view.findViewById(R.id.iv_more_heartbeat_help);
        ivSrongHelp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ChannelActivity.this)
                        .setMessage(R.string.more_heartbeat_help)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });

        //提示音
        final LinearLayout llNotif = (LinearLayout) view.findViewById(R.id.ll_notification);
        llNotif.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                notifSetting();    //关于音量的设置
            }
        });

        //耳机按键支持
        final ImageView ivHeadset = (ImageView) view.findViewById(R.id.iv_headset_key);
        boolean bt = mService.getSupportHeadsetKey();
        ivHeadset.setImageResource(bt ? R.drawable.checkbox_on : R.drawable.checkbox_off);

        final LinearLayout llHeadset = (LinearLayout) view.findViewById(R.id.ll_headset_key);
        llHeadset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean oldChecked = mService.getSupportHeadsetKey();
                boolean now = !oldChecked;
                mService.setSupportHeadsetKey(now);
                ivHeadset.setImageResource(now ? R.drawable.checkbox_on : R.drawable.checkbox_off);
            }
        });
        final ImageView ivHeadsetHelp = (ImageView) view.findViewById(R.id.iv_headset_help);
        ivHeadsetHelp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ChannelActivity.this)
                        .setMessage(R.string.headset_help)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });

        //禁用蓝牙话筒
        final ImageView ivForbidBtMic = view.findViewById(R.id.iv_forbid_bt_mic);
        boolean fbd = mService.getForbidBtMic();
        ivForbidBtMic.setImageResource(fbd ? R.drawable.checkbox_on : R.drawable.checkbox_off);

        final LinearLayout llFbd = view.findViewById(R.id.ll_forbid_bt_mic);
        llFbd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean oldChecked = mService.getForbidBtMic();
                boolean now = !oldChecked;
                mService.setForbidBtMic(now);
                ivForbidBtMic.setImageResource(now ? R.drawable.checkbox_on : R.drawable.checkbox_off);
            }
        });
        final ImageView ivForbidHelp = view.findViewById(R.id.iv_bt_mic_help);
        ivForbidHelp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ChannelActivity.this)
                        .setMessage(R.string.forbid_bt_mic_help)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });

        //PTT按键定义
        final LinearLayout llPttKey = (LinearLayout) view.findViewById(R.id.ll_ptt_key);
        final TextView tvCode = (TextView) view.findViewById(R.id.tv_ptt_keycode);
        int code = mService.getPttKeycode();
        tvCode.setText(String.valueOf(code));

        llPttKey.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
                LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
                View layout = inflater.inflate(R.layout.set_ptt_key, null);
                final EditText etKeycode = (EditText) layout.findViewById(R.id.et_keycode);
                Button btnDelete = (Button) layout.findViewById(R.id.btn_delete_keycode);
                LinearLayout llBroadcast = (LinearLayout) layout.findViewById(R.id.ll_set_ptt_broadcast);
                btnDelete.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        etKeycode.setText(String.valueOf(0));
                    }
                });

                llBroadcast.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
                        LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
                        View layout = inflater.inflate(R.layout.set_ptt_broadcast, null);
                        final EditText etDown = (EditText) layout.findViewById(R.id.et_broadcast_down);
                        final EditText etUp = (EditText) layout.findViewById(R.id.et_broadcast_up);
                        String savedDown = mService.getBroadcastDown();
                        String savedUp = mService.getBroadcastUp();
                        etDown.setText(savedDown);
                        etUp.setText(savedUp);

                        builder.setView(layout);
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String strDown = etDown.getText().toString();
                                String strUp = etUp.getText().toString();
                                mService.setBroadcastDown(strDown);
                                mService.setBroadcastUp(strUp);
                                //通知service，准备reciever
                                if (mService != null) {
                                    mService.updateCustomPttKeyReceiver();
                                }
                            }
                        });
                        builder.setNegativeButton(R.string.cancel, null);

                        builder.show();
                    }
                });

                int code = mService.getPttKeycode();
                etKeycode.setText(String.valueOf(code));

                builder.setView(layout);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String txt = etKeycode.getText().toString();
                        mService.setPttKeycode(Integer.valueOf(txt).intValue());
                        tvCode.setText(txt);
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);

                AlertDialog dlg = builder.create();
                dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_HOME && keyCode != KeyEvent.KEYCODE_MENU
                                && keyCode != KeyEvent.KEYCODE_MEDIA_PLAY
                                && keyCode != KeyEvent.KEYCODE_MEDIA_PAUSE
                                && keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                                && keyCode != KeyEvent.KEYCODE_HEADSETHOOK
                                ) {
                            //不允许设置某些特殊按键为ptt
                            etKeycode.setText(String.valueOf(keyCode));
                            return true;
                        }

                        return false;
                    }
                });
                dlg.show();
            }
        });

        //录音模式
        final LinearLayout llRecord = (LinearLayout) view.findViewById(R.id.ll_record_mode);
        final TextView tvMode = (TextView) view.findViewById(R.id.tv_record_mode);
        int mode = mService.getRecordMode();
//		tvMode.setText((mode==0) ? getString(R.string.normal_mode) : getString(R.string.process_mode));

        llRecord.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
                LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
                View layout = inflater.inflate(R.layout.set_record_mode, null);
                RadioButton rbNormal = (RadioButton) layout.findViewById(R.id.rb_recordmode_normal);
                RadioButton rbProcess = (RadioButton) layout.findViewById(R.id.rb_recordmode_process);
                RadioButton rbOther1 = (RadioButton) layout.findViewById(R.id.rb_recordmode_other1);
                RadioButton rbOther2 = (RadioButton) layout.findViewById(R.id.rb_recordmode_other2);
                RadioButton rbOther3 = (RadioButton) layout.findViewById(R.id.rb_recordmode_other3);
                RadioButton rbOther4 = (RadioButton) layout.findViewById(R.id.rb_recordmode_other4);
                RadioButton rbOther5 = (RadioButton) layout.findViewById(R.id.rb_recordmode_other5);
                RadioButton rbOther6 = (RadioButton) layout.findViewById(R.id.rb_recordmode_other6);

                int mode = mService.getRecordMode();
                switch (mode) {
                    case 0:
                        rbNormal.setChecked(true);
                        break;
                    case 1:
                        rbProcess.setChecked(true);
                        break;
                    case 2:
                        rbOther1.setChecked(true);
                        break;
                    case 3:
                        rbOther2.setChecked(true);
                        break;
                    case 4:
                        rbOther3.setChecked(true);
                        break;
                    case 5:
                        rbOther4.setChecked(true);
                        break;
                    case 6:
                        rbOther5.setChecked(true);
                        break;
                    case 7:
                        rbOther6.setChecked(true);
                        break;
                }
                rbNormal.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mService.setRecordMode(0);
                        }
                    }
                });
                rbProcess.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mService.setRecordMode(1);
                        }
                    }
                });
                rbOther1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mService.setRecordMode(2);
                        }
                    }
                });
                rbOther2.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mService.setRecordMode(3);
                        }
                    }
                });
                rbOther3.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mService.setRecordMode(4);
                        }
                    }
                });
                rbOther4.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mService.setRecordMode(5);
                        }
                    }
                });
                rbOther5.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mService.setRecordMode(6);
                        }
                    }
                });
                rbOther6.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mService.setRecordMode(7);
                        }
                    }
                });

                //解决蓝牙录音只能3秒的问题
                final ImageView ivFix3s = (ImageView) layout.findViewById(R.id.iv_fix_3s);
                boolean f = (mService != null) && (mService.getFix3s());
                ivFix3s.setImageResource(f ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                final LinearLayout llFix3s = (LinearLayout) layout.findViewById(R.id.ll_fix_3s);
                llFix3s.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean f = (mService != null) && (mService.getFix3s());
                        boolean n = !f;
                        ivFix3s.setImageResource(n ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                        if (mService != null) {
                            mService.setFix3s(n);
                        }
                    }
                });

                builder.setView(layout);

                builder.setPositiveButton(R.string.ok, null);
                builder.show();
            }
        });
//电话叫车设置
        final LinearLayout callSetting = (LinearLayout) view.findViewById(R.id.ll_call_setting);
        callSetting.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callSetting();
            }
        });
        callSetting.setVisibility(View.GONE);

//其他设置
        final LinearLayout otherSetting = (LinearLayout) view.findViewById(R.id.ll_other_setting);
        otherSetting.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
                LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
                View layout = inflater.inflate(R.layout.other_setting, null);
                final ImageView ivFoceScreen = (ImageView) layout.findViewById(R.id.iv_foce_screen);
                boolean fscreen = (mService != null) && (mService.getIsFoceScreenOrientation());
                ivFoceScreen.setImageResource(fscreen ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                final LinearLayout llFoceScreen = (LinearLayout) layout.findViewById(R.id.ll_foce_screen);
                llFoceScreen.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean f = (mService != null) && (mService.getIsFoceScreenOrientation());
                        boolean n = !f;
                        ivFoceScreen.setImageResource(n ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                        if (mService != null) {
                            mService.setIsFoceScreenOrientation(n);
                        }
                    }
                });

                final ImageView ivBtNoClick = (ImageView) layout.findViewById(R.id.iv_foce_bt_no_click);
                boolean btNoClick = (mService != null) && (mService.getBtNoClick());
                ivBtNoClick.setImageResource(btNoClick ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                final LinearLayout llBtNoClick = (LinearLayout) layout.findViewById(R.id.ll_foce_bt_no_click);
                llBtNoClick.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean f = (mService != null) && (mService.getBtNoClick());
                        boolean n = !f;
                        ivBtNoClick.setImageResource(n ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                        if (mService != null) {
                            mService.setBtNoClick(n);
                        }
                    }
                });

                final ImageView ivReLoginMinute = (ImageView) layout.findViewById(R.id.iv_relogin_after_minute);
                boolean reLoginMinute = (mService != null) && (mService.getReLoginMinute());
                ivReLoginMinute.setImageResource(reLoginMinute ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                final LinearLayout llReLoginMinute = (LinearLayout) layout.findViewById(R.id.ll_relogin_after_minute);
                llReLoginMinute.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean f = (mService != null) && (mService.getReLoginMinute());
                        boolean n = !f;
                        ivReLoginMinute.setImageResource(n ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                        if (mService != null) {
                            mService.setReLoginMinute(n);
                        }
                    }
                });

                //强制tcp
                boolean tcp = (mService != null) && (mService.getForceTcp());
                final ImageView ivTcp = layout.findViewById(R.id.iv_force_tcp);
                ivTcp.setImageResource(tcp ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                final LinearLayout llTcp = layout.findViewById(R.id.ll_force_tcp);
                llTcp.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean f = (mService != null) && (mService.getForceTcp());
                        boolean n = !f;
                        ivTcp.setImageResource(n ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                        if (mService != null) {
                            mService.setForceTcp(n);
                        }
                    }
                });

                //固定mtc
                boolean f = (mService != null) && (mService.getFixMtu());
                final ImageView ivFixMtu = layout.findViewById(R.id.iv_fix_mtu);
                ivFixMtu.setImageResource(f ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                final LinearLayout llFixMtu = layout.findViewById(R.id.ll_fix_mtu);
                llFixMtu.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean f = (mService != null) && (mService.getFixMtu());
                        boolean n = !f;
                        ivFixMtu.setImageResource(n ? R.drawable.checkbox_on : R.drawable.checkbox_off);
                        if (mService != null) {
                            mService.setFixMtu(n);
                            if (n) {
                                LibConstants.BLE_MTU= 23;
                            }else{
                                LibConstants.BLE_MTU=LibConstants.START_BLE_MTU;
                            }
                        }
                    }
                });

                int ch = mService.getQuickCh();
                final EditText edQuickCh = (EditText) layout.findViewById(R.id.ed_quick_switch_ch);
                edQuickCh.setText(String.valueOf(ch));

                final Button btQuickCh = (Button) layout.findViewById(R.id.bt_quick_switch_ch);
                btQuickCh.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(edQuickCh.getText().toString().equals("")){
                            edQuickCh.setText(String.valueOf(0));
                        }
                       boolean res =  mService.setQuickCh(Integer.parseInt(edQuickCh.getText().toString()));
                       if(res==false){
                           edQuickCh.setText(String.valueOf(0));
                           mService.showToast("频道ID无效");
                       }else{
                           mService.showToast("保存成功");
                       }
                    }
                });


                builder.setView(layout);
                builder.setPositiveButton(R.string.ok, null);
                builder.show();
            }
        });
        //显示
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    private void notifSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.settings_alert, null);
        builder.setTitle(R.string.notification);
        builder.setView(view);

        //提示音风格
        final LinearLayout llAlertType = (LinearLayout) view.findViewById(R.id.ll_alert_type);
        final TextView tvAlert = (TextView) view.findViewById(R.id.tv_alert_type);
        int alert = mService.getAlertType();
        tvAlert.setText((alert == 0) ? getString(R.string.normal) : "HAM");

        llAlertType.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
                LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
                View layout = inflater.inflate(R.layout.set_alert_type, null);
                LinearLayout llNormal = (LinearLayout) layout.findViewById(R.id.ll_alert_type_normal);
                LinearLayout llHam = (LinearLayout) layout.findViewById(R.id.ll_alert_type_ham);
                final ImageView ivNormal = (ImageView) layout.findViewById(R.id.iv_alert_type_normal);
                final ImageView ivHam = (ImageView) layout.findViewById(R.id.iv_alert_type_ham);

                int type = mService.getAlertType();
                if (type == 0) {
                    ivNormal.setVisibility(View.VISIBLE);
                    ivHam.setVisibility(View.INVISIBLE);
                } else {
                    ivNormal.setVisibility(View.INVISIBLE);
                    ivHam.setVisibility(View.VISIBLE);
                }

                llNormal.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mService.setAlertType(0);
                        ivNormal.setVisibility(View.VISIBLE);
                        ivHam.setVisibility(View.INVISIBLE);
                        tvAlert.setText(R.string.normal);
                    }
                });
                llHam.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mService.setAlertType(1);
                        ivNormal.setVisibility(View.INVISIBLE);
                        ivHam.setVisibility(View.VISIBLE);
                        tvAlert.setText("HAM");
                    }
                });

                builder.setView(layout);

                builder.setPositiveButton(R.string.ok, null);
                builder.show();
            }
        });

        //提示音量
        final LinearLayout llVolume = (LinearLayout) view.findViewById(R.id.ll_alert_volume);
        llVolume.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
                LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
                View layout = inflater.inflate(R.layout.set_alert_volume, null);
                final SeekBar sbOnline = (SeekBar) layout.findViewById(R.id.sb_volume_online);
                final SeekBar sbOffline = (SeekBar) layout.findViewById(R.id.sb_volume_offline);
                final SeekBar sbIBegin = (SeekBar) layout.findViewById(R.id.sb_volume_i_begin);
                final SeekBar sbIEnd = (SeekBar) layout.findViewById(R.id.sb_volume_i_end);
                final SeekBar sbOtherBegin = (SeekBar) layout.findViewById(R.id.sb_volume_other_begin);
                final SeekBar sbOtherEnd = (SeekBar) layout.findViewById(R.id.sb_volume_other_end);

                //把Appsetting里面的方法全部放进service里面，避免上线时声音出现问题

                //未按确定前改变当前的volume
                int volOnline = mService.getVolumeOnline();
                int volOffline = mService.getVolumeOffline();
                int volIBegin = mService.getVolumeTalkroomBegin();
                int volIEnd = mService.getVolumeTalkroomEnd();
                int volOtherBegin = mService.getVolumeOtherBegin();
                int volOtherEnd = mService.getVolumeOtherEnd();

                sbOnline.setProgress(volOnline);
                sbOffline.setProgress(volOffline);
                sbIBegin.setProgress(volIBegin);
                sbIEnd.setProgress(volIEnd);
                sbOtherBegin.setProgress(volOtherBegin);
                sbOtherEnd.setProgress(volOtherEnd);

                builder.setView(layout);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int iOnline = sbOnline.getProgress(); //此处能正确得到当前的上线音量
                        int iOffline = sbOffline.getProgress();
                        int iIBegin = sbIBegin.getProgress();
                        int iIEnd = sbIEnd.getProgress();
                        int iOtherBegin = sbOtherBegin.getProgress();
                        int iOtherEnd = sbOtherEnd.getProgress();
                        //按完确定键后，保存到sharedpreference中
                        if (mService != null) {
                            mService.setVolumeOnline(iOnline);
                            mService.setVolumeOffline(iOffline);
                            mService.setVolumeTalkroomBegin(iIBegin);
                            mService.setVolumeTalkroomEnd(iIEnd);
                            mService.setVolumeOtherBegin(iOtherBegin);
                            mService.setVolumeOtherEnd(iOtherEnd);
                        }
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);

                builder.show();
            }
        });

        //手咪追加提示音
        final ImageView ivHandmicAlarm = (ImageView) view.findViewById(R.id.iv_handmic_alarm);
        boolean alarm = mService.getHandmicAlarm();
        ivHandmicAlarm.setImageResource(alarm ? R.drawable.checkbox_on : R.drawable.checkbox_off);

        final LinearLayout llHandmicAlarm = (LinearLayout) view.findViewById(R.id.ll_handmic_alarm);
        llHandmicAlarm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean oldChecked = mService.getHandmicAlarm();
                boolean now = !oldChecked;
                mService.setHandmicAlarm(now);
                ivHandmicAlarm.setImageResource(now ? R.drawable.checkbox_on : R.drawable.checkbox_off);
            }
        });
        final ImageView ivHandmicAlarmHelp = (ImageView) view.findViewById(R.id.iv_handmic_alarm_help);
        ivHandmicAlarmHelp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ChannelActivity.this)
                        .setMessage(R.string.handmic_alarm_help)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });

        //显示
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    private void callSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.call_settings_alert, null);
        builder.setTitle(R.string.call_setting);
        builder.setView(view);

        //提示音风格
        final LinearLayout llAlertType = (LinearLayout) view.findViewById(R.id.ll_set_sms_content);
        llAlertType.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
                LayoutInflater inflater = LayoutInflater.from(ChannelActivity.this);
                View layout = inflater.inflate(R.layout.call_setting, null);
                LinearLayout layoutSetSms =(LinearLayout)layout.findViewById(R.id.ll_sms_set);
                final EditText etSms =(EditText)layout.findViewById(R.id.et_str_sms_content);
                if(mService.isDriver()) {
                    etSms.setFocusable(true);
                    etSms.setFocusableInTouchMode(true);
                }else{
                    etSms.setFocusable(false);
                    etSms.setFocusableInTouchMode(false);
                }
                etSms.setText(mService.getSms());
                builder.setView(layout);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mService.setSms(etSms.getText().toString());
                    }
                });

                builder.show();
            }
        });

        //关于叫车宝
        final LinearLayout llabout = (LinearLayout) view.findViewById(R.id.ll_call_about);
        llabout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                aboutCallSystem();
            }
        });

        final LinearLayout llreboot = (LinearLayout) view.findViewById(R.id.ll_call_reboot);
        llreboot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ChannelActivity.this)
                        .setTitle("提示")
                        .setMessage("确定重启接单系统？(只有管理员有效)")
                        .setPositiveButton("是",  new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                rebootCallSystem();
                            }})
                        .setNegativeButton("否", null)
                        .show();

            }
        });

        //显示
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    private void about() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.app_about, null);
        TextView tvVersion = (TextView) layout.findViewById(R.id.tv_about_version);
        TextView tvQQ = (TextView) layout.findViewById(R.id.tv_qq_group);
        tvQQ.setMovementMethod(LinkMovementMethod.getInstance());
        tvVersion.setText(AppCommonUtil.getAppVersionName());
        TextView tvUpdate = (TextView) layout.findViewById(R.id.tv_about_update);
        tvUpdate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                AppCommonUtil.checkUpdate(true);
            }
        });

        LinearLayout llShare = (LinearLayout) layout.findViewById(R.id.ll_share);
        llShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                share();
            }
        });

        builder.setTitle(R.string.app_name);
        builder.setView(layout);

        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }



    private void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_content));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void quit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.confirm_quit_app, null);
        builder.setTitle(R.string.quit_app);
        builder.setView(layout);

        final CheckBox cb = (CheckBox) layout.findViewById(R.id.cb_auto_login);
        boolean autoLogin = AppSettings.getInstance(this).getAutoLogin();
        cb.setChecked(autoLogin);
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                // TODO Auto-generated method stub
                cb.setChecked(arg1);
                AppSettings.getInstance(ChannelActivity.this).setAutoLogin(arg1);
            }
        });

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                doQuit();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    private void doQuit() {
        if (mService != null) {
            appWantQuit = true;    //表示app要退出了，在disconnected回调中进行判断
            //首先停止音频，因为当前可能正在讲话
            mService.userPressUp();

            //先断开连接。此时可能已经处于断开状态了，若已断开，则无需再次调用断开，但需要停止重连
            ConnState connState = mService.getConnectionState();
            //加上connecting判断，否则在3g连接、然后连上一个实际不能上网的wifi时，无法退出，因为mService.disconnect超时
            if (connState == CONNECTION_STATE_DISCONNECTED ||
                    connState == CONNECTION_STATE_CONNECTING) {
                //若已断开，无需先调用disconnect，再等待disconnected回调，直接退出即可

            } else {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mService.disconnect();
//                    }
//                }).start();
                //如果新线程断开，有时候退出不成功，所以直接disconnect。
                //但是有新的问题：退出时会toast提示：连接失败，请重试
                mService.disconnect();
            }
        }

        bye();
    }

    private void bye() {
        //记录用户意图停止
        if (mService != null) {
            mService.appWantQuit();
        }

        if (mServiceConnection != null) {
            if (mServiceBind) {
                unbindService(mServiceConnection);
            }
            mServiceConnection = null;
        }

        stopService(mServiceIntent);
        finish();
    }

    public void setupList() {
        channelAdapter = new ChannelListAdapter(this, mService);
        mLVChannel.setAdapter(channelAdapter);
        updateChannelList();
    }

    public void setListAdapter(ChannelListAdapter adapter) {
        if (mLVChannel != null) {
            mLVChannel.setAdapter(adapter);
        }
    }

    public void updateChannelList() {
        if (channelAdapter == null) {
            channelAdapter = new ChannelListAdapter(this, mService);
            mLVChannel.setAdapter(channelAdapter);
        }

        channelAdapter.updateChannelList();
        /**
         * Registers the passed observer and calls the most recent connection state callback immediately.
         * @param observer
         */
        channelAdapter.notifyDataSetChanged();
        //如果没有频道，则显示提示
        if (mService!=null && mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
            if (channelAdapter.isEmpty()) {
                mLLTips.setVisibility(View.VISIBLE);
                mLVChannel.setVisibility(View.INVISIBLE);
            }
            else {
                mLLTips.setVisibility(View.INVISIBLE);
                mLVChannel.setVisibility(View.VISIBLE);
            }
        }

        updateApplyList();
    }


    private void channelOptions(int position) {
        if (mService == null) {
            return;
        }
        if (mService.getConnectionState()==CONNECTION_STATE_DISCONNECTED) {
            return;
        }

        final Channel channel = (Channel) channelAdapter.getGroup(position);
        if (channel == null) {
            return;
        }
        if (channel.isTemporary) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.detail));

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.channel_option, null);
        builder.setView(layout);

        //显示内容
        final TextView tvChanId = (TextView) layout.findViewById(R.id.tv_chan_info_id);
        tvChanId.setText(String.valueOf(channel.id));
        final TextView tvCreator = (TextView) layout.findViewById(R.id.tv_chan_info_creator);
        tvCreator.setText(channel.creatorNick);
        final EditText etChanName = (EditText) layout.findViewById(R.id.et_chan_info_name);
        etChanName.setText(channel.name);
        final EditText etChanPwd = (EditText) layout.findViewById(R.id.et_chan_info_pwd);
        etChanPwd.setText(channel.pwd);
        final CheckBox cbPub = (CheckBox) layout.findViewById(R.id.cb_public_channel);
        final CheckBox cbNeedApply = (CheckBox) layout.findViewById(R.id.cb_need_apply);
        final CheckBox cbAllowOrderBg = (CheckBox) layout.findViewById(R.id.cb_allow_order_bg);
        cbPub.setChecked(channel.searchable);
        cbNeedApply.setChecked(channel.needApply);
        cbAllowOrderBg.setChecked(channel.allowOrderBg);
        //按键响应
        ImageView ivShare = (ImageView) layout.findViewById(R.id.iv_channel_info_share);
        ivShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareChannel(channel);
            }
        });

        LinearLayout llMembers = (LinearLayout) layout.findViewById(R.id.ll_channel_info_members);
        llMembers.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                memberChannel(channel);
            }
        });

        LinearLayout llAuth = (LinearLayout) layout.findViewById(R.id.ll_channel_info_auth);
        final User me = mService.getCurrentUser();
        if (me == null) {
            return;
        }
        if (me.iId==channel.creatorId || mService.isMonitor(me.iId, channel)) {
            llAuth.setVisibility(View.VISIBLE);
        }

        llAuth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                manageMember(channel, me.iId==channel.creatorId, 0);
            }
        });

        ImageView ivDelete = (ImageView) layout.findViewById(R.id.iv_channel_info_delete);
        ivDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断是否创建者
                if (me.iId==channel.creatorId) {
                    //删除
                    deleteChannel(channel.id, channel.name);
                }
                else {
                    quitChannel(channel);
                }
            }
        });

        //只有创建者，才能修改名称、口令、公开私密
        if (me.iId != channel.creatorId) {
            etChanName.setFocusable(false);
            etChanPwd.setFocusable(false);
            cbPub.setEnabled(false);
            cbPub.setClickable(false);
            cbNeedApply.setEnabled(false);
            cbNeedApply.setClickable(false);
            cbAllowOrderBg.setEnabled(false);
            cbAllowOrderBg.setClickable(false);
        }

        //记录名称、口令、公开私密对初始状态
        final String oldName = etChanName.getText().toString();
        final String oldPwd = etChanPwd.getText().toString();
        final boolean oldPub = cbPub.isChecked();
        final boolean oldNeedApply = cbNeedApply.isChecked();
        final boolean oldAllowOrderBg = cbAllowOrderBg.isChecked();

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String newName = etChanName.getText().toString();
                String newPwd = etChanPwd.getText().toString();
                boolean newPub = cbPub.isChecked();
                boolean newNeedApply = cbNeedApply.isChecked();
                boolean newAllowOrderBg = cbAllowOrderBg.isChecked();

                if (! oldName.equals(newName)) {
                    if (!AppCommonUtil.validChannelName(newName)) {
                        AppCommonUtil.showToast(ChannelActivity.this, R.string.channel_name_bad_format);
                    } else {
                        mService.changeChannelName(channel.id, newName);
                    }
                }
                if (! oldPwd.equals(newPwd)) {
                    if (newPwd.length()!=0 && !AppCommonUtil.validChannelPwd(newPwd)) {
                        AppCommonUtil.showToast(ChannelActivity.this, R.string.channel_pwd_bad_format);
                    } else {
                        mService.changeChannelPwd(channel.id, newPwd);
                    }
                }
                if (oldPub != newPub) {
                    mService.setChannelSearchable(channel.id, newPub);
                }
                if (oldNeedApply != newNeedApply) {
                    mService.setChannelNeedApply(channel.id, newNeedApply);
                }
                if (oldAllowOrderBg != newAllowOrderBg) {
                    mService.setChannelAllowOrderBg(channel.id, newAllowOrderBg);
                }
            }
        });

        LinearLayout llCreator = layout.findViewById(R.id.ll_creator);
        LinearLayout llPwd = layout.findViewById(R.id.ll_channel_pwd);
        cbPub.setVisibility(View.GONE);
        cbNeedApply.setVisibility(View.GONE);
        cbAllowOrderBg.setVisibility(View.GONE);
        llCreator.setVisibility(View.GONE);
        llPwd.setVisibility(View.GONE);
        llAuth.setVisibility(View.GONE);
        ivDelete.setVisibility(View.GONE);
        ivShare.setVisibility(View.GONE);
        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    private void memberChannel(Channel c) {
        if (c==null) {
            return;
        }
        Intent i = new Intent(this, MemberChannel.class);
        i.putExtra("ChanId", c.id);
        i.putExtra("ChanName", c.name);
        i.putExtra("ChanMemberCount", c.memberCount);
        startActivity(i);
    }

    private void shareChannel(final Channel c) {
        if (c==null || mService==null) {
            return;
        }
        User me = mService.getCurrentUser();
        boolean isAdmin = (me.iId == c.creatorId);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
        String s = null;
        if (isAdmin) {
            s = getString(R.string.i_create) + c.name + "]";
        } else {
            s = getString(R.string.i_in_totalk) + c.name + "]" + getString(R.string.channel);
        }

        s += "，" + getString(R.string.channelId) + c.id + "，";
        if (c.pwd == null || c.pwd.length() == 0) {
            s += getString(R.string.no_pwd);
        } else {
            s += getString(R.string.kouling) + c.pwd;
        }
        s += "，来聊聊吧！在App Store或应用市场搜索\"滔滔对讲\"下载安装即可";

        intent.putExtra(Intent.EXTRA_TEXT, s);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, "分享"));
    }

    //uid是目标用户id。uid为0时，表示未指定用户
    private void manageMember(final Channel c, final boolean isCreator, final int uid) {
        if (c==null || mService==null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.manage_member_auth));
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.manage_member, null);
        builder.setView(layout);

        final EditText etUid = (EditText) layout.findViewById(R.id.et_ban_member_uid);
        if (uid>1000000 && uid<9999999) {
            etUid.setText(uid + "");
        }
        final RadioGroup rgOpt = (RadioGroup) layout.findViewById(R.id.rg_manage_member);
        final RadioButton rbBan = (RadioButton) layout.findViewById(R.id.rb_ban);
        final RadioButton rbCancelBan = (RadioButton) layout.findViewById(R.id.rb_cancel_ban);
        final RadioButton rbMute = (RadioButton) layout.findViewById(R.id.rb_mute);
        final RadioButton rbCancelMute = (RadioButton) layout.findViewById(R.id.rb_cancel_mute);
        final RadioButton rbMonitor = (RadioButton) layout.findViewById(R.id.rb_monitor);
        final RadioButton rbCancelMonitor = (RadioButton) layout.findViewById(R.id.rb_cancel_monitor);
        final RadioButton rbPrior = (RadioButton) layout.findViewById(R.id.rb_prior);
        final RadioButton rbCancelPrior = (RadioButton) layout.findViewById(R.id.rb_cancel_prior);
        //副管不能设置增删其他副管
        if (! isCreator) {
            rbMonitor.setEnabled(false);
            rbCancelMonitor.setEnabled(false);
            rbPrior.setEnabled(false);
            rbCancelPrior.setEnabled(false);
        }

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                final String strUid = etUid.getText().toString();
                int uid = 0;
                try {
                    uid = Integer.parseInt(strUid);
                } catch (Exception e) {

                }

                final int check = rgOpt.getCheckedRadioButtonId();

                if (!AppCommonUtil.validTotalkId(strUid)) {
                    AppCommonUtil.showToast(ChannelActivity.this, R.string.userid_bad_format);
                }
                else if (check == rbBan.getId()) {
                    mService.manageMember(c.id, uid, 0);
                }
                else if (check == rbCancelBan.getId()) {
                    mService.manageMember(c.id, uid, 1);
                }
                else if (check == rbMute.getId()) {
                    mService.manageMember(c.id, uid, 2);
                }
                else if (check == rbCancelMute.getId()) {
                    mService.manageMember(c.id, uid, 3);
                }
                else if (check == rbMonitor.getId()) {
                    mService.manageMember(c.id, uid, 4);
                }
                else if (check == rbCancelMonitor.getId()) {
                    mService.manageMember(c.id, uid, 5);
                }
                else if (check == rbPrior.getId()) {
                    mService.manageMember(c.id, uid, 6);
                }
                else if (check == rbCancelPrior.getId()) {
                    mService.manageMember(c.id, uid, 7);
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    private void deleteChannel(final int chanId, final String chanName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.channel) + chanName + getString(R.string.confirm_delete_chan));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mService.deleteChannel(chanId);
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }


    private void quitChannel(final Channel c) {
        if (c==null || mService==null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.confirm_quit_chan));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                mService.quitChannel(c.id);
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void updateUser(User user) {
        if (user == null) {
            return;
        }

        if (channelAdapter == null) {
            return;
        }

        channelAdapter.refreshUser(user);
    }

    /**
     * Scrolls to the passed channel.
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    private void scrollToChannel(Channel channel) {
        if (channelAdapter==null || channelAdapter.channels==null) {
            return;
        }

        int channelPosition = channelAdapter.channels.indexOf(channel);
        int flatPosition = channelAdapter.getVisibleFlatGroupPosition(channelPosition);
        Log.i(AppConstants.LOG_TAG, "SCROLLING TO: " + flatPosition);
        mLVChannel.smoothScrollToPosition(flatPosition);
    }

    @Override
    public void onNestedChildClick(AdapterView<?> parent, View view,
                                   int groupPosition, int childPosition, long id) {
        final AlertDialog builder = new AlertDialog.Builder(this).create();
        builder.setCanceledOnTouchOutside(true);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.member_detail, null);

        final User user = (User) channelAdapter.getChild(groupPosition, childPosition);
        if (user == null) {
            return;
        }
        builder.setTitle(user.nick);
        builder.setView(layout);

        final ImageView ivAvatar = (ImageView) layout.findViewById(R.id.iv_user_detail_avatar);
        final TextView tvId = (TextView) layout.findViewById(R.id.tv_user_detail_id);
        final ImageView ivManage = (ImageView) layout.findViewById(R.id.iv_user_detail_manage);
        final ImageView ivAdd = (ImageView) layout.findViewById(R.id.iv_user_detail_add_contact);
        ivAvatar.setImageResource(R.drawable.ic_default_avatar);

        tvId.setText(String.valueOf(user.iId));

        ivManage.setVisibility(View.INVISIBLE);
        if (mService!=null && mService.getCurrentUser()!=null && user.getChannel()!=null) {
            final User me = mService.getCurrentUser();
            final Channel chan = user.getChannel();
            if (me.iId==chan.creatorId || mService.isMonitor(me.iId, chan)) {
                ivManage.setVisibility(View.VISIBLE);
                ivManage.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        manageMember(chan, (me.iId==chan.creatorId), user.iId);
                    }
                });
            }
        }

        ivAdd.setVisibility(View.INVISIBLE);
        if (mService!=null && mService.getContacts()!=null && mService.getCurrentUser()!=null) {
            if (! mService.getContacts().containsKey(user.iId) && user.iId != mService.getCurrentUser().iId) {
                //不是好友，且非本人
                ivAdd.setVisibility(View.VISIBLE);
                ivAdd.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(ChannelActivity.this)
                                .setTitle(getString(R.string.notif))
                                .setMessage(getString(R.string.add) + user.nick + getString(R.string.as_contact))
                                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mService.applyContact(true, user.iId);
                                        AppCommonUtil.showToast(ChannelActivity.this, getString(R.string.apply_sent));
                                    }
                                })
                                .setNegativeButton(getString(R.string.cancel), null)
                                .show();
                    }
                });
            }
        }

        builder.show();
    }

    @Override
    public void onNestedChildLongClick(AdapterView<?> parent, View view, int groupPosition, int childPosition, long id) {

    }

    @Override
    public void onNestedGroupClick(AdapterView<?> parent, View view, int groupPosition, long id) {
        if (mService.getConnectionState() != CONNECTION_STATE_CONNECTED) {
            return;
        }

        final Channel channel = (Channel) channelAdapter.getGroup(groupPosition);
        final Channel curr = mService.getCurrentChannel();
        if (curr.id == channel.id) {
            channelAdapter.groupClicked(groupPosition);
        } else {
            mService.enterChannel(channel.id);
        }
    }

    @Override
    public void onNestedGroupLongClick(AdapterView<?> parent, View view, int groupPosition, long id) {

    }

    class ChannelListAdapter extends InterpttNestedAdapter {
        private final InterpttService service;
        private ArrayList<Channel> channels = new ArrayList<Channel>();
        @SuppressLint("UseSparseArrays") // Don't like 'em
        private Map<Integer, List<User>> channelMap = new ConcurrentHashMap<Integer, List<User>>();

        public ChannelListAdapter(final Context context, final InterpttService service) {
            super(context);
            this.service = service;
        }

        /**
         * Fetches a new list of channels from the service.
         */
        public void updateChannelList() {
            if (service != null) {
                this.channels = service.getChannelList();
                this.channelMap = service.getSortedChannelMap();
            }
        }

        public void groupClicked(int pos) {
            Channel c = channels.get(pos);

            if (channelAdapter.isGroupExpanded(pos)) {
                channelAdapter.collapseGroup(pos);
                c.expanded = 0;
            }
            else {
                expandGroup(pos);
                c.expanded = 1;
            }

            notifyVisibleSetChanged();
        }

        public void refreshUser(User user) {
            if (!service.getUserList().contains(user))
                return;

            if (channels == null) {
                return;
            }

            int channelPosition = channels.indexOf(user.getChannel());
            if (!channelAdapter.isGroupExpanded(channelPosition))
                return; // Don't refresh

            if (channelMap==null || channelMap.get(user.getChannel().id)==null) {
                return;
            }

            int userPosition = channelMap.get(user.getChannel().id).indexOf(user);
            int position = channelAdapter.getVisibleFlatChildPosition(channelPosition, userPosition);

            View userView = mLVChannel.getChildAt(position - mLVChannel.getFirstVisiblePosition());

            //经测，这些判断不能省略，否则崩溃
            if (userView != null && userView.getTag() != null && userView.getTag().equals(user)) {
                refreshElements(userView, user);
            }
        }

        private void refreshElements(final View view, final User user) {
            final ImageView ivPerm = (ImageView) view.findViewById(R.id.iv_perm);
            final CircleImageView avatar = (CircleImageView) view.findViewById(R.id.userRowAvatar);
            final TextView tvNick = (TextView) view.findViewById(R.id.userRowNick);
            final ImageView ivPrior = (ImageView) view.findViewById(R.id.iv_prior_mic);	//插话权限
            final ImageView ivMute = (ImageView) view.findViewById(R.id.iv_mute);
            final ImageView ivAudioSource = (ImageView) view.findViewById(R.id.iv_audio_source);
            final TextView tvId = (TextView) view.findViewById(R.id.userRowId);

            if (user == null) {
                return;
            }

            if (user.iId == user.getChannel().creatorId) {
                ivPerm.setImageResource(R.drawable.owner);
                ivPerm.setVisibility(View.VISIBLE);
            }
            else if (mService!=null && mService.isMonitor(user.iId, user.getChannel())) {
                ivPerm.setImageResource(R.drawable.monitor);
                ivPerm.setVisibility(View.VISIBLE);
            }
            else {
                ivPerm.setVisibility(View.INVISIBLE);
            }

            //头像
            if (true) {//(user.avatar == null) {
                avatar.setImageResource(R.drawable.ic_default_avatar);
            } else {
                Bitmap bm = BitmapFactory.decodeByteArray(user.avatar, 0, user.avatar.length);
                avatar.setImageBitmap(bm);
            }

            //talking状态
            //如果跟自己在同一频道，根据localTalking状态判断；如果在其他频道，根据服务器消息判断
            if (user.getChannel() != null && mService.getCurrentChannel() != null) {
                final ImageView circle = (ImageView) view.findViewById(R.id.iv_avatar_circle);
                if (user.getChannel().id == mService.getCurrentChannel().id) {
                    //同一频道
                    if (user.isLocalTalking) {
                        circle.setVisibility(View.VISIBLE);
                        //                      circle.startAnimation(AppCommonUtil.createTalkingAnimation());
                    } else {
                        //先停止动画再set gone，尝试解决停止讲话后红圈仍闪烁的问题。测试无效
//                        circle.clearAnimation();
                        circle.setVisibility(View.GONE);
                    }
                } else {
                    if (user.isTalking) {
                        circle.setVisibility(View.VISIBLE);
                        //circle.startAnimation(AppCommonUtil.createTalkingAnimation());
                    } else {
                        //先停止动画再set gone，尝试解决停止讲话后红圈仍闪烁的问题。测试无效
//                        circle.clearAnimation();
                        circle.setVisibility(View.GONE);
                    }
                }
            }

            //zcx change
            tvNick.setText(user.nick);
            if (mService.getCurrentUser() != null && user.session == mService.getCurrentUser().session) {
                tvNick.setTextColor(AppConstants.CURRENT_NICK_COLOR);
            } else {
                tvNick.setTextColor(AppConstants.OTHER_NICK_COLOR);
            }

            //audio source
            ivAudioSource.setImageLevel(user.audioSource);

            //是否有插话权限
            if (mService.isPrior(user.iId, user.getChannel())) {
                ivPrior.setVisibility(View.VISIBLE);
            }
            else {
                ivPrior.setVisibility(View.INVISIBLE);
            }
            //是否禁言
            if (mService.isMute(user.iId, user.getChannel())) {
                ivMute.setVisibility(View.VISIBLE);
            }
            else {
                ivMute.setVisibility(View.INVISIBLE);
            }

            tvId.setText(String.valueOf(user.iId));
        }

        public int getNestedLevel(Channel channel) {
            return 0;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            Channel channel = null;
            try {
                channel = channels.get(groupPosition);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                return null;
            }

            List<User> channelUsers = channelMap.get(channel.id);
            if (channelUsers == null) {
                return null;
            }

            Object obj = null;
            try {
                obj = channelUsers.get(childPosition);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }

            return obj;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, int depth, View v, ViewGroup arg4) {
            if (v == null) {
                final LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.listitem_user, null);
            }

            User user = (User) getChild(groupPosition, childPosition);

            refreshElements(v, user);
            v.setTag(user);

            return v;
        }

        @Override
        public int getChildCount(int arg0) {
            if (channels == null || channelMap==null) {
                return 0;
            }
            int i = channels.get(arg0).id;
            List<User> l = channelMap.get(i);
            //#0015实现时，删除频道事，如果频道里有人在线，则会出现l为null的情况
            if (l == null)
                return 0;
            return l.size();
        }

        @Override
        public Object getGroup(int arg0) {
            return channels.get(arg0);
        }

        @Override
        public int getGroupCount() {
            return channels.size();
        }


        //wocao
        @Override
        public View getGroupView(final int groupPosition, int depth, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.listitem_channel, null);
            }

            final Channel channel = channels.get(groupPosition);
            if (mService.getCurrentChannel() == null) {
                return v;
            }


            ImageView expandView = (ImageView) v.findViewById(R.id.channel_row_expand);
            expandView.setImageResource(isGroupExpanded(groupPosition) ? R.drawable.ic_action_minus : R.drawable.ic_action_add);
            expandView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isGroupExpanded(groupPosition)) {
                        collapseGroup(groupPosition);
                        channel.expanded = 0;
                    }
                    else {
                        expandGroup(groupPosition);
                        channel.expanded = 1;
                    }

                    notifyVisibleSetChanged();
                }
            });

            //wocao add
            ImageView add = v.findViewById(R.id.iv_channel_add);
            add.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //new dialog get number message;
                    //add post
                }
            });


            TextView nameView = (TextView) v.findViewById(R.id.channel_row_name);
            TextView countView = (TextView) v.findViewById(R.id.channel_row_count);
            TextView tvTalker = (TextView) v.findViewById(R.id.tv_chan_talker);
            TextView tvChanNumber = (TextView) v.findViewById(R.id.tv_channel_number);

            ImageView ivLoc = (ImageView) v.findViewById(R.id.iv_channel_location);
            if (mService.getCurrentChannel().id == channel.id) {
                ivLoc.setVisibility(View.VISIBLE);
            }
            else {
                ivLoc.setVisibility(View.GONE);
            }
            ivLoc.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(ChannelActivity.this)
                            .setTitle(R.string.notif)
                            .setMessage(R.string.map_notice)
                            .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent i = new Intent(ChannelActivity.this, MapActivity.class);
                                    i.putExtra("ChanId", channel.id);
                                    startActivity(i);
                                }
                            })
                            .setNegativeButton(R.string.decline, null)
                            .show();
                }
            });
            ImageView ivListen = (ImageView) v.findViewById(R.id.iv_channel_listen);
            ivListen.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean oldListen = mService.isListen(mService.getCurrentUser(), channel.id);
                    mService.setListen(channel.id, ! oldListen);
                }
            });

            ImageView ivOption = (ImageView) v.findViewById(R.id.iv_channel_option);
            ivOption.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    channelOptions(groupPosition);
                }
            });

            nameView.setText(channel.name);

            //是否监听
            if (mService.isListen(mService.getCurrentUser(), channel.id)) {
                ivListen.setImageResource(R.drawable.ic_listen);
            }
            else {
                ivListen.setImageResource(R.drawable.ic_listen_gray);
            }

            if (channel.isTemporary) {
                ivOption.setVisibility(View.INVISIBLE);
            }
            else {
                ivOption.setVisibility(View.VISIBLE);
            }

            countView.setText("(" + channel.userCount + "/" + channel.memberCount + ")");

            User talker = mService.whoIsTalking(channel);
            if (talker != null) {
                tvTalker.setVisibility(View.VISIBLE);
                tvTalker.setText(talker.nick);
            } else {
                tvTalker.setText("");
                tvTalker.setVisibility(View.GONE);
            }

            if (channel.id == mService.getCurrentChannel().id) {
                v.setBackgroundResource(R.drawable.selector_current_channel);
            } else {
                v.setBackgroundResource(R.drawable.selector_other_channel);
            }

            tvChanNumber.setText(getString(R.string.chanid, channel.id));

            return v;
        }

        @Override
        public int getGroupDepth(int groupPosition) {
            Channel channel = (Channel) getGroup(groupPosition);
            return getNestedLevel(channel);
        }

        @Override
        public int getParentId(int groupPosition) {
            return -1;
        }

        @Override
        public int getGroupId(int groupPosition) {
            Channel channel = channels.get(groupPosition);
            return channel.id;
        }

        @Override
        public int getChildId(int groupPosition, int childPosition) {
            return channelMap.get(channels.get(groupPosition)).get(childPosition).iId;
        }

        @Override
        public boolean isGroupExpandedByDefault(int groupPosition) {
            if (mService == null) {
                return false;
            }
            if (mService.getCurrentChannel() == null) {
                return false;
            }
            Channel c = channels.get(groupPosition);
            if (c.expanded == -1) {
                return (c.id == mService.getCurrentChannel().id);
            }
            else {
                return (c.expanded == 1);
            }
        }
    }

    public void handleCurrentChannelChanged() {
        updateChannelList();
        Channel curChan = mService.getCurrentChannel();
        scrollToChannel(curChan);
    }

    public void handleChannelAdded(Channel channel) {
        if (mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
            updateChannelList();
        }
    }

    public void handleChannelRemoved(Channel channel) {
        if (mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
            updateChannelList();
        }
    }

    public void handleChannelUpdated(Channel channel) {
        if (mService.getConnectionState()==CONNECTION_STATE_CONNECTED) {
            updateChannelList();
        }
    }

    //////////////  channel apply to join member
    class ApplyListAdapter extends BaseAdapter {
        private final InterpttService service;
        private ArrayList<PendingMember> mPms;
        private LayoutInflater mInflator;

        public ApplyListAdapter(final InterpttService service) {
            super();

            this.service = service;
            mPms = new ArrayList<PendingMember>();
            mInflator = getLayoutInflater();
        }

        public void updateData() {
            mPms.clear();
            if (service == null) {
                return;
            }

            //pending contact
            Map<String, PendingMember> pms = service.getPendingMembers();
            for (PendingMember pm:pms.values()) {
                mPms.add(pm);
            }
        }

        public PendingMember getItem(int position) {
            return mPms.get(position);
        }

        public void clear() {
            mPms.clear();
        }

        @Override
        public int getCount() {
            return mPms.size();
        }

        public void setItem(int i, PendingMember pm) {
            mPms.set(i, pm);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            InviteViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_apply_member, null);
                viewHolder = new InviteViewHolder();
                viewHolder.tvApply = (TextView) view.findViewById(R.id.tv_member_apply);
                view.setTag(viewHolder);
            }
            else {
                viewHolder = (InviteViewHolder) view.getTag();
            }

            return view;
        }
    }

    static class InviteViewHolder {
        TextView tvApply;
    }

    private AdapterView.OnItemClickListener mApplyItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            final PendingMember pm = applyAdapter.getItem(position);
            if (pm == null) {
                return;
            }
            if (mService==null || mService.getCurrentUser()==null) {
                return;
            }

            String content = String.valueOf(pm.iId) + " " + pm.nick;
            String chanName = "";
            ArrayList<Channel> channels = mService.getChannelList();
            if (channels != null && channels.size() > 0) {
                for (Channel c : channels) {
                    if (c.id == pm.targetChanId) {
                        chanName = c.name;
                        break;
                    }
                }
            }

            content += "\n" + "申请加入 " + chanName;
            content += "\n" + "附言：\n";
            content += pm.comment;

            new AlertDialog.Builder(ChannelActivity.this)
                    .setTitle(R.string.notif)
                    .setMessage(content)
                    .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mService.acceptApply(pm.iId, pm.targetChanId, true);
                        }
                    })
                    .setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mService.acceptApply(pm.iId, pm.targetChanId, false);
                        }
                    })
                    .show();
        }
    };

    public void updateApplyList() {
        if (applyAdapter == null) {
            applyAdapter = new ApplyListAdapter(mService);
            mLVPendingMember.setAdapter(applyAdapter);
        }

        applyAdapter.updateData();
        applyAdapter.notifyDataSetChanged();
    }
}
