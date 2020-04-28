package com.kylindev.totalk;

import android.content.Context;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;

//import com.baidu.mapapi.CoordType;
//import com.baidu.mapapi.SDKInitializer;


public class MainApp extends android.app.Application {
	private static MainApp instance;
	
	public MainApp() { 
		instance = this;
	}

	public static Context getContext() {
		if (instance != null) {
			return instance.getApplicationContext();
		}
		
		return null;
	}


	@Override
	public void onCreate() {
		super.onCreate();

//		 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
		SDKInitializer.initialize(this);
		//自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
		//包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
		SDKInitializer.setCoordType(CoordType.BD09LL);
	}
}
