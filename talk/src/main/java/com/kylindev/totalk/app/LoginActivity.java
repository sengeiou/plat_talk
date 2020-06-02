package com.kylindev.totalk.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.InterpttService.ConnState;
import com.kylindev.pttlib.service.InterpttService.LocalBinder;
import com.kylindev.pttlib.utils.ServerProto.Reject.RejectType;
import com.kylindev.totalk.AppConstants;
import com.kylindev.totalk.MainApp;
import com.kylindev.totalk.R;
import com.kylindev.totalk.utils.AppCommonUtil;
import com.kylindev.totalk.utils.AppSettings;

import static com.kylindev.pttlib.LibConstants.INTERPTT_SERVICE;

public class LoginActivity extends Activity implements View.OnClickListener, View.OnLongClickListener {
	private EditText mETHost, mETUserId, mETPassword;
	private String mCountryCode;	//"86"
	private String mHost, mUser, mPwd;
	private Button mBtnCountry, mBtnLogin;
	private ImageView mIVSlogan;
	private TextView mTVRegister, mTVForgetPwd, mTVVersion;
	private Intent mServiceIntent = null;
	private InterpttService mService = null;
	private boolean autoFinish = false;    //
	private final int REQUEST_CODE_REGISTER = 0;
	private final int REQUEST_CODE_FORGET_PWD = 1;
	private boolean showDisconnect = true;    //注册返回后，因为注册的connection会断开，从而使本界面显示“连接失败”，会困扰用户。加此标识以区分，只显示“登录”引起的disconnection

	private ServiceConnection mServiceConnection = null;
	private boolean mServiceBind = false;

	private void initServiceConn() {
		mServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				LocalBinder localBinder = (LocalBinder) service;
				mService = localBinder.getService();
				mService.registerObserver(serviceObserver);

				//有可能是已经连接成功过至少一次后，用户再次点击app图标进入的。应先检查是否已经连接成功过
				if (mService.isLoginOnceOK()) {
					//如果已经连接成功过，即使现在是断开状态，也应立即进入对讲界面
					jumpToChannel();
				} else if (mUser != null && mPwd != null) {
					//说明进入此界面后，至少点击过一次"登陆"按钮
					showBtn(false);
					if (AppCommonUtil.validTotalkId(mUser)) {
						mService.login(AppSettings.getInstance(LoginActivity.this).getHost(), 0, mUser, mPwd);
					}
					else {
						String realUserId = mUser;
						mService.login(AppSettings.getInstance(LoginActivity.this).getHost(), 0, realUserId, mPwd);
					}
				}
			}

			//此方法调用时机：This is called when the connection with the service has been unexpectedly disconnected
			//-- that is, its process crashed. Because it is running in our same process, we should never see this happen.
			@Override
			public void onServiceDisconnected(ComponentName name) {
				mServiceConnection = null;
				mService = null;
			}
		};
	}
	String userName;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		//之前在验证手机号时，按home，查看短信，再点击icon启动时，会重新显示本界面
		//加这个判断后，会直接显示按home时所在的界面
        //车台去调此判断（4.4版本）
		/*if (!isTaskRoot()) {
			finish();
			return;
		}*/


		//检查新版本
		AppCommonUtil.checkUpdate(false);

		mETHost = findViewById(R.id.et_host);
		mIVSlogan = findViewById(R.id.iv_slogan);
		mBtnCountry = (Button) findViewById(R.id.btn_country_login);
		String countryCode = AppSettings.getInstance(this).getKeyCountry();	//"+86"
		mBtnCountry.setText(countryCode);
		mCountryCode = countryCode.substring(1);

		mBtnCountry.setOnClickListener(this);

		mBtnLogin = (Button) findViewById(R.id.btn_login);
		mETUserId = (EditText) findViewById(R.id.et_serverUsername);
		mETPassword = (EditText) findViewById(R.id.et_serverPassword);
		EditChangedListener txtListner = new EditChangedListener();
		mETUserId.addTextChangedListener(txtListner);
		mETPassword.addTextChangedListener(txtListner);
		mTVRegister = (TextView) findViewById(R.id.tv_register);
		mTVForgetPwd = (TextView) findViewById(R.id.tv_forget_pwd);
		mTVVersion = (TextView) findViewById(R.id.tv_version);
		mBtnLogin.setOnClickListener(this);
		mTVRegister.setOnClickListener(this);

		mTVForgetPwd.setOnClickListener(this);
		mTVVersion.setText(AppCommonUtil.getAppVersionName());

		mETHost.setVisibility(View.VISIBLE);
		mETUserId.setHint(R.string.ent_username_hint);
		mETUserId.setInputType(InputType.TYPE_CLASS_TEXT);
		mIVSlogan.setImageResource(R.drawable.slogan_ent);
		mBtnCountry.setVisibility(View.GONE);
		mTVRegister.setVisibility(View.GONE);
		mTVForgetPwd.setVisibility(View.GONE);

		refreshLayout();	//切换个人版和企业版

		mServiceIntent = new Intent(this, InterpttService.class);
		initServiceConn();
		Log.d("MODEL"," Build.MODEL="+ Build.MODEL);
		//本activity有可能是已经开始工作，用户又点击app图标而启动的。此时，应先判断service是否已经运行。
		if (AppCommonUtil.isServiceRunning(this, INTERPTT_SERVICE)) {
			//service正在运行。此时还不能直接跳转到channel里，因为此service有可能是之前登录失败了，而service还在。
			//因此，这里不jump，而是bindService。bind成功后，在serviceConnected里进行处理
			mServiceBind = bindService(mServiceIntent, mServiceConnection, 0);
		}
		else {
			//如果service未运行，表明是首次进入，则检查是否要自动登录
			if (AppSettings.getInstance(this).getAutoLogin() && !isH6) {
				mUser = AppSettings.getInstance(this).getUserid();
				mPwd = AppSettings.getInstance(this).getPassword();
				if (AppCommonUtil.validUserId(mUser) && AppCommonUtil.validPwd(mPwd)) {
					startService(mServiceIntent);

					mServiceBind = bindService(mServiceIntent, mServiceConnection, 0);
					//此后，在onServiceConnected里会开始mService.connect()。然后connected之后，会自动jumpToChannel
				}
			}
		}
		mETHost.setText("140.143.236.23");
		mETHost.setVisibility(View.GONE);
		userName=getIntent().getStringExtra("account");
		login2(getIntent().getStringExtra("account"),"123456");
	}

	@Override
	protected void onDestroy() {
		//执行到onDestroy，有两种情况，一种是登录成功，自动finish()，一种是未登录成功，用户退出。
		//所以应判断登录是否成功，如果未成功，则应同时销毁service
		if (mService != null) {
			mService.unregisterObserver(serviceObserver);
			if (! autoFinish) {
				//如果不在线程里disconnect，ui会卡住
				//mService.disconnect();
				new Thread(new Runnable() {
					@Override
					public void run() {
						mService.disconnect();
					}
				}).start();

				mService.stopSelf();
				System.exit(0);	//增加，彻底退出
			}

			if (mServiceConnection != null) {
				// Unbind to service
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
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();

		if (id == R.id.btn_country_login) {
			Intent intent = new Intent();
			intent.setClass(LoginActivity.this, CountryActivity.class);
			startActivityForResult(intent, 12);
		} else if (id == R.id.btn_login) {
			showDisconnect = true;
			String countryCode = mBtnCountry.getText().toString();    //"+86"
			mCountryCode = countryCode.substring(1);    //"86"

			mHost = mETHost.getText().toString();
			mUser = mETUserId.getText().toString();
			mPwd = mETPassword.getText().toString();
			//debug时，不检查合法性
			if (!AppCommonUtil.validUserId(mUser)) {
				AppCommonUtil.showToast(this, R.string.userid_bad_format);
				return;
			}
			if (!AppCommonUtil.validPwd(mPwd)) {
				AppCommonUtil.showToast(this, R.string.password_bad_format);
				return;
			}

			AppSettings.getInstance(this).setHost(mHost);
			AppSettings.getInstance(this).setUserid(mUser);
			AppSettings.getInstance(this).setPassword(mPwd);

			//用户可在此界面多次尝试登录
			if (!AppCommonUtil.isServiceRunning(this, INTERPTT_SERVICE)) {
				startService(mServiceIntent);
			}

			if (mService == null) {
				//如果之前没有启动并bind mService，则需先bind，在onServiceConnected里开始connect
				if (mServiceConnection == null) {
					initServiceConn();
				}

				mServiceBind = bindService(mServiceIntent, mServiceConnection, 0);
			} else {
				//如果之前已经有mService，则可以直接开始connect
				showBtn(false);
				//为支持国家，修改：如果是滔滔id，则忽略国家码；如果可能是手机号，则上传国家码
				if (AppCommonUtil.validTotalkId(mUser)) {
					mService.login(AppSettings.getInstance(LoginActivity.this).getHost(), 1, mUser, mPwd);
				} else {
					String realUserId = mUser;
					mService.login(AppSettings.getInstance(LoginActivity.this).getHost(),
							1,
							realUserId, mPwd);
				}
			}
		} else if (id == R.id.tv_register) {
		} else if (id == R.id.tv_forget_pwd) {
		}
	}


	public void login2(String account, String passwd){
            showDisconnect = true;
            String countryCode = mBtnCountry.getText().toString();    //"+86"
            mCountryCode = countryCode.substring(1);    //"86"
            mHost = mETHost.getText().toString();
//            mUser = mETUserId.getText().toString();
		mUser=account;
//            mPwd = mETPassword.getText().toString();
		mPwd=passwd;
            //debug时，不检查合法性
            if (!AppCommonUtil.validUserId(mUser)) {
                AppCommonUtil.showToast(this, R.string.userid_bad_format);
                return;
            }
            if (!AppCommonUtil.validPwd(mPwd)) {
                AppCommonUtil.showToast(this, R.string.password_bad_format);
                return;
            }

            AppSettings.getInstance(this).setHost(mHost);
            AppSettings.getInstance(this).setUserid(mUser);
            AppSettings.getInstance(this).setPassword(mPwd);

            //用户可在此界面多次尝试登录
            if (!AppCommonUtil.isServiceRunning(this, INTERPTT_SERVICE)) {
                startService(mServiceIntent);
            }

            if (mService == null) {
                //如果之前没有启动并bind mService，则需先bind，在onServiceConnected里开始connect
                if (mServiceConnection == null) {
                    initServiceConn();
                }

                mServiceBind = bindService(mServiceIntent, mServiceConnection, 0);
            } else {
                //如果之前已经有mService，则可以直接开始connect
                showBtn(false);
                //为支持国家，修改：如果是滔滔id，则忽略国家码；如果可能是手机号，则上传国家码
                if (AppCommonUtil.validTotalkId(mUser)) {
                    mService.login(AppSettings.getInstance(LoginActivity.this).getHost(), 1, mUser, mPwd);
                } else {
                    String realUserId = mUser;
                    mService.login(AppSettings.getInstance(LoginActivity.this).getHost(),
                            1,
                            realUserId, mPwd);
                }
            }
    }

	@Override
	public boolean onLongClick(View v) {
		if (v.getId() == R.id.tv_version) {
			final EditText etHost = new EditText(this);
			String oldHost = AppSettings.getInstance(LoginActivity.this).getHost();
			etHost.setHint(AppConstants.DEFAULT_PERSONAL_HOST);
			etHost.setText(oldHost);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("服务器地址，勿改!").setView(etHost)
					.setNegativeButton("取消", null);
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String host = etHost.getText().toString();
					if (host.length() == 0) {
						AppSettings.getInstance(LoginActivity.this).setHost(AppConstants.DEFAULT_PERSONAL_HOST);
					} else {
						AppSettings.getInstance(LoginActivity.this).setHost(host);
					}
				}
			});
			builder.show();
		}
		return false;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (Activity.RESULT_OK != resultCode) {
			return;
		}

		if (requestCode == REQUEST_CODE_REGISTER) {
		}
		else if (requestCode == REQUEST_CODE_FORGET_PWD) {

		}
		else if (requestCode == 12) {
			//选择国家
			Bundle bundle = data.getExtras();
			String countryNumber = bundle.getString("countryNumber");
			mBtnCountry.setText(countryNumber);
			AppSettings.getInstance(this).setKeyCountry(countryNumber);
			mCountryCode = countryNumber.substring(1);
		}
	}

	Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message message) {
			showBtn(true);

			switch (message.what) {
				case 0:
					AppCommonUtil.showToast(LoginActivity.this, R.string.user_or_password_wrong);
					break;
				case 1:
					AppCommonUtil.showToast(LoginActivity.this, R.string.server_full);
					break;
				case 2:
					AppCommonUtil.showToast(LoginActivity.this, R.string.wrong_version);
					break;
				case 3:
					AppCommonUtil.showToast(LoginActivity.this, R.string.wrong_client_type);
					break;
				case 4:
					Toast t = Toast.makeText(LoginActivity.this, R.string.connect_fail_please_retry, Toast.LENGTH_SHORT);
					t.setGravity(Gravity.BOTTOM, 0, 0);
					t.show();
					break;
				default:
					break;
			}
		}
	};

	private void showBtn(boolean enable) {
		mBtnLogin.setEnabled(enable);
		mBtnLogin.setText(enable ? R.string.login : R.string.logging_in);

		mTVRegister.setEnabled(enable);
		mTVRegister.setClickable(enable);

		mTVForgetPwd.setEnabled(enable);
		mTVForgetPwd.setClickable(enable);

		mETUserId.setEnabled(enable);
		mETUserId.setClickable(enable);
		mETPassword.setEnabled(enable);
		mETPassword.setClickable(enable);
	}

	private void refreshLayout() {
		String countryCode = AppSettings.getInstance(this).getKeyCountry();	//"+86"
		mBtnCountry.setText(countryCode);

		String host = AppSettings.getInstance(this).getHost();
		String id = AppSettings.getInstance(this).getUserid();
		String pwd = AppSettings.getInstance(this).getPassword();

		mETHost.setText(host);
		if (AppCommonUtil.validUserId(id) && AppCommonUtil.validPwd(pwd)) {
			mETUserId.setText(id);
			mETPassword.setText(pwd);

			mBtnLogin.setEnabled(true);
		}
		else {
			mETUserId.setText("");
			mETPassword.setText("");

			mBtnLogin.setEnabled(false);
		}

		mTVVersion.setOnLongClickListener(this);
	}

	////////////////////////////////
	//接收service的广播
	private BaseServiceObserver serviceObserver = new BaseServiceObserver() {
		public void onConnectionStateChanged(ConnState state) throws RemoteException {
			switch (state) {
				case CONNECTION_STATE_CONNECTING:
					break;
				case CONNECTION_STATE_SYNCHRONIZING:
					break;
				case CONNECTION_STATE_CONNECTED:
					break;
				case CONNECTION_STATE_DISCONNECTED:
					showBtn(true);
					if (showDisconnect) {
						Message msg = new Message();
						msg.what = 4;
						mHandler.sendMessage(msg);
					}
					break;
			}
		};

		@Override
		public void onRejected(RejectType type) throws RemoteException {
			Message msg = new Message();

			switch (type) {
				case None:
					//登录成功
					jumpToChannel();
					break;
				case InvalidUsername:
				case WrongUserPW:
				case AuthenticatorFail:
					//杀掉service，无需再重试
					if (mService != null) {
						mService.stopSelf();
					}
					msg.what = 0;
					mHandler.sendMessage(msg);
					break;
				case ServerFull:
					msg.what = 1;
					mHandler.sendMessage(msg);
					break;
				case WrongVersion:
					msg.what = 2;
					mHandler.sendMessage(msg);
					break;
				case WrongClientType:
					msg.what = 3;
					mHandler.sendMessage(msg);
					break;
				default:
					break;
			}
		}
	};

	private void jumpToChannel() {
		MainApp.setUserName(userName);
		Intent i = new Intent(LoginActivity.this, ChannelActivity.class);
		startActivity(i);
		autoFinish = true;

		//从finish到真正执行onDestroy，中间可能隔10秒钟。如果这期间用户退出app，则这里还会收到并显示提示
		//因此，这里提前unregisterObserver。
		if (mService != null) {
			mService.unregisterObserver(serviceObserver);
		}

		if (mServiceConnection != null) {
			// Unbind to service
			if (mServiceBind) {
				unbindService(mServiceConnection);
			}
			mServiceConnection = null;
		}

		finish();
	}

	private class EditChangedListener implements TextWatcher {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			String userId = mETUserId.getText().toString();
			String pwd = mETPassword.getText().toString();

			if (AppCommonUtil.validUserId(userId) && AppCommonUtil.validPwd(pwd)) {
				mBtnLogin.setEnabled(true);
			}
			else {
				mBtnLogin.setEnabled(false);
			}
		}

		@Override
		public void afterTextChanged(Editable s) {

		}
	};
	private boolean isH6 = (Build.MODEL != null && Build.MODEL.equals("HX88S"));
	//private boolean isH6 = true;
}
