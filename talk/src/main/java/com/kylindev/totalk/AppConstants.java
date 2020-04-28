package com.kylindev.totalk;

import android.graphics.Bitmap;
import android.graphics.Color;

public class AppConstants {
	public final static String DEFAULT_PERSONAL_HOST = "";
	public final static boolean DEBUG = false;

	public final static int SEND_LOC_INTERVAL = 10;
	public final static int LOCATION_MAX_INTERVAL_SECONDS = 300;	//位置不变时，减少上报次数，但是仍然需要一个最大时间
	public final static double LOCATION_DIFF_DISTANCE = 5.0;	//位置不变时，减少上报次数。如果位置偏离超过此数值，认为位置变化了

	//用户名和密码的正则表达式
	public static final int NAME_MAX_LENGTH = 512;	//频道和用户名称的最大长度
	public static final String EX_CHANNELNAME = "[ \\-=\\w\\#\\[\\]\\{\\}\\(\\)\\@\\|]+";	//从服务器copy过来，与服务器保持一致
	public static final String EX_PASSWORD = "\\w{6,32}+";				//6-32位，不含空格
	public static final String EX_CHANNEL_PASSWORD = "^\\d{4}$";				//4-16位，不含空格
	public static final String EX_NICK = "[-=\\w\\[\\]\\{\\}\\(\\)\\@\\|\\. ]+";		//允许点和空格
	public static final String EX_VERIFY_CODE = "^\\d{4}$";		//4位数字

	public final static int CURRENT_NICK_COLOR = Color.rgb(29, 103, 203);	//本人名字高亮显示颜色
	public final static int OTHER_NICK_COLOR = Color.rgb(0x33, 0x33, 0x33);	//别人名字

	//以下来自原来的Globals.java
	public static final String LOG_TAG = "Totalk";

	public enum NETWORK_STATE {DISCONNECT, WIFI, MOBILE};

	public static final String[] permReason =
			{
					"没有权限", "频道名称格式错误", "用户名格式错误", "频道人数已满", "频道已过期" ,
					"单个用户创建频道数已达上限", "频道不存在", "频道口令错误"
			};
}
