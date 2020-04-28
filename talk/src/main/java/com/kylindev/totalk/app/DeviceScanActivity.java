package com.kylindev.totalk.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.InterpttService.HandmicState;
import com.kylindev.pttlib.service.InterpttService.LocalBinder;
import com.kylindev.pttlib.service.model.HandmicInfo;
import com.kylindev.totalk.AppConstants;
import com.kylindev.totalk.R;
import com.kylindev.totalk.utils.AppCommonUtil;
import com.kylindev.totalk.utils.AppSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.kylindev.pttlib.service.InterpttService.HandmicState.HANDMIC_CONNECTED;
import static com.kylindev.pttlib.service.InterpttService.HandmicState.HANDMIC_CONNECTING;


public class DeviceScanActivity extends Activity implements ListView.OnItemClickListener, OnClickListener {
	/**
	 * The InterpttService instance that drives this activity's data.
	 */
	private InterpttService mService;

	private ListView myList;
	private DeviceListAdapter myAdapter;
	private LinearLayout mLLNoDevice;
	private ImageView mIVHandmic, mIVHandmicCircle;
	private TextView mTVConnectionState;
	private LinearLayout mLLInfo;
	private TextView mTVAppVersion, mTVModel, mTVVersion,mTVConnected;
	private Button mBtnScan;
	private Button mBleSpeedTest;
	private ProgressBar mPBScan;

	private boolean mServiceBind = false;
	/**
	 * Management of service connection state.
	 */
	private ServiceConnection mServiceConnection = null;

	private void initServiceConnection() {
		mServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				LocalBinder localBinder = (LocalBinder) service;
				mService = localBinder.getService();
				mService.registerObserver(serviceObserver);

				myAdapter = new DeviceListAdapter(mService);
				myList.setAdapter(myAdapter);
				if (AppConstants.DEBUG) {
					if (mService != null) {
						if (mService.getIsLeSpeed()) {
							mBleSpeedTest.setText("测试中");
						} else {
							mBleSpeedTest.setText("测速");
						}
					}
				}
				refreshTargetHandmic();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				mService = null;
				mServiceConnection = null;
			}
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_devicescan);
		//可能是首次运行，也可能是用户重新launch
		//因此，先检查service是否在运行，如果是，则直接bind以获取mService实例；如果没有，则startService，再bind
		Intent serviceIntent = new Intent(this, InterpttService.class);
		initServiceConnection();
		mServiceBind = bindService(serviceIntent, mServiceConnection, 0);
		mTVAppVersion = (TextView) findViewById(R.id.tv_app_version_device);
		mTVAppVersion.setText(AppCommonUtil.getAppVersionName());
		mTVModel = (TextView) findViewById(R.id.tv_handmic_model);
		mTVVersion = (TextView) findViewById(R.id.tv_handmic_version);
		mTVConnected = (TextView) findViewById(R.id.tv_handmic_connected);
		mIVHandmic = (ImageView) findViewById(R.id.iv_handmic_scanactivity);
		mIVHandmic.setOnClickListener(this);
		mIVHandmicCircle = (ImageView) findViewById(R.id.iv_handmic_circle);
		mTVConnectionState = (TextView) findViewById(R.id.tv_handmic_connection_state);
		if (AppConstants.DEBUG) {
			mBleSpeedTest=(Button) findViewById(R.id.tv_handmic_lespeedtest);
			mBleSpeedTest.setVisibility(View.VISIBLE);
			mBleSpeedTest.setOnClickListener(this);
		}

		//之前channelView相关的
		ImageView ivLeave = (ImageView) findViewById(R.id.iv_ds_leave);
		ivLeave.setOnClickListener(this);
		TextView tvTaobao = findViewById(R.id.tv_taobao);
		tvTaobao.setOnClickListener(this);
		tvTaobao.setVisibility(View.GONE);

		mBtnScan = (Button) findViewById(R.id.btn_scan_ble);
		mBtnScan.setOnClickListener(this);
		mPBScan = (ProgressBar) findViewById(R.id.pb_scan);

		// Get the UI views
		myList = (ListView) findViewById(R.id.lv_device);
		myList.setOnItemClickListener(this);
		mLLNoDevice = (LinearLayout) findViewById(R.id.ll_no_device);
		myList.setEmptyView(mLLNoDevice);
		getPermissions();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//点击选择，开始连接
		final MyDevice mydev = myAdapter.getDevice(position);
		if (mydev == null)
			return;

		//如果目前连接了其他设备，先断开。简单起见，一律先断开
		mService.disconnectTargetDevice(true);
		try {
			Thread.sleep(200);
		} catch (Exception e) {

		}

		HandmicState s = mydev.state;
		if (s != HANDMIC_CONNECTED) {
			mService.connectDevice(mydev.device);
		}
	}

	@Override
	protected void onDestroy() {
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

	////////////////////////////////
	private BaseServiceObserver serviceObserver = new BaseServiceObserver() {
		@Override
		public void onTargetHandmicStateChanged(BluetoothDevice device, HandmicState s) throws RemoteException {
			//Log.d("audio_ble","onTargetHandmicStateChanged");
			refreshTargetHandmic();

			//找到对应的device，为ready赋值，并刷新显示
			for (int i=0; i<myAdapter.getCount(); i++) {
				MyDevice dev = (MyDevice) myAdapter.getItem(i);
				//String addr = dev.device.getAddress();
				if (dev.device.equals(device)) {
					dev.state = s;
					myAdapter.setItem(i, dev);
					myAdapter.notifyDataSetChanged();
					break;
				}
			}

			//连接成功后，自动停止扫描
			if (s == HANDMIC_CONNECTED) {
				mService.scanLeDevice(false);
			}
		}

		@Override
		public void onLeDeviceScanStarted(boolean start) throws RemoteException {
			refreshTargetHandmic();

			mBtnScan.setText(start ? R.string.stop_search_handmic : R.string.search_handmic);
			mPBScan.setVisibility(start ? View.VISIBLE : View.GONE);
			if (start) {
				myAdapter.clear();
				myAdapter.notifyDataSetChanged();
			}
		}

		@Override
		public void onLeDeviceFound(BluetoothDevice device) throws RemoteException {
			//判断地址，去重
			for (int i=0; i<myAdapter.getCount(); i++) {
				MyDevice dev = (MyDevice) myAdapter.getItem(i);
				String addr = dev.device.getAddress();
				if (addr.equals(device.getAddress())) {
					//发现相同地址的
					return;
				}
			}

			MyDevice dev = new MyDevice(device, HandmicState.HANDMIC_DISCONNECTED);
			myAdapter.addDevice(dev);
			myAdapter.notifyDataSetChanged();
		}
	};

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();

		if (id == R.id.iv_ds_leave) {
			finish();
		} else if (id == R.id.tv_taobao) {
			PackageManager pm = getPackageManager();
			if (AppCommonUtil.isPackageInstalled("com.taobao.taobao", pm)) {
				Intent intent = new Intent();
				intent.setAction("android.intent.action.VIEW");
				String url = "taobao://shop251475702.taobao.com";
				Uri uri = Uri.parse(url);
				intent.setData(uri);
				startActivity(intent);
			} else {
				Intent intent = new Intent();
				intent.setAction("android.intent.action.VIEW");
				Uri content_url = Uri.parse("https://shop251475702.taobao.com");
				intent.setData(content_url);
				startActivity(intent);
			}
		} else if (id == R.id.iv_handmic_scanactivity) {
			HandmicState s = mService.getTargetHandmicState();
			if (s == HANDMIC_CONNECTED || s == HANDMIC_CONNECTING) {
				mService.disconnectTargetDevice(true);
			} else if (s == HandmicState.HANDMIC_DISCONNECTED) {
				mService.scanLeDevice(true);// .connectDevice(addr);
			}
		} else if (id == R.id.btn_scan_ble) {
			if (mService != null) {
				boolean isScanning = mService.getIsBleScanning();
				mService.scanLeDevice(!isScanning);
			}
		} else if (id == R.id.tv_handmic_lespeedtest) {
			if (mService != null) {
				mService.LeSpeedTest(!mService.getIsLeSpeed());
				if (mService.getIsLeSpeed()) {
					mBleSpeedTest.setText("测试中");
				} else {
					mBleSpeedTest.setText("测速");
				}
			}
		}
	}

	class DeviceListAdapter extends BaseAdapter {
		private ArrayList<MyDevice> mLeDevices;
		private LayoutInflater mInflator;

		public DeviceListAdapter(final InterpttService service) {
			super();
			mLeDevices = new ArrayList<MyDevice>();
			mInflator = DeviceScanActivity.this.getLayoutInflater();
		}

		public void addDevice(MyDevice device) {
			if (!mLeDevices.contains(device)) {
				mLeDevices.add(device);
			}
		}

		public MyDevice getDevice(int position) {
			return mLeDevices.get(position);
		}

		public void clear() {
			mLeDevices.clear();
		}

		@Override
		public int getCount() {
			return mLeDevices.size();
		}

		@Override
		public Object getItem(int i) {
			return mLeDevices.get(i);
		}

		public void setItem(int i, MyDevice dev) {
			mLeDevices.set(i, dev);
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
				view = mInflator.inflate(R.layout.listitem_device, null);
				viewHolder = new ViewHolder();
				viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
				viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
				viewHolder.ivReady = (ImageView) view.findViewById(R.id.iv_device_ready);
				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			MyDevice device = mLeDevices.get(i);
			final String deviceName = device.device.getName();
			if (deviceName != null && deviceName.length() > 0) {
				String showDeviceName = deviceName +"-"+ device.device.getAddress().substring(15,17);
				viewHolder.deviceName.setText(showDeviceName);
			}
			else
				viewHolder.deviceName.setText(R.string.unknown_device);
			viewHolder.deviceAddress.setText(device.device.getAddress());

			//连接指示
			if (device.state == HANDMIC_CONNECTED) {
				viewHolder.ivReady.setImageResource(R.drawable.handmic_connected);
				viewHolder.ivReady.clearAnimation();
			}
			else if (device.state == HandmicState.HANDMIC_DISCONNECTED) {
				viewHolder.ivReady.setImageResource(R.drawable.handmic_disconnected);
				viewHolder.ivReady.clearAnimation();
			}
			else {
				//正在连接
				viewHolder.ivReady.setImageResource(R.drawable.handmic_connected);
				viewHolder.ivReady.startAnimation(AppCommonUtil.createTalkingAnimation());
			}

			return view;
		}
	}

	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
		ImageView ivReady;
	}

	public class MyDevice {
		BluetoothDevice device;
		InterpttService.HandmicState state;

		public MyDevice(BluetoothDevice btDevice, InterpttService.HandmicState _state) {
			device = btDevice;
			state = _state;
		}
	}

	private void refreshTargetHandmic() {
		if (mService == null) {
			return;
		}

		InterpttService.HandmicState s = mService.getTargetHandmicState();
		//Log.d("audio_ble","HandmicState="+s);
		switch (s) {
			case HANDMIC_CONNECTED:
				mIVHandmic.setImageResource(R.drawable.handmic_connected);
				mIVHandmicCircle.clearAnimation();
				mIVHandmicCircle.setVisibility(View.INVISIBLE);
				mTVConnectionState.setText(getString(R.string.device_ok_click_to_disconnect));
				break;
			case HANDMIC_CONNECTING:
				mIVHandmic.setImageResource(R.drawable.handmic_connected);
				mIVHandmicCircle.setVisibility(View.VISIBLE);
				mIVHandmicCircle.startAnimation(AppCommonUtil.createRotateAnimation());
				mTVConnectionState.setText(getString(R.string.connecting_device));
				break;
			case HANDMIC_DISCONNECTED:
				//再看是否在扫描期间
				boolean scanning = mService.getIsBleScanning();
				if (scanning) {
					mIVHandmic.setImageResource(R.drawable.handmic_disconnected);
					mIVHandmicCircle.setVisibility(View.VISIBLE);
					mIVHandmicCircle.startAnimation(AppCommonUtil.createRotateAnimation());
					mTVConnectionState.setText(getString(R.string.scaning_device));
				}
				else {
					mIVHandmic.setImageResource(R.drawable.handmic_disconnected);
					mIVHandmicCircle.clearAnimation();
					mIVHandmicCircle.setVisibility(View.INVISIBLE);
					mTVConnectionState.setText(getString(R.string.device_disc_click_to_reconnect));
				}
				break;
		}

		//显示手咪信息
		//if (s == HANDMIC_CONNECTED) {
			//Log.d("audio_ble","refreshTargetHandmic");
			HandmicInfo info = mService.getHandmicInfo();
		  	String CurrentDeviceName= mService.getCurrentTargetDeviceName();
			mTVModel.setText(info.model);
			mTVVersion.setText(info.firmware);
			mTVConnected.setText(CurrentDeviceName);
		//}
	}


	private static final int SDK_PERMISSION_REQUEST = 127;
	@TargetApi(23)
	protected void getPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			ArrayList<String> permissions = new ArrayList<String>();
			// 麦克风权限
			addPermission(permissions, Manifest.permission.ACCESS_COARSE_LOCATION);
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
				perms.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
				// Fill with results
				for (int i = 0; i < permissions.length; i++)
					perms.put(permissions[i], grantResults[i]);

				if (perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					AppCommonUtil.showToast(this, R.string.need_corse_location_permission);
				}
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
}
