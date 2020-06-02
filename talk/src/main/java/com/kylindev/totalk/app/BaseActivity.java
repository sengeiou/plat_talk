package com.kylindev.totalk.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kylindev.pttlib.LibConstants;
import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttProtocolHandler.DisconnectReason;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.InterpttService.HandmicState;
import com.kylindev.pttlib.service.InterpttService.HeadsetState;
import com.kylindev.pttlib.service.InterpttService.LocalBinder;
import com.kylindev.pttlib.service.InterpttService.MicState;
import com.kylindev.pttlib.service.model.Channel;
import com.kylindev.pttlib.service.model.Contact;
import com.kylindev.pttlib.service.model.User;
import com.kylindev.totalk.AppConstants;
import com.kylindev.totalk.R;
import com.kylindev.totalk.bjxt.SePortActivity;
import com.kylindev.totalk.chat.RecyclerViewChatActivity;
import com.kylindev.totalk.utils.AppCommonUtil;
import com.kylindev.totalk.utils.AppSettings;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.HexUtil;

import static com.kylindev.pttlib.LibConstants.INTERPTT_SERVICE;
import static com.kylindev.pttlib.service.InterpttService.ConnState;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTED;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTING;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_SYNCHRONIZING;

//import com.kylindev.totalk.MainApp;

public abstract class BaseActivity extends SePortActivity implements OnClickListener {

    byte formData[] = new byte[1024];
    int len232 = 0;//接收到的数据长度
    int len = 0;//数据总长度
    boolean isStart232 = false;
    String mEncodeHexStr;
    final java.util.Timer timer = new java.util.Timer(true);
    private boolean valid = true;
    private String mHexStringToString;
    String A1 = "AA5531";
    String A2 = "AA5532";
    String A3 = "01";
    String A4 = "02";
    String ackStart = "AA 55 00 05 80 F1 02 01 00 88";
    String ackStop = "AA 55 00 05 80 F1 02 02 00 87";
    String openLink = "AA 55 00 04 B0 F2 01 02 58";
    String closeLink = "AA 55 00 04 B0 F2 01 00 5A";
    String openLunch = "AA 55 00 04 B0 F2 01 01 59";
    String closeLunch = "AA 55 00 04 B0 F2 01 00 5A";
    String signallingACK = "AA 55 00 04 80 F3 08 00 82";
    String answer = "AA 55 00 16 00 F4 12 34 56 78 A0 88 12 34 56 FF FF FF FF FF FF FF FF FF FF FF 2A";
    String request = "AA 55 00 16 00 F4 12 34 56 78 A0 0D 12 34 56 FF FF FF FF FF FF FF FF FF FF FF A5";
    private String mSubstring;

    public static void onReceived(final byte[] buffer, final int size, final int type) {
        String hexStr = HexUtil.encodeHexStr(buffer, false, size);
        String opLink = "AA 55 00 04 B0 F2 01 02 58";
        String cloLink = "AA 55 00 04 B0 F2 01 00 5A";
        String ackSta = "AA 55 00 05 80 F1 02 01 00 88";
        String opLunch = "AA 55 00 04 B0 F2 01 01 59";
        String ackSto = "AA 55 00 05 80 F1 02 02 00 87";
        String cloLunch = "AA 55 00 04 B0 F2 01 00 5A";

        boolean specialChar = isSpecialChar(hexStr);
        boolean messyCode = isMessyCode(hexStr);
        if (specialChar && messyCode) {
            //Toast.makeText(getApplicationContext(), "包含特殊字符", Toast.LENGTH_SHORT).show();
            Log.e("包含", "包含");
        } else if (!specialChar && !messyCode) {
            //去掉16进制中的空格，不然计算或者比较数组越界
            String ope = opLink.replaceAll(" ", "");
            String clos = cloLink.replaceAll(" ", "");
            String ackstar = ackSta.replaceAll(" ", "");
            String opLun = opLunch.replaceAll(" ", "");
            String acksto = ackSto.replaceAll(" ", "");
            String cloLun = cloLunch.replaceAll(" ", "");

            if (hexStr.equals(ope)) {
                actived = true;
                Log.e("接收到载波", "接收到载波");
                mService.userPressDown();
            } else if (hexStr.equals(clos)) {
                Log.e("载波消失", "载波消失");
                mService.userPressUp();
            } else if (hexStr.equals(ackstar)) {
                Log.e("PTT_ON 指令 ACK", "AA 55 1A 41 59 11" + "  七个值");
            }else if (hexStr.equals(opLun)) {
                Log.e("车台发射(打开 400M 对讲链路)", "车台发射(打开 400M 对讲链路)");
            }else if (hexStr.equals(acksto)) {
                Log.e("PTT_OFF 指令 ACK", " AA 55 1A 41 59 12" + "  七个值");
            }else if (hexStr.equals(cloLun)) {
                Log.e("车台发射(关闭 400M 对讲链路)", "车台发射(关闭 400M 对讲链路)");
            }
        }
    }

    @Override
    protected void onDataReceived(final byte[] buffer, final int size, final int type) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (type == 232) {
                    Log.i("testData", "ssssssss");
                    Log.i("testDataSize", String.valueOf(size));
                    mEncodeHexStr = HexUtil.encodeHexStr(buffer, false, size);
                    mSubstring = mEncodeHexStr.substring(mEncodeHexStr.length() - 2, mEncodeHexStr.length());
                    Log.i("hexStr", mEncodeHexStr + "    hexStr");
                    Log.i("substring", mSubstring + "    substring");


                    boolean specialChar = isSpecialChar(mEncodeHexStr);
                    boolean messyCode = isMessyCode(mEncodeHexStr);
                    if (specialChar && messyCode) {
                        Toast.makeText(getApplicationContext(), "包含特殊字符", Toast.LENGTH_SHORT).show();
                        Log.e("包含", "包含");
                    } else if (!specialChar && !messyCode) {
                        //Toast.makeText(getApplicationContext(),"不包含特殊字符",Toast.LENGTH_SHORT).show();
                        Log.e("不包含", "不包含");
                        //去掉16进制中的空格，不然计算活比较数组越界
                        String open = openLink.replaceAll(" ", "");
                        String close = closeLink.replaceAll(" ", "");
                        String start = ackStart.replaceAll(" ", "");
                        String stop = ackStop.replaceAll(" ", "");
                        String openlunch = openLunch.replaceAll(" ", "");
                        String answer1 = answer.replaceAll(" ", "");
                        String request1 = request.replaceAll(" ", "");
                        String closelunch = closeLunch.replaceAll(" ", "");
                        String signallingACK1 = signallingACK.replaceAll(" ", "");

                        if (mEncodeHexStr.equals(start)) {
                            Log.e("PTT_ON 指令 ACK", "AA 55 1A 41 59 11" + "  七个值");
                        } else if (mEncodeHexStr.equals(stop)) {
                            Log.e("PTT_OFF 指令 ACK", " AA 55 1A 41 59 12" + "  七个值");
                        } else if (mEncodeHexStr.equals(open)) {
                            actived = true;
                            Log.e("接收到载波", "接收到载波");
                            mService.userPressDown();
                        } else if (mEncodeHexStr.equals(close)) {
                            Log.e("载波消失", "载波消失");
                            mService.userPressUp();
                        } else if (mEncodeHexStr.equals(openlunch)) {
                            Log.e("车台发射(打开 400M 对讲链路)", "车台发射(打开 400M 对讲链路)");
                        } else if (mEncodeHexStr.equals(closelunch)) {
                            Log.e("车台发射(关闭 400M 对讲链路)", "车台发射(关闭 400M 对讲链路)");
                        } else if (mEncodeHexStr.equals(signallingACK1)) {
                            Log.e("信令ack", "信令ack");
                        } else if (mEncodeHexStr.equals(answer1)) {
                            String req = "AA 55 00 04 80 F4 88 00 01";
                            //sendHexString(req.replaceAll("\\s*", ""), "232");
                        } else if (mEncodeHexStr.equals(request1)) {
                            String req = "AA 55 00 04 80 F4 0D 00 01 7B";
                            //sendHexString(req.replaceAll("\\s*", ""), "232");
                        } else {
                            //Toast.makeText(getApplicationContext(), "ACK校验失败", Toast.LENGTH_SHORT).show();
                            //Log.e("ack", "ack对不上");
                            return;
                            /*for (int i = 0; i < 3; i++) {
                                mHandler.postAtTime(new Runnable() {
                                    @Override
                                    public void run() {
                                        String dat = "A5 01 0C 21 01 00 2C";
                                        sendHexString(dat.replaceAll("\\s*", ""), "232");
                                        Log.e("重新请求", "A5 01 0C 21 01 00 2C" + "  七个值");
                                    }
                                }, 900);
                            }*/
                        }
                    }
                }
            }
        });
    }

    /**
     * 校验数据
     * @param buffer 接收到的数据
     * @param size 数据长度
     */
    private boolean checkData(byte[] buffer, int size) {
        String strData = HexUtil.encodeHexStr(buffer,false,size);
        Log.i("TAGstrData收到的数据",strData);
        /**
         * 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
         * A 8 0 1 1 2 3 4 5 6  7  8  0  0  A  A
         *
         */
        //total里存放的是累加和
        int total = 0;
        for (int i = 0;i<strData.length();i+=2){
            //strB.append("0x").append(strData.substring(i,i+2));  //0xC30x3C0x010x120x340x560x780xAA
            total = total + Integer.parseInt(strData.substring(i,i+2),16);
        }
        //noTotal为累加和取反加一
        int noTotal = ~total +1;
        Log.i("total",String.valueOf(noTotal));
        String hex = Integer.toHexString(noTotal).toUpperCase();
        Log.i("TAGhex",hex);
        String key = hex.substring(hex.length()-2);
        Log.i("TAG校验码key",key);
        Log.i("TAGhex",key);
        if (key.equals(strData.substring(strData.length()-2))){
            Log.i("jiaoyan","校验成功");
            return true;
        }else {

            Log.i("jiaoyan","校验失败");
            return false;
        }
        //Log.i("total", hex.substring(hex.length()-2));

    }

    private static boolean isMessyCode(String strName) {
        try {
            Pattern p = Pattern.compile("\\s*|\t*|\r*|\n*");
            Matcher m = p.matcher(strName);
            String after = m.replaceAll("");
            String temp = after.replaceAll("\\p{P}", "");
            char[] ch = temp.trim().toCharArray();

            int length = (ch != null) ? ch.length : 0;
            for (int i = 0; i < length; i++) {
                char c = ch[i];
                if (!Character.isLetterOrDigit(c)) {
                    String str = "" + ch[i];
                    if (!str.matches("[\u4e00-\u9fa5]+")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 判断是否含有特殊字符
     *
     * @param str
     * @return true为包含，false为不包含
     */
    public static boolean isSpecialChar(String str) {
        String regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }

    /**
     * 转化为int类型
     *
     * @param bytes
     * @param size
     * @return
     */
    private int toInt2(byte[] bytes, int size) {
        return Integer.parseInt(new BigInteger((HexUtil.encodeHexStr(bytes, size).replace(
                " ", ""
        )), 16).toString(10));
    }

    /**
     * The InterpttService instance that drives this activity's data.
     */
    protected static InterpttService mService;
//    protected InterpttService mService2;

    // Create dialog
    //private ProgressDialog mConnectDialog = null;
    protected Intent mServiceIntent = null;

    private Handler mHandler = new Handler();    //用于其他线程更新UI

    //顶部
    protected ImageView mIVBarLeft, mIVBarRight, mIVBarLeftInner, mIVBarRightInner;
    protected TextView mTVBarTitle, mTVBarCount;
    protected ViewGroup activityView; //子类布局

    private ImageView mIVVoice;
    private TextView mTVConnect;
    private TextView mTVCurrentChanName, mTVCurrentChanTalker, mTVCurrentChanTalkerBg;
    private TextView mTVListenChanName, mTVListenChanTalker;
    private ImageView mIVCurrentChanTalkerDevice, mIVListenChanTalkerDevice;
    private TextView mTVHistoryTalker0, mTVHistoryTalker1, mTVHistoryTalker2;

    //以下是ptt相关界面
    private TextView mTVDebug;
    private ImageView mIVPttCirc, mIVPtt;
    private ImageView mIVHistory, mIVHeadset, mIVHandmic;
    private TextView mTVPttTimer;
    protected LinearLayout mLLlcd;
    private ImageView mIVHoriVolume;
    protected boolean mServiceBind = false;        //有时在unbindService时报错java.lang.IllegalArgumentException:  Service not registered，因此增加此变量unbind之前判断

    private boolean isScreenOn = false;
    private boolean isMap = false;
    protected LinearLayout mLLPttArea;
    /**
     * Management of service connection state.
     */
    protected ServiceConnection mServiceConnection = null;

    public void initServiceConnection() {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalBinder localBinder = (LocalBinder) service;
                mService = localBinder.getService();
                serviceConnected(); //通知子类

                mService.registerObserver(serviceObserver);

                //channel界面出现过一次后，才设置notification对应的activity
//                Intent notifIntent = new Intent(MainApp.getContext(), ChannelActivity.class);
//                mService.setNotifIntent(notifIntent);

                refreshLcdChannel();
                //设置ptt界面的初始状态
                refreshHeadsetIcon();
                refreshHandmicIcon();
                //初始化声音开关
                boolean isOn = mService.getVoiceOn();
                mIVVoice.setImageResource(isOn ? R.drawable.voice_on : R.drawable.voice_off);

                refreshMicState(mService.getMicState());
                //设置连接提示是否可见
                showConnection();
            }

            //此方法调用时机：This is called when the connection with the service has been unexpectedly disconnected
            //-- that is, its process crashed. Because it is running in our same process, we should never see this happen.
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
                mServiceConnection = null;
                stopService(mServiceIntent);

                //此函数只有在service被异常停止时才会调用，如被系统或其他软件强行停止
                finish();
            }
        };
    }


    public String makeChecksum(String hexdata) {
        if (hexdata == null || hexdata.equals("")) {
            return "00";
        }
        hexdata = hexdata.replaceAll(" ", "");
        int total = 0;
        int len = hexdata.length();
        if (len % 2 != 0) {
            return "00";
        }
        int num = 0;
        while (num < len) {
            String s = hexdata.substring(num, num + 2);
            total += Integer.parseInt(s, 16);
            num = num + 2;
        }
        return hexInt(total);
    }

    private String hexInt(int total) {
        int a = total / 256;
        int b = total % 256;
        if (a > 255) {
            return hexInt(a) + format(b);
        }
        return format(a) + format(b);
    }

    private String format(int hex) {
        String hexa = Integer.toHexString(hex);
        int len = hexa.length();
        if (len < 2) {
            hexa = "0" + hexa;
        }
        return hexa;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTVConnect = (TextView) findViewById(R.id.tv_connection_lost);
        mTVConnect.setOnClickListener(this);
        /*File file = new File("/dev/ashmem");
        String absolutePath = file.getAbsolutePath();*/
        //SerialPortUtil.open("/dev/ashmem",19200,0);

        //顶部栏
        mIVBarLeft = (ImageView) findViewById(R.id.iv_bar_left);
        mIVBarRight = (ImageView) findViewById(R.id.iv_bar_right);
        mIVBarLeftInner = (ImageView) findViewById(R.id.iv_bar_left_inner);
        mIVBarRightInner = (ImageView) findViewById(R.id.iv_bar_right_innner);


        mTVBarTitle = (TextView) findViewById(R.id.tv_bar_title);
        mTVBarCount = (TextView) findViewById(R.id.tv_bar_count);
        activityView = (ViewGroup) findViewById(R.id.fl_act_content);
        activityView.addView(View.inflate(this, getContentViewId(), null));
        mLLlcd = (LinearLayout) findViewById(R.id.ll_lcd);

        mTVCurrentChanName = (TextView) findViewById(R.id.tv_current_chan_name);
        mIVCurrentChanTalkerDevice = (ImageView) findViewById(R.id.iv_current_talker_device);
        mTVCurrentChanTalker = (TextView) findViewById(R.id.tv_current_chan_talker);

        mTVListenChanName = (TextView) findViewById(R.id.tv_listen_chan_name);
        mIVListenChanTalkerDevice = (ImageView) findViewById(R.id.iv_listen_talker_device);
        mTVListenChanTalker = (TextView) findViewById(R.id.tv_listen_chan_talker);

        mTVHistoryTalker0 = (TextView) findViewById(R.id.tv_history_talker0);
        mTVHistoryTalker1 = (TextView) findViewById(R.id.tv_history_talker1);
        mTVHistoryTalker2 = (TextView) findViewById(R.id.tv_history_talker2);

        findViewById(R.id.img).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(this,MainActivity.class);
                finish();
            }
        });

        //可能是首次运行，也可能是用户重新launch
        //因此，先检查service是否在运行，如果是，则直接bind以获取mService实例；如果没有，则startService，再bind
        mServiceIntent = new Intent(this, InterpttService.class);

        if (!AppCommonUtil.isServiceRunning(this, INTERPTT_SERVICE)) {
            startService(mServiceIntent);
        }

        initServiceConnection();
        mServiceBind = bindService(mServiceIntent, mServiceConnection, 0);

        // Handle differences in CallMode
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mIVVoice = (ImageView) findViewById(R.id.iv_voice_switch);
        mIVVoice.setOnClickListener(this);

        //debug按钮
        mTVDebug = (TextView) findViewById(R.id.tv_debug);
        if (AppConstants.DEBUG) {
            mTVDebug.setVisibility(View.VISIBLE);
            mTVDebug.setOnClickListener(this);
        }


        // Set up PTT button.
        mLLPttArea = findViewById(R.id.ll_ptt_area);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mLLPttArea.getLayoutParams();
        WindowManager wm = this.getWindowManager();
        params.height = wm.getDefaultDisplay().getHeight() * 36 / 180;
        mLLPttArea.setLayoutParams(params);
        mTVPttTimer = (TextView) findViewById(R.id.tv_ptt_timer);
        mIVPtt = (ImageView) findViewById(R.id.iv_ptt);
        mIVPttCirc = (ImageView) findViewById(R.id.iv_ptt_circle);
        FrameLayout flPtt = (FrameLayout) findViewById(R.id.fl_ptt);
        findViewById(R.id.fl_ptt);
        flPtt.setOnTouchListener(new View.OnTouchListener() {
            //private static final int TOGGLE_INTERVAL = 250; // 250ms is the interval needed to toggle push to talk.
            //private long lastTouch = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mService == null) {
                    return false;
                }

                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            ArrayList<String> permissions = new ArrayList<String>();
                            permissions.add(Manifest.permission.RECORD_AUDIO);
                            requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
                        } else {
                            mService.userPressDown();
                        }
                    } else {
                        mService.userPressDown();
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mService.userPressUp();
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    mService.userPressUp();
                }

                return true; // We return true so that the selector that changes the background does not fire.
            }
        });

        mIVHoriVolume = (ImageView) findViewById(R.id.iv_volume);

        mIVHistory = (ImageView) findViewById(R.id.iv_history);
        mIVHistory.setOnClickListener(this);
        mIVHeadset = (ImageView) findViewById(R.id.iv_headset);
        mIVHeadset.setOnClickListener(this);
        mIVHandmic = (ImageView) findViewById(R.id.iv_handmic);
        mIVHandmic.setOnClickListener(this);

        //首次运行，显示帮助信息
        boolean firstUse = AppSettings.getInstance(this).getFirstUse();
        if (firstUse) {
            help();
            AppSettings.getInstance(this).setFirstUse(false);
        }

        getPermissions();

        String makeChecksum1 = makeChecksum(answer);
        Log.e("123",makeChecksum1+"  123");
    }

    public void setIsMap(boolean isMapOrNo) {
        isMap = isMapOrNo;
    }

    @Override
    public void onResume() {
        Intent bi = new Intent();
        bi.setAction(LibConstants.ACTION_FLOAT_WINDOW_HIDE);
        this.sendBroadcast(bi);
        isScreenOn = true;

        //mService.activityShowing(false);
        //关闭悬窗。在serviceConnected和onResume都需要调用close，因为resume时可能service仍为空，而按home再进入时，不会调用bindService
        if (mService != null) {
            //声音开关在pause期间，可能被改变了
            boolean voiceOn = mService.getVoiceOn();
            mIVVoice.setImageResource(voiceOn ? R.drawable.voice_on : R.drawable.voice_off);
        }

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        isScreenOn = false;
        Intent bi = new Intent();
        bi.setAction(LibConstants.ACTION_FLOAT_WINDOW_SHOW);
        this.sendBroadcast(bi);
    }

    @Override
    protected void onDestroy() {

        // Unbind to service
        //为保证service不被杀死，activity在back按键时，只pause，不destroy。
        //那么，如果发现destroy，则应检查是否是用户关闭的。如果不是，则应重新启动activity
        //此时，说明activity不是用户退出的，而是被系统或其他应用杀死的。
        //应通知service，让其稍后重启activity

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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            boolean selecting = mService != null && mService.isSelectingContact();
            if (selecting) {
                mService.cancelSelect();
                return true;
            }
        } else {
            if (mService != null) {
                int savedCode = mService.getPttKeycode();
                if (savedCode != 0 && savedCode == keyCode) {
                    //至此，说明需要响应ptt事件

                    mService.userPressDown();
                    return true;
                }
            } else {
                return false;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mService != null) {
            int savedCode = mService.getPttKeycode();
            if (savedCode != 0 && savedCode == keyCode) {
                mService.userPressUp();
                super.onKeyUp(keyCode, event);
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    protected abstract void serviceConnected();

    protected abstract int getContentViewId();

    private static boolean actived = false;
    private boolean next = false;

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
                            mTVConnect.setVisibility(View.GONE);
                        }
                    });
                    break;
                case CONNECTION_STATE_DISCONNECTED:
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            refreshVolumeWave((short) 0);
                        }
                    });
                    break;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mService != null) {
                        refreshMicState(mService.getMicState());
                        refreshLcdChannel();
                        showConnection();
                    }
                }
            });

            if (mService != null) {
                mService.userPressUp();
            }
        }

        @Override
        public void onPermissionDenied(String reason, int denyType) throws RemoteException {
            AppCommonUtil.showToast(BaseActivity.this, AppConstants.permReason[denyType]);
        }

        @Override
        public void onCurrentChannelChanged() throws RemoteException {
            if (mService == null) {
                return;
            }

            if (mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
                refreshLcdChannel();
                //zcx add
                refreshMicState(mService.getMicState());
                //如果按着ptt时切换频道，则需先放弃讲话
                mService.userPressUp();
            }
        }

        @Override
        public void onChannelAdded(Channel channel) throws RemoteException {
        }

        @Override
        public void onChannelRemoved(Channel channel) throws RemoteException {
        }

        //某个频道信息有变化
        @Override
        public void onChannelUpdated(Channel channel) throws RemoteException {
        }

        @Override
        public void onUserUpdated(User user) throws RemoteException {
        }

        @Override
        public void onUserTalkingChanged(User user, boolean talk) throws RemoteException {
        }

        @Override
        public void onLocalUserTalkingChanged(User user, boolean talk) throws RemoteException {
            Log.e("actived", talk + "  123");
            if (!actived) {
                if (talk && !next) {
                    //String dat = "A5 01 0C 21 01 00 2C";
                    String dat = "AA 55 00 04 00 F1 02 01 09";
                    sendHexString(dat.replaceAll("\\s*", ""), "232");
                    Log.e("PTT_ON", "AA 55 00 04 00 F1 02 01 09" + "  七个值");
                    next = true;
                    return;
                } else if (!talk && next) {
                    String date = "AA 55 00 04 00 F1 02 02 08";
                    sendHexString(date.replaceAll("\\s*", ""), "232");
                    Log.e("PTT_OFF", "AA 55 00 04 00 F1 02 02 08" + "  七个值");
                    next = false;
                    return;
                }
            }
            actived = false;
            refreshLcdChannel();
        }

        @Override
        public void onNewVolumeData(short volume) throws RemoteException {
            refreshVolumeWave(volume);
        }

        @Override
        public void onMicStateChanged(MicState s) throws RemoteException {
            refreshMicState(s);
        }

        @Override
        public void onHeadsetStateChanged(HeadsetState s) throws RemoteException {
            refreshHeadsetIcon();
        }

        @Override
        public void onScoStateChanged(int s) throws RemoteException {
            refreshHeadsetIcon();
        }

        @Override
        public void onTargetHandmicStateChanged(BluetoothDevice device, HandmicState s) throws RemoteException {
            refreshHandmicIcon();
        }

        @Override
        public void onTalkingTimerTick(int seconds) throws RemoteException {
            String time = String.format("%02d", seconds / 60) + ":" + String.format("%02d", seconds % 60);
            mTVPttTimer.setText(time);
        }

        @Override
        public void onTalkingTimerCanceled() throws RemoteException {
            mTVPttTimer.setText(getString(R.string.push_to_talk));
        }

        @Override
        public void onUserAdded(final User u) throws RemoteException {
        }

        @Override
        public void onUserRemoved(final User user) throws RemoteException {
        }

        @Override
        public void onLeDeviceScanStarted(boolean start) throws RemoteException {
            refreshHandmicIcon();
        }

        @Override
        public void onShowToast(final String str) throws RemoteException {
            AppCommonUtil.showToast(BaseActivity.this, str);
        }

        @Override
        public void onInvited(final Channel chan) throws RemoteException {
            //mayNotifyInvitation();
        }

        @Override
        public void onUserSearched(User user) throws RemoteException {

        }

        @Override
        public void onPendingMemberChanged() throws RemoteException {
        }

        @Override
        public void onApplyContactReceived(boolean add, Contact contact) throws RemoteException {
        }

        @Override
        public void onPendingContactChanged() throws RemoteException {
        }

        @Override
        public void onContactChanged() throws RemoteException {

        }

        @Override
        public void onSynced() throws RemoteException {

        }

        @Override
        public void onVoiceToggleChanged(boolean on) throws RemoteException {
            mIVVoice.setImageResource(on ? R.drawable.voice_on : R.drawable.voice_off);
        }

        @Override
        public void onListenChanged(boolean listen) throws RemoteException {
            refreshLcdChannel();
        }

        @Override
        public void onApplyOrderResult(int uid, int cid, String phone, boolean success) throws RemoteException {

        }

        @Override
        public void onUserOrderCall(final User user, boolean talk, String number) {
        }

        @Override
        public void onGeneralMessageGot(final int type, final String content) throws RemoteException {
        }
    };

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        int id = v.getId();

        if (id == R.id.iv_voice_switch) {
            if (mService != null) {
                mService.toggleVoiceOn();
            }
        } else if (id == R.id.iv_handmic) {
            handMicActivity();
        } else if (id == R.id.iv_history) {
            if (mService != null && mService.getCurrentChannel() != null) {
                Intent intentshowChatUi = new Intent(this, RecyclerViewChatActivity.class);
                //同时启动chat界面
                intentshowChatUi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (isMap) {
                    intentshowChatUi.putExtra("SOURCE", "OPEN");
                } else {
                    if (mService.getAllowAcceptOrder()) {
                        intentshowChatUi.putExtra("SOURCE", "OPEN");
                    } else {
                        intentshowChatUi.putExtra("SOURCE", "NULL");
                    }
                }
                startActivity(intentshowChatUi);
            }
        } else if (id == R.id.iv_headset) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (id == R.id.tv_connection_lost) {
            help();
        } else if (id == R.id.tv_debug) {
            String debugInfo = mService.getDebug();
            EditText et = new EditText(BaseActivity.this);
            et.setText(debugInfo);
            et.setTextSize(12);
            new AlertDialog.Builder(BaseActivity.this)
                    .setView(et)
                    .setPositiveButton(getString(R.string.close), null)
                    .show();
        }
    }

    protected void handMicActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Intent i = new Intent(BaseActivity.this, DeviceScanActivity.class);
            startActivity(i);
        } else {
            AppCommonUtil.showToast(this, "蓝牙外设只支持Android 4.3及以上版本");
        }
    }

    private void refreshHeadsetIcon() {
        if (mService == null) {
            return;
        }
        int sco = mService.getScoState();
        if (sco == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
            mIVHeadset.setImageResource(R.drawable.headset_recording);
        } else {
            HeadsetState s = mService.getHeadsetState();
            if (s == HeadsetState.HEADSET_CONNECTED) {
                mIVHeadset.setImageResource(R.drawable.headset_connected);
                boolean scoRecording = mService.getSupportScoRecording();
            } else {
                mIVHeadset.setImageResource(R.drawable.headset_disconnected);
            }
        }
    }

    private void refreshHandmicIcon() {
        if (mService == null) {
            return;
        }

        InterpttService.HandmicState s = mService.getTargetHandmicState();
        switch (s) {
            case HANDMIC_CONNECTED:
                mIVHandmic.setImageResource(R.drawable.handmic_connected);
                mIVHandmic.clearAnimation();
                break;
            case HANDMIC_CONNECTING:
                mIVHandmic.setImageResource(R.drawable.handmic_connected);
                mIVHandmic.startAnimation(AppCommonUtil.createTalkingAnimation());
                break;
            case HANDMIC_DISCONNECTED:
                mIVHandmic.setImageResource(R.drawable.handmic_disconnected);
                //再看是否在扫描期间
                boolean scanning = mService.getIsBleScanning();
                if (scanning) {
                    mIVHandmic.startAnimation(AppCommonUtil.createTalkingAnimation());
                } else {
                    mIVHandmic.clearAnimation();
                }
                break;
        }
    }

    private void refreshMicState(MicState state) {
        boolean ready = true;

        if (mService == null) {
            ready = false;
        } else if (mService.getConnectionState() != CONNECTION_STATE_CONNECTED) {
            ready = false;
        } else {
            Channel c = mService.getCurrentChannel();
            if (c == null || c.id == 0) {
                ready = false;
            }
        }

        if (!ready) {
            mIVPtt.setImageResource(R.drawable.ic_ptt_up);
            mIVPttCirc.setImageResource(R.drawable.ic_ptt_circle_noready);
        } else {
            switch (state) {
                case MIC_READY:
                    mIVPtt.setImageResource(R.drawable.ic_ptt_up);
                    mIVPttCirc.setImageResource(R.drawable.ic_ptt_circle_ready);
                    break;
                case MIC_GIVINGBACK:
                    break;
                case MIC_APPLYING:

                    //注意：申请期间，按键是按下状态，模仿真实按钮
                    mIVPtt.setImageResource(R.drawable.ic_ptt_down);
                    mIVPttCirc.setImageResource(R.drawable.ic_ptt_circle_applying);
                    break;
                case MIC_OPENING_SCO:
                    mIVPtt.setImageResource(R.drawable.ic_ptt_down);
                    mIVPttCirc.setImageResource(R.drawable.ic_ptt_circle_opening_sco);
                    break;
                case MIC_TALKING:
                    mIVPtt.setImageResource(R.drawable.ic_ptt_down);
                    mIVPttCirc.setImageResource(R.drawable.ic_ptt_circle_talking);
                    break;
            }
        }
    }

    private void refreshVolumeWave(short volume) {
        int horiVol = volume / 1024;    //32768max, 分20级别，适当增加点级别，取1500
        if (isScreenOn) {
            mIVHoriVolume.setImageLevel(horiVol);
        }
    }

    private void refreshLcdChannel() {
        if (mService == null) {
            return;
        }

        Channel c = mService.getCurrentChannel();
        if (c == null || c.id == 0) {
            mTVCurrentChanName.setText(getString(R.string.idle));
        } else {
            mTVCurrentChanName.setText(c.name);
        }

        Channel lc = mService.getListenChannel();
        if (lc == null) {
            mTVListenChanName.setText("");
        } else {
            mTVListenChanName.setText(lc.name);
        }
        //显示历史讲话者
        String historyTalker0 = mService.getHistoryTalker0();
        if (historyTalker0 != null) {
            mTVHistoryTalker0.setText(historyTalker0);
        }
        String historyTalker1 = mService.getHistoryTalker1();
        if (historyTalker1 != null) {
            mTVHistoryTalker1.setText(historyTalker1);
        }
        String historyTalker2 = mService.getHistoryTalker2();
        if (historyTalker2 != null) {
            mTVHistoryTalker2.setText(historyTalker2);
        }

        refreshCurrentVoiceTalker();
    }

    private void refreshCurrentVoiceTalker() {
        //先清空
        mIVCurrentChanTalkerDevice.setVisibility(View.INVISIBLE);
        mIVListenChanTalkerDevice.setVisibility(View.INVISIBLE);

        mTVCurrentChanTalker.setText("");
        mTVCurrentChanTalker.setVisibility(View.INVISIBLE);
        mTVListenChanTalker.setText("");
        mTVListenChanTalker.setVisibility(View.INVISIBLE);

        if (mService == null) {
            return;
        }
        User talker = mService.getcurrentVoiceTalker();

        //刷新talkinguser
        if (talker != null) {
            //open = true;

            Channel tc = talker.getChannel();
            Channel cc = mService.getCurrentChannel();
            if (tc != null && cc != null && tc.id == cc.id) {
                //在同一频道
                //设备图标
                mIVCurrentChanTalkerDevice.setVisibility(View.VISIBLE);
                mIVCurrentChanTalkerDevice.setImageLevel(talker.audioSource);

                mTVCurrentChanTalker.setText(talker.nick);
                mTVCurrentChanTalker.setVisibility(View.VISIBLE);
            }

            //可能监听本频道。此时两个都显示
            Channel lc = mService.getListenChannel();
            if (tc != null && lc != null && tc.id == lc.id) {
                //在监听频道
                mIVListenChanTalkerDevice.setVisibility(View.VISIBLE);
                mIVListenChanTalkerDevice.setImageLevel(talker.audioSource);

                mTVListenChanTalker.setText(talker.nick);
                mTVListenChanTalker.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showConnection() {
        ConnState connState = mService.getConnectionState();
        if (connState == CONNECTION_STATE_CONNECTED) {
            mTVConnect.setVisibility(View.GONE);
        } else if (connState == CONNECTION_STATE_CONNECTING || connState == CONNECTION_STATE_SYNCHRONIZING) {
            mTVConnect.setText(getString(R.string.syncing));
            mTVConnect.setVisibility(View.VISIBLE);
        } else {
            //此时可能是暂时断线，也可能是其他设备登录而被踢掉
            if (mService.getDisconnectReason() == DisconnectReason.Kick) {
                mTVConnect.setText(getString(R.string.be_kicked));
            } else {
                mTVConnect.setText(R.string.net_fail_retry);
            }

            mTVConnect.setVisibility(View.VISIBLE);
        }
    }

    private static final int SDK_PERMISSION_REQUEST = 127;

    @TargetApi(23)
    protected void getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            // 麦克风权限
            addPermission(permissions, Manifest.permission.RECORD_AUDIO);
            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
            }
        }
    }

    @TargetApi(23)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
            if (shouldShowRequestPermissionRationale(permission)) {
                return true;
            } else {
                permissionsList.add(permission);
                return false;
            }

        } else {
            return true;
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case SDK_PERMISSION_REQUEST:
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                if (perms.get(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    AppCommonUtil.showToast(this, R.string.need_mic_permission);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected void help() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.app_help, null);
        TextView tvContactUs = layout.findViewById(R.id.tv_contact_us);
        tvContactUs.setVisibility(View.GONE);

        builder.setTitle(R.string.help);
        builder.setView(layout);

        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }
}
