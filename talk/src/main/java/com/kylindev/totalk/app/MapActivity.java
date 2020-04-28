package com.kylindev.totalk.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.kylindev.pttlib.LibConstants;
import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.model.Channel;
import com.kylindev.pttlib.service.model.User;
import com.kylindev.totalk.AppConstants;
import com.kylindev.totalk.R;
import com.kylindev.totalk.utils.AppCommonUtil;
import com.kylindev.totalk.utils.AppSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static com.kylindev.totalk.MainApp.getContext;
import static java.lang.System.currentTimeMillis;

public class  MapActivity extends BaseActivity implements View.OnClickListener, SensorEventListener {
    //private Spinner mSpinnerUser;
    //private User mSelectedUser = null; //记录spinner当前选择的用户。未选，或者选择"全部"时，为null
    //private List<User> userList;
    private MapView mMapView;
    private ImageView mIVGather, mIVSelfCenter, mIVApplyOrder, mIVCallPassenger;
    private TextView mTVWhoget, mTVCurrentChanTalkerBg;
    private BaiduMap mBaiduMap;
    private LatLng mAutoZoomLatLng;
    private float mAutoZoomLevel;   //记录自动缩放的中心和缩放倍数。监听地图回调时，如果发现跟设置的不同，说明用户操作过，不再自动缩放
    private boolean userTouched = false;
    private boolean selfCenter = false;
    private boolean isCanApplyOrder = false;
    private long lastTouchedTime = 0;
    private float selfCenterZoom = 15;

    private double selfLongitude;
    private double selfLatitude;
    private MapStatus mMapStatus;
    //地图定位
    private LocationClient mLocationClient = null;
    private MyLocationListener myLocListener = new MyLocationListener();
    SensorManager mSensorManager;
    private float myCurrentX, myDir;
    private String phoneCaller;
    private int mTimer = 10;
    //本对话中所有用户位置。记录这个，是为了刷新某用户的位置时，找到之前的overlay并删除
    private Map<Integer, Overlay> locationMarkers = new ConcurrentHashMap<Integer, Overlay>();  //用户实时位置，key是每个用户的id

    //    View rootView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**常亮*/
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setIsMap(true);
        findView();
        getPersimmions();
        //地图相关
        //声明LocationClient类
        mLocationClient = new LocationClient(getApplicationContext());
        //注册监听函数
        mLocationClient.registerLocationListener(myLocListener);
        //设置定位模块的参数
        LocationClientOption option = new LocationClientOption();
        //可选，设置定位模式，默认高精
        //LocationMode.Hight_Accuracy：高精度；
        //LocationMode. Battery_Saving：低功耗；
        //LocationMode. Device_Sensors：仅使用设备；
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);

        //可选，设置返回经纬度坐标类型，默认GCJ02
        //GCJ02：国测局坐标；
        //BD09ll：百度经纬度坐标；
        //BD09：百度墨卡托坐标；
        //海外地区定位，无需设置坐标类型，统一返回WGS84类型坐标
        option.setCoorType("BD09ll");

        //可选，设置发起定位请求的间隔，int类型，单位ms
        //如果设置为0，则代表单次定位，即仅定位一次，默认为0
        //如果设置非0，需设置1000ms以上才有效
        option.setScanSpan(AppConstants.SEND_LOC_INTERVAL * 1000);

        //可选，设置是否使用gps，默认false
        //使用高精度和仅用设备两种定位模式的，参数必须设置为true
        option.setOpenGps(true);
        //option.setIsNeedAddress(true);
        //可选，设置是否当GPS有效时按照1S/1次频率输出GPS结果，默认false
        option.setLocationNotify(false);

        //可选，定位SDK内部是一个service，并放到了独立进程。
        //设置是否在stop的时候杀死这个进程，默认（建议）不杀死，即setIgnoreKillProcess(true)
        option.setIgnoreKillProcess(true);

        //可选，设置是否收集Crash信息，默认收集，即参数为false
        option.SetIgnoreCacheException(false);

        //可选，V7.2版本新增能力
        //如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位
        option.setWifiCacheTimeOut(5 * 60 * 1000);

        //可选，设置是否需要过滤GPS仿真结果，默认需要，即参数为false
        option.setEnableSimulateGps(false);
        //mLocationClient为第二步初始化过的LocationClient对象
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        //更多LocationClientOption的配置，请参照类参考中LocationClientOption类的详细说明
        mLocationClient.setLocOption(option);
        mBaiduMap.getUiSettings().setRotateGesturesEnabled(false);
        mBaiduMap.getUiSettings().setCompassEnabled(false);
        mBaiduMap.getUiSettings().setOverlookingGesturesEnabled(false);
        // 获取传感器管理服务
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mBaiduMap.setMyLocationEnabled(true);//开启允许定位
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();//开启定位
        }

        //开启方向传感器
        // 为系统的方向传感器注册监听器
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);

        selfCenterZoom = AppSettings.getInstance(this).getSelfCenter();
        if (selfCenterZoom > 0) {
            selfCenter = true;
            mIVSelfCenter.setImageResource(R.drawable.ic_selfcenter_on);
        } else {
            selfCenter = false;
            mIVSelfCenter.setImageResource(R.drawable.ic_selfcenter_off);
        }
        mTVBarCount.setVisibility(View.VISIBLE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(LibConstants.BROADCAST_CLOSE_PHONEALERT);
        filter.addAction(LibConstants.BROADCAST_START_APPLY_ORDER);
        registerReceiver(closeReceiver, filter);
        //testHandler.postDelayed(testRunnable,500);
    }

    private boolean isapplyOrder;
    final Handler applyOrderHandler = new Handler();
    final Runnable applyOrderRunnable = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //要做的事情
            Log.d("uart","applyOrderRunnable");
            mIVApplyOrder.setVisibility(View.GONE);
            isapplyOrder = false;
        }

    };

        final Handler testHandler = new Handler();
        final Runnable testRunnable = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                //要做的事情
                testHandler.postDelayed(this, 100);
                if (mService != null) {
                    int uid = mService.getMyUserId();
                    double x = Math.random();
                    double y = Math.random();
                    drwMark(uid, selfLongitude + x, selfLatitude + y, myCurrentX);
                    setCenterself(selfLongitude + x, selfLatitude + y, selfCenterZoom);
                    if (locationMarkers != null) {
                        Channel c = mService.getCurrentChannel();
                        if (c != null) {
                            mTVBarTitle.setText(c.name);
                        }
                        mTVBarCount.setText("(" + locationMarkers.size() + ")");
                    }
                }
            }
        };


        private boolean isWhogetAdd = false;
        final Handler WhogetAddHandler = new Handler();
        final Runnable WhogetAddRunnable = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                //要做的事情
                mTVWhoget.setVisibility(View.GONE);
                isWhogetAdd = false;
            }
        };


        private BroadcastReceiver closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(LibConstants.BROADCAST_CLOSE_PHONEALERT)) {

                } else if (action.equals(LibConstants.BROADCAST_START_APPLY_ORDER)) {
                    isCanApplyOrder = true;
                    mIVApplyOrder.setImageDrawable(getResources().getDrawable(R.drawable.accept_call_pttlib));
                    applyOrderHandler.removeCallbacks(applyOrderRunnable);
                    applyOrderHandler.postDelayed(applyOrderRunnable, 10000);//10s执行一次runnable.
                }
            }
        };


        private String add;
        private Handler mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (add != null) {
                    //发送一个消息通知其他用户
                    if (add != null && mService != null) {
                        mService.sendGeneralMessage(3, add);
                    }
                }
            }

            ;
        };

        private void drwMark(int uid, Double lng, Double lat, float rotate) {
            View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.location_marker, null);
            //for(int i=0;i<50;i++) {
            LatLng ll = new LatLng(lat, lng);
            //   lng=lng+0.01;
            TextView tvUserNick = (TextView) view.findViewById(R.id.user_nick);
            ImageView ivUserLocation = (ImageView) view.findViewById(R.id.user_location);
            User u = mService.getUser(uid);
            if (u == null) {
                return;
            }

            tvUserNick.setText(u.nick.trim());
            tvUserNick.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            //设置自己的位置图标为红色，其他人在线的为蓝色，离线的为灰色
            if (u.iId == mService.getMyUserId()) {
                ivUserLocation.setImageResource(R.drawable.mylocation);
                tvUserNick.setBackgroundResource(R.drawable.area_bg_red);
            } else {
                ivUserLocation.setImageResource(R.drawable.userlocation);
                tvUserNick.setBackgroundResource(R.drawable.area_bg_blue);
            }
            ivUserLocation.setRotation(rotate);
            //把view转成image

            Bundle bundle = new Bundle();
            bundle.putDouble("longitude", lng);
            bundle.putDouble("latitude", lat);
            BitmapDescriptor bitmap = BitmapDescriptorFactory.fromBitmap(getViewBitmap(view));
            OverlayOptions option = new MarkerOptions().anchor(0.5f, 0.7f).position(ll).icon(bitmap).extraInfo(bundle);

            //先清除旧的，再显示新的
            Overlay oldOverlay = locationMarkers.get(uid);
            if (oldOverlay != null) {
                oldOverlay.remove();
                locationMarkers.remove(uid);
            }
            Overlay newOverlay = mBaiduMap.addOverlay(option);
            locationMarkers.put(uid, newOverlay);
            if (locationMarkers != null) {
                Channel c = mService.getCurrentChannel();
                if (c != null) {
                    mTVBarTitle.setText(c.name);
                }
                mTVBarCount.setText("(" + locationMarkers.size() + ")");
            }
        }

        public String getAddress(final String nickname, final double lng, final double lat) {

            new Thread(new Runnable() {     //开启子线程来执行网络请求
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        runHttpgetAddress(nickname, lng, lat);
                        Message msg = mHandler.obtainMessage(); //获取一个Message对象
                        mHandler.sendMessage(msg);  //通知UI进行更新
                    } catch (Exception ex) {

                    }
                }
            }).start();
            return "";
        }

        public void runHttpgetAddress(String nickname, double lng, double lat) throws IOException {
            String address = null;
            String key = "QNFfAn1hiHyXGm419VtV81Hp";
            //test
//        lng=114.948772;
//        lat=35.975538;
            //http://api.map.baidu.com/geocoder/v2/?callback=renderReverse&location=39.543922,117.82732&output=json&pois=2&ak=QNFfAn1hiHyXGm419VtV81Hp
            String url = String.format("http://api.map.baidu.com/geocoder/v2/?location=%s,%s&output=json&pois=1&ak=%s", String.valueOf(lat), String.valueOf(lng), key);
            URL myURL = null;
            URLConnection httpsConn = null;
            try {
                myURL = new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            InputStreamReader insr = null;
            BufferedReader br = null;
            try {
                httpsConn = (URLConnection) myURL.openConnection();// 不使用代理
                if (httpsConn != null) {
                    InputStream in = httpsConn.getInputStream();
                    InputStream raw = new BufferedInputStream(in);
                    insr = new InputStreamReader(raw, "UTF-8");
                    br = new BufferedReader(insr);
                    String nickNum = "";
                    if (nickname.length() > 4) {
                        nickNum = nickname.substring(0, 4);
                    } else {
                        nickNum = nickname;
                    }
                    String data = null;
                    while ((data = br.readLine()) != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            JSONObject result = jsonObject.getJSONObject("result");
                            String resAddress = result.getString("formatted_address");
                            JSONArray pois = result.getJSONArray("pois");
                            JSONObject pois_0 = pois.getJSONObject(0);
                            String adddsc = pois_0.getString("name");
                            if (adddsc != null) {
                                add = resAddress + "(" + adddsc + "附近" + ")";
                            } else {
                                add = resAddress;
                            }
                            int maxlen = 25;
                            if (add.length() > maxlen) {
                                add = add.substring(add.length() - maxlen, add.length());
                            }
                            add = nickNum + ":" + add;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                add = e.toString();
                e.printStackTrace();
            } finally {

                if (insr != null) {
                    insr.close();
                }
                if (br != null) {
                    br.close();
                }
            }
        }

        final Handler callHandler = new Handler();
        final Runnable callRunnable = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                //要做的事情
                mTimer--;
                if (mTimer > 0) {
                    callHandler.postDelayed(this, 1000);
                } else {
                    mIVCallPassenger.setVisibility(View.GONE);
                    iscallHandler = false;
                }
            }
        };
        private boolean iscallHandler = false;


        private void findView() {
            mIVBarLeft.setImageResource(R.drawable.arrow_left);
            mIVBarRightInner.setImageResource(R.drawable.switch_ch);
            mIVBarLeft.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            mIVBarRightInner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   boolean res= mService.switchChannel();
                   if(res==true){
                       mIVBarRightInner.setImageResource(R.drawable.switch_ch_red);
                   }else{
                       mIVBarRightInner.setImageResource(R.drawable.switch_ch);
                   }
                }
            });

            mMapView = findViewById(R.id.view_map);
            mBaiduMap = mMapView.getMap();
            mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
                @Override
                public void onMapStatusChangeStart(MapStatus mapStatus) {

                }

                @Override
                public void onMapStatusChangeStart(MapStatus mapStatus, int i) {

                }

                @Override
                public void onMapStatusChange(MapStatus mapStatus) {

                }

                @Override
                public void onMapStatusChangeFinish(MapStatus mapStatus) {
                    if (mAutoZoomLatLng != mapStatus.target || mAutoZoomLevel != mapStatus.zoom) {
                        //说明用户有过缩放、拖拽操作
                        userTouched = true;
                        //zoomToTextSize(mapStatus.zoom);
                        mMapStatus = mapStatus;
                        if (selfCenter) {
                            lastTouchedTime = System.currentTimeMillis();
                        }
                    }
                }
            });
            mBaiduMap.setTrafficEnabled(true);//开启路况
            mIVGather = (ImageView) findViewById(R.id.iv_gather);
            mTVWhoget = (TextView) findViewById(R.id.tv_whogetorderaddress);
            mTVWhoget.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            mIVSelfCenter = (ImageView) findViewById(R.id.iv_selfcenter);
            mIVApplyOrder = (ImageView) findViewById(R.id.iv_applyorder);
            mIVApplyOrder.setImageDrawable(getResources().getDrawable(R.drawable.deny_call_pttlib));
            mIVCallPassenger = (ImageView) findViewById(R.id.iv_callpassenger);
            mTVCurrentChanTalkerBg = (TextView) findViewById(R.id.tv_current_chan_talker_bg);
            mIVApplyOrder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mService != null && phoneCaller != null) {
                        if (isCanApplyOrder) {
                            mService.acceptOrder(true, phoneCaller);
                            mIVApplyOrder.setVisibility(View.GONE);
                            if (isapplyOrder) {
                                isapplyOrder = false;
                                applyOrderHandler.removeCallbacks(applyOrderRunnable);
                            }
                        } else {
                            mIVApplyOrder.setVisibility(View.GONE);
                        }
                    }

                }
            });

            mIVCallPassenger.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onClick(View v) {
                    mIVCallPassenger.setVisibility(View.GONE);
                    if (iscallHandler) {
                        iscallHandler = false;
                        callHandler.removeCallbacks(callRunnable);
                    }
                    try {
                        Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneCaller));//直接拨打电话
                        startActivity(dialIntent);
                        return;
                    } catch (RuntimeException e) {
                        Toast.makeText(getApplicationContext(), "请打开拨打电话权限", Toast.LENGTH_LONG).show();
                    }

                }
            });


            mIVGather.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setCenterAndZoom(true);
                }
            });
            mIVSelfCenter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selfCenter = !selfCenter;

                    if (selfCenter) {
                        mIVSelfCenter.setImageResource(R.drawable.ic_selfcenter_on);
                        selfCenterZoom = mBaiduMap.getMapStatus().zoom;
                        AppSettings.getInstance(MapActivity.this).setSelfCenter(selfCenterZoom);
                        setCenterself(selfLongitude, selfLatitude, selfCenterZoom);
                    } else {
                        mIVSelfCenter.setImageResource(R.drawable.ic_selfcenter_off);
                        AppSettings.getInstance(MapActivity.this).setSelfCenter(0);
                    }

                }
            });
        }


        @Override
        protected void onDestroy() {
            if (closeReceiver != null) {
                unregisterReceiver(closeReceiver);
                closeReceiver = null;
            }

            if (mBaiduMap != null) {
                mBaiduMap.setMyLocationEnabled(false);
            }
            if (mLocationClient != null) {
                mLocationClient.stop();//关闭定位
            }
            if (mSensorManager != null) {
                mSensorManager.unregisterListener(this);
            }
            if (mMapView != null) {
                mMapView.onDestroy();
            }

            if (mService != null) {
                mService.sendGeneralMessage(2, String.valueOf(mService.getMyUserId()));   //通知其他人，自己停止定位
                mService.unregisterObserver(serviceObserver);
                mService.setAllowAcceptOrder(false);
                mService.setIsMapShow(false);
            }

            super.onDestroy();
        }

        @Override
        public void onPause() {
            super.onPause();
            // 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
            if(mService!=null){
                mService.setIsMapShow(false);
                //Log.d("audio_ble", "onPause");
            }
            mMapView.onPause();

        }

    @Override
    public void onStop() {
        super.onStop();
        // 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        if(mService!=null){
            mService.setIsMapShow(false);
            //Log.d("audio_ble", "onStop");
        }
        mMapView.onPause();

    }

        @Override
        public void onResume() {
            super.onResume();
            // 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
            if(mService!=null) {
                mService.setIsMapShow(true);
            }
            mMapView.onResume();
        }


        @Override
        protected void serviceConnected() {
            Channel c = mService.getCurrentChannel();
            if (c != null) {
                mTVBarTitle.setText(c.name);
            }
            mService.registerObserver(serviceObserver);

            //立即发一次消息，通知其他人自己进来了
            mService.sendGeneralMessage(0, "");

            mService.setAllowAcceptOrder(true);
            mService.setIsMapShow(true);
            if (mService.isDriver()) {
                mLLlcd.setVisibility(View.GONE);
            }
        }

        @Override
        protected int getContentViewId() {
            return R.layout.activity_map;
        }

        private float zoomToTextSize(float zoom) {
            float size = (float) Math.floor(zoom) + 3;
            if (size < 13) {
                size = 13;
            }
            if (size > 25) {
                size = 25;
            }
            return size;
        }

        ////////////////////////////////
        private BaseServiceObserver serviceObserver = new BaseServiceObserver() {
            @Override
            public void onGeneralMessageGot(final int type, final String content) throws RemoteException {
                if (type == 0) {
                    //有人进来了

                    newUserEnter = true;
                } else if (type == 1) {

                    //说明是位置
                    String[] items = content.split(",");
                    if (items.length < 4) {
                        return;
                    }

                    if (!AppCommonUtil.validTotalkId(items[0])) {
                        return;
                    }

                    int uid = 0;
                    try {
                        uid = Integer.parseInt(items[0]);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (uid == 0) {
                        return;
                    }

                    //todo: 检查合法性
                    double longitude = Double.parseDouble(items[1]);
                    double latitude = Double.parseDouble(items[2]);
                    double radius = Double.parseDouble(items[3]);

                    float rotate = 0;
                    if (items.length > 4) {
                        rotate = Float.parseFloat(items[4]);
                    }
                    LatLng ll = new LatLng(latitude, longitude);
                    if (uid != mService.getMyUserId()) {
                        drwMark(uid, longitude, latitude, rotate);

                    }
                    //自动缩放
                    if (!selfCenter) {
                        setCenterAndZoom(false);
                    }

                } else if (type == 2) {
                    //停止定位
                    if (!AppCommonUtil.validTotalkId(content)) {
                        return;
                    }

                    int uid = 0;
                    try {
                        uid = Integer.parseInt(content);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (uid == 0) {
                        return;
                    }

                    //清除该用户
                    Overlay oldOverlay = locationMarkers.get(uid);
                    if (oldOverlay != null) {
                        oldOverlay.remove();
                        locationMarkers.remove(uid);
                    }

                    if (!selfCenter) {
                        setCenterAndZoom(false);
                    }
                    if (locationMarkers != null) {
                        Channel c = mService.getCurrentChannel();
                        if (c != null) {
                            mTVBarTitle.setText(c.name);
                        }
                        mTVBarCount.setText("(" + locationMarkers.size() + ")");
                    }
                } else if (type == 3) {
                    mTVWhoget.setVisibility(View.VISIBLE);
                    mTVWhoget.setText(content);
                    if (!isWhogetAdd) {
                        isWhogetAdd = true;
                        WhogetAddHandler.postDelayed(WhogetAddRunnable, 15000);
                    }
                }
            }

            @Override
            public void onLocalUserTalkingChanged(User user, boolean talk) throws RemoteException {
                if (mService == null) {
                    return;
                }
                mTVCurrentChanTalkerBg.setText("");
                mTVCurrentChanTalkerBg.setVisibility(View.INVISIBLE);
                User talker = mService.getcurrentVoiceTalker();
                if (talker != null) {
                    Channel tc = talker.getChannel();
                    Channel cc = mService.getCurrentChannel();
                    if (tc != null && cc != null && tc.id == cc.id) {
                        //在同一频道
                        //设备图标
                        mTVCurrentChanTalkerBg.setText(talker.nick);
                        mTVCurrentChanTalkerBg.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onApplyOrderResult(int uid, int cid, String phone, boolean success) throws RemoteException {
                int myuid = mService.getMyUserId();
                if (myuid == uid && success) {
                    mIVApplyOrder.setVisibility(View.GONE);
                    mIVCallPassenger.setVisibility(View.VISIBLE);
                    if (!iscallHandler) {
                        iscallHandler = true;
                        mTimer = 20;
                        callHandler.postDelayed(callRunnable, 1000);//第一次1s
                    }
                    User u = mService.getCurrentUser();
                    double lng = selfLongitude;
                    double lat = selfLatitude;
                    getAddress(u == null ? "" : u.nick, lng, lat);
                }
                if (myuid == uid) {
                    mIVApplyOrder.setVisibility(View.GONE);
                }


            }

            @Override
            public void onCurrentChannelChanged() throws RemoteException {
                Channel c = mService.getCurrentChannel();
                mTVBarTitle.setText(c.name);
            }

            @Override
            public void onUserOrderCall(final User user, boolean talk, String number) {
                if (user == null) {
                    return;
                }
                if (talk && number != null) {
                    isCanApplyOrder = false;
                    phoneCaller = number;
                    //if (!isapplyOrder) {
                        applyOrderHandler.removeCallbacks(applyOrderRunnable);
                        mIVApplyOrder.setVisibility(View.VISIBLE);
                        mIVApplyOrder.setImageDrawable(getResources().getDrawable(R.drawable.deny_call_pttlib));
                        //有时用户来电后立即挂机,则没有语音过来,但是会有电话过来,这时会有抢单,但是不会执行开始抢单,
                        // 这里为了避免抢单按钮不消失
                        applyOrderHandler.postDelayed(applyOrderRunnable, 25000);

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");// HH:mm:ss //获取当前时间
                        Date date = new Date(System.currentTimeMillis());
                        simpleDateFormat.format(date);
                        mTVWhoget.setVisibility(View.VISIBLE);
                        mTVWhoget.setText("新订单:" + "(" + simpleDateFormat.format(date) + ")");
                        isapplyOrder = true;

                    //}
                }
            }

            //中途有成员掉线或退出频道、切换频道，都会收到UserRemoved回调
            //因此，在这里查询，如果地图里有该用户，则删除
            //不用管新出现的用户。因为如果有新用户进来，自然会发送位置，从而显示在地图上
            @Override
            public void onUserRemoved(final User user) throws RemoteException {
                if (user == null) {
                    return;
                }
                Overlay oldOverlay = locationMarkers.get(user.iId);
                if (oldOverlay != null) {
                    oldOverlay.remove();
                }

                if (!selfCenter) {
                    setCenterAndZoom(false);
                }
            }
        };


        private static final int SDK_PERMISSION_REQUEST = 127;

        @TargetApi(23)
        protected void getPersimmions() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ArrayList<String> permissions = new ArrayList<String>();
                // 读写权限
                addPermission(permissions, Manifest.permission.ACCESS_COARSE_LOCATION);
                addPermission(permissions, Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissions.size() > 0) {
                    requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
                }
            }
        }

        @TargetApi(23)
        private boolean addPermission(ArrayList<String> permissionsList, String permission) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
                //这里如果调用shouldShow,,,发现第一次正常，拒绝后，第二次还能进入，所以改为false，每次都强制request
                if (false) {//(shouldShowRequestPermissionRationale(permission)) {
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
            switch (requestCode) {
                case SDK_PERMISSION_REQUEST:
                    Map<String, Integer> perms = new HashMap<String, Integer>();
                    // Initial
                    perms.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                    perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                    // Fill with results
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

                    if (perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            || perms.get(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        finish();
                    }

                    break;
            }

            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // 获取触发event的传感器类型
            int sensorType = event.sensor.getType();
            if (sensorType == Sensor.TYPE_ORIENTATION) {
                // 获取绕Z轴转过的角度
                myDir = event.values[0];
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }


        //根据原始数据计算中心坐标和缩放级别，并为地图设置中心坐标和缩放级别。
        private void setCenterself(double longitude, double latitude, float zoom) {
            double cenLng = longitude;
            double cenLat = latitude;
            float mZoom = zoom;
            LatLng cen = new LatLng(cenLat, cenLng);
            MapStatusUpdate mapStatusUpdatePoint = MapStatusUpdateFactory.newLatLng(cen);
            MapStatusUpdate mapStatusUpdateZoom = MapStatusUpdateFactory.zoomTo(mZoom);
            mBaiduMap.setMapStatus(mapStatusUpdatePoint);
            mBaiduMap.setMapStatus(mapStatusUpdateZoom);
            mAutoZoomLatLng = cen;
            mAutoZoomLevel = zoom;
        }

        //根据原始数据计算中心坐标和缩放级别，并为地图设置中心坐标和缩放级别。
        private void setCenterAndZoom(boolean force) {
            if (force) {
                //如果用户点击了zoom按钮，则清除userTouched，重新开始自动zoom
                userTouched = false;
            }
            if (userTouched) {
                return;
            }

            double maxLng = -180.0, minLng = 180.0, maxLat = -90.0, minLat = 90.0;
            for (Overlay ol : (locationMarkers.values())) {
                double lng = ol.getExtraInfo().getDouble("longitude");
                double lat = ol.getExtraInfo().getDouble("latitude");
                if (lng > maxLng) {
                    maxLng = lng;
                }
                if (lng < minLng) {
                    minLng = lng;
                }
                if (lat > maxLat) {
                    maxLat = lat;
                }
                if (lat < minLat) {
                    minLat = lat;
                }
            }

            double cenLng = (maxLng + minLng) / 2;
            double cenLat = (maxLat + minLat) / 2;
            float zoom = getZoom(maxLng, minLng, maxLat, minLat);
            LatLng cen = new LatLng(cenLat, cenLng);

            MapStatusUpdate mapStatusUpdatePoint = MapStatusUpdateFactory.newLatLng(cen);
            MapStatusUpdate mapStatusUpdateZoom = MapStatusUpdateFactory.zoomTo(zoom);
            mBaiduMap.setMapStatus(mapStatusUpdatePoint);
            mBaiduMap.setMapStatus(mapStatusUpdateZoom);

            mAutoZoomLatLng = cen;
            mAutoZoomLevel = zoom;
        }

        //把view转换成image
        private Bitmap getViewBitmap(View addViewContent) {
            addViewContent.setDrawingCacheEnabled(true);
            addViewContent.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            addViewContent.layout(0, 0,
                    addViewContent.getMeasuredWidth(),
                    addViewContent.getMeasuredHeight());
            addViewContent.buildDrawingCache();

            Bitmap cacheBitmap = addViewContent.getDrawingCache();
            Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);
            return bitmap;
        }

        //根据经纬极值计算绽放级别
        private float getZoom(double maxLng, double minLng, double maxLat, double minLat) {
            //级别18到3
            double[] zoom = {50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 25000, 50000, 100000, 200000, 500000, 1000000, 2000000, 4000000};
            //获取两点距离,保留小数点后两位
            double distance = getDistance(new LatLng(maxLat, maxLng), new LatLng(minLat, minLng));
            for (int i = 0, zoomLen = zoom.length; i < zoomLen; i++) {
                if (zoom[i] - distance > 0) {
                    return 18 - i + 3;//之所以会多3，是因为地图范围常常是比例尺距离的10倍以上。所以级别会增加3。
                }
            }
            return 5;
        }

        /**
         *计算两点之间距离
         *@paramstart
         *@paramend
         *@return米
         */
        private double getDistance(LatLng start, LatLng end) {
            double lat1 = (Math.PI / 180) * start.latitude;
            double lat2 = (Math.PI / 180) * end.latitude;
            double lon1 = (Math.PI / 180) * start.longitude;
            double lon2 = (Math.PI / 180) * end.longitude;
            //地球半径
            double EARTH_RADIUS = 6378137.0;

            //两点间距离km，如果想要米的话，结果*1000就可以了
            double d = Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1)) * EARTH_RADIUS;
            return d;
        }

        //考虑到很多情况下，位置不变。为了去除冗余数据
        //过滤一下。只有当上次上报的位置偏移xx米以上，或者时间大于xx秒时，才上报。以去掉冗余数据
        private double lastReportedLongitude = 0.0, lastReportedLatitude = 0.0;
        private float lastReportedDegree = -10;  //方向
        private long lastReportedTime = 0;
        //因为上传位置，有个过滤机制，即如果位置不变，则降低上传频率
        //这样可能产生一个问题：新用户进来后，过很久才看到其他人
        //解决办法：新用户进来时，立即发送一个GeneralMessage，通知其他人"我进来了"
        //其他人收到后，下次获取位置时，不能再过滤，而应该保证发送一次，使新用户能看到每个人
        //具体方法：收到别人的进入事件后，把newUserEnter设为true
        private boolean newUserEnter = false;

        //定位相关
        private class MyLocationListener extends BDAbstractLocationListener {
            @Override
            public void onReceiveLocation(BDLocation location) {
                if (mService == null) {
                    return;
                }
                //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
                //以下只列举部分获取经纬度相关（常用）的结果信息
                //更多结果信息获取说明，请参照类参考中BDLocation类中的说明
                final double latitude = location.getLatitude();    //获取纬度信息
                final double longitude = location.getLongitude();    //获取经度信息
                final float radius = location.getRadius();    //获取定位精度，默认值为0.0f
                String coorType = location.getCoorType();
                //获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准
                int errorCode = location.getLocType();
                myCurrentX = location.getDirection();
                float speed = location.getSpeed();
                //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明
                selfLongitude = longitude;
                selfLatitude = latitude;
                //经纬度格式：userid,经度,纬度,半径
                int uid = mService.getMyUserId();
                String content = String.valueOf(uid);
                content += "," + String.valueOf(longitude);
                content += "," + latitude;
                content += "," + radius;
                content += "," + myCurrentX;
                long now = currentTimeMillis();
                drwMark(uid, longitude, latitude, myCurrentX);
                long mTime = System.currentTimeMillis() - lastTouchedTime;
                if (selfCenter && mTime > 6000) {//中心跟踪下,6秒内有过触摸不更新位置
                    setCenterself(longitude, latitude, selfCenterZoom);
                }
                boolean timeTooShort = now - lastReportedTime < AppConstants.LOCATION_MAX_INTERVAL_SECONDS * 1000;
                //间隔未超过最大时间，再计算跟上次上报过的位置之间的距离。如果距离很小，则不上报
                double dist = getDistance(new LatLng(latitude, longitude), new LatLng(lastReportedLatitude, lastReportedLongitude));
                boolean distTooShort = dist < AppConstants.LOCATION_DIFF_DISTANCE;
                boolean degreeDiffTooSmall = (Math.abs(myCurrentX - lastReportedDegree) < 5.0);

                if (newUserEnter) {
                    newUserEnter = false;
                } else {
                    if (timeTooShort && distTooShort && degreeDiffTooSmall) {
                        return;
                    }
                }

                mService.sendGeneralMessage(1, content);

                lastReportedLatitude = latitude;
                lastReportedLongitude = longitude;
                lastReportedDegree = myCurrentX;
                lastReportedTime = now;
            }
        }
    }
