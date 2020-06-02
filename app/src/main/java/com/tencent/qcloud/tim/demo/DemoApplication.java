package com.tencent.qcloud.tim.demo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.huawei.android.hms.agent.HMSAgent;
import com.kylindev.totalk.app.BaseActivity;
import com.meizu.cloud.pushsdk.PushManager;
import com.meizu.cloud.pushsdk.util.MzSystemUtils;
import com.squareup.leakcanary.LeakCanary;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.imsdk.TIMBackgroundParam;
import com.tencent.imsdk.TIMCallBack;
import com.tencent.imsdk.TIMConversation;
import com.tencent.imsdk.TIMConversationType;
import com.tencent.imsdk.TIMManager;
import com.tencent.imsdk.TIMMessage;
import com.tencent.imsdk.TIMOfflinePushNotification;
import com.tencent.imsdk.TIMTextElem;
import com.tencent.imsdk.TIMValueCallBack;
import com.tencent.imsdk.session.SessionWrapper;
import com.tencent.imsdk.utils.IMFunc;
import com.tencent.openqq.protocol.imsdk.msg;
import com.tencent.qcloud.tim.demo.bjxt.SerialPortApplication;
import com.tencent.qcloud.tim.demo.bjxt.util.HexUtil;
import com.tencent.qcloud.tim.demo.helper.ConfigHelper;
import com.tencent.qcloud.tim.demo.helper.CustomAVCallUIController;
import com.tencent.qcloud.tim.demo.helper.CustomMessage;
import com.tencent.qcloud.tim.demo.signature.GenerateTestUserSig;
import com.tencent.qcloud.tim.demo.thirdpush.ThirdPushTokenMgr;
import com.tencent.qcloud.tim.demo.utils.DemoLog;
import com.tencent.qcloud.tim.demo.utils.PrivateConstants;
import com.tencent.qcloud.tim.uikit.TUIKit;
import com.tencent.qcloud.tim.uikit.base.IMEventListener;
import com.vivo.push.PushClient;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android_serialport_api.SerialPortUtil;
import util.MyUtil;

public class DemoApplication extends SerialPortApplication {

    private static final String TAG = DemoApplication.class.getSimpleName();

    private static DemoApplication instance;
    public static String mCommand;
    String setup = "AA 55 00 16 00 F3 12 34 56 78 A0 08 12 34 56 FF FF FF FF FF FF FF FF FF FF FF";

    public static DemoApplication instance() {
        return instance;
    }
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
    @Override
    public void onCreate() {
        DemoLog.i(TAG, "onCreate");
        super.onCreate();
        instance = this;
        // bugly上报
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(getApplicationContext());
        strategy.setAppVersion(TIMManager.getInstance().getVersion());
        CrashReport.initCrashReport(getApplicationContext(), PrivateConstants.BUGLY_APPID, true, strategy);

        //判断是否是在主线程
        if (SessionWrapper.isMainProcess(getApplicationContext())) {
            /**
             * TUIKit的初始化函数
             *
             * @param context  应用的上下文，一般为对应应用的ApplicationContext
             * @param sdkAppID 您在腾讯云注册应用时分配的sdkAppID
             * @param configs  TUIKit的相关配置项，一般使用默认即可，需特殊配置参考API文档
             */
            TUIKit.init(this, GenerateTestUserSig.SDKAPPID, new ConfigHelper().getConfigs());

            if (ThirdPushTokenMgr.USER_GOOGLE_FCM) {
                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(Task<InstanceIdResult> task) {
                                if (!task.isSuccessful()) {
                                    DemoLog.w(TAG, "getInstanceId failed exception = " + task.getException());
                                    return;
                                }

                                // Get new Instance ID token
                                String token = task.getResult().getToken();
                                DemoLog.i(TAG, "google fcm getToken = " + token);

                                ThirdPushTokenMgr.getInstance().setThirdPushToken(token);
                            }
                        });
            } else if (IMFunc.isBrandXiaoMi()) {
                // 小米离线推送
                MiPushClient.registerPush(this, PrivateConstants.XM_PUSH_APPID, PrivateConstants.XM_PUSH_APPKEY);
            } else if (IMFunc.isBrandHuawei()) {
                // 华为离线推送
                HMSAgent.init(this);
            } else if (MzSystemUtils.isBrandMeizu(this)) {
                // 魅族离线推送
                PushManager.register(this, PrivateConstants.MZ_PUSH_APPID, PrivateConstants.MZ_PUSH_APPKEY);
            } else if (IMFunc.isBrandVivo()) {
                // vivo离线推送
                PushClient.getInstance(getApplicationContext()).initialize();
            }

            registerActivityLifecycleCallbacks(new StatisticActivityLifecycleCallback());
        }
//        if (BuildConfig.DEBUG) {
//            if (LeakCanary.isInAnalyzerProcess(this)) {
//                return;
//            }
//            LeakCanary.install(this);
//        }
        CustomAVCallUIController.getInstance().onCreate();
        IMEventListener imEventListener = new IMEventListener() {
            @Override
            public void onNewMessages(List<TIMMessage> msgs) {
                DemoLog.i(TAG, "onNewMessages");
                CustomAVCallUIController.getInstance().onNewMessage(msgs);

                //发送串口数据
                TIMTextElem textElem = (TIMTextElem) msgs.get(0).getElement(0);
                Log.e("wocao", textElem.getText());

                mCommand = textElem.getText();
                int length = mCommand.length();
                Log.i("length",length+"  123");
                if (mCommand.length()>12) {
                    String setUp = setup.replaceAll(" ", "");
                    int total = 0;
                    for (int i = 0;i<setUp.length();i+=2){
                        //strB.append("0x").append(strData.substring(i,i+2));  //0xC30x3C0x010x120x340x560x780xAA
                        total = total + Integer.parseInt(setUp.substring(i,i+2),16);
                    }
                    //noTotal为累加和取反加一
                    int noTotal = ~total + 1;
                    Log.i("total",String.valueOf(noTotal));
                    //负整数时，前面输入了多余的 FF ，没有去掉前面多余的 FF，按并双字节形式输出
                    //0xFF会像转换成0x000000FF后再进行位运算
                    String hex = Integer.toHexString(noTotal).toUpperCase();
                    Log.i("TAGhex",hex);
                    String key = hex.substring(hex.length()-2);
                    Log.i("TAG校验码key",key);
                    Log.i("TAGhex",key);
                    //将求得的最后两位拼接到setup字符串后面
                    String s = setUp + key;
                    Log.e("setUp",setUp+"    00");
                    Log.e("s",s+"    00");
                    sendHexString(s.replaceAll("\\s*", ""), "232");
                }

                //SerialPortUtil.open("/dev/ttyS3",19200,0);

                //发送串口
                if (mCommand.equals("停车")) {
                    String setUp = setup.replaceAll(" ", "");
                    int total = 0;
                    for (int i = 0;i<setUp.length();i+=2){
                        //strB.append("0x").append(strData.substring(i,i+2));  //0xC30x3C0x010x120x340x560x780xAA
                        total = total + Integer.parseInt(setUp.substring(i,i+2),16);
                    }
                    //noTotal为累加和取反加一
                    int noTotal = ~total + 1;
                    Log.i("total",String.valueOf(noTotal));
                    //负整数时，前面输入了多余的 FF ，没有去掉前面多余的 FF，按并双字节形式输出
                    //0xFF会像转换成0x000000FF后再进行位运算
                    String hex = Integer.toHexString(noTotal).toUpperCase();
                    Log.i("TAGhex",hex);
                    String key = hex.substring(hex.length()-2);
                    Log.i("TAG校验码key",key);
                    Log.i("TAGhex",key);
                    //将求得的最后两位拼接到setup字符串后面
                    String s = setUp + hex;
                    Log.e("s",s+"    00");
                    sendHexString(s.replaceAll("\\s*", ""), "232");
                    //ack(msgs.get(0).getSender(), mCommand);
                }
                if (mCommand.equals("推进")) {
                    //A5 = "41";
                    String mDat = "A5  01  0B  06  41  01  FA";
                    Log.e("mDat", mDat + "  七个值");
                    //SerialPortUtil.sendString(mDat);
                    //sendHexString(mDat.replaceAll("\\s*", ""), "232");
                    //ack(msgs.get(0).getSender(), mCommand);
                }
                if (mCommand.equals("十车")) {
                    //A5 = "71";
                    String mDat = "A5  01  0B  06  71  01  09";
                    Log.e("mDat", mDat + "  七个值");
                    //sendHexString(mDat.replaceAll("\\s*", ""), "232");
                    //SerialPortUtil.sendString(mDat);
                    //ack(msgs.get(0).getSender(), mCommand);
                }
            }
        };
        TUIKit.addIMEventListener(imEventListener);
    }

    String signallingACK = "AA 55 00 04 80 F3 08 00 82";
    String answer = "AA 55 00 16 00 F4 12 34 56 78 A0 88 12 34 56 FF FF FF FF FF FF FF FF FF FF FF";
    String request = "AA 55 00 16 00 F4 12 34 56 78 A0 0D 12 34 56 FF FF FF FF FF FF FF FF FF FF FF";

    @Override
    protected void onDataReceived(byte[] buffer, int size, int type) {
        Log.e("qq","qq");
        String encodeHexStr = util.HexUtil.encodeHexStr(buffer, false, size);
        //截取接收的十六进制后两位
        String substring = encodeHexStr.substring(encodeHexStr.length() - 2, encodeHexStr.length());

        //判断接收的数据是否含有数字或者特殊字符
        boolean specialChar = isSpecialChar(encodeHexStr);
        boolean messyCode = isMessyCode(encodeHexStr);
        if (specialChar && messyCode) {
            //Toast.makeText(getApplicationContext(), "包含特殊字符", Toast.LENGTH_SHORT).show();
            Log.e("包含", "包含");
        } else if (!specialChar && !messyCode) {
            String signallingACK1 = signallingACK.replaceAll(" ", "");
            String answer1 = answer.replaceAll(" ", "");
            String request1 = request.replaceAll(" ", "");
            int total = 0;
            for (int i = 0;i<answer1.length();i+=2){
                //strB.append("0x").append(strData.substring(i,i+2));  //0xC30x3C0x010x120x340x560x780xAA
                //计算16进制累加和
                total = total + Integer.parseInt(answer1.substring(i,i+2),16);
            }
            //noTotal为累加和取反加一
            int noTotal = ~total + 1;
            Log.i("total",total+" ");
            Log.i("total",String.valueOf(noTotal));
            //负整数时，前面输入了多余的 FF ，没有去掉前面多余的 FF，按并双字节形式输出
            //0xFF会像转换成0x000000FF后再进行位运算
            String hex = Integer.toHexString(noTotal).toUpperCase();
            Log.i("TAGhex",hex);
            String key = hex.substring(hex.length()-2);
            Log.i("TAG校验码key",key);
            Log.i("TAGhex",key);

            int sum = 0;
            for (int i = 0;i<request1.length();i+=2){
                //strB.append("0x").append(strData.substring(i,i+2));  //0xC30x3C0x010x120x340x560x780xAA
                sum = sum + Integer.parseInt(request1.substring(i,i+2),16);
            }
            //nototal为累加和取反加一
            int nototal = ~sum + 1;
            Log.i("total",sum+" ");
            Log.i("total",String.valueOf(nototal));
            //负整数时，前面输入了多余的 FF ，没有去掉前面多余的 FF，按并双字节形式输出
            //0xFF会像转换成0x000000FF后再进行位运算
            String hex1 = Integer.toHexString(nototal).toUpperCase();
            Log.i("TAGhex",hex1);
            String key1 = hex.substring(hex1.length()-2);
            Log.i("TAG校验码key",key1);
            Log.i("TAGhex",key1);

            if (encodeHexStr.equals(signallingACK1)) {
                Log.e("车台回的ack", "车台回的ack");
            } else if (substring.equals(key)) {
                Log.e("列尾ID设置应答(输号应答)", "列尾ID设置应答(输号应答)");
                String mDat = "AA 55 00 04 80 F4 88 00 01";
                //sendHexString(mDat.replaceAll("\\s*", ""), "232");
                sendMessage("1001025","ok");
            } else if (substring.equals(key1)) {
                Log.e("列尾主机输号请求", "列尾主机输号请求");
                String mDat = "AA 55 00 04 80 F4 0D 00 01 7B";
                //sendHexString(mDat.replaceAll("\\s*", ""), "232");
                sendMessage("1001026","ok");
            }

            BaseActivity.onReceived(buffer, size, type);
        }
    }

    byte formData[] = new byte[1024];
    int len232 = 0;//接收到的数据长度
    int len = 0;//数据总长度
    boolean isStart232 = false;
    private String mEncodeHexStr;

    private void ack(String id, String s) {
        String peer = id;
        final TIMConversation conversation = TIMManager.getInstance().getConversation(
                TIMConversationType.C2C,    //会话类型：单聊
                peer);                      //会话对方用户帐号//对方ID
        final TIMMessage msg = new TIMMessage();
        //添加文本内容
        TIMTextElem elem = new TIMTextElem();
        elem.setText(s);

        //将elem添加到消息
        if (msg.addElement(elem) != 0) {
            Log.d("wocao", "addElement failed");
            return;
        }

        sendMessage(conversation, msg);

    }

    private void sendMessage(TIMConversation conversation, TIMMessage msg) {
        conversation.sendMessage(msg, new TIMValueCallBack<TIMMessage>() {//发送消息回调
            @Override
            public void onError(int code, String desc) {//发送消息失败
                //错误码 code 和错误描述 desc，可用于定位请求失败原因
                //错误码 code 含义请参见错误码表
                Log.d("wocao", "send message failed. code: " + code + " errmsg: " + desc);
            }

            @Override
            public void onSuccess(TIMMessage msg) {//发送消息成功
                Log.e("wocao", "SendMsg ok");
            }
        });
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

    class StatisticActivityLifecycleCallback implements ActivityLifecycleCallbacks {
        private int foregroundActivities = 0;
        private boolean isChangingConfiguration;
        private IMEventListener mIMEventListener = new IMEventListener() {
            @Override
            public void onNewMessages(List<TIMMessage> msgs) {
                if (CustomMessage.convert2VideoCallData(msgs) != null) {
                    // 会弹出接电话的对话框，不再需要通知
                    return;
                }
                for (TIMMessage msg : msgs) {
                    // 小米手机需要在设置里面把demo的"后台弹出权限"打开才能点击Notification跳转。TIMOfflinePushNotification后续不再维护，如有需要，建议应用自己调用系统api弹通知栏消息。
                    TIMOfflinePushNotification notification = new TIMOfflinePushNotification(DemoApplication.this, msg);
                    notification.doNotify(DemoApplication.this, R.drawable.default_user_icon);
                }
            }
        };

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
            DemoLog.i(TAG, "onActivityCreated bundle: " + bundle);
            if (bundle != null) { // 若bundle不为空则程序异常结束
                // 重启整个程序
                Intent intent = new Intent(activity, SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
            foregroundActivities++;
            if (foregroundActivities == 1 && !isChangingConfiguration) {
                // 应用切到前台
                DemoLog.i(TAG, "application enter foreground");
                TIMManager.getInstance().doForeground(new TIMCallBack() {
                    @Override
                    public void onError(int code, String desc) {
                        DemoLog.e(TAG, "doForeground err = " + code + ", desc = " + desc);
                    }

                    @Override
                    public void onSuccess() {
                        DemoLog.i(TAG, "doForeground success");
                    }
                });
                TUIKit.removeIMEventListener(mIMEventListener);
            }
            isChangingConfiguration = false;
        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {
            foregroundActivities--;
            if (foregroundActivities == 0) {
                // 应用切到后台
                DemoLog.i(TAG, "application enter background");
                int unReadCount = 0;
                List<TIMConversation> conversationList = TIMManager.getInstance().getConversationList();
                for (TIMConversation timConversation : conversationList) {
                    unReadCount += timConversation.getUnreadMessageNum();
                }
                TIMBackgroundParam param = new TIMBackgroundParam();
                param.setC2cUnread(unReadCount);
                TIMManager.getInstance().doBackground(param, new TIMCallBack() {
                    @Override
                    public void onError(int code, String desc) {
                        DemoLog.e(TAG, "doBackground err = " + code + ", desc = " + desc);
                    }

                    @Override
                    public void onSuccess() {
                        DemoLog.i(TAG, "doBackground success");
                    }
                });
                // 应用退到后台，消息转化为系统通知
                TUIKit.addIMEventListener(mIMEventListener);
            }
            isChangingConfiguration = activity.isChangingConfigurations();
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }

    public void sendMessage(String uid, String s) {
        uid = "1001026";
        TIMConversation conversation = TIMManager.getInstance().getConversation(
                TIMConversationType.C2C,    //会话类型：单聊
                uid);                      //会话对方用户帐号//对方ID

        TIMMessage msg = new TIMMessage();
        //添加文本内容
        TIMTextElem elem = new TIMTextElem();
        elem.setText("ok");
        msg.addElement(elem);
        conversation.sendMessage(msg, new TIMValueCallBack<TIMMessage>() {//发送消息回调
            @Override
            public void onError(int code, String desc) {//发送消息失败
                //错误码 code 和错误描述 desc，可用于定位请求失败原因
                //错误码 code 含义请参见错误码表
                Log.d("wocao", "send message failed. code: " + code + " errmsg: " + desc);
            }

            @Override
            public void onSuccess(TIMMessage msg) {//发送消息成功
                Log.e("wocao", "SendMsg ok");
            }
        });

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
}
