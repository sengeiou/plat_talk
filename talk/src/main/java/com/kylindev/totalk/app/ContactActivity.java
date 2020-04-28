package com.kylindev.totalk.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.model.Contact;
import com.kylindev.pttlib.service.model.User;
import com.kylindev.totalk.AppConstants;
import com.kylindev.totalk.R;
import com.kylindev.totalk.utils.AppCommonUtil;
import com.kylindev.totalk.view.CircleImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.kylindev.pttlib.service.InterpttService.ConnState;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTED;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTING;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_DISCONNECTED;

public class ContactActivity extends BaseActivity implements OnClickListener {
    private Handler mHandler = new Handler();    //用于其他线程更新UI
    //联系人
    private ContactListAdapter contactAdapter;
    private ListView mLVContact;
    private AlertDialog mDetailDialog = null;

    private CircleImageView mCIVAvatarOfSearchedUser;
    private TextView mTVNickOfSearchedUser;
    private TextView mTVIdOfSearchedUser;
    private Button mBtnAddContact;
    private User searchedUser;

    private TextView mTVTmpTalk;
    private TextView mTVCancelTmp;

    @Override
    protected void serviceConnected() {
        mService.registerObserver(serviceObserver); //baseactivity里注册observer，这里也要注册

        //此时有可能是服务器断开状态，且用户点击app图标执行到这里的。因此，应先检查service的状态是否是CONNECTION_STATE_CONNECTED
        //是的话，才能执行setupChannelList等操作。否则，无需setup，只需等待自动重连后，自动setup
        if (mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
            setupList();
        }

        refreshTmpTalk();
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_contact;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIVBarLeft.setImageResource(R.drawable.ic_leave);
        mIVBarLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mIVBarLeftInner.setVisibility(View.INVISIBLE);
        mIVBarRightInner.setVisibility(View.INVISIBLE);

        mIVBarRight.setImageResource(R.drawable.ic_add);
        mIVBarRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addContact();
            }
        });

        mTVBarTitle.setText(R.string.contact);
        mLLlcd.setVisibility(View.GONE);
        mLLPttArea.setVisibility(View.GONE);

        mLVContact = (ListView) findViewById(R.id.lv_contacts);
        mLVContact.setOnItemClickListener(mContactOnItemClickListener);

        mTVTmpTalk = (TextView) findViewById(R.id.tv_tmp_talk);
        mTVTmpTalk.setOnClickListener(this);
        mTVCancelTmp = (TextView) findViewById(R.id.tv_cancel_tmp);
        mTVCancelTmp.setOnClickListener(this);
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
            boolean selecting = mService != null && mService.isSelectingContact();
            if (selecting) {
                mService.cancelSelect();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    class ContactListAdapter extends BaseAdapter {
        private final InterpttService service;
        private ArrayList<Contact> mContacts;
        private LayoutInflater mInflator;

        public ContactListAdapter(final InterpttService service) {
            super();

            this.service = service;
            mContacts = new ArrayList<Contact>();
            mInflator = getLayoutInflater();
        }

        public void updateData() {
            mContacts.clear();
            if (service == null) {
                return;
            }

            Contact c;
            //最上面显示自己
            User me = mService.getCurrentUser();
            if (me != null) {
                c = new Contact(false, true, me.iId, me.nick, me.avatar, false, me.audioSource);
                mContacts.add(c);
            }

            //pending contact  待定联系人
            Map<Integer, Contact> pendingContactMap = service.getPendingContacts();
            if (pendingContactMap != null) {
                for (Contact value : pendingContactMap.values()) {
                    c = new Contact(true, true, value.iId, value.nick, value.avatar, false, value.audioSource);
                    mContacts.add(c);

                }
            }

            Map<Integer, Contact> contactMap = service.getContacts();
            if (contactMap != null) {
                for (Contact value : contactMap.values()) {
                    if (value.online) {
                        c = new Contact(false, value.online, value.iId, value.nick, value.avatar, value.selected, value.audioSource);
                        mContacts.add(c);
                    }
                }
                //先显示在线的，再显示离线的
                for (Contact value : contactMap.values()) {
                    if (!value.online) {
                        c = new Contact(false, value.online, value.iId, value.nick, value.avatar, value.selected, 0);
                        mContacts.add(c);
                    }
                }
            }
        }

        public Contact getContact(int position) {
            return mContacts.get(position);
        }

        public void clear() {
            mContacts.clear();
        }

        @Override
        public int getCount() {
            return mContacts.size();
        }

        @Override
        public Object getItem(int i) {
            return mContacts.get(i);
        }

        public void setItem(int i, Contact c) {
            mContacts.set(i, c);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ContactViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_contact, null);
                viewHolder = new ContactViewHolder();
                viewHolder.ivAvatar = (CircleImageView) view.findViewById(R.id.civ_contact_avatar);
                viewHolder.tvNick = (TextView) view.findViewById(R.id.tv_contact_nick);
                viewHolder.tvApply = (TextView) view.findViewById(R.id.tv_contact_apply);
                viewHolder.ivAudioSource = (ImageView) view.findViewById(R.id.iv_contact_audio_source);
                viewHolder.tvId = (TextView) view.findViewById(R.id.tv_contact_id);
                viewHolder.ivSelected = (ImageView) view.findViewById(R.id.iv_contact_selected);

                view.setTag(viewHolder);
            } else {
                viewHolder = (ContactViewHolder) view.getTag();
            }

            Contact con = mContacts.get(i);

            //是否待同意
            boolean pending = con.pending;
            if (pending) {
                viewHolder.tvApply.setVisibility(View.VISIBLE);
                view.setBackgroundColor(ContextCompat.getColor(ContactActivity.this, R.color.holo_orange_light));
            } else {
                viewHolder.tvApply.setVisibility(View.GONE);
                view.setBackgroundColor(ContextCompat.getColor(ContactActivity.this, R.color.white));
            }

            //头像
            viewHolder.ivAvatar.setImageResource(con.online ? R.drawable.ic_default_avatar : R.drawable.ic_default_avatar_gray);

            viewHolder.tvNick.setText(con.nick);
            if (!con.online) {
                viewHolder.tvNick.setTextColor(ContextCompat.getColor(ContactActivity.this, R.color.gray_b0));
            } else {
                if (mService.getCurrentUser() != null && con.iId == mService.getCurrentUser().iId) {
                    //自己高亮显示
                    viewHolder.tvNick.setTextColor(AppConstants.CURRENT_NICK_COLOR);
                } else {
                    viewHolder.tvNick.setTextColor(AppConstants.OTHER_NICK_COLOR);
                }
            }

            //audio source
            viewHolder.ivAudioSource.setImageLevel(con.audioSource);
            //id
            viewHolder.tvId.setText(String.valueOf(con.iId));

            //选择指示
            viewHolder.ivSelected.setVisibility(con.selected ? View.VISIBLE : View.INVISIBLE);

            return view;
        }
    }

    static class ContactViewHolder {
        CircleImageView ivAvatar;
        TextView tvNick;
        TextView tvApply;
        ImageView ivAudioSource;
        TextView tvId;
        ImageView ivSelected;
    }

    //联系人相关
    private AdapterView.OnItemClickListener mContactOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            final Contact con = contactAdapter.getContact(position);
            if (con == null) {
                return;
            }
            if (mService == null || mService.getCurrentUser() == null) {
                return;
            }

            boolean isSelecting = mService.isSelectingContact();
            if (isSelecting) {
                if (!con.pending && con.iId != mService.getCurrentUser().iId && con.online) {
                    mService.selectContact(con, !con.selected);
                }
            } else {
                if (con.pending) {
                    new AlertDialog.Builder(ContactActivity.this)
                            .setTitle(R.string.notif)
                            .setMessage(con.nick + getString(R.string.add_you_as_contact))
                            .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mService.applyContact(true, con.iId);
                                    mService.deletePendingContact(con.iId);
                                }
                            })
                            .setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mService.applyContact(false, con.iId);
                                    mService.deletePendingContact(con.iId);
                                }
                            })
                            .show();
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(ContactActivity.this);

                    LayoutInflater inflater = LayoutInflater.from(ContactActivity.this);
                    View layout = inflater.inflate(R.layout.contact_detail, null);

                    builder.setTitle(con.nick);
                    builder.setView(layout);

                    final ImageView ivAvatar = (ImageView) layout.findViewById(R.id.iv_contact_detail_avatar);
                    final TextView tvId = (TextView) layout.findViewById(R.id.tv_contact_detail_id);
                    final TextView tvOnline = (TextView) layout.findViewById(R.id.tv_contact_detail_online);
                    final ImageView ivDelete = (ImageView) layout.findViewById(R.id.iv_contact_detail_delete_contact);
                    final Button btnSelect = (Button) layout.findViewById(R.id.btn_contact_detail_select);
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar);

                    tvId.setText(String.valueOf(con.iId));
                    if (con.online) {
                        tvOnline.setText(getString(R.string.connected));
                    } else {
                        tvOnline.setText(getString(R.string.disconnected));
                        tvOnline.setTextColor(ContextCompat.getColor(ContactActivity.this, R.color.gray_b0));
                    }
                    //删除标识
                    if (con.iId == mService.getCurrentUser().iId) {
                        ivDelete.setVisibility(View.INVISIBLE);
                    }

                    ivDelete.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(ContactActivity.this)
                                    .setTitle(getString(R.string.notif))
                                    .setMessage(getString(R.string.confirm_delete_contact))
                                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mService.applyContact(false, con.iId);
                                            mDetailDialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.cancel), null)
                                    .show();
                        }
                    });

                    if (mService != null && mService.getCurrentUser() != null && !con.pending && con.iId != mService.getCurrentUser().iId && con.online) {
                        btnSelect.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //是否可选呼
                                boolean isSelecting = mService.isSelectingContact();
                                if (!isSelecting) {
                                    if (mService != null && mService.getCurrentUser() != null && !con.pending && con.iId != mService.getCurrentUser().iId && con.online) {
                                        mService.selectContact(con, true);
                                        mDetailDialog.dismiss();
                                    }
                                }
                            }
                        });
                    } else {
                        btnSelect.setEnabled(false);
                    }

                    mDetailDialog = builder.show();
                }
            }
        }
    };

    public void setupList() {
        contactAdapter = new ContactListAdapter(mService);
        mLVContact.setAdapter(contactAdapter);
        updateContactList();
    }

    public void setListAdapter(ListAdapter adapter) {
        if (mLVContact != null) {
            mLVContact.setAdapter(adapter);
        }
    }

    public void updateContactList() {
        if (contactAdapter == null) {
            contactAdapter = new ContactListAdapter(mService);
            mLVContact.setAdapter(contactAdapter);
        }

        contactAdapter.updateData();

        contactAdapter.notifyDataSetChanged();
    }

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
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setListAdapter(null);
                        }
                    });
                    break;
            }
        }

        @Override
        public void onApplyContactReceived(boolean add, Contact contact) throws RemoteException {
            updateContactList();
        }

        @Override
        public void onPendingContactChanged() throws RemoteException {
            updateContactList();
        }

        @Override
        public void onContactChanged() throws RemoteException {
            updateContactList();
            refreshTmpTalk();
        }

        @Override
        public void onSynced() throws RemoteException {
            updateContactList();
        }

        @Override
        public void onUserSearched(User user) throws RemoteException {
            searchedUser = user;

            if (user.avatar != null) {
                Bitmap bm = BitmapFactory.decodeByteArray(user.avatar, 0, user.avatar.length);
                mCIVAvatarOfSearchedUser.setImageBitmap(bm);
            }
            mCIVAvatarOfSearchedUser.setVisibility(View.VISIBLE);
            mTVNickOfSearchedUser.setText(user.nick);
            mTVNickOfSearchedUser.setVisibility(View.VISIBLE);
            mTVIdOfSearchedUser.setText(String.valueOf(user.iId));
            mTVIdOfSearchedUser.setVisibility(View.VISIBLE);

            mBtnAddContact.setVisibility(View.VISIBLE);
        }
    };


    private void addContact() {
        if (mService.getConnectionState() == CONNECTION_STATE_DISCONNECTED) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.add_contact);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.add_contact, null);
        builder.setView(layout);

        mCIVAvatarOfSearchedUser = (CircleImageView) layout.findViewById(R.id.civ_searched_user_avatar);
        mTVNickOfSearchedUser = (TextView) layout.findViewById(R.id.tv_searched_user_nick);
        mTVIdOfSearchedUser = (TextView) layout.findViewById(R.id.tv_searched_user_id);
        mBtnAddContact = (Button) layout.findViewById(R.id.btn_add_contact);
        mBtnAddContact.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchedUser != null) {
                    if (searchedUser.iId == mService.getCurrentUser().iId) {
                        AppCommonUtil.showToast(ContactActivity.this, getString(R.string.cant_add_yourself));
                    } else {
                        mService.applyContact(true, searchedUser.iId);
                        AppCommonUtil.showToast(ContactActivity.this, "已发送申请");
                    }
                }
            }
        });

        final EditText etUser = layout.findViewById(R.id.et_search_contact);
        etUser.setHint(R.string.ent_username_hint);
        final ImageButton ibSearch = layout.findViewById(R.id.ib_search_contact);
        ibSearch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String str = etUser.getText().toString();
                if (AppCommonUtil.validUserId(str)) {
                    mCIVAvatarOfSearchedUser.setVisibility(View.INVISIBLE);
                    mTVNickOfSearchedUser.setVisibility(View.INVISIBLE);
                    mTVIdOfSearchedUser.setVisibility(View.INVISIBLE);
                    mBtnAddContact.setVisibility(View.INVISIBLE);
                    searchedUser = null;

                    mService.searchUser(str);
                } else {
                    AppCommonUtil.showToast(ContactActivity.this, R.string.userid_bad_format);
                }
            }
        });

        builder.setNegativeButton(R.string.close, null);

        builder.show();
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        int id = v.getId();

        if (id == R.id.tv_tmp_talk) {
            if (mService != null && mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
                String members = mService.getSelects();
                if (members.length() > 0) {
                    //lenth>0说明选中了至少一个人。先加上自己
                    members += ",";
                    members += mService.getCurrentUser().iId;

                    mService.createChannel("", "", members, false, false, true);
                    mService.cancelSelect();
                    finish();
                }
            }

            mService.cancelSelect();
        } else if (id == R.id.tv_cancel_tmp) {
            mService.cancelSelect();
        }

        super.onClick(v);
    }

    //遍历contact，确定是否需要显示临时呼叫按钮
    private void refreshTmpTalk() {
        boolean hasSelected = false;
        if (mService == null) {
            return;
        }
        Map<Integer, Contact> m = mService.getContacts();
        if (m == null) {
            return;
        }
        for (Contact c : m.values()) {
            if (c.selected) {
                hasSelected = true;
            }

            mTVTmpTalk.setEnabled(hasSelected);
            mTVCancelTmp.setEnabled(hasSelected);
        }
    }

}