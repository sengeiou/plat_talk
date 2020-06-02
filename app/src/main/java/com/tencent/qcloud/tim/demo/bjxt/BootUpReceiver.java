package com.tencent.qcloud.tim.demo.bjxt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tencent.qcloud.tim.demo.SplashActivity;

/**
 * @author  BJXT-QGS
 * @version  1.0
 * @date  2019/9/10 10:08
 * @description  广播接收类，用于开机自动启动界面
 */
public class BootUpReceiver extends BroadcastReceiver {
    /**
     * 如果BroadcastReceiver的onReceive（）的方法不能在10秒内执行完成，android会
     * 认为该程序无响应，会弹出AND(Application No Response)对话框
     * @param context 上下文
     * @param intent 意图，这里是收到广播后将要开启的activity
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("XRGPS", "BootReceiver.onReceive: " + intent.getAction());
        System.out.println("自启动程序即将执行");
        Intent i = new Intent(context, SplashActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
