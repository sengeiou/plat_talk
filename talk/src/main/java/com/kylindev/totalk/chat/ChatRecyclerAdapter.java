package com.kylindev.totalk.chat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.kylindev.pttlib.db.ChatMessageBean;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.model.Channel;
import com.kylindev.pttlib.service.model.User;
import com.kylindev.totalk.R;
import com.kylindev.totalk.utils.AppCommonUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Mao Jiqing on 2016/9/29.
 */
public class ChatRecyclerAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private InterpttService mService;
    private List<ChatMessageBean> beanList = new ArrayList<ChatMessageBean>();
    public static final int FROM_USER_MSG = 0;//接收消息类型
    public static final int TO_USER_MSG = 1;//发送消息类型
    public static final int FROM_USER_IMG = 2;//接收消息类型
    public static final int TO_USER_IMG = 3;//发送消息类型
    public static final int FROM_USER_VOICE = 4;//接收消息类型
    public static final int TO_USER_VOICE = 5;//发送消息类型
    public static final int FROM_USER_VIDEO = 6;//接收消息类型
    public static final int TO_USER_VIDEO = 7;//发送消息类型
    public static final int MSG_UNKNOWN = 8;//发送消息类型

    private int mMinItemWith;// 设置对话框的最大宽度和最小宽度
    private int mMaxItemWith;
    public Handler handler;
    private Animation an;
    private SendErrorListener sendErrorListener;
    private VoiceIsRead voiceIsRead;
    public List<String> unReadPosition = new ArrayList<String>();
    private int voicePlayPosition = -1;


    public interface SendErrorListener {
        public void onClick(int position);
    }

    public void setSendErrorListener(SendErrorListener sendErrorListener) {
        this.sendErrorListener = sendErrorListener;
    }

    public interface VoiceIsRead {
        public void voiceOnClick(int position);
    }


    public void setVoiceIsReadListener(VoiceIsRead voiceIsRead) {
        this.voiceIsRead = voiceIsRead;
    }


    public ChatRecyclerAdapter(Context context, List<ChatMessageBean> userList, InterpttService service) {
        this.context = context;
        mService = service;
        this.beanList = userList;
        // 获取系统宽度
        WindowManager wManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wManager.getDefaultDisplay().getMetrics(outMetrics);
        mMaxItemWith = (int) (outMetrics.widthPixels * 0.5f) * 5 / 7;   //因为后来改成左侧1/4显示频道列表，所以屏宽调整
        mMinItemWith = (int) (outMetrics.widthPixels * 0.2f) * 5 / 7;
        handler = new Handler();
    }

    public void clearData() {
        beanList.clear();
    }

    /**
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        RecyclerView.ViewHolder holder = null;
        switch (viewType) {
            case FROM_USER_MSG:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_from_message, parent, false);
                holder = new FromUserMsgViewHolder(view);
                break;
            case FROM_USER_IMG:
                break;
            case FROM_USER_VOICE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_voicefrom, parent, false);
                holder = new FromUserVoiceViewHolder(view);
                break;
            case TO_USER_MSG:
                break;
            case TO_USER_IMG:
                break;
            case TO_USER_VOICE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_voiceto, parent, false);
                holder = new ToUserVoiceViewHolder(view);
                break;
            case FROM_USER_VIDEO:
                break;
            case TO_USER_VIDEO:
                break;
            case MSG_UNKNOWN:
                break;
        }
        return holder;
    }
    class FromUserMsgViewHolder extends RecyclerView.ViewHolder {
        private ImageView headicon;
        private TextView talkernick,ordertime;
        private TextView chat_time;
        private LinearLayout voice_group;
        private LinearLayout voice_order_group;
        private TextView voice_time;
        private TextView message_text;
        private TextView who_get;
        private FrameLayout voice_image;
        private FrameLayout voice_order_image;
        private View voice_anim;
        public FromUserMsgViewHolder(View view) {
            super(view);
            headicon = (ImageView) view.findViewById(R.id.tb_other_user_icon);
            talkernick = (TextView) view.findViewById(R.id.tv_talker_nick);
            ordertime = (TextView) view.findViewById(R.id.tv_order_time);
            chat_time = (TextView) view.findViewById(R.id.chat_time);
            voice_group = (LinearLayout) view
                    .findViewById(R.id.voice_group);
            voice_order_group = (LinearLayout) view
                    .findViewById(R.id.voice_order_group);
            voice_time = (TextView) view
                    .findViewById(R.id.voice_time);
            voice_image = (FrameLayout) view
                    .findViewById(R.id.voice_receiver_image);

            voice_order_image = (FrameLayout) view
                    .findViewById(R.id.voice_order_image);
            voice_anim = (View) view
                    .findViewById(R.id.id_receiver_recorder_anim);
            message_text = (TextView) view
                    .findViewById(R.id.tv_talker_message);
            who_get = (TextView) view
                    .findViewById(R.id.tv_who_get);
        }
    }

    class FromUserVoiceViewHolder extends RecyclerView.ViewHolder {
        private ImageView headicon;
        private TextView talkernick;
        private TextView chat_time;
        private LinearLayout voice_group;
        private TextView voice_time;
        private FrameLayout voice_image;
        private View voice_anim;

        public FromUserVoiceViewHolder(View view) {
            super(view);
            headicon = (ImageView) view.findViewById(R.id.tb_other_user_icon);
            talkernick = (TextView) view.findViewById(R.id.tv_talker_nick);
            chat_time = (TextView) view.findViewById(R.id.chat_time);
            voice_group = (LinearLayout) view
                    .findViewById(R.id.voice_group);
            voice_time = (TextView) view
                    .findViewById(R.id.voice_time);
            voice_image = (FrameLayout) view
                    .findViewById(R.id.voice_receiver_image);
            voice_anim = (View) view
                    .findViewById(R.id.id_receiver_recorder_anim);
        }
    }

    class ToUserVoiceViewHolder extends RecyclerView.ViewHolder {
        private ImageView headicon;
        private TextView chat_time;
        private LinearLayout voice_group;
        private TextView voice_time;
        private FrameLayout voice_image;
        private View receiver_voice_unread;
        private View voice_anim;
        private TextView talkernick;

        public ToUserVoiceViewHolder(View view) {
            super(view);
            headicon = (ImageView) view
                    .findViewById(R.id.tb_my_user_icon);
            chat_time = (TextView) view
                    .findViewById(R.id.mychat_time);
            voice_group = (LinearLayout) view
                    .findViewById(R.id.voice_group);
            voice_time = (TextView) view
                    .findViewById(R.id.voice_time);
            voice_image = (FrameLayout) view
                    .findViewById(R.id.voice_image);
            voice_anim = (View) view
                    .findViewById(R.id.id_recorder_anim);
            talkernick = (TextView) view
                    .findViewById(R.id.tv_talker_nick);
        }
    }

    /**
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChatMessageBean tbub = beanList.get(position);
        int itemViewType = getItemViewType(position);
        switch (itemViewType) {
            case FROM_USER_MSG:
                fromMsgUserLayout((FromUserMsgViewHolder) holder, tbub, position);
                break;
            case FROM_USER_IMG:
                break;
            case FROM_USER_VOICE:
                fromVoiceUserLayout((FromUserVoiceViewHolder) holder, tbub, position);
                break;
            case TO_USER_MSG:
                fromMsgUserLayout((FromUserMsgViewHolder) holder, tbub, position);
                break;
            case TO_USER_IMG:
                break;
            case TO_USER_VOICE:
                toVoiceUserLayout((ToUserVoiceViewHolder) holder, tbub, position);
                break;
            case FROM_USER_VIDEO:
                break;
            case TO_USER_VIDEO:
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessageBean cmb = beanList.get(position);
        int myId = mService.getMyUserId();
        if (myId < 1001001 || myId > 10000000) {
            return MSG_UNKNOWN;
        }
        boolean isMe = (cmb.getUid() == myId);
        if (cmb.getVoice() != null && cmb.getVoice().length > 0 && cmb.getText() == null) {
            if (isMe) {
                return TO_USER_VOICE;
            } else {
                return FROM_USER_VOICE;
            }
        }
        //视频
        else if (cmb.getVideopath() != null && cmb.getVideopath().length() > 0) {
            if (isMe) {
                return TO_USER_VIDEO;
            } else {
                return FROM_USER_VIDEO;
            }
        } else if (cmb.getImagepath() != null && cmb.getImagepath().length() > 0) {
            if (isMe) {
                return TO_USER_IMG;
            } else {
                return FROM_USER_IMG;
            }
        } else if (cmb.getText() != null && cmb.getText().length() > 0) {
            return FROM_USER_MSG;
        }


        return MSG_UNKNOWN;
    }

    @Override
    public int getItemCount() {
        return beanList.size();
    }

    //uid是目标用户id。uid为0时，表示未指定用户
    private void manageMembers(final Channel c, final boolean isCreator, final int uid) {
        if (c == null || mService == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.manage_member_auth));
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.manage_member, null);
        builder.setView(layout);


        final EditText etUid = (EditText) layout.findViewById(R.id.et_ban_member_uid);
        if (uid > 1000000 && uid < 9999999) {
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
        if (!isCreator) {
            rbMonitor.setEnabled(false);
            rbCancelMonitor.setEnabled(false);
            rbPrior.setEnabled(false);
            rbPrior.setEnabled(false);
            rbCancelPrior.setEnabled(false);
        }
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final String strUid = etUid.getText().toString();
                int uid = 0;
                try {
                    uid = Integer.parseInt(strUid);
                } catch (Exception e) {

                }
                final int check = rgOpt.getCheckedRadioButtonId();
                if (!AppCommonUtil.validTotalkId(strUid)) {
                    AppCommonUtil.showToast(context, R.string.userid_bad_format);

                } else if (check == rbBan.getId()) {
                    mService.manageMember(c.id, uid, 0);
                } else if (check == rbCancelBan.getId()) {
                    mService.manageMember(c.id, uid, 1);
                } else if (check == rbMute.getId()) {
                    mService.manageMember(c.id, uid, 2);
                } else if (check == rbCancelMute.getId()) {
                    mService.manageMember(c.id, uid, 3);
                } else if (check == rbMonitor.getId()) {
                    mService.manageMember(c.id, uid, 4);
                } else if (check == rbCancelMonitor.getId()) {
                    mService.manageMember(c.id, uid, 5);
                } else if (check == rbPrior.getId()) {
                    mService.manageMember(c.id, uid, 6);
                } else if (check == rbCancelPrior.getId()) {
                    mService.manageMember(c.id, uid, 7);

                }
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();

    }

    private void fromMsgUserLayout(final FromUserMsgViewHolder holder, final ChatMessageBean tbub, final int position) {
        final int tbubUid = tbub.getUid();
        final String tbubNick = tbub.getNick();
        final String mMsg= tbub.getText();
        final Long mTime= tbub.getTime();
        final int mWhoGet= tbub.getWhoGet();
        final String mWhoGetNickName= tbub.getWhoGetNiceName();
        User u = mService.getUser(tbubUid);
        if (u != null) {
            //昵称
            holder.talkernick.setText(u.nick);

            //在线
            if (u.avatar != null && u.avatar.length > 0) {
                //有头像
                Bitmap bm = BitmapFactory.decodeByteArray(u.avatar, 0, u.avatar.length);
                holder.headicon.setImageBitmap(bm);
            } else {
                //没头像，设置默认
                holder.headicon.setImageResource(R.drawable.ic_default_avatar);
            }
        } else {
            //不在线
            holder.talkernick.setText(tbubNick);
            holder.headicon.setImageResource(R.drawable.ic_default_avatar);
        }

        /* time */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String bubTime = sdf.format(new Date(tbub.getTime()));
        SimpleDateFormat ordf = new SimpleDateFormat("MM-dd HH:mm:ss");
        String orTime = ordf.format(new Date(mTime));
        String newestTime = null;
//        if (position > 0) {
//            newestTime = sdf.format(new Date(beanList.get(position - 1).getTime()));
//        }
//        if (position != 0) {
//            String showTime = getTime(bubTime, newestTime);
//            if (showTime != null) {
//                holder.chat_time.setVisibility(View.VISIBLE);
//                holder.chat_time.setText(showTime);
//            } else {
//                holder.chat_time.setVisibility(View.GONE);
//            }
//        } else {
//            String showTime = getTime(bubTime, null);
//            holder.chat_time.setVisibility(View.VISIBLE);
//            holder.chat_time.setText(showTime);
//        }
        holder.voice_group.setVisibility(View.VISIBLE);
        holder.message_text.setText("号码:"+mMsg);
        holder.ordertime.setText(orTime);
        if (mWhoGet == 0) {
            holder.who_get.setText("无");
            holder.who_get.setTextColor(Color.BLACK);
        } else {
            if(mWhoGetNickName!=null){
                if (mService.getMyUserId() == mWhoGet) {
                    holder.who_get.setText(mWhoGetNickName + "(自己)");
                    holder.who_get.setTextColor(Color.BLUE);
                } else {
                    holder.who_get.setText(mWhoGetNickName);
                    holder.who_get.setTextColor(Color.BLACK);
                }
            }else {
                if (mService.getMyUserId() == mWhoGet) {
                    holder.who_get.setText(String.valueOf(mWhoGet) + "(自己)");
                    holder.who_get.setTextColor(Color.BLUE);
                } else {
                    holder.who_get.setText(String.valueOf(mWhoGet));
                    holder.who_get.setTextColor(Color.BLACK);
                }
            }
        }

        AnimationDrawable drawable;
        holder.voice_anim.setId(position);

        if (position == voicePlayPosition) {
            holder.voice_anim.setBackgroundResource(R.drawable.receiver_voice_node_playing003);
            holder.voice_anim.setBackgroundResource(R.drawable.voice_play_receiver);
            drawable = (AnimationDrawable) holder.voice_anim.getBackground();
            drawable.start();
        } else {
            holder.voice_anim.setBackgroundResource(R.drawable.receiver_voice_node_playing003);
        }
        holder.voice_order_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //zcx add
//                if (mService.getcurrentVoiceTalker() != null) {
//                    //有人讲话，禁止回放
//                    return;
//                }
                // TODO Auto-generated method stub
                holder.voice_anim.setBackgroundResource(R.drawable.receiver_voice_node_playing003);
                if (voiceIsRead != null) {
                    voiceIsRead.voiceOnClick(position);
                }

                //回放
                if (voicePlayPosition == holder.voice_anim.getId()) {
                    //正在播放，则停止
                    mService.stopPlayback();
                } else {
                    byte[] data = tbub.getVoice();
                    mService.playback(data, currentChanId, holder.voice_anim.getId());
                }
            }
        });
        if(tbub.getVoice()!=null) {
            holder.voice_order_group.setVisibility(View.VISIBLE);
            int voiceTime = getVoiceDuration(tbub.getVoice());
            holder.voice_time.setText(voiceTime + "\"");
            ViewGroup.LayoutParams lParams = holder.voice_order_image.getLayoutParams();
            int tmpWidth = (int) (mMinItemWith + mMaxItemWith / 64f * voiceTime);//tbub.getUserVoiceTime());
            //考虑有时有2分钟的时长，所以设个上限
            if (tmpWidth > mMaxItemWith) {
                tmpWidth = mMaxItemWith;
            }
            lParams.width = tmpWidth;
            holder.voice_order_image.setLayoutParams(lParams);
        }else{
            holder.voice_order_group.setVisibility(View.GONE);
        }
        holder.voice_group.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                try {
                        Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mMsg));//直接拨打电话
                        context.startActivity(dialIntent);
                        return;
                } catch (RuntimeException e) {
                    Toast.makeText(context, "请打开拨打电话权限", Toast.LENGTH_LONG).show();
                }
            }
        });

        //点击头像
        holder.headicon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = LayoutInflater.from(context);
                View layout = inflater.inflate(R.layout.member_detail, null);
                TextView tvUid = (TextView) layout.findViewById(R.id.tv_user_detail_id);
                tvUid.setText(tbubUid + "");
                ImageView ivFriend = (ImageView) layout.findViewById(R.id.iv_user_detail_add_contact);
                ImageView ivManage = (ImageView) layout.findViewById(R.id.iv_user_detail_manage);
                ivFriend.setVisibility(View.INVISIBLE);
                ivManage.setVisibility(View.INVISIBLE);

                new AlertDialog.Builder(context)
                        .setTitle(tbubNick)
                        .setView(layout)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });

    }


    private void fromVoiceUserLayout(final FromUserVoiceViewHolder holder, final ChatMessageBean tbub, final int position) {
        //从内存读取用户头像
        final int tbubUid = tbub.getUid();
        final String tbubNick = tbub.getNick();
        //点击头像出现人物信息
        holder.headicon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = LayoutInflater.from(context);
                View layout = inflater.inflate(R.layout.member_detail, null);
                ImageView user_ImgHead = (ImageView) layout.findViewById(R.id.iv_user_detail_avatar);
                ImageView user_ImgAdd = (ImageView) layout.findViewById(R.id.iv_user_detail_add_contact);
                ImageView user_ImgManage = (ImageView) layout.findViewById(R.id.iv_user_detail_manage);
                TextView user_ID = (TextView) layout.findViewById(R.id.tv_user_detail_id);
                user_ID.setText("" + tbubUid);

                // if (mService != null && mService.getCurrentUser() != null && u.getChannel() != null)
                if (mService != null && mService.getCurrentUser() != null) {
                    final int myuid = mService.getMyUserId();
                    ArrayList<Channel> channels = mService.getChannelList();
                    Channel curChan = null;
                    if (channels != null && channels.size() > 0) {
                        for (Channel c : channels) {
                            if (c.id == currentChanId) {
                                curChan = c;
                                break;
                            }
                        }
                    }

                    final Channel cur = curChan;
                    if (cur!=null && (myuid == cur.creatorId || mService.isMonitor(myuid, cur))) {
                        user_ImgManage.setVisibility(View.VISIBLE);
                        user_ImgManage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                manageMembers(cur, (myuid == cur.creatorId), tbubUid);
                            }
                        });
                    }

                    if (mService != null && mService.getContacts() != null && mService.getCurrentUser() != null) {
                        if (!mService.getContacts().containsKey(tbubUid) && tbubUid != mService.getCurrentUser().iId) {
                            user_ImgAdd.setVisibility(View.VISIBLE); //如果不是好友且非本人,就显示添加好友的标记
                            user_ImgAdd.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    new AlertDialog.Builder(context)
                                            .setTitle(context.getString(R.string.notif))
                                            .setMessage(context.getString(R.string.add) + tbubNick + context.getString(R.string.as_contact))
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    mService.applyContact(true, tbubUid);
                                                    AppCommonUtil.showToast(context, context.getString(R.string.apply_sent));
                                                }
                                            })
                                            .setNegativeButton(context.getString(R.string.cancel), null)
                                            .show();
                                }
                            });
                        }
                    }
                }
                else {

                }

                user_ImgHead.setImageResource(R.drawable.ic_default_avatar);
                new AlertDialog.Builder(context)
                        .setTitle(tbubNick)
                        .setView(layout)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });

        final User u = mService.getUser(tbubUid);
        if (u != null) {
            //在线
            //昵称
            holder.talkernick.setText(u.nick);

            if (u.avatar != null && u.avatar.length > 0) {
                //z
                Bitmap bm = BitmapFactory.decodeByteArray(u.avatar, 0, u.avatar.length);
                holder.headicon.setImageBitmap(bm);
            } else {
                //没头像，设置默认
                holder.headicon.setImageResource(R.drawable.ic_default_avatar);
            }
        } else {
            //不在线
            //昵称
            String nick = tbub.getNick();
            holder.talkernick.setText(nick);

            holder.headicon.setImageResource(R.drawable.ic_default_avatar);
        }

        /* time */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String bubTime = sdf.format(new Date(tbub.getTime()));
        String newestTime = null;
        if (position > 0) {
            newestTime = sdf.format(new Date(beanList.get(position - 1).getTime()));
        }
        if (position != 0) {
            String showTime = getTime(bubTime, newestTime);
            if (showTime != null) {
                holder.chat_time.setVisibility(View.VISIBLE);
                holder.chat_time.setText(showTime);
            } else {
                holder.chat_time.setVisibility(View.GONE);
            }
        } else {
            String showTime = getTime(bubTime, null);
            holder.chat_time.setVisibility(View.VISIBLE);
            holder.chat_time.setText(showTime);
        }

        holder.voice_group.setVisibility(View.VISIBLE);

        AnimationDrawable drawable;
        holder.voice_anim.setId(position);

        if (position == voicePlayPosition) {
            holder.voice_anim.setBackgroundResource(R.drawable.receiver_voice_node_playing003);
            holder.voice_anim.setBackgroundResource(R.drawable.voice_play_receiver);
            drawable = (AnimationDrawable) holder.voice_anim.getBackground();
            drawable.start();
        } else {
            holder.voice_anim.setBackgroundResource(R.drawable.receiver_voice_node_playing003);
        }
        holder.voice_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //zcx add
//                if (mService.getcurrentVoiceTalker() != null) {
//                    //有人讲话，禁止回放
//                    return;
//                }
                // TODO Auto-generated method stub
                holder.voice_anim.setBackgroundResource(R.drawable.receiver_voice_node_playing003);
                if (voiceIsRead != null) {
                    voiceIsRead.voiceOnClick(position);
                }

                //回放
                if (voicePlayPosition == holder.voice_anim.getId()) {
                    //正在播放，则停止
                    mService.stopPlayback();
                } else {
                    byte[] data = tbub.getVoice();
                    mService.playback(data, currentChanId, holder.voice_anim.getId());
                }
            }
        });

        int voiceTime = getVoiceDuration(tbub.getVoice());
        holder.voice_time.setText(voiceTime + "\"");
        ViewGroup.LayoutParams lParams = holder.voice_image.getLayoutParams();
        int tmpWidth = (int) (mMinItemWith + mMaxItemWith / 64f * voiceTime);//tbub.getUserVoiceTime());
        //考虑有时有2分钟的时长，所以设个上限
        if (tmpWidth > mMaxItemWith) {
            tmpWidth = mMaxItemWith;
        }
        lParams.width = tmpWidth;
        holder.voice_image.setLayoutParams(lParams);
    }

    private void toVoiceUserLayout(final ToUserVoiceViewHolder holder, final ChatMessageBean tbub, final int position) {
        final int tbubUid = tbub.getUid();
        final String tbubNick = tbub.getNick();
        User u = mService.getUser(tbubUid);
        if (u != null) {
            //昵称
            holder.talkernick.setText(u.nick);

            //在线
            if (u.avatar != null && u.avatar.length > 0) {
                //有头像
                Bitmap bm = BitmapFactory.decodeByteArray(u.avatar, 0, u.avatar.length);
                holder.headicon.setImageBitmap(bm);
            } else {
                //没头像，设置默认
                holder.headicon.setImageResource(R.drawable.ic_default_avatar);
            }
        } else {
            //不在线
            holder.talkernick.setText(tbubNick);
            holder.headicon.setImageResource(R.drawable.ic_default_avatar);
        }

        /* time */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String bubTime = sdf.format(new Date(tbub.getTime()));
        String newestTime = null;
        if (position > 0) {
            newestTime = sdf.format(new Date(beanList.get(position - 1).getTime()));
        }
        if (position != 0) {
            String showTime = getTime(bubTime, newestTime);
            if (showTime != null) {
                holder.chat_time.setVisibility(View.VISIBLE);
                holder.chat_time.setText(showTime);
            } else {
                holder.chat_time.setVisibility(View.GONE);
            }
        } else {
            String showTime = getTime(bubTime, null);
            holder.chat_time.setVisibility(View.VISIBLE);
            holder.chat_time.setText(showTime);
        }
        holder.voice_group.setVisibility(View.VISIBLE);
        if (holder.receiver_voice_unread != null)
            holder.receiver_voice_unread.setVisibility(View.GONE);
        if (holder.receiver_voice_unread != null && unReadPosition != null) {
            for (String unRead : unReadPosition) {
                if (unRead.equals(position + "")) {
                    holder.receiver_voice_unread
                            .setVisibility(View.VISIBLE);
                    break;
                }
            }
        }
        AnimationDrawable drawable;
        holder.voice_anim.setId(position);
        if (position == voicePlayPosition) {
            holder.voice_anim.setBackgroundResource(R.drawable.adj);
            holder.voice_anim.setBackgroundResource(R.drawable.voice_play_send);
            drawable = (AnimationDrawable) holder.voice_anim
                    .getBackground();
            drawable.start();
        } else {
            holder.voice_anim.setBackgroundResource(R.drawable.adj);
        }
        holder.voice_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //zcx add
//                if (mService.getcurrentVoiceTalker() != null) {
//                    //有人讲话，禁止回放
//                    return;
//                }

                // TODO Auto-generated method stub
                if (holder.receiver_voice_unread != null)
                    holder.receiver_voice_unread.setVisibility(View.GONE);
                holder.voice_anim.setBackgroundResource(R.drawable.adj);
                //这里不立刻开始显示动画，等service里，播放成功后回调里才开始

                if (voiceIsRead != null) {
                    voiceIsRead.voiceOnClick(position);
                }

                //回放
                if (voicePlayPosition == holder.voice_anim.getId()) {
                    //正在播放，则停止
                    mService.stopPlayback();
                } else {
                    byte[] data = tbub.getVoice();
                    mService.playback(data, currentChanId, holder.voice_anim.getId());
                }
            }
        });

        //点击头像
        holder.headicon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = LayoutInflater.from(context);
                View layout = inflater.inflate(R.layout.member_detail, null);
                TextView tvUid = (TextView) layout.findViewById(R.id.tv_user_detail_id);
                tvUid.setText(tbubUid + "");
                ImageView ivFriend = (ImageView) layout.findViewById(R.id.iv_user_detail_add_contact);
                ImageView ivManage = (ImageView) layout.findViewById(R.id.iv_user_detail_manage);
                ivFriend.setVisibility(View.INVISIBLE);
                ivManage.setVisibility(View.INVISIBLE);

                new AlertDialog.Builder(context)
                        .setTitle(tbubNick)
                        .setView(layout)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });

        byte[] voiceBytes = tbub.getVoice();
        int voiceTime = getVoiceDuration(voiceBytes);
        holder.voice_time.setText(voiceTime + "\"");
        ViewGroup.LayoutParams lParams = holder.voice_image.getLayoutParams();
        int tmpWidth = (int) (mMinItemWith + mMaxItemWith / 64f * voiceTime);
        //考虑有时有2分钟的时长，所以设个上限
        if (tmpWidth > mMaxItemWith) {
            tmpWidth = mMaxItemWith;
        }
        lParams.width = tmpWidth;
        holder.voice_image.setLayoutParams(lParams);
    }

    //duration, in Seconds.
    private int getVoiceDuration(byte[] data) {
        int frames = data.length / 30; //帧数
        int ms = frames * 20; //每帧20ms

        return (ms / 1000 + 1);
    }

    @SuppressLint("SimpleDateFormat")
    public String getTime(String time, String before) {
        String show_time = null;
        if (before != null) {
            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date now = df.parse(time);
                Date date = df.parse(before);
                long l = now.getTime() - date.getTime();
                long day = l / (24 * 60 * 60 * 1000);
                long hour = (l / (60 * 60 * 1000) - day * 24);
                long min = ((l / (60 * 1000)) - day * 24 * 60 - hour * 60);
                if (min >= 1) {
                    show_time = time.substring(11);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            show_time = time.substring(11);
        }
        String getDay = getDay(time);
        if (show_time != null && getDay != null)
            show_time = getDay + " " + show_time;
        return show_time;
    }

    @SuppressLint("SimpleDateFormat")
    public static String returnTime() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = sDateFormat.format(new Date());
        return date;
    }

    @SuppressLint("SimpleDateFormat")
    public String getDay(String time) {
        String showDay = null;
        String nowTime = returnTime();
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date now = df.parse(nowTime);
            Date date = df.parse(time);
            long diff = now.getTime() - date.getTime();
            long day = diff / (24 * 60 * 60 * 1000);

            if (day >= 365) {
                showDay = time.substring(0, 10);
            }
            //zcx change,只要不是同一天，就显示日期
//            else if (day >= 1 && day < 365) {
//                showDay = time.substring(5, 10);
//            }
            else {
                String today = nowTime.substring(0, 10);
                String timeDay = time.substring(0, 10);
                if (!today.equals(timeDay)) {
                    showDay = time.substring(5, 10);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return showDay;
    }

    private int currentChanId = 0;

    public void setCurrentChanId(int cid) {
        currentChanId = cid;
    }

    public void startPlayVoice(int chanId, int resId) {
        if (chanId == currentChanId) {
            voicePlayPosition = resId;
        }
    }

    public void stopPlayVoice() {
        voicePlayPosition = -1;
    }
}
