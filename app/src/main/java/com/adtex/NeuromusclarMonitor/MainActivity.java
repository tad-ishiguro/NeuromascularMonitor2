package com.adtex.NeuromusclarMonitor;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbManager;
import android.icu.util.Output;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
//import android.view.Menu;
//import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

class CustomProber {



	static UsbSerialProber getCustomProber() {
		ProbeTable customTable = new ProbeTable();
		customTable.addProduct(0x16d0, 0x087e, CdcAcmSerialDriver.class); // e.g. Digispark CDC
		return new UsbSerialProber(customTable);
	}

}

class GlobalVariable
{
    /**
    * グローバル変数x3。
    */
	public static MainActivity	m_ma;
	public static Settings		m_settings;
	public static Settings2 m_settings2;
}

public class MainActivity extends Activity  implements Runnable, FileSelectionDialog.OnFileSelectListener
{
	long m_lPrevToastTime;		//前回パルスのToast表示をした時間
	int		m_nOldToastPulseMode;	//前回　Toast表示したパルスモード

	int   nstep = 0;		// adtex 筋弛緩の深さのシミュレート用

	BatteryObj	m_BatteryObj;
	long	m_lBlinkTime = 0;
	boolean		m_bBtnBlinkFlg = false;
	long	m_lLastPulseLimitTime = 0;
	Drawable m_btn_unable;
	Drawable m_btn_red;
	Drawable m_btn_green;
	Drawable m_btn_yellow;
	Drawable m_btn_pink;
	Drawable m_btn_white;
	Drawable m_btn_blue;
	Drawable m_btn_play_circle;
	Drawable m_btn_pause_circle;
	Drawable m_btn_arrow_downward;
	Drawable m_btn_arrow_upward;
	Drawable m_btn_arrow_downward_unable;
	Drawable m_btn_arrow_upward_unable;

	int	m_nPrgEndCount = 0;			//プログラム終了処理
	boolean m_bPrgEndFlg = false;
	int m_nCurrentBtnPos = 0;
	boolean m_bDAPowerOnFlg = false;
	String	m_Msg = "";
	public boolean	m_bMsgReq = false;
	int		m_nCurrentPulseWidth = 0;
	short	m_sWork1[], m_sWork2[];
	private static final int	REQUEST_FILESELECT	= 0;

//public class MainActivity extends Activity {
	InputComment			m_dialog;		//コメント入力のためのダイアログ
	HiddenDlg				m_HiddenDlg;
	StartDlg				m_StartDlg;
	long		m_lLastAutoGainTime = 0;
	SurfaceView m_surfaceView;
	MySurfaceView m_SView;
    ProgressDialog m_progressDialog = null;		//保存中のダイアログ
    Thread m_thread;				//ファイル保存スレッド
	public String		m_ParamFileName = "";
	public String		m_ParamFileDir = "/";

	int		m_nComKind2, m_nComKind, m_nOldComKind;
	//m_nComKind2 ハードウェアー制御
	//m_nComKind データ制御（ファイル読み込み時）
	//m_nOldComKind パラメータ変更時の処理
	//0 内部　BLE　シングルセンサー
	//1 内部　BLE　マルチセンサー
	public String				m_strInitialDir		= "/";
	int		m_nFileReadFlg = 0;		//0ならデータなし　　1:測定したデータ　　2:ファイルから読み込んだデータ
	BLEObj	m_BLEObj;			//BLE　通信クラス
	long	m_lConnectTime;		//測定終了時の時間
	private Timer mainTimer;					//タイマー用
	private MainTimerTask mainTimerTask;		//タイマタスククラス
	private Handler mHandler = new Handler();
	int		m_nDisConnectStatus = 0;	//0:測定している状態   1:測定していない状態　 2:接続していない状態
	int		m_nDisConnectTime;			//測定終了後　BLEとの接続を切り離すまでの時間
	public boolean m_bReConnectFlg;	//BLEの再接続が必要なら ture
    public boolean m_bMeasStartFlg;	//測定開始が必要な場合 true
	public boolean m_bConnectStartFlg;	//通信接続開始フラグ
    public boolean m_bDisConnectFlg;	//BLEs切断を行う場合true
    public	String	m_Address = " ";	//最後に接続したBLEデバイスのアドレス
    public boolean		m_bAddressFlg = false;	//アプリ起動後　、BLEに接続したらtrue
	public	String	m_DeviceName = "AYA";	//BLE接続を許可する　デバイス名
	public String	m_SensorName, m_SensorName2;	//センサーの名前　表示用
	int	m_nCertifyFlg = 0;			//0:認証まだ　　1:認証中　　2:認証終了	3:認証失敗
	AESObj	m_AESObj = new AESObj();	//認証　暗号　クラス

	long	m_lLastDataTime;		//最後にデータを取得した時間
	int		m_nSendGainLevelFlg = -1;		//0 測定開始　　１：ch1 GainLevel 送る　　2:ch2 GainLevel送る
	int		m_nBtnSize = 24;	//ボタンサイズ
	public boolean		m_bBLELogFlg;		//BLE通信ログファイルを作成するか否か
	public	boolean		m_bUARTStartFlg = false;	//USBシリアル通信測定中フラグ
	long		m_lConnectTime2 = 0;	//接続時にセット　スタートコマンドを送信するための時間
	long		m_lConnectTime3 = 0;	//認証コマンド　送ったときにセット　2秒以内に認証終了しなければ　認証エラー
    SharedPreferences m_preferences, m_pref_private;	//パラメータ、登録情報を記録
    final static int REQUEST_CODE=0;

	public SignalObj m_View;		//データ処理、描画　
	CUsbCom			m_UsbCom;		//USBシリアル通信
	boolean m_bTouchFlg;			//タッチしたら true
	long		m_lTouchTime;

	boolean m_bADStartFlg = false;	//ADが動作していたらtrue

    Button	m_Button[];
    int		m_nNoOfButton;
	private Object UsbSerialProber;

	/*--------------------------
     電話の固有番号、ストレージへのアクセス、GPSへのアクセス（BLEへのアクセスのため）、
     インターネットへのアクセス、許可を得る
    ------------------------- */
	public void CheckPermissionMain()
	{
		boolean bRet;
		int nAPILevel = Build.VERSION.SDK_INT;

		if(23 <= nAPILevel)
		{
			bRet = CheckPermissionSub(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if(bRet) {
				if ( 29 <= nAPILevel )
					bRet = CheckPermissionSub(Manifest.permission.ACCESS_FINE_LOCATION);
				else
					bRet = CheckPermissionSub(Manifest.permission.ACCESS_COARSE_LOCATION);
			}
		}
	}
/*
 * (非 Javadoc)
 * @see android.app.Activity#onRequestPermissionsResult(int, java.lang.String[], int[])
 * デバイスの使用許可のダイアログを表示する
 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
		{
			if(permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE))
				MessageBoxPermission(getString(R.string.permission_storage));
			if(permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION))
				MessageBoxPermission(getString(R.string.permission_location));
			if(permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION))
				MessageBoxPermission(getString(R.string.permission_location));
			if(permissions[0].equals(Manifest.permission.INTERNET))
				MessageBoxPermission(getString(R.string.permission_internet));
//			finish();
		}
		CheckPermissionMain();
	}

	/*
	 *  まだデバイスの使用が許可されていない場合は、許可の要求を行う
	 */
	@TargetApi(23)
	public boolean CheckPermissionSub(String str)
	{
		boolean bRet = true;
		if(23 <= Build.VERSION.SDK_INT)
		{
			bRet = checkPermission(str);
			if(bRet == false)
			{
				requestPermissions(new String[]{str}, 1);
			}
		}
		return bRet;
	}

	/*
	 * 既にデバイスの使用が許可されている場合は true  許可されていない場合はfalse を返す
	 */
    public boolean checkPermission(String str) {
        // 既に許可している

      if (ContextCompat.checkSelfPermission(this, str) == PackageManager.PERMISSION_GRANTED)
        	return true;
        // 拒否していた場合
        else
        	return false;
//            requestLocationPermission();
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		GlobalVariable.m_ma = this;
		CheckPermissionMain();		//アプリが最初の起動されたときにデバイスの使用許可を得る
		onCreateSub();
	}

	@SuppressLint("ClickableViewAccessibility")	//setontouchlistener のワーニングを消す
	protected void onCreateSub()
	{
// open device from github

		// Find all available drivers from attached devices.
		UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
		if (availableDrivers.isEmpty()) {
			return;
		}

		// Open a connection to the first available driver.
		UsbSerialDriver driver = availableDrivers.get(0);
		UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
		if (connection == null) {
			// add UsbManager.requestPermission(driver.getDevice(), ..) handling here
			return;
		}

		UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
		port.open(connection);
		port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);



		m_nNoOfButton = 15;
		m_Button = new Button[m_nNoOfButton];
		byte[] dummy = new byte[16];	//AES 暗号キー
/*----------*/
		dummy[0] = -87;
		dummy[1] = -51;
		dummy[2] = -101;
		dummy[3] = 95;
		dummy[4] = 24;
		dummy[5] = -8;
		dummy[6] = 40;
		dummy[7] = 96;
		dummy[8] = 23;
		dummy[9] = 50;
		dummy[10] = 91;
		dummy[11] = 23;
		dummy[12] = -42;
		dummy[13] = -127;
		dummy[14] = -43;
		dummy[15] = 9;
/*---------------*/
		m_BatteryObj = new BatteryObj();
		m_BatteryObj.InitObj();
		m_sWork1 = new short[20];
		m_sWork2 = new short[20];
		m_AESObj.SetCommonKey(dummy);	//AES　暗号キーをセット
		m_bReConnectFlg = false;
		m_bDisConnectFlg = false;
		GlobalVariable.m_ma = this;
		m_preferences = PreferenceManager.getDefaultSharedPreferences(this);
		m_pref_private = getSharedPreferences("SmartPulseAnalzer", MODE_PRIVATE);

		m_lPrevToastTime = 0;		//前回パルスのToast表示をした時間
		m_nOldToastPulseMode = -1;	//前回　Toast表示したパルスモード

        m_bTouchFlg = false;

		final Handler handler = new Handler();	/* adtex */


		m_UsbCom = new CUsbCom(this);	//UART 通信オブジェクト
		m_View = new SignalObj();	//データ処理オブジェクト
		m_BLEObj = new BLEObj(this);	//BLE 通信オブジェクト

        readPreferences();	//パラメータの読み込み
        m_SensorName = m_SensorName2;
        m_bMeasStartFlg = false;
        m_bConnectStartFlg = false;
        m_lConnectTime = 0;
        m_nOldComKind = m_nComKind2;
        m_nComKind = m_nComKind2;

       	m_BLEObj.OnCreate();
       	m_UsbCom.InitObj(this);
		//タイマーインスタンス生成
		mainTimer = new Timer();
		//タスククラスインスタンス生成
		mainTimerTask = new MainTimerTask();
		//タイマースケジュール設定＆開始
//		mainTimer.schedule(mainTimerTask, 100, 1500);
		mainTimer.schedule(mainTimerTask, 100, 100);
		m_View.InitObj(this);
        m_surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
		m_SView = new MySurfaceView(this);
		m_SView.SetSurface(m_surfaceView);

		m_btn_unable = ResourcesCompat.getDrawable(getResources(), R.drawable.button_unable, null);
		m_btn_red = ResourcesCompat.getDrawable(getResources(), R.drawable.button_red, null);
		m_btn_green = ResourcesCompat.getDrawable(getResources(), R.drawable.button_green_state, null);
		m_btn_blue = ResourcesCompat.getDrawable(getResources(), R.drawable.button_blue_state, null);
		m_btn_yellow = ResourcesCompat.getDrawable(getResources(), R.drawable.button_yellow_state, null);
		m_btn_pink = ResourcesCompat.getDrawable(getResources(), R.drawable.button_pink_state, null);
		m_btn_white = ResourcesCompat.getDrawable(getResources(), R.drawable.button_white_state, null);
		m_btn_play_circle = ResourcesCompat.getDrawable(getResources(), R.drawable.play_circle, null);
		m_btn_pause_circle = ResourcesCompat.getDrawable(getResources(), R.drawable.pause_circle, null);
		m_btn_arrow_downward = ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_downward, null);
		m_btn_arrow_upward = ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_upward, null);
		m_btn_arrow_downward_unable = ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_downward_unable, null);
		m_btn_arrow_upward_unable = ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_upward_unable, null);

		m_Button[0] = (Button)findViewById(R.id.start_btn);
		m_Button[1] = (Button)findViewById(R.id.TOF_btn);
		m_Button[2] = (Button)findViewById(R.id.PTC_btn);
		m_Button[3] = (Button)findViewById(R.id.Twitch_btn);
		m_Button[4] = (Button)findViewById(R.id.AUTO_btn);
		m_Button[5] = (Button)findViewById(R.id.cal_btn);
		m_Button[6] = (Button)findViewById(R.id.current_down_btn);
		m_Button[7] = (Button)findViewById(R.id.current_up_btn);
		m_Button[8] = (Button)findViewById(R.id.setting_btn);
		m_Button[9] = (Button)findViewById(R.id.TET_btn);
		m_Button[10] = (Button)findViewById(R.id.DBS_btn);
		m_Button[11] = (Button)findViewById(R.id.stop_btn);
		m_Button[12] = (Button)findViewById(R.id.file_save_btn);
		m_Button[13] = (Button)findViewById(R.id.file_open_btn);
		m_Button[14] = (Button)findViewById(R.id.recalc_btn);


        m_Button[0].setOnClickListener(new View.OnClickListener() {		//スタート
            @Override
            public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				StartMeasBtn();
            }
        });


		m_Button[1].setOnTouchListener(new View.OnTouchListener()	//STOP　ボタンが長押しされた場合の処理 10秒長押しで隠しダイアログ表示
		{
			// ボタンがタッチされた時のハンドラ
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */

				// TOF　Button
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					// 指がタッチした時の処理を記述
					m_lTouchTime = System.currentTimeMillis();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					// タッチした指が離れた時の処理を記述
					if(m_View.m_nPulseMode == 0) {
						long lTime = System.currentTimeMillis();
						float fSub = (lTime - m_lLastPulseLimitTime) / 1000.0F;
						if(fSub < m_View.m_fTOFTimeLimit)
						{
							String msg, str;
							str = getString(R.string.TOF_LIMIT);
							msg = String.format(str, (m_View.m_fTOFTimeLimit - fSub));
							MessageBox(msg);
							return false;
						}
						m_lLastPulseLimitTime = lTime;
						long lSub = (lTime - m_lTouchTime);
						if ( 500 < lSub )
							m_View.AutoTOFStart();
						else
							m_View.OneTOFPulse();
					}
					else if(m_View.m_nPulseMode == 4)
						m_View.m_nPulseMode = 0;
				}
				return false;
			}
		});
		m_Button[2].setOnTouchListener(new View.OnTouchListener()	//STOP　ボタンが長押しされた場合の処理 10秒長押しで隠しダイアログ表示
		{
			// ボタンがタッチされた時のハンドラ
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{	//PTC
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				// PTC　Button
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					// 指がタッチした時の処理を記述
					m_lTouchTime = System.currentTimeMillis();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					// タッチした指が離れた時の処理を記述
					if(m_View.m_nPulseMode == 0) {
						long lTime = System.currentTimeMillis();
						float fSub = (lTime - m_lLastPulseLimitTime) / 1000.0F;
						if(fSub < m_View.m_fPTCTimeLimit)
						{
							String msg, str;
							str = getString(R.string.PTC_LIMIT);
							msg = String.format(str, (m_View.m_fPTCTimeLimit - fSub));
							MessageBox(msg);
							return false;
						}
						m_lLastPulseLimitTime = lTime;
						long lSub = (lTime - m_lTouchTime);
						if ( 500 < lSub )
							m_View.AutoPTCStart();
						else
							m_View.OnePTCPulse();
					}
					else if(m_View.m_nPulseMode == 12)
						m_View.m_nPulseMode = 0;
				}
				return false;
			}
		});
		m_Button[3].setOnTouchListener(new View.OnTouchListener()	//STOP　ボタンが長押しされた場合の処理 10秒長押しで隠しダイアログ表示
		{
			// ボタンがタッチされた時のハンドラ
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				// TWITCH　Button
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					// 指がタッチした時の処理を記述
					m_lTouchTime = System.currentTimeMillis();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					// タッチした指が離れた時の処理を記述
					if(m_View.m_nPulseMode == 0) {
						long lTime = System.currentTimeMillis();
						long lSub = (lTime - m_lTouchTime);
						if ( 500 < lSub )
							m_View.AutoTwitchStart();
						else
							m_View.OneTwitchPulse();
					}
					else if(m_View.m_nPulseMode == 6)
						m_View.m_nPulseMode = 0;
				}
				return false;
			}
		});
		m_Button[4].setOnTouchListener(new View.OnTouchListener()	//STOP　ボタンが長押しされた場合の処理 10秒長押しで隠しダイアログ表示
		{
			// ボタンがタッチされた時のハンドラ
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{	//AutoPilot
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				// AutoPilot　Button
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					// 指がタッチした時の処理を記述
					m_lTouchTime = System.currentTimeMillis();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					// タッチした指が離れた時の処理を記述
					if(m_View.m_nPulseMode == 0) {
						long lTime = System.currentTimeMillis();
						float fSub = (lTime - m_lLastPulseLimitTime) / 1000.0F;
						if(fSub < m_View.m_fTOFTimeLimit)
						{
							String msg, str;
							str = getString(R.string.TOF_LIMIT);
							msg = String.format(str, (m_View.m_fTOFTimeLimit - fSub));
							MessageBox(msg);
							return false;
						}
						long lSub = (lTime - m_lTouchTime);
						if ( 500 < lSub ) {
							m_lLastPulseLimitTime = lTime;
							m_View.AutoPilotStart();
						}

					}
					else if(m_View.m_nPulseMode == 13 || m_View.m_nPulseMode == 14)
						m_View.m_nPulseMode = 0;
				}
				return false;
			}
		});



		m_Button[5].setOnTouchListener(new View.OnTouchListener()	//Calibration
		{
			// ボタンがタッチされた時のハンドラ
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				// Calc　Button
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					// 指がタッチした時の処理を記述
					m_lTouchTime = System.currentTimeMillis();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					if(m_View.m_nCorrectionKind == 1)
					{
						MessageBox(getString(R.string.Correction_Setting_ON));
						return false;
					}
					// タッチした指が離れた時の処理を記述
					if(m_View.m_nPulseMode == 0) {
						long lTime = System.currentTimeMillis();
						long lSub = (lTime - m_lTouchTime);
						if ( 500 < lSub )
							m_View.AutoCalibrationStart();
						else
							m_View.PulseCalibration();
					}
					else if(m_View.m_nPulseMode == 2)
						m_View.m_nPulseMode = 0;

				}
				return false;
			}
		});
		m_Button[6].setOnClickListener(new View.OnClickListener() {		//ファイルを保存
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				CurrentBtn(false);

			}
		});
		m_Button[7].setOnClickListener(new View.OnClickListener() {		//ファイルを保存
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				CurrentBtn(true);

			}
		});

		m_Button[8].setOnTouchListener(new View.OnTouchListener()	//設定　ボタンが長押しされた場合の処理 10秒長押しで隠しダイアログ表示
		{
			// ボタンがタッチされた時のハンドラ
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				// TODO Auto-generated method stub
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					// 指がタッチした時の処理を記述
					m_lTouchTime = System.currentTimeMillis();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					// タッチした指が離れた時の処理を記述
					long	lTime = System.currentTimeMillis();
					long	lSub = (lTime - m_lTouchTime);
					if(7000 < lSub) {
						DspSettings();
//					    showHiddenDlg();
					}
					else
						DspSettings2();
				}
				return false;

			}
		});

		m_Button[9].setOnTouchListener(new View.OnTouchListener()	//TET
		{
			// ボタンがタッチされた時のハンドラ
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				// TET　Button
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					// 指がタッチした時の処理を記述
					m_lTouchTime = System.currentTimeMillis();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					// タッチした指が離れた時の処理を記述
					if(m_View.m_nPulseMode == 0) {
						long lTime = System.currentTimeMillis();
						float fSub = (lTime - m_lLastPulseLimitTime) / 1000.0F;
						m_lConnectTime = lTime;		//BLE　切断のための時間
						if(fSub < m_View.m_fTETTimeLimit)
						{
							String msg, str;
							str = getString(R.string.TET_LIMIT);
							msg = String.format(str, (m_View.m_fTETTimeLimit - fSub));
							MessageBox(msg);
							return false;
						}
						m_lLastPulseLimitTime = lTime;
						long lSub = (lTime - m_lTouchTime);
						if ( 500 < lSub )
							m_View.AutoTETStart();
						else
							m_View.OneTETPulse();
					}
					else if(m_View.m_nPulseMode == 8)
						m_View.m_nPulseMode = 0;
				}
				return false;
			}
		});
		m_Button[10].setOnTouchListener(new View.OnTouchListener()	//DBS
		{
			// ボタンがタッチされた時のハンドラ
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				// DBS　Button
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					// 指がタッチした時の処理を記述
					m_lTouchTime = System.currentTimeMillis();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					// タッチした指が離れた時の処理を記述
					if(m_View.m_nPulseMode == 0) {
						long lTime = System.currentTimeMillis();
						float fSub = (lTime - m_lLastPulseLimitTime) / 1000.0F;
						m_lConnectTime = lTime;		//BLE　切断のための時間
						if(fSub < m_View.m_fDBSTimeLimit)
						{
							String msg, str;
							str = getString(R.string.DBS_LIMIT);
							msg = String.format(str, (m_View.m_fDBSTimeLimit - fSub));
							MessageBox(msg);
							return false;
						}
						m_lLastPulseLimitTime = lTime;
						long lSub = (lTime - m_lTouchTime);
						if ( 500 < lSub )
							m_View.AutoDBSStart();
						else
							m_View.OneDBSPulse();
					}
					else if(m_View.m_nPulseMode == 10)
						m_View.m_nPulseMode = 0;
				}
				return false;
			}
		});
		m_Button[11].setOnClickListener(new View.OnClickListener() {		//Pulse Stop
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300); /* adtex */
				m_View.DAStop();
			}
		});
		m_Button[12].setOnClickListener(new View.OnClickListener() {		//ファイルを保存
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				FileSaveBtn();

			}
		});

		m_Button[13].setOnClickListener(new View.OnClickListener() {		//ファイルを開く
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				FileSelectOpen();

			}
		});
		m_Button[14].setOnClickListener(new View.OnClickListener() {		//再計算
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				if(!m_bADStartFlg && 0 < m_nFileReadFlg)
				{
					ProgressDialogStart(2);
					m_View.m_bDrawFlg = true;
				}
			}
		});



		(new Thread(new Runnable()	//スレッド開始　測定時のデータ解析
        {
        	@Override
        	public void run()
            {
            	int		i;

        		while(true)
        		{
        			try
        			{
						if ( m_bADStartFlg && m_View.m_nMeasMode == 1 ) {
							if ( m_View.m_nCorrectionKind == 1 ) {
								for (i = 0; i < 10; i++) {
									m_View.OutputCorrectPulse();
									Thread.sleep(10);
								}
							} else
								Thread.sleep(100);
							m_View.CalcMain();

							handler.post(new Runnable() {
								@Override
								public void run() {
									String	str;												/* adtex */
									TextView textView1 = (TextView)findViewById(R.id.displayCurrent);	/* adtex */
									TextView textView2 = (TextView)findViewById(R.id.batteryLevel);	    /* adtex */
									TextView textView3 = (TextView)findViewById(R.id.displayTOF);	    /* adtex */
									TextView textView4 = (TextView)findViewById(R.id.displayPTC);	    /* adtex */
									TextView textView5 = (TextView)findViewById(R.id.displayTOFP);	    /* adtex */
									TextView textView6 = (TextView)findViewById(R.id.displayTitleTOF);	    /* adtex */
									TextView textView7 = (TextView)findViewById(R.id.displayTitlePTC);	    /* adtex */
									TextView textView8 = (TextView)findViewById(R.id.displayTitleT4);	    /* adtex */
									TextView textView9 = (TextView)findViewById(R.id.Logo);	    /* adtex */
									TextView textView10 = (TextView)findViewById(R.id.SimMode);	    /* adtex */

									str = String.format("%.0f mA", m_View.m_fCurrentValue);		/* adtex */
									textView1.setText(str);										/* adtex */

									str = String.format("%d%%", m_View.m_nBatteryLevel);		/* adtex */
									textView2.setText(str);										/* adtex */

									// sim中かどうかで切り替え
									if (m_View.m_nCorrectionKind == 1)
									{
										textView10.setVisibility(View.VISIBLE);
										textView9.setVisibility(View.INVISIBLE);
									}
									else
									{
										textView9.setVisibility(View.VISIBLE);
										textView10.setVisibility(View.INVISIBLE);
									}

									// PTC
									if (m_View.m_fResultVal[m_View.m_nCalcGraphAveFlg][6] >= 0.5f)
									{
										str = String.format("%2.0f", m_View.m_fResultVal[m_View.m_nCalcGraphAveFlg][6]);
										textView4.setText(str);
										textView3.setVisibility(View.INVISIBLE);
										textView4.setVisibility(View.VISIBLE);
										textView5.setVisibility(View.INVISIBLE);
										textView6.setVisibility(View.INVISIBLE);
										textView7.setVisibility(View.VISIBLE);
										textView8.setVisibility(View.INVISIBLE);
									}
									// TOFP
									else if (m_View.m_fResultVal[m_View.m_nCalcGraphAveFlg][5] > 3.5f) {
										str = String.format("%3.0f", m_View.m_fResultVal[m_View.m_nCalcGraphAveFlg][4]);
										textView5.setText(str);
										textView3.setVisibility(View.INVISIBLE);
										textView4.setVisibility(View.INVISIBLE);
										textView5.setVisibility(View.VISIBLE);
										textView6.setVisibility(View.INVISIBLE);
										textView7.setVisibility(View.INVISIBLE);
										textView8.setVisibility(View.VISIBLE);
									}
									// TOF
									else {
										str = String.format("%2.0f", m_View.m_fResultVal[m_View.m_nCalcGraphAveFlg][5]);
										textView3.setText(str);
										textView3.setVisibility(View.VISIBLE);
										textView4.setVisibility(View.INVISIBLE);
										textView5.setVisibility(View.INVISIBLE);
										textView6.setVisibility(View.VISIBLE);
										textView7.setVisibility(View.INVISIBLE);
										textView8.setVisibility(View.INVISIBLE);
									}

								}
							});
						}
        			}
        			catch (InterruptedException e)
        			{
        				int a = 0;
        			}
        		}
            }
        })).start();
		CheckBLESetting();
		CheckLocationSetting();
		StartMeasBtn();
        m_View.SaveCorrectLog("clear");
        m_View.m_nCorrectionAllCount = 0;
    }

	public void ChangeButtonColor(long lTime) {
		int i;
		long	lSub = lTime - m_lBlinkTime;
		if ( m_View.m_nPulseMode == 2 ) {
			if(m_bBtnBlinkFlg)
				m_Button[5].setBackground(m_btn_pink);
			else
				m_Button[5].setBackground(m_btn_red);
		}
		else if ( m_View.m_nPulseMode == 4 ) {
			if(m_bBtnBlinkFlg)
				m_Button[1].setBackground(m_btn_blue);
			else
				m_Button[1].setBackground(m_btn_red);
		}
		else if ( m_View.m_nPulseMode == 6 ) {
			if(m_bBtnBlinkFlg)
				m_Button[3].setBackground(m_btn_white);
			else
				m_Button[3].setBackground(m_btn_red);
		}
		else if ( m_View.m_nPulseMode == 12 ) {
			if(m_bBtnBlinkFlg)
				m_Button[2].setBackground(m_btn_yellow);
			else
				m_Button[2].setBackground(m_btn_red);
		}
		else if ( m_View.m_nPulseMode == 13 || m_View.m_nPulseMode == 14 ) {
			if(m_bBtnBlinkFlg)
				m_Button[4].setBackground(m_btn_green);
			else
				m_Button[4].setBackground(m_btn_red);
		}
		if(1000 < lSub) {
			m_bBtnBlinkFlg = !m_bBtnBlinkFlg;
			m_lBlinkTime = lTime;
		}

		if ( m_View.m_nPulseMode != m_View.m_nOldPulseMode || m_View.m_nMeasMode != m_View.m_nOldMeasMode ) {
			m_Button[6].setBackground(m_btn_arrow_downward);
			m_Button[7].setBackground(m_btn_arrow_upward);

			if ( m_View.m_nMeasMode == 0 ) {
				m_Button[0].setBackground(m_btn_play_circle);
				m_Button[1].setBackground(m_btn_unable);
				m_Button[2].setBackground(m_btn_unable);
				m_Button[3].setBackground(m_btn_unable);
				m_Button[4].setBackground(m_btn_unable);
				m_Button[5].setBackground(m_btn_unable);
				m_Button[6].setBackground(m_btn_arrow_downward_unable);
				m_Button[7].setBackground(m_btn_arrow_upward_unable);
				m_Button[9].setBackground(m_btn_unable);
				m_Button[10].setBackground(m_btn_unable);
				m_Button[11].setBackground(m_btn_unable);
				m_Button[12].setBackground(m_btn_green);
				m_Button[13].setBackground(m_btn_green);
				m_Button[14].setBackground(m_btn_green);
			} else {
				m_Button[0].setBackground(m_btn_pause_circle);
				m_Button[1].setBackground(m_btn_blue);
				m_Button[2].setBackground(m_btn_yellow);
				m_Button[3].setBackground(m_btn_white);
				m_Button[4].setBackground(m_btn_green);
				m_Button[5].setBackground(m_btn_pink);
				m_Button[6].setBackground(m_btn_arrow_downward);
				m_Button[7].setBackground(m_btn_arrow_upward);
				m_Button[9].setBackground(m_btn_white);
				m_Button[10].setBackground(m_btn_white);
				m_Button[11].setBackground(m_btn_white);
				m_Button[12].setBackground(m_btn_unable);
				m_Button[13].setBackground(m_btn_unable);
				m_Button[14].setBackground(m_btn_unable);
			}
			if ( m_View.m_nPulseMode == 2 ) {
				m_Button[1].setBackground(m_btn_unable);
				m_Button[2].setBackground(m_btn_unable);
                m_Button[3].setBackground(m_btn_unable);
                m_Button[4].setBackground(m_btn_unable);
				m_Button[6].setBackground(m_btn_arrow_downward_unable);
				m_Button[7].setBackground(m_btn_arrow_upward_unable);
				m_Button[9].setBackground(m_btn_unable);
				m_Button[10].setBackground(m_btn_unable);
				m_Button[12].setBackground(m_btn_unable);
				m_Button[13].setBackground(m_btn_unable);
				m_Button[14].setBackground(m_btn_unable);
			} else if ( m_View.m_nPulseMode == 4 ) {
				m_Button[2].setBackground(m_btn_unable);
				m_Button[3].setBackground(m_btn_unable);
				m_Button[4].setBackground(m_btn_unable);
				m_Button[5].setBackground(m_btn_unable);
				m_Button[9].setBackground(m_btn_unable);
				m_Button[10].setBackground(m_btn_unable);
				m_Button[12].setBackground(m_btn_unable);
				m_Button[13].setBackground(m_btn_unable);
				m_Button[14].setBackground(m_btn_unable);
			} else if ( m_View.m_nPulseMode == 6 ) {
				m_Button[1].setBackground(m_btn_unable);
				m_Button[2].setBackground(m_btn_unable);
				m_Button[4].setBackground(m_btn_unable);
				m_Button[5].setBackground(m_btn_unable);
				m_Button[9].setBackground(m_btn_unable);
				m_Button[10].setBackground(m_btn_unable);
				m_Button[12].setBackground(m_btn_unable);
				m_Button[13].setBackground(m_btn_unable);
				m_Button[14].setBackground(m_btn_unable);
			} else if ( m_View.m_nPulseMode == 8 ) {
                m_Button[1].setBackground(m_btn_unable);
                m_Button[2].setBackground(m_btn_unable);
                m_Button[3].setBackground(m_btn_unable);
                m_Button[4].setBackground(m_btn_unable);
                m_Button[5].setBackground(m_btn_unable);
				m_Button[9].setBackground(m_btn_unable);
				m_Button[10].setBackground(m_btn_unable);
				m_Button[12].setBackground(m_btn_unable);
				m_Button[13].setBackground(m_btn_unable);
				m_Button[14].setBackground(m_btn_unable);
			}
			else if ( m_View.m_nPulseMode == 10 ){
                m_Button[1].setBackground(m_btn_unable);
                m_Button[2].setBackground(m_btn_unable);
                m_Button[3].setBackground(m_btn_unable);
                m_Button[4].setBackground(m_btn_unable);
                m_Button[5].setBackground(m_btn_unable);
				m_Button[9].setBackground(m_btn_unable);
				m_Button[10].setBackground(m_btn_unable);
				m_Button[12].setBackground(m_btn_unable);
				m_Button[13].setBackground(m_btn_unable);
				m_Button[14].setBackground(m_btn_unable);
			}
			else if ( m_View.m_nPulseMode == 12 ) {
				m_Button[1].setBackground(m_btn_unable);
				m_Button[3].setBackground(m_btn_unable);
				m_Button[4].setBackground(m_btn_unable);
				m_Button[5].setBackground(m_btn_unable);
				m_Button[9].setBackground(m_btn_unable);
				m_Button[10].setBackground(m_btn_unable);
				m_Button[12].setBackground(m_btn_unable);
				m_Button[13].setBackground(m_btn_unable);
				m_Button[14].setBackground(m_btn_unable);
			}
			else if ( m_View.m_nPulseMode == 13 || m_View.m_nPulseMode == 14) {
				m_Button[1].setBackground(m_btn_unable);
				m_Button[2].setBackground(m_btn_unable);
				m_Button[3].setBackground(m_btn_unable);
				m_Button[5].setBackground(m_btn_unable);
				m_Button[9].setBackground(m_btn_unable);
				m_Button[10].setBackground(m_btn_unable);
				m_Button[12].setBackground(m_btn_unable);
				m_Button[13].setBackground(m_btn_unable);
				m_Button[14].setBackground(m_btn_unable);
			}
			if(m_View.m_nOldPulseMode == 13 && m_View.m_nPulseMode == 14) {
				Toast toast = Toast.makeText(this, "Change to PTC mode", Toast.LENGTH_LONG);
				toast.show();
				toast.setGravity(Gravity.TOP, 50, 110);
			}
			if(m_View.m_nOldPulseMode == 14 && m_View.m_nPulseMode == 13) {
				Toast toast = Toast.makeText(this, "Change to TOF mode", Toast.LENGTH_LONG);
				toast.show();
				toast.setGravity(Gravity.TOP, 50, 110);
			}
			m_View.m_nOldPulseMode = m_View.m_nPulseMode;
			m_View.m_nOldMeasMode = m_View.m_nMeasMode;
		}
	}

	public void CurrentBtn(boolean bUpFlg)
	{
//		if(m_nCertifyFlg != 2)
//			ReConnect(false);
		if(m_View.m_nPulseMode == 2)
			return;
		if(bUpFlg)
			m_View.m_fCurrentValue += 1.0F;
		else
			m_View.m_fCurrentValue -= 1.0F;
		m_View.m_fCurrentValue = CheckParamRange(m_View.m_fCurrentValue, 1.0F, 60.0F);
		m_View.m_bDrawFlg = true;
		WritePreferencesGain();
	}

	public void StartMeasBtn()
	{
		nstep = 0;
		if(m_View.m_nMeasMode == 0)	//Start
		{
			if(m_View.m_nPulseMode == 8)
			{
				MessageBox(getString(R.string.Cannot_START_IN_TET));
				return;
			}
			else if(m_View.m_nPulseMode == 10)
			{
				MessageBox(getString(R.string.Cannot_START_IN_DBS));
				return;
			}
			m_StartDlg = new StartDlg(this, new StartDlgListener());
			m_StartDlg.m_PatientID = m_View.m_PatientID;
			m_StartDlg.show();
		}
		else if(m_View.m_nMeasMode == 1)
		{
//			m_Button[0].setText("START");

			m_nFileReadFlg = 1;
			m_View.MeasStop();
			if(m_View.m_bStopSaveFlg)
				ProgressDialogStart(0);
			m_View.m_nMeasMode = 0;

		}
	}


	//測定開始処理
	public void StartMeasBtnSub()
	{
		m_nComKind = m_nComKind2;
		m_View.m_nMeasMode = 1;
		{
			m_View.InitMeas(true, false);
			ReConnect(true);
		}
		m_bADStartFlg = true;
//		m_Button[0].setText(" STOP ");
		m_View.m_nCorrectionStartCount = 0;
		m_View.m_fCorrectionSum = 0.0F;
		m_View.m_nCorrectionAveCount = 0;
		m_View.SetMaxMin();
	}


	public class StartDlgListener implements StartDlg.DialogListener
	{
		public void onRegistSelected()
		{
			m_View.m_PatientID = m_StartDlg.m_PatientID;
			WritePreferences();
			StartMeasBtnSub();
		}

		public void onCancel()
		{
			// キャンセルした際の処理.
			;
		}
	}
	//隠しダイアログの表示　FTPサーバー関係
	public void showHiddenDlg()
	{
		m_HiddenDlg = new HiddenDlg(this, new HiddenListener());
		m_HiddenDlg.m_bDebugModeFlg = m_View.m_bDebugModeFlg;
		m_HiddenDlg.show();
	}

	//隠しダイアログの設定を受ける
	public class HiddenListener implements HiddenDlg.DialogListener
	{
		public void onRegistSelected()
		{
			m_View.m_bDebugModeFlg = m_HiddenDlg.m_bDebugModeFlg;

			WritePreferences();
		}

		public void onCancel()
		{
			// キャンセルした際の処理.
			;
		}
	}

	private void CheckBLESetting()
	{
		BluetoothAdapter bluetoothAdapter;
// Initializes Bluetooth adapter.
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();
// Ensures Bluetooth is available on the device and it is enabled. If not,
// displays a dialog requesting user permission to enable Bluetooth.
		if ( bluetoothAdapter == null || !bluetoothAdapter.isEnabled() )
		{
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, 1);
		}
	}

	private void CheckLocationSetting()
	{
		int nAPILevel = Build.VERSION.SDK_INT;
		if( nAPILevel < 23)
			return;
		LocationRequest locationRequest = LocationRequest.create();
//		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
		LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

		SettingsClient client = LocationServices.getSettingsClient(this);
		Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
		task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
			@Override
			public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
				// All location settings are satisfied. The client can initialize
				// location requests here.
				// ...
//				MessageBox("GPS ON　ですよ");
			}
		});

		task.addOnFailureListener(this, new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				if (e instanceof ResolvableApiException ) {
					// Location settings are not satisfied, but this can be fixed
					// by showing the user a dialog.
//					MessageBox(getString(R.string.GPS_ON_FOR_BLE));
					try {
						// Show the dialog by calling startResolutionForResult(),
						// and check the result in onActivityResult().
						ResolvableApiException resolvable = (ResolvableApiException) e;
						resolvable.startResolutionForResult(MainActivity.this,20);

					} catch (IntentSender.SendIntentException sendEx) {
						// Ignore the error.
					}
				}
			}
		});
	}

	public void FileSelectOpen()
	{
    	if(m_bADStartFlg)
    	{
    		MessageBox(getString(R.string.Cannot_Open_In_Measure));
    		return;
    	}
		FileSelectionDialog dlg = new FileSelectionDialog(this, this, "csv; CSV" );
		dlg.show( new File( m_strInitialDir ) );
	}

	public void DspSettings()
	{
		Intent intent = new Intent(this,Settings.class);
		//Intent intent = new Intent(this, SubActivity.class);
	    startActivityForResult(intent,REQUEST_CODE);
	}

	public void DspSettings2()
	{
		Intent intent = new Intent(this,Settings2.class);
		//Intent intent = new Intent(this, SubActivity.class);
		startActivityForResult(intent,REQUEST_CODE);
	}


	public void FileSaveBtn()
	{
		if(m_bADStartFlg)
			MessageBox(getString(R.string.Can_not_save_in_measurement));

		else if(0 < m_View.m_nLastPos)
			ProgressDialogStart(0);
		else
			MessageBox(getString(R.string.There_is_no_data));
	}

/*
 * 　データ保存ダイアログを表示して保存を開始する
 */
    public void ProgressDialogStart(int nKind)
    {
    	//nKind 0:Save   1:Read    2:Calc
    	if(m_progressDialog == null)
    		m_progressDialog = new ProgressDialog(this);
    	String	titile, msg;
		m_View.m_nDialogKind = nKind;
    	if(nKind == 0)
    	{
    		titile = getString(R.string.save_title);
    		msg = getString(R.string.save_message1);
    	}
    	else if(nKind == 1)
    	{
    		titile = getString(R.string.read_title);
    		msg = getString(R.string.read_message);
    	}
    	else
    	{
    		titile = getString(R.string.calc_title);
    		msg = getString(R.string.calc_message);
    	}
    	m_progressDialog.setTitle(titile);
    	m_progressDialog.setMessage(msg);
    	m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    	m_progressDialog.setIndeterminate(false);
        // プログレスダイアログの最大値を設定します
        m_progressDialog.setMax(100);
        // プログレスダイアログの値を設定します
        m_progressDialog.setProgress(0);

    	m_progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // ProgressDialog をキャンセル
//                    Log.d("test", "BUTTON_CANCEL clicked");
                	m_View.m_bDlgCancelFlg = true;
                    dialog.cancel();
                }
            });
    	m_progressDialog.show();

    	m_thread = new Thread(this);	//保存時スレッドをスタートさせる
    	m_thread.start();
    }


	@Override
	public void run() {
		// TODO 自動生成されたメソッド・スタブ
        try
        {
        	if(m_View.m_nDialogKind == 0)
        		//m_View.SaveData(m_progressDialog);
				m_View.SaveData(m_progressDialog, this);
        	else if(m_View.m_nDialogKind == 1)
        	{
        		m_View.ReadDataDlg(m_progressDialog);
        		m_View.ReCalcData();
//        		m_View.ReCalcDlg(m_progressDialog);
        	}
        	else
        		m_View.ReCalcDlg(m_progressDialog);
            Thread.sleep(100);
        }
        catch (InterruptedException e)
        {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }
        m_progressDialog.dismiss();
//        m_progressDialog = null;
//        handler.sendEmptyMessage(0);

	}



	//ファイル選択ダイアログにより読み出すファイルが選択された場合の処理
	public void onFileSelect( File file )
	{
		m_strInitialDir = file.getParent();
		String filename = file.getPath();
		m_View.m_SelectFileName = filename;
		ProgressDialogStart(1);
		SaveInitialDir();
	}



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {

        case BLEObj.REQUEST_SELECT_DEVICE:
        	if(!m_BLEObj.SelectDevice(resultCode, data))	//デバイスが選択された
			{
				m_bMeasStartFlg = false;
				m_View.m_nMeasMode = 0;
				m_bADStartFlg = false;
				m_bConnectStartFlg = false;

			}
        	//When the DeviceListActivity return, with the selected device address
//            ((TextView) findViewById(R.id.deviceName)).setText(m_BLEObj.mDevice.getName()+ " - connecting");
            break;

        case BLEObj.REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
//                Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
				Toast toast = Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT);
				toast.show();
				toast.setGravity(Gravity.TOP, 50, 110);

			} else {
                // User did not enable Bluetooth or an error occurred
  //              Log.d(TAG, "BT not enabled");
 //               Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
				Toast toast = Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT);
				toast.show();
				toast.setGravity(Gravity.TOP, 50, 110);

				finish();
            }

			break;
        default:
//            Log.e(TAG, "wrong request code");
            break;
        }
    }

	public class MainTimerTask extends TimerTask
	{
		@Override
		public void run()
		{
			//ここに定周期で実行したい処理を記述します

			mHandler.post( new Runnable()
			{
				public void run()
				{
					boolean bSendFlg = false;
					long	lDisconnectTime = m_nDisConnectTime * 60000;
					long	lTime = System.currentTimeMillis();
					long	lSub = lTime - m_lConnectTime;		//測定終了後　接続を切り離すための時間
					long	lSub4 = lTime - m_lConnectTime2;	//connect してからの時間
					long	lSub5 = lTime - m_lConnectTime3;	//認証コマンドを送ってからの時間
					long	lSub7 = lTime - m_lLastAutoGainTime;	//最後にオートゲインを調べた時間
					ChangeButtonColor(lTime);
					if(m_View.m_nFileVersion == 1)
					{
//						Toast.makeText(this,  getString(R.string.file_version_err), Toast.LENGTH_LONG).show();
						MessageBox(getString(R.string.file_version_err));
						m_View.m_nFileVersion = 0;
					}
					if(m_bMsgReq) {
						MessageBox(m_Msg);
						m_bMsgReq = false;
					}
					if(3000 < lSub7 && m_bADStartFlg)
					{
						m_lLastAutoGainTime = lTime;
						m_View.AutoGain();
					}
					if(m_bPrgEndFlg) {	//プログラム終了処理
						if(m_nPrgEndCount == 0) {
							WritePreferences();
							m_View.MeasStop();
							bSendFlg = true;
						}
						else if(m_nPrgEndCount == 1)
							m_View.DAPowerOn(false);
						else if(m_nPrgEndCount == 2)
							m_View.PowerOff();
						else if(m_nPrgEndCount == 3) {
							if ( !m_View.m_bDebugModeFlg )
								m_BLEObj.Connect(false);
						}
						else {
							m_bPrgEndFlg = false;
							finish();
						}
						m_nPrgEndCount++;
					}
					if(!m_bConnectStartFlg && m_nCertifyFlg == 2)	//通信確立済
					{
						if(m_nSendGainLevelFlg == 0)
						{
							m_View.SendGainCommand(0, m_View.m_nGainLevel[0]);
							m_nSendGainLevelFlg = 1;
							bSendFlg = true;

						}
						else if(m_nSendGainLevelFlg == 1 && 0 < m_nComKind)
						{
							m_View.SendGainCommand(1, m_View.m_nGainLevel[1]);
							m_nSendGainLevelFlg = 2;
							bSendFlg = true;
						}
						else if(!m_bDAPowerOnFlg)
						{
							m_View.DAPowerOn(true);
							m_bDAPowerOnFlg = true;
							bSendFlg = true;
						}
						else if(m_bDAPowerOnFlg && m_nCurrentPulseWidth != m_View.m_nPulseWidth)
						{
							m_View.SendPulseWidthCommand();
							m_nCurrentPulseWidth = m_View.m_nPulseWidth;
							bSendFlg = true;
						}
						else if(m_bMeasStartFlg == false)	//パルス出力処理
						{
							if(m_View.m_bDBSSecondPulse)	//DBS 第二パターンパルスの出力
							{
								if(m_View.m_lDBSSecondTime <= lTime) {
									if ( m_View.OutPutDBSPulse2() )
										bSendFlg = true;
								}
							}
							else if(m_View.m_nPTCPhase == 1)	//PTC 第一フェーズから第二フェーズへ
							{
								if(m_View.m_lPTCPhaseTime <= lTime) {
									if ( m_View.OutputPTCPulse2() )
										bSendFlg = true;
								}

							}
							else if(m_View.m_nPTCPhase == 2)	//PTC 第二フェーズから第三フェーズへ
							{
								if(m_View.m_lPTCPhaseTime <= lTime) {
									if ( m_View.OutputPTCPulse3())
										bSendFlg = true;
								}
							}
							else if(m_View.m_nPTCPhase == 3)	//PTC 第三フェーズから終了へ
							{
								if(m_View.m_lPTCPhaseTime <= lTime) {
									m_View.m_nPTCPhase = 0;
								}
							}
							else if(m_View.m_nPulseMode == 2)	//CAL
							{
								if(m_View.AutoCalibration())
									bSendFlg = true;
							}
							else if(m_View.m_nPulseMode == 4 || m_View.m_nPulseMode == 13)	//TOF
							{
								if(m_View.AutoTOFPulse())
									bSendFlg = true;
							}
							else if(m_View.m_nPulseMode == 6)	//TWITCH
							{
								if(m_View.AutoTwitchPulse())
									bSendFlg = true;
							}
							else if(m_View.m_nPulseMode == 8)	//TET
							{
								if(m_View.AutoTETPulse())
									bSendFlg = true;
							}
							else if(m_View.m_nPulseMode == 10)	//DBS
							{
								if(m_View.AutoDBSPulse())
									bSendFlg = true;
							}
							else if(m_View.m_nPulseMode == 12 || m_View.m_nPulseMode == 14)	//PTC
							{
								if(m_View.AutoPTCPulse())
									bSendFlg = true;
							}
						}
					}
					if(m_View.m_bDebugModeFlg)	//UART
					{
						if(m_nCertifyFlg == 2 && m_bMeasStartFlg && !bSendFlg)
						{
							SendMeasStartUART();
							m_bMeasStartFlg = false;
							bSendFlg = true;
						}
						if(m_bConnectStartFlg)
						{
							if(m_nCertifyFlg == 0 && m_lConnectTime2 != 0 && 2000 < lSub4)
							{
								m_nCertifyFlg = 1;
								SendCertifyUART();
								m_lConnectTime3 = lTime;
							}
							else if(m_nCertifyFlg == 1)
							{
								if(3000 < lSub5)
								{
									m_nCertifyFlg = 3;
									m_bConnectStartFlg = false;
									MessageBox(getString(R.string.certify_timeout_err));
									m_View.m_nMeasMode = 0;
//									m_Button[0].setText("START");
								}
							}
							else if(m_nCertifyFlg == 2)
								m_bConnectStartFlg = false;
						}
					}
					else //BLE
					{
						if(m_nCertifyFlg == 2 && !m_bConnectStartFlg && m_bMeasStartFlg && !bSendFlg)
						{
							m_BLEObj.StartBLEMeas(m_nComKind, m_View.m_nSamplingRate, 12);
							m_bMeasStartFlg = false;
							bSendFlg = true;
						}
						if(m_bReConnectFlg)
						{
							m_BLEObj.Connect(true);
							m_bReConnectFlg = false;
						}
						else if(m_BLEObj.m_bConnectFlg)
						{
							if(m_bConnectStartFlg && m_lConnectTime2 != 0 && 2000 < lSub4)
							{
								if(m_nCertifyFlg == 0)
								{
									m_nCertifyFlg = 1;
									m_BLEObj.SendCertify();
									m_lConnectTime3 = lTime;
								}
								else if(m_nCertifyFlg == 1)
								{
									if(3000 < lSub5)
									{
										m_nCertifyFlg = 3;
										m_bConnectStartFlg = false;
										MessageBox(getString(R.string.certify_timeout_err));
									}
								}
								else if(m_nCertifyFlg == 2)
									m_bConnectStartFlg = false;
							}
//							if(!m_bADStartFlg && m_View.m_nPulseMode == 0)	//測定終了後　接続を切り離すための処理
							if(!m_bADStartFlg)	//測定終了後　接続を切り離すための処理
							{
								if(m_nDisConnectStatus == 0)
								{
									m_nDisConnectStatus = 1;	//測定していない状態
									m_lConnectTime = lTime;
								}
								else if(lDisconnectTime < lSub && m_nDisConnectStatus == 1 && !m_bReConnectFlg && !m_bConnectStartFlg)
								{
									m_BLEObj.Connect(false);
									m_nDisConnectStatus = 2;	//接続していない状態
									m_nCertifyFlg = 0;
								}
							}
							else
							{
								m_nDisConnectStatus = 0;	//測定している状態
								m_lConnectTime = lTime;
							}
						}
						if(m_bDisConnectFlg && !m_BLEObj.m_bMeasFlg)
						{
							m_BLEObj.Connect(false);
							m_bDisConnectFlg = false;
							m_nDisConnectStatus = 2;
							m_nCertifyFlg = 0;
						}
					}
				}
			});
		}
	}

    /*
     * 　文字列をint値に変換
     */
	int StringToInt(SharedPreferences pref, String str, int nDef)
    {
    	int		ret = nDef;
    	int		dummy;
    	String	str2;
    	String	str3;
    	try
    	{
    		str3 = Integer.toString(nDef);
        	str2 = pref.getString(str, str3);
    		dummy = Integer.parseInt(str2);
    	}
    	catch(Exception ex)
    	{
    		dummy = nDef;

            Editor e = pref.edit();
            try
            {
                str2 = Integer.toString(dummy);
                e.putString(str,  str2);
                e.commit();
            }
    		catch(Exception exc)
            {
    			MessageBox("Error Occured", this);
            }
    	}
    	ret = dummy;
    	return ret;
    }

    /*
     * 　文字列をfloat値に変換
     */
    float StringToFloat(SharedPreferences pref, String str, float fDef)
    {
    	float	ret = fDef;
    	float	dummy;
    	String	str2;
    	String	str3;
    	try
    	{
    		str3 = Float.toString(fDef);
        	str2 = pref.getString(str, str3);
    		dummy = Float.parseFloat(str2);
    	}
    	catch(Exception ex)
    	{
    		dummy = fDef;

            Editor e = pref.edit();
            try
            {
                str2 = Float.toString(dummy);
                e.putString(str,  str2);
                e.commit();
            }
    		catch(Exception exc)
            {
    			MessageBox("Error Occured", this);
            }
    	}
    	ret = dummy;
    	return ret;
    }

    public void readPreferences() {
        String	str, str2;
        str = "";
        int		i;
        float	fDefValu, fDummy;
        float	fGainTable[] = new float[20];
		fGainTable[0] = 94.4F;
		fGainTable[1] = 162.0F;
		fGainTable[2] = 272.5F;
		fGainTable[3] = 458.5F;
		fGainTable[4] = 744.3F;
		fGainTable[5] = 1251.7F;
		fGainTable[6] = 2105.7F;
		fGainTable[7] = 3522.8F;
		fGainTable[8] = 5924.8F;
		fGainTable[9] = 9967.1F;

		try
        {
			String	filepath;
			filepath = Environment.getExternalStorageDirectory().getPath();
			m_ParamFileDir = m_preferences.getString("ParamFileDir_key2", filepath);
            m_strInitialDir = m_preferences.getString("InitialDir_key2", filepath);
        	m_View.m_DataFolder = "ADTEX Neuromuscular Monitor";
            m_nComKind2 = StringToInt(m_preferences, "ComKind_key", 0);
            m_nDisConnectTime = StringToInt(m_preferences, "disconnect_time_key3", 5);
//        	m_UsbCom.baudRate = 115200;
			m_UsbCom.baudRate = 115200;
        	m_UsbCom.dataBit = 8;
        	m_UsbCom.stopBit = 1;
        	m_UsbCom.parity = 0;
        	m_UsbCom.flowControl = 0;

			m_View.m_nNoOfGain = StringToInt(m_preferences, "NoOfGain_key", 10);
			for(i = 0; i < m_View.m_nNoOfGain; i++)
			{
				str = String.format("GainTableA%d_key", i);
				m_View.m_fGainAmp[i] = StringToFloat(m_preferences, str, fGainTable[i]);
			}
			m_View.SetGainConst();

//        	m_nBtnSize = StringToInt(m_preferences, "btn_font_size_key3", 14);
//        	m_nBtnSize = CheckParamRange(m_nBtnSize, 8, 40);
			m_nBtnSize = 14;

        	m_View.m_nCharSize = StringToInt(m_preferences, "mem_font_size_key2", 42);
        	m_View.m_nCharSize = CheckParamRange(m_View.m_nCharSize, 8, 100);
        	m_View.m_frPaint.setTextSize(m_View.m_nCharSize - 1);
        	m_View.m_MarkerPaint.setTextSize(m_View.m_nCharSize - 1);

        	m_View.m_nCharSize2 = StringToInt(m_preferences, "text_font_size_key2", 40);
        	m_View.m_nCharSize2 = CheckParamRange(m_View.m_nCharSize2, 8, 100);
        	m_View.m_Text.setTextSize(m_View.m_nCharSize2 - 1);
        	m_View.m_Bold.setTextSize(m_View.m_nCharSize2 * 2);
        	m_View.m_ErrText.setTextSize(m_View.m_nCharSize2 - 1);
        	m_View.m_OKText.setTextSize(m_View.m_nCharSize2 - 1);

        	m_View.m_fLowBandPassFreq1 = StringToFloat(m_preferences, "low_band_pass_freq_key1", 0.01F);
        	m_View.m_fLowBandPassFreq1 = CheckParamRange(m_View.m_fLowBandPassFreq1, 0.0F, 1000.0F);

        	m_View.m_fLowBandPassFreq2 = StringToFloat(m_preferences, "low_band_pass_freq_key2", 0.4F);
        	m_View.m_fLowBandPassFreq2 = CheckParamRange(m_View.m_fLowBandPassFreq2, 0.0F, 1000.0F);

        	if(m_View.m_fLowBandPassFreq2 < m_View.m_fLowBandPassFreq1)
        	{
        		fDummy = m_View.m_fLowBandPassFreq1;
        		m_View.m_fLowBandPassFreq1 = m_View.m_fLowBandPassFreq2;
        		m_View.m_fLowBandPassFreq2 = fDummy;
        	}

        	m_View.m_fHighPassFreq = StringToFloat(m_preferences, "hi_pass_freq_key", 15.0F);
        	m_View.m_fHighPassFreq = CheckParamRange(m_View.m_fHighPassFreq, 0.0F, 1000.0F);

        	m_View.m_fBandPassFreq1 = StringToFloat(m_preferences, "band_pass_freq_key1", 0.6F);
        	m_View.m_fBandPassFreq1 = CheckParamRange(m_View.m_fBandPassFreq1, 0.0F, 1000.0F);

        	m_View.m_fBandPassFreq2 = StringToFloat(m_preferences, "band_pass_freq_key2", 2.0F);
        	m_View.m_fBandPassFreq2 = CheckParamRange(m_View.m_fBandPassFreq2, 0.0F, 1000.0F);


        	if(m_View.m_fBandPassFreq2 < m_View.m_fBandPassFreq1)
        	{
        		fDummy = m_View.m_fBandPassFreq1;
        		m_View.m_fBandPassFreq1 = m_View.m_fBandPassFreq2;
        		m_View.m_fBandPassFreq2 = fDummy;
        	}

			m_View.m_fCalibrationStart = StringToFloat(m_preferences, "Calibration_Start_key", 15.0F);
			m_View.m_fCalibrationStart = CheckParamRange(m_View.m_fCalibrationStart, 0.0F, 60.0F);

			m_View.m_fCalibrationStep = StringToFloat(m_preferences, "Calibration_Step_key", 3.0F);
			m_View.m_fCalibrationStep = CheckParamRange(m_View.m_fCalibrationStep, 0.0F, 30.0F);

			m_View.m_nCalibrationInterval = StringToInt(m_preferences, "Calibration_Interval_key", 1000);
			m_View.m_nCalibrationInterval = CheckParamRange(m_View.m_nCalibrationInterval, 1000, 10000);

            m_View.m_nNoOfCalAve = StringToInt(m_preferences, "Calibration_Ave_key", 3);
            m_View.m_nNoOfCalAve = CheckParamRange(m_View.m_nNoOfCalAve, 1, 99);

            m_View.m_fCurrentValue = StringToFloat(m_preferences, "Current_Value_key", 50.0F);
			m_View.m_fCurrentValue = CheckParamRange(m_View.m_fCurrentValue, 0.0F, 60.0F);


			m_View.m_fDetectionThreshold = StringToFloat(m_preferences, "Detection_Threshold_key", 10.0F);
			m_View.m_fDetectionThreshold = CheckParamRange(m_View.m_fDetectionThreshold, 0.0F, 100.0F);

			m_View.m_fControlValue = StringToFloat(m_preferences, "Control_Value_key", 300.0F);
			m_View.m_fControlValue = CheckParamRange(m_View.m_fControlValue, 0.0F, 10000000.0F);

			m_View.m_nPulseWidth = StringToInt(m_preferences, "Pulse_Width_key", 200);
			m_View.m_nPulseWidth = CheckParamRange(m_View.m_nPulseWidth, 100, 1000);

			m_View.m_nTwitchInterval = StringToInt(m_preferences, "Twitch_Interval_key", 1);
			m_View.m_nTwitchInterval = CheckParamRange(m_View.m_nTwitchInterval, 1, 1000);

			m_View.m_fTOFStimInterval = StringToFloat(m_preferences, "TOF_STIM_INTERVAL_key", 0.5F);
			m_View.m_fTOFStimInterval = CheckParamRange(m_View.m_fTOFStimInterval, 0.01F, 1000.0F);

			m_View.m_fTOFInterval = StringToFloat(m_preferences, "TOF_INTERVAL_key", 15.0F);
			m_View.m_fTOFInterval = CheckParamRange(m_View.m_fTOFInterval, 0.01F, 1000.0F);

			m_View.m_fTOFTimeLimit = StringToFloat(m_preferences, "TOF_TIME_LIMIT_key", 12.0F);
			m_View.m_fTOFTimeLimit = CheckParamRange(m_View.m_fTOFTimeLimit, 1.0F, 10000.0F);

			m_View.m_nPTCTwitch1Num = StringToInt(m_preferences, "PTC_Twitch1_NUM_key", 0);
			m_View.m_nPTCTwitch1Num = CheckParamRange(m_View.m_nPTCTwitch1Num, 0, 30);

			m_View.m_nPTC_TETStimFreq = StringToInt(m_preferences, "PTC_TET_STIM_FREQ_key", 50);
			m_View.m_nPTC_TETStimFreq = CheckParamRange(m_View.m_nPTC_TETStimFreq, 1, 200);

			m_View.m_fPTC_TETStimTime = StringToFloat(m_preferences, "PTC_TET_STIM_TIME_key", 5.0F);
			m_View.m_fPTC_TETStimTime = CheckParamRange(m_View.m_fPTC_TETStimTime, 0.1F, 100.0F);

			m_View.m_nPTCTwitch2Num = StringToInt(m_preferences, "PTC_Twitch2_NUM_key", 10);
			m_View.m_nPTCTwitch2Num = CheckParamRange(m_View.m_nPTCTwitch2Num, 1, 30);

			m_View.m_fPTCAutoInterval = StringToFloat(m_preferences, "PTC_AUTO_INTERVAL_key", 120.0F);
			m_View.m_fPTCAutoInterval = CheckParamRange(m_View.m_fPTCAutoInterval, 0.0F, 10000.0F);

			m_View.m_fPTCTimeLimit = StringToFloat(m_preferences, "PTC_TIME_LIMIT_key", 120.0F);
			m_View.m_fPTCTimeLimit = CheckParamRange(m_View.m_fPTCTimeLimit, 0.0F, 10000.0F);

			m_View.m_nAutoPilotPTCLevel = StringToInt(m_preferences, "AUTO_PILOT_PTC_LEVEL_key", 10);
			m_View.m_nAutoPilotPTCLevel = CheckParamRange(m_View.m_nAutoPilotPTCLevel, 0, 30);

			m_View.m_fDBSStimInterval = StringToFloat(m_preferences, "DBS_STIM_INTERVAL_key", 20.0F);
			m_View.m_fDBSStimInterval = CheckParamRange(m_View.m_fDBSStimInterval, 1.0F, 10000.0F);

			m_View.m_fDBS_1_2_Interval = StringToFloat(m_preferences, "DBS_1_2_INTERVAL_key", 0.75F);
			m_View.m_fDBS_1_2_Interval = CheckParamRange(m_View.m_fDBS_1_2_Interval, 0.1F, 1000.0F);

			m_View.m_fDBS_1_1_Interval = StringToFloat(m_preferences, "DBS_1_1_INTERVAL_key", 20.0F);
			m_View.m_fDBS_1_1_Interval = CheckParamRange(m_View.m_fDBS_1_1_Interval, 0.1F, 1000.0F);

			m_View.m_nDBSPattern = StringToInt(m_preferences, "DBS_PATTERN_key", 0);
			m_View.m_nDBSPattern = CheckParamRange(m_View.m_nDBSPattern, 0, 2);

			m_View.m_nBatteryType = StringToInt(m_preferences, "BatteryType_key", 1);
			m_View.m_nBatteryType = CheckParamRange(m_View.m_nBatteryType, 0, 2);

			m_View.m_fBatteryVolt = StringToFloat(m_preferences, "BatteryVoltage_key", 3.0F);
			m_View.m_fBatteryVolt = CheckParamRange(m_View.m_fBatteryVolt, 0.0F, 10.0F);

			m_View.m_nBatteryLevel = GetBatteryLevel(m_View.m_fBatteryVolt, m_View.m_nBatteryType);

			m_View.m_fDBSTimeLimit = StringToFloat(m_preferences, "DBS_TIME_LIMIT_key", 20.0F);
			m_View.m_fDBSTimeLimit = CheckParamRange(m_View.m_fDBSTimeLimit, 0.0F, 10000.0F);

			m_View.m_nTETStimFreq = StringToInt(m_preferences, "TET_STIM_FREQ_key", 50);
			m_View.m_nTETStimFreq = CheckParamRange(m_View.m_nTETStimFreq, 50, 100);

			m_View.m_fTETStimTime = StringToFloat(m_preferences, "TET_STIM_TIME_key", 5.0F);
			m_View.m_fTETStimTime = CheckParamRange(m_View.m_fTETStimTime, 0.1F, 1000.0F);

			m_View.m_fTETTimeLimit = StringToFloat(m_preferences, "TET_TIME_LIMIT_key", 120.0F);
			m_View.m_fTETTimeLimit = CheckParamRange(m_View.m_fTETTimeLimit, 1.0F, 10000.0F);

			m_View.m_fHeartRateUpper = StringToFloat(m_preferences, "Heart_Rate_Upper_key", 200.0F);
        	m_View.m_fHeartRateUpper = CheckParamRange(m_View.m_fHeartRateUpper, 5.0F, 1000.0F);

        	m_View.m_fHeartRateLower = StringToFloat(m_preferences, "Heart_Rate_Lower_key", 40.0F);
        	m_View.m_fHeartRateLower = CheckParamRange(m_View.m_fHeartRateLower, 5.0F, 1000.0F);
        	if(m_View.m_fHeartRateUpper < m_View.m_fHeartRateLower)
        	{
        		fDummy = m_View.m_fHeartRateUpper;
        		m_View.m_fHeartRateUpper = m_View.m_fHeartRateLower;
        		m_View.m_fHeartRateLower = fDummy;
        	}
        	m_View.m_fRespiratoryRateUpper = StringToFloat(m_preferences, "Respiratory_Rate_Upper_key", 40.0F);
        	m_View.m_fRespiratoryRateUpper = CheckParamRange(m_View.m_fRespiratoryRateUpper, 5.0F, 200.0F);

        	m_View.m_fRespiratoryRateLower = StringToFloat(m_preferences, "Respiratory_Rate_Lower_key", 8.0F);
        	m_View.m_fRespiratoryRateLower = CheckParamRange(m_View.m_fRespiratoryRateLower, 5.0F, 1000.0F);
        	if(m_View.m_fRespiratoryRateUpper < m_View.m_fRespiratoryRateLower)
        	{
        		fDummy = m_View.m_fRespiratoryRateUpper;
        		m_View.m_fRespiratoryRateUpper = m_View.m_fRespiratoryRateLower;
        		m_View.m_fRespiratoryRateLower = fDummy;
        	}
        	m_View.m_nAverageTime = StringToInt(m_preferences, "average_time_key", 5);
        	m_View.m_nCalcGraphAveFlg = StringToInt(m_preferences, "calc_graph_ave_key", 0);
            m_DeviceName = m_preferences.getString("device_name_aya_key", "AYA");
        	m_bBLELogFlg = m_preferences.getBoolean("ble_log_key", true);
			m_View.m_bStopSaveFlg = m_preferences.getBoolean("stop_save_key", true);

        	m_View.m_nGainLevel[0] = StringToInt(m_preferences, "GainLevel_key", 1);
        	m_View.m_nGainLevel[1] = StringToInt(m_preferences, "GainLevel2_key", 1);
           	m_View.m_bAbsoluteTimeFlg = m_preferences.getBoolean("absolute_time_flg_key", true);
			m_View.m_bGraphCheck[0] = m_preferences.getBoolean("hr_check_key", true);
			m_View.m_bGraphCheck[1] = m_preferences.getBoolean("rr_check_key", true);
			m_View.m_bGraphCheck[2] = m_preferences.getBoolean("T1_check_key", true);
			m_View.m_bGraphCheck[3] = m_preferences.getBoolean("T1_ratio_check_key", false);
			m_View.m_bGraphCheck[4] = m_preferences.getBoolean("T4_check_key", true);
			m_View.m_bGraphCheck[5] = m_preferences.getBoolean("TOF_check_key", true);
			m_View.m_bGraphCheck[6] = m_preferences.getBoolean("PTC_check_key", true);
			m_View.m_bGraphCheck[7] = m_preferences.getBoolean("ch1_raw_graph_check_key", false);
			m_View.m_bGraphCheck[8] = m_preferences.getBoolean("ch2_raw_graph_check_key", false);
			m_View.m_bDebugModeFlg = m_preferences.getBoolean("debug_mode_check_key", false);
			m_View.m_bCorrectionLogFlg = m_preferences.getBoolean("correction_log_key", false);

			m_View.m_bAutoGainFlg = m_preferences.getBoolean("auto_gain_check_key", true);
        	m_View.m_fHeartAmpMag = StringToFloat(m_preferences, "Heart_Amp_Mag_key", 1.0F);
        	m_View.m_fHeartAmpMag = CheckParamRange(m_View.m_fHeartAmpMag, 0.001F, 10000.0F);
        	m_View.m_fRespiratoryAmpMag = StringToFloat(m_preferences, "Respiratory_Amp_Mag_key", 2.0F);
        	m_View.m_fRespiratoryAmpMag = CheckParamRange(m_View.m_fRespiratoryAmpMag, 0.001F, 10000.0F);
        	m_View.m_fTOF1AmpMag = StringToFloat(m_preferences, "TOF1_Amp_Mag_key", 3.0F);
        	m_View.m_fTOF1AmpMag = CheckParamRange(m_View.m_fTOF1AmpMag, 0.001F, 10000.0F);
			m_View.m_PatientID = m_preferences.getString("PatientID_key", "001");

			m_View.m_nCorrectionKind = StringToInt(m_preferences, "Correction_kind_key", 0);
			m_View.m_nCorrectionKind = CheckParamRange(m_View.m_nCorrectionKind, 0, 1);

			m_View.m_fAChReceptor = StringToFloat(m_preferences, "ACh_receptors_key", 300F);
//			m_View.m_fAChReceptor = CheckParamRange(m_View.m_fAChReceptor, .0F, .0F);

			m_View.m_fAChBlockRate = StringToFloat(m_preferences, "ACh_block_rate_key", 70.0F);  //ADTEX 変数化
			m_View.m_fAChBlockRate = CheckParamRange(m_View.m_fAChBlockRate, 0.0F, 100.0F);
			m_View.m_fAChBlockRateX = m_View.m_fAChBlockRate;

			m_View.m_adtex_AChBlockRate[0] = StringToFloat(m_preferences, "ACh_block_rate_key_0", 90.0F);  //ADTEX 変数化
			m_View.m_adtex_AChBlockRate[0] = CheckParamRange(m_View.m_adtex_AChBlockRate[0], 0.0F, 100.0F);

			m_View.m_adtex_AChBlockRate[1] = StringToFloat(m_preferences, "ACh_block_rate_key_1", 95.0F);  //ADTEX 変数化
			m_View.m_adtex_AChBlockRate[1] = CheckParamRange(m_View.m_adtex_AChBlockRate[1], 0.0F, 100.0F);

			m_View.m_adtex_AChBlockRate[2] = StringToFloat(m_preferences, "ACh_block_rate_key_2", 98.0F);  //ADTEX 変数化
			m_View.m_adtex_AChBlockRate[2] = CheckParamRange(m_View.m_adtex_AChBlockRate[2], 0.0F, 100.0F);

			m_View.m_adtex_AChBlockRate[3] = StringToFloat(m_preferences, "ACh_block_rate_key_3", 99.0F);  //ADTEX 変数化
			m_View.m_adtex_AChBlockRate[3] = CheckParamRange(m_View.m_adtex_AChBlockRate[3], 0.0F, 100.0F);

			m_View.m_adtex_AChBlockRate[4] = StringToFloat(m_preferences, "ACh_block_rate_key_4", 99.5F);  //ADTEX 変数化
			m_View.m_adtex_AChBlockRate[4] = CheckParamRange(m_View.m_adtex_AChBlockRate[4], 0.0F, 100.0F);

			m_View.m_adtex_AChBlockRate[5] = StringToFloat(m_preferences, "ACh_block_rate_key_5", 99.9F);  //ADTEX 変数化
			m_View.m_adtex_AChBlockRate[5] = CheckParamRange(m_View.m_adtex_AChBlockRate[5], 0.0F, 100.0F);

			m_View.m_adtex_AChBlockRate[6] = StringToFloat(m_preferences, "ACh_block_rate_key_6", 99.95F);  //ADTEX 変数化
			m_View.m_adtex_AChBlockRate[6] = CheckParamRange(m_View.m_adtex_AChBlockRate[6], 0.0F, 100.0F);

			m_View.m_adtex_AChBlockRate[7] = StringToFloat(m_preferences, "ACh_block_rate_key_7", 99.0F);  //ADTEX 変数化
			m_View.m_adtex_AChBlockRate[7] = CheckParamRange(m_View.m_adtex_AChBlockRate[7], 0.0F, 100.0F);

			m_View.m_adtex_AChBlockRate[8] = StringToFloat(m_preferences, "ACh_block_rate_key_8", 90.0F);  //ADTEX 変数化
			m_View.m_adtex_AChBlockRate[8] = CheckParamRange(m_View.m_adtex_AChBlockRate[8], 0.0F, 100.0F);

			m_View.m_adtex_AChBlockRate[9] = StringToFloat(m_preferences, "ACh_block_rate_key_9", 70.0F);  //ADTEX 変数化
			m_View.m_adtex_AChBlockRate[9] = CheckParamRange(m_View.m_adtex_AChBlockRate[9], 0.0F, 100.0F);

			m_View.m_fBlockOneStim = StringToFloat(m_preferences, "blocks_one_stim_key", 30F);
//			m_View.m_fBlockOneStim = CheckParamRange(m_View.m_fBlockOneStim, .0F, .0F);

			m_View.m_fRecoveryHalfLife = StringToFloat(m_preferences, "recovery_half_life_key", 0.3F);
//			m_View.m_fRecoveryHalfLife = CheckParamRange(m_View.m_fRecoveryHalfLife, .0F, .0F);

			m_View.m_fPTP = StringToFloat(m_preferences, "PTP_key", 0.1F);
//			m_View.m_fPTP = CheckParamRange(m_View.m_fPTP, .0F, .0F);

			m_View.m_fPTP_HalfLife = StringToFloat(m_preferences, "PTP_Half_life_key", 4.0F);
//			m_View.m_fPTP_HalfLife = CheckParamRange(m_View.m_fPTP_HalfLife, .0F, .0F);

//			m_View.m_fMinimumSensitivity = StringToFloat(m_preferences, "minimum_sensitivity_key", 2.0F);
//			m_View.m_fMinimumSensitivity = CheckParamRange(m_View.m_fMinimumSensitivity, 0.0F, 100.0F);

            m_View.m_fNoOfUnblockedReceptors = m_View.m_fAChReceptor * (1.0F - m_View.m_fAChBlockRateX / 100.0F);	//筋弛緩剤投与下でもブロックされていない受容体数(万)
            m_View.m_fNoOfBlocksDueStim = 0.0F;		//神経刺激によるACh受容体ブロック数(万)
            m_View.m_fNoOfUnblockedACh = m_View.m_fNoOfUnblockedReceptors;	//神経刺激後の非ブロックACh受容体数(万)
            m_View.m_fAChRecoveryRate = 0.0F;	//神経刺激によるACh受容体ブロック回復率
            m_View.m_fAChRecovery = 0.0F;		//ACh受容体ブロック回復量
            m_View.m_fPTP_Stim = 0.0F;	//神経刺激によるACh増加量(PTP)万
            m_View.m_fNoOfUnblockedReceptors2 = m_View.m_fNoOfUnblockedACh;	//増加量を加えた非ブロックACh受容体数(万)
            m_View.m_fInitReceptor = m_View.m_fNoOfUnblockedReceptors2 - 0.1F;
            m_View.m_nCorrectionCount = 0;


            boolean bFlg = false;
			for(i = 0; i < m_View.m_nNoOfCalc; i++)
			{
				if(m_View.m_bGraphCheck[i])
				{
					bFlg = true;
					break;
				}
			}
			if(!bFlg)
				m_View.m_bGraphCheck[0] = true;
        }
		catch (Exception e)
		{
			String	msg;
			msg = "readpreference err";
			MessageBox(msg, this);
        }
    }

    float	CheckParamRange(float fVal, float fMin, float fMax)
    {
    	fVal = (fVal < fMin)? fMin : (fMax < fVal)? fMax : fVal;
    	return fVal;
    }

    int		CheckParamRange(int fVal, int fMin, int fMax)
    {
    	fVal = (fVal < fMin)? fMin : (fMax < fVal)? fMax : fVal;
    	return fVal;
    }
    /*
     *　パラメータを記録する
     */


    public void StdSetting(int nCh, Context con)
    {
    	m_View.m_nCorrectionKind = 0;
		m_View.m_fCalibrationStart = 15.0F;	//キャリブレーション　スタート電流値 (mA)
		m_View.m_fCalibrationStep = 1.0F;		//キャリブレーション　ステップ電流値 (mA)
		m_View.m_nCalibrationInterval = 1000;	//キャリブレーション　パルス　出力間隔 (msec)
        m_View.m_nNoOfCalAve = 3;      //キャリブレーション時の移動平均数
        m_View.m_fCurrentValue = 50.0F;		//電流値(mA)
		m_View.m_fDetectionThreshold = 3.0F;	//検出閾値　　コントロール値の％で指定
//	m_View.m_fControlValue = 1024.0F;		//コントロール値
		m_View.m_nPulseWidth = 200;			//パルス幅(μsec)
		m_View.m_nTwitchInterval = 1;		//Twitch間隔(sec)
		m_View.m_fTOFInterval = 15.0F;			//パルスの間隔 T1-T1 sec
		m_View.m_fTOFStimInterval = 0.5F;		//パルスの間隔 T1-T2 sec
		m_View.m_fTOFTimeLimit = 12.0F;	//TOFマニュアル制限時間(sec)
		m_View.m_nPTCTwitch1Num = 0;	//PTC Twitch1 刺激回数
		m_View.m_nPTC_TETStimFreq = 50;	//PTC TET 刺激周波数(Hz)
		m_View.m_fPTC_TETStimTime = 5.0F;		//PTC TET 刺激時間(sec)
		m_View.m_nPTCTwitch2Num = 10;		//PTC Twitch2 刺激回数
		m_View.m_fPTCAutoInterval = 120.0F;	//PTC 自動繰り返し時間(sec)
		m_View.m_fPTCTimeLimit = 120.0F;	//PTC マニュアル制限時間(sec)
		m_View.m_nAutoPilotPTCLevel = 10;	//AutoPilot PTCからTOFにもどるためのPTC閾値
		m_View.m_fDBSStimInterval = 20.0F;	//DBS 刺激間隔(msec)
		m_View.m_fDBS_1_2_Interval = 0.75F;	//DBS 1-2 間隔(sec)
		m_View.m_fDBS_1_1_Interval = 20.0F;	//DBS 1-1 間隔(sec)
		m_View.m_nDBSPattern = 0;		//DBS バーストパターン   0:3.3    1:3.1   2:2.3
		m_View.m_fDBSTimeLimit = 20.0F;	//DBS マニュアル制限時間(sec)
		m_View.m_nTETStimFreq = 50;		//TET 刺激周波数(Hz)
		m_View.m_fTETStimTime = 5.0F;		//TET 刺激時間(sec)
		m_View.m_fTETTimeLimit = 120.0F;	//TET 制限時間(sec)

    	m_View.m_fHighPassFreq = 15.0F;
    	m_View.m_fLowBandPassFreq1 = 0.01F;
    	m_View.m_fLowBandPassFreq2 = 0.4F;
    	m_View.m_fBandPassFreq1 = 0.6F;
    	m_View.m_fBandPassFreq2 = 2.0F;
    	m_View.m_fHeartRateUpper = 240.0F;
    	m_View.m_fHeartRateLower = 30.0F;
    	m_View.m_fRespiratoryRateUpper = 50.0F;
    	m_View.m_fRespiratoryRateLower = 5.0F;
    	m_View.m_bAutoGainFlg = true;
    	m_View.m_fHeartAmpMag = 1.0F;
    	m_View.m_fRespiratoryAmpMag = 2.0F;
		m_View.m_fTOF1AmpMag = 3.0F;
    	m_nDisConnectTime = 5;
    	m_View.m_nCalcGraphAveFlg = 0;
    	m_View.m_nAverageTime = 5;
    	m_View.m_bAbsoluteTimeFlg = true;
    	int		i;
    	for(i = 0; i < 9; i++)
    	{
    		if(i == 0 || i == 1 || i == 2 || i == 4 || i == 5 || i == 6)
    			m_View.m_bGraphCheck[i] = true;
    		else
    			m_View.m_bGraphCheck[i] = false;
    	}
    	m_DeviceName = "AYA";
		m_View.m_fPulseMin = 0.0F;
		m_View.m_fPulseMax = 120.0F;
    	WritePreferences();
		String	msg;
		if(nCh == 0) {
			m_nComKind2 = 0;
			msg = getString(R.string.StdCh1SettigParam2);
		}
		else {
			m_nComKind2 = 1;
			msg = getString(R.string.StdCh2SettigParam2);
		}
		MessageBox(msg, con);
    }
    public void RabbitSetting(int nCh, Context con)
    {
		m_View.m_nCorrectionKind = 0;
		m_View.m_fHighPassFreq = 10.0F;
    	m_View.m_fLowBandPassFreq1 = 0.01F;
    	m_View.m_fLowBandPassFreq2 = 0.6F;
    	m_View.m_fBandPassFreq1 = 2.0F;
    	m_View.m_fBandPassFreq2 = 6.7F;
    	m_View.m_fHeartRateUpper = 400.0F;
    	m_View.m_fHeartRateLower = 150.0F;
    	m_View.m_fRespiratoryRateUpper = 70.0F;
    	m_View.m_fRespiratoryRateLower = 20.0F;
//    	m_View.m_fTOFInterval = 15.0F;
//    	m_View.m_fTOFStimInterval = 0.5F;

		m_View.m_fCalibrationStart = 15.0F;	//キャリブレーション　スタート電流値 (mA)
		m_View.m_fCalibrationStep = 3.0F;		//キャリブレーション　ステップ電流値 (mA)
		m_View.m_nCalibrationInterval = 1000;	//キャリブレーション　パルス　出力間隔 (msec)
        m_View.m_nNoOfCalAve = 3;      //キャリブレーション時の移動平均数
		m_View.m_fCurrentValue = 50.0F;		//電流値(mA)
		m_View.m_fDetectionThreshold = 10.0F;	//検出閾値　　コントロール値の％で指定
//	m_View.m_fControlValue = 1024.0F;		//コントロール値
		m_View.m_nPulseWidth = 200;			//パルス幅(μsec)
		m_View.m_nTwitchInterval = 1;		//Twitch間隔(sec)
		m_View.m_fTOFInterval = 15.0F;			//パルスの間隔 T1-T1 sec
		m_View.m_fTOFStimInterval = 0.5F;		//パルスの間隔 T1-T2 sec
		m_View.m_fTOFTimeLimit = 12.0F;	//TOFマニュアル制限時間(sec)
		m_View.m_nPTCTwitch1Num = 0;	//PTC Twitch1 刺激回数
		m_View.m_nPTC_TETStimFreq = 50;	//PTC TET 刺激周波数(Hz)
		m_View.m_fPTC_TETStimTime = 5.0F;		//PTC TET 刺激時間(sec)
		m_View.m_nPTCTwitch2Num = 15;		//PTC Twitch2 刺激回数
		m_View.m_fPTCAutoInterval = 120.0F;	//PTC 自動繰り返し時間(sec)
		m_View.m_fPTCTimeLimit = 360.0F;	//PTC マニュアル制限時間(sec)
		m_View.m_nAutoPilotPTCLevel = 5;	//AutoPilot PTCからTOFにもどるためのPTC閾値
		m_View.m_fDBSStimInterval = 20.0F;	//DBS 刺激間隔(msec)
		m_View.m_fDBS_1_2_Interval = 0.75F;	//DBS 1-2 間隔(sec)
		m_View.m_fDBS_1_1_Interval = 20.0F;	//DBS 1-1 間隔(sec)
		m_View.m_nDBSPattern = 0;		//DBS バーストパターン   0:3.3    1:3.1   2:2.3
		m_View.m_fDBSTimeLimit = 20.0F;	//DBS マニュアル制限時間(sec)
		m_View.m_nTETStimFreq = 50;		//TET 刺激周波数(Hz)
		m_View.m_fTETStimTime = 5.0F;		//TET 刺激時間(sec)
		m_View.m_fTETTimeLimit = 120.0F;	//TET 制限時間(sec)

    	m_View.m_bAutoGainFlg = true;
    	m_View.m_fHeartAmpMag = 1.0F;
    	m_View.m_fRespiratoryAmpMag = 2.0F;
		m_View.m_fTOF1AmpMag = 3.0F;
		m_View.m_fPulseMin = 0.0F;
		m_View.m_fPulseMax = 400.0F;

    	m_nDisConnectTime = 5;

		m_View.m_nCalcGraphAveFlg = 0;
		m_View.m_nAverageTime = 5;
		m_View.m_bAbsoluteTimeFlg = true;

    	int		i;
		for(i = 0; i < 9; i++)
		{
			if(i == 0 || i == 1 || i == 2 || i == 4 || i == 5 || i == 6)
				m_View.m_bGraphCheck[i] = true;
			else
				m_View.m_bGraphCheck[i] = false;
		}
    	m_DeviceName = "AYA";
    	WritePreferences();
		String	msg;
		if(nCh == 0) {
			m_nComKind2 = 0;
			msg = getString(R.string.RabbitCh1SettigParam2);
		}
		else {
			m_nComKind2 = 1;
			msg = getString(R.string.RabbitCh2SettigParam2);
		}
		MessageBox(msg, con);

    }

    public void WritePreferencesGain()
	{
		String	str, str2;
		int			i;

		Editor e = m_preferences.edit();
		try
		{
			str = Float.toString(m_View.m_fCurrentValue);
			e.putString("Current_Value_key", str);

			str = Integer.toString(m_View.m_nGainLevel[0]);
			e.putString("GainLevel_key", str);

			str = Integer.toString(m_View.m_nGainLevel[1]);
			e.putString("GainLevel2_key", str);

			e.commit();
		}
		catch (Exception f)
		{
			String	msg;
			msg = getString(R.string.param_err);
			MessageBox(msg);
		}
	}

	public void WritePreferencesCtrl()
	{
		String	str, str2;
		int			i;

		Editor e = m_preferences.edit();
		try
		{
			str = Float.toString(m_View.m_fControlValue);
			e.putString("Control_Value_key", str);

			e.commit();
		}
		catch (Exception f)
		{
			String	msg;
			msg = getString(R.string.param_err);
			MessageBox(msg);
		}
	}

    public void WritePreferences()
    {
    	String	str, str2;
    	int			i;

        Editor e = m_preferences.edit();
        try
        {
			e.putString("ParamFileDir_key2", m_ParamFileDir);

			e.putString("PatientID_key", m_View.m_PatientID);

			str = Integer.toString(m_View.m_nGainLevel[0]);
            e.putString("GainLevel_key", str);

        	str = Integer.toString(m_View.m_nGainLevel[1]);
            e.putString("GainLevel2_key", str);

			str = Integer.toString(m_View.m_nNoOfGain);
			e.putString("NoOfGain_key", str);

			for(i = 0; i < m_View.m_nNoOfGain; i++)
			{
				str2 = String.format("GainTableA%d_key", i);
				str = Float.toString(m_View.m_fGainAmp[i]);
				e.putString(str2, str);
			}

			e.putString("InitialDir_key2", m_strInitialDir);
        	str = Integer.toString(m_nComKind2);
            e.putString("ComKind_key", str);
            str = Integer.toString(m_nDisConnectTime);
            e.putString("disconnect_time_key3", str);
/*--
            str = Integer.toString(m_nBtnSize);
    		e.putString("btn_font_size_key3", str);
----*/
    		str = Integer.toString(m_View.m_nCharSize);
    		e.putString("mem_font_size_key2", str);
    		str = Integer.toString(m_View.m_nCharSize2);
    		e.putString("text_font_size_key2", str);

            str = Float.toString(m_View.m_fLowBandPassFreq1);
            e.putString("low_band_pass_freq_key1", str);

            str = Float.toString(m_View.m_fLowBandPassFreq2);
            e.putString("low_band_pass_freq_key2", str);

            str = Float.toString(m_View.m_fHighPassFreq);
            e.putString("hi_pass_freq_key", str);

            str = Float.toString(m_View.m_fBandPassFreq1);
            e.putString("band_pass_freq_key1", str);

            str = Float.toString(m_View.m_fBandPassFreq2);
            e.putString("band_pass_freq_key2", str);


			str = Float.toString(m_View.m_fCalibrationStart);
			e.putString("Calibration_Start_key", str);

			str = Float.toString(m_View.m_fCalibrationStep);
			e.putString("Calibration_Step_key", str);

			str = Integer.toString(m_View.m_nCalibrationInterval);
			e.putString("Calibration_Interval_key", str);

            str = Integer.toString(m_View.m_nNoOfCalAve);
            e.putString("Calibration_Ave_key", str);

			str = Float.toString(m_View.m_fCurrentValue);
			e.putString("Current_Value_key", str);

			str = Float.toString(m_View.m_fDetectionThreshold);
			e.putString("Detection_Threshold_key", str);

			str = Float.toString(m_View.m_fControlValue);
			e.putString("Control_Value_key", str);

			str = Integer.toString(m_View.m_nPulseWidth);
			e.putString("Pulse_Width_key", str);

			str = Integer.toString(m_View.m_nTwitchInterval);
			e.putString("Twitch_Interval_key", str);

			str = Float.toString(m_View.m_fTOFStimInterval);
			e.putString("TOF_STIM_INTERVAL_key", str);

			str = Float.toString(m_View.m_fTOFInterval);
            e.putString("TOF_INTERVAL_key", str);

			str = Float.toString(m_View.m_fTOFTimeLimit);
			e.putString("TOF_TIME_LIMIT_key", str);

			str = Integer.toString(m_View.m_nPTCTwitch1Num);
			e.putString("PTC_Twitch1_NUM_key", str);

			str = Integer.toString(m_View.m_nPTC_TETStimFreq);
			e.putString("PTC_TET_STIM_FREQ_key", str);

			str = Float.toString(m_View.m_fPTC_TETStimTime);
			e.putString("PTC_TET_STIM_TIME_key", str);

			str = Integer.toString(m_View.m_nPTCTwitch2Num);
			e.putString("PTC_Twitch2_NUM_key", str);

			str = Float.toString(m_View.m_fPTCAutoInterval);
			e.putString("PTC_AUTO_INTERVAL_key", str);

			str = Float.toString(m_View.m_fPTCTimeLimit);
			e.putString("PTC_TIME_LIMIT_key", str);

			str = Integer.toString(m_View.m_nAutoPilotPTCLevel);
			e.putString("AUTO_PILOT_PTC_LEVEL_key", str);

			str = Float.toString(m_View.m_fDBSStimInterval);
			e.putString("DBS_STIM_INTERVAL_key", str);

			str = Float.toString(m_View.m_fDBS_1_2_Interval);
			e.putString("DBS_1_2_INTERVAL_key", str);

			str = Float.toString(m_View.m_fDBS_1_1_Interval);
			e.putString("DBS_1_1_INTERVAL_key", str);

			str = Integer.toString(m_View.m_nDBSPattern);
			e.putString("DBS_PATTERN_key", str);

			str = Integer.toString(m_View.m_nBatteryType);
			e.putString("BatteryType_key", str);

			str = Float.toString(m_View.m_fBatteryVolt);
			e.putString("BatteryVoltage_key", str);

			str = Float.toString(m_View.m_fDBSTimeLimit);
			e.putString("DBS_TIME_LIMIT_key", str);

			str = Integer.toString(m_View.m_nTETStimFreq);
			e.putString("TET_STIM_FREQ_key", str);

			str = Float.toString(m_View.m_fTETStimTime);
			e.putString("TET_STIM_TIME_key", str);

			str = Float.toString(m_View.m_fTETTimeLimit);
			e.putString("TET_TIME_LIMIT_key", str);

        	e.putString("device_name_aya_key", m_DeviceName);
            e.putBoolean("ble_log_key", m_bBLELogFlg);
            e.putBoolean("absolute_time_flg_key", m_View.m_bAbsoluteTimeFlg);

            str = Float.toString(m_View.m_fHeartRateUpper);
            e.putString("Heart_Rate_Upper_key", str);

            str = Float.toString(m_View.m_fHeartRateLower);
            e.putString("Heart_Rate_Lower_key", str);

            str = Float.toString(m_View.m_fRespiratoryRateUpper);
            e.putString("Respiratory_Rate_Upper_key", str);

            str = Float.toString(m_View.m_fRespiratoryRateLower);
            e.putString("Respiratory_Rate_Lower_key", str);

            str = Integer.toString(m_View.m_nAverageTime);
            e.putString("average_time_key", str);

            str = Integer.toString(m_View.m_nCalcGraphAveFlg);
            e.putString("calc_graph_ave_key", str);

			str = Integer.toString(m_View.m_nCorrectionKind);
			e.putString("Correction_kind_key", str);

			str = Float.toString(m_View.m_fAChReceptor);
			e.putString("ACh_receptors_key", str);

			str = Float.toString(m_View.m_fAChBlockRate);
			e.putString("ACh_block_rate_key", str);

			str = Float.toString(m_View.m_adtex_AChBlockRate[0]);
			e.putString("ACh_block_rate_key_0", str);

			str = Float.toString(m_View.m_adtex_AChBlockRate[1]);
			e.putString("ACh_block_rate_keyy_1", str);

			str = Float.toString(m_View.m_adtex_AChBlockRate[2]);
			e.putString("ACh_block_rate_key_2", str);

			str = Float.toString(m_View.m_adtex_AChBlockRate[3]);
			e.putString("ACh_block_rate_key_3", str);

			str = Float.toString(m_View.m_adtex_AChBlockRate[4]);
			e.putString("ACh_block_rate_key_4", str);

			str = Float.toString(m_View.m_adtex_AChBlockRate[5]);
			e.putString("ACh_block_rate_key_5", str);

			str = Float.toString(m_View.m_adtex_AChBlockRate[6]);
			e.putString("ACh_block_rate_key_6", str);

			str = Float.toString(m_View.m_adtex_AChBlockRate[7]);
			e.putString("ACh_block_rate_key_7", str);

			str = Float.toString(m_View.m_adtex_AChBlockRate[8]);
			e.putString("ACh_block_rate_key_8", str);

			str = Float.toString(m_View.m_adtex_AChBlockRate[9]);
			e.putString("ACh_block_rate_key_9", str);


			str = Float.toString(m_View.m_fBlockOneStim);
			e.putString("blocks_one_stim_key", str);

			str = Float.toString(m_View.m_fRecoveryHalfLife);
			e.putString("recovery_half_life_key", str);

			str = Float.toString(m_View.m_fPTP);
			e.putString("PTP_key", str);

			str = Float.toString(m_View.m_fPTP_HalfLife);
			e.putString("PTP_Half_life_key", str);

//			str = Float.toString(m_View.m_fMinimumSensitivity);
//			e.putString("minimum_sensitivity_key", str);
			e.putBoolean("correction_log_key", m_View.m_bCorrectionLogFlg);
			e.putBoolean("hr_check_key", m_View.m_bGraphCheck[0]);
            e.putBoolean("rr_check_key", m_View.m_bGraphCheck[1]);
            e.putBoolean("T1_check_key", m_View.m_bGraphCheck[2]);
            e.putBoolean("T1_ratio_check_key", m_View.m_bGraphCheck[3]);
			e.putBoolean("T4_check_key", m_View.m_bGraphCheck[4]);
			e.putBoolean("TOF_check_key", m_View.m_bGraphCheck[5]);
			e.putBoolean("PTC_check_key", m_View.m_bGraphCheck[6]);
			e.putBoolean("ch1_raw_graph_check_key", m_View.m_bGraphCheck[7]);
			e.putBoolean("ch2_raw_graph_check_key", m_View.m_bGraphCheck[8]);

			e.putBoolean("auto_gain_check_key", m_View.m_bAutoGainFlg);
			e.putBoolean("debug_mode_check_key", m_View.m_bDebugModeFlg);

            str = Float.toString(m_View.m_fHeartAmpMag);
            e.putString("Heart_Amp_Mag_key", str);

            str = Float.toString(m_View.m_fRespiratoryAmpMag);
            e.putString("Respiratory_Amp_Mag_key", str);

            str = Float.toString(m_View.m_fTOF1AmpMag);
            e.putString("TOF1_Amp_Mag_key", str);

			e.putBoolean("stop_save_key", m_View.m_bStopSaveFlg);

            e.commit();
        }
		catch (Exception f)
		{
			String	msg;
			msg = getString(R.string.param_err);
			MessageBox(msg);
        }
    }

/*----------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
------------*/

/*
 * パーミッションについての説明を表示するメッセージボックス
 */
	public void MessageBoxPermission(String str)
	{
		AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
		alertDlg.setTitle(R.string.app_name);
		alertDlg.setMessage(str);
		alertDlg.setPositiveButton(
				"Retry",
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						// OK ボタンクリック処理
						CheckPermissionMain();
					}
				});

		alertDlg.setNegativeButton(
				"Cancel",
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						// OK ボタンクリック処理
						finish();
					}
				});
	// 表示
		try
		{
			alertDlg.create().show();
		}
		catch (Exception f)
		{
			int error;
			error = 0;
		}
	}


	/*
	 * メッセージボックスの表示
	 */
	public void MessageBox(String str, Context con)
	{
		AlertDialog.Builder alertDlg = new AlertDialog.Builder(con);
		alertDlg.setTitle(R.string.app_name);
		alertDlg.setMessage(str);
		alertDlg.setPositiveButton(
				"OK",
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						// OK ボタンクリック処理
						;
					}
				});

	// 表示
		try
		{
			alertDlg.create().show();
		}
		catch (Exception f)
		{
			int error;
			error = 0;
		}
	}

	/*
	 * メッセージボックスの表示
	 */
	public void MessageBox(String str)
	{
		AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
		alertDlg.setTitle(R.string.app_name);
		alertDlg.setMessage(str);
		alertDlg.setPositiveButton(
				"OK",
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						// OK ボタンクリック処理
						;
//						m_View.m_nFTPErrorFlg2 = 0;
					}
				});
	// 表示
		try
		{
			alertDlg.create().show();
		}
		catch (Exception f)
		{
			int error;
			error = 0;
		}
	}

	//　通信機器　再接続
    public void ReConnect(boolean bADStartFlg)
    {
    	//m_nCertifyFlg 0:認証まだ　　1:認証中　　2:認証終了	3:認証失敗
//    	if(m_View.m_nDevID == m_View.m_nProtectNo)	//プロテクトIDが入力されている場合は　認証は終了しているとする
//    		m_nCertifyFlg = 2;
        if(m_nCertifyFlg == 3)
        	m_nCertifyFlg = 0;
	 	if(!m_View.m_bDebugModeFlg)
	 	{
	 		if(!m_BLEObj.m_bConnectFlg)
	 			m_bReConnectFlg = true;
	 	}
	 	else
	 		m_UsbCom.Reconnect();
	 	if(bADStartFlg)
 			m_bMeasStartFlg = true;
	 	if(m_nCertifyFlg != 2)
		 	m_bConnectStartFlg = true;
//	 	if(m_nSendGainLevelFlg == -1)
 		m_nSendGainLevelFlg = 0;
		m_bDAPowerOnFlg = false;
		m_nCurrentPulseWidth = 0;
    }

    //USBシリアル通信の場合の認証キーを送る
    public void SendCertifyUART()
    {
    	byte[] RandKey = new byte[16];
    	m_AESObj.MakeRandKey(RandKey);
    	m_AESObj.SetEncrypt(RandKey);
		byte[] SendData = new byte[17];
		int		i;
		SendData[0] = 'x';
		for(i = 0; i < 16; i++)
			SendData[i + 1] = RandKey[i];
		m_UsbCom.sendData(17, SendData);
/*---
		SendBin(SendData);

    	String	str1, str2;
    	str1 = "x";
    	int		i;
    	for(i = 0; i < 16; i++)
    	{
    		str2 = String.format("%02x", RandKey[i]);
    		str1 += str2;
    	}
    	str1 += "\r\n";
		try {
			byte[] strByte = str1.getBytes("US-ASCII");
	    	m_UsbCom.sendData(35, strByte);
		} catch (UnsupportedEncodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
-----------*/
    }

    //USBシリアル通信の場合のシングルセンサー測定停止コマンドを送る
    void SendMeasEndUART()
    {
    	if(!m_bUARTStartFlg)
    		return;
    	byte[] strByte = new byte[3];
    	strByte[0] = 'b';
    	m_UsbCom.sendData(1, strByte);
    	m_bUARTStartFlg = false;
    }

    //USBシリアル通信の場合のゲインコマンドを送る
    void SendGainCommandUART(int ch, int nGainLevel)
    {
    	int		nCh = 0;
    	if(0 < m_nComKind2)
    		nCh = ch;
    	byte[] strByte = new byte[5];
    	strByte[0] = 'g';
    	strByte[1] = (byte)('0' + nCh);
    	strByte[2] = IntToHex(nGainLevel);
    	m_UsbCom.sendData(3, strByte);
    }



	public void SendPulseWidthUART(int nPulseWidth)		//パルス幅のコマンドをBLE機器に転送
	{
		String	str;
		str = String.format("td%03X", nPulseWidth);
		m_UsbCom.sendString(str);
	}

	public void SendPulseCommandUART(int nValue, int nNoOfPulse, int nInterval)
	{
		String	str;
		str = String.format("te%02X%03X%02X", nValue, nNoOfPulse, nInterval);
		m_UsbCom.sendString(str);
	}
	public void SendPulseStopCommandUART()
	{
		String	str;
		str = "tc";
		m_UsbCom.sendString(str);
	}

	public void DAPowerOnUART(boolean bOnFlg)
	{
		String	str;
		if(bOnFlg)
			str = "tb";
		else
			str = "ta";
		m_UsbCom.sendString(str);
	}

	public void PowerOffUART()
	{
		String	str;
		str = "p";
		m_UsbCom.sendString(str);
	}

	//十進を１６進数に
    byte IntToHex(int nNo)
    {
    	byte	c;
    	c = 0;
    	if(0 <= nNo && nNo <= 9)
    		c = (byte) ('0' + nNo);
    	else if(10 <= nNo && nNo <= 15)
    		c = (byte) ('A' + nNo - 10);
    	return c;
    }

//測定コマンドの文字列を作成
    String GetMeasCommand()
    {
    	String	str;
    	if(m_nComKind == 0)
			str = "s0A3c";	//200Hz 12bit
		else
			str = "w0A3c";	//200Hz 12bit
    	/*----------
		m_View.m_nSamplingRate = m_nBLESamplingRate;	//設定サンプリング周波数と実サンプリング周波数が離れている場合
		m_View.m_PsychoObj.m_nSamplingFreq = m_nBLESamplingRate;
		ChangeSamplingFreq();
		if(m_nBLESamplingRate == 200)
		{
			if(m_nBLEDataBit == 10)
				str = "s0A3b";
			else
				str = "s0A3c";
		}
		else if(m_nBLESamplingRate == 300)
		{
			if(m_nBLEDataBit == 10)
				str = "s06Cb";
			else
				str = "s06Cc";
		}
		else if(m_nBLESamplingRate == 400)
		{
			if(m_nBLEDataBit == 10)
				str = "s051b";
			else
				str = "s051c";
		}
		else if(m_nBLESamplingRate == 480)
		{
			if(m_nBLEDataBit == 10)
				str = "s043b";
			else
				str = "s043c";
		}
		else if(m_nBLESamplingRate == 600)
		{
			if(m_nBLEDataBit == 10)
				str = "s036b";
			else
				str = "s036c";
		}
		else //240Hz
		{
			if(m_nBLEDataBit == 10)
				str = "s088b";
			else
				str = "s088c";
		}
------------------*/
//		int no = (m_nSamplingRate / m_View.m_nPowerFreq) - 1;
/*-----
		int		no = 200 / 50 - 1;
		String	str2;
		str2 = String.format("%1X", no);
		str += str2;
--------------- */
		return str;
    }

    //USBシリアル通信の場合の測定スタートコマンドを送る
    void SendMeasStartUART()
    {
    	String	str;
    	str = GetMeasCommand();
		try {
			byte[] strByte = str.getBytes("US-ASCII");
	    	m_UsbCom.sendData(5, strByte);
	    	m_bUARTStartFlg = true;
		} catch (UnsupportedEncodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
    }

    /*
     *  USBシリアル通信、BLE通信　での測定データを得る
     */
    public void SetData(int nNoOfData, short ReadData[], short ReadData2[])
    {
    	m_lLastDataTime = System.currentTimeMillis();
    	if(m_bADStartFlg)
    	{
    		if(nNoOfData == 0 || 1000 <= nNoOfData)
	    		return;
	    	m_View.SetDataView(nNoOfData, ReadData, ReadData2);
	    }
	}
    /*
     * 　認証コードのチェック
     */



	public void CheckABSCommand(int nLength, byte ptr[])
	{
		// TODO: ここに実装コードを追加します.
		int	i;
		boolean		bFlg = true;
		byte[] code = new byte[16];
		for (i = 0; i < 16; i++)
			code[i] = ptr[i + 4];
		if(m_AESObj.CheckByteArray(m_AESObj.m_encrypt, code))
		{
//			Toast.makeText(this, getString(R.string.certify_ok), Toast.LENGTH_LONG).show();
			Toast toast = Toast.makeText(this, getString(R.string.certify_ok), Toast.LENGTH_SHORT);
			toast.show();
			toast.setGravity(Gravity.TOP, 50, 110);
			m_nCertifyFlg = 2;

		}
		else
		{
			m_nCertifyFlg = 3;
			MessageBox(getString(R.string.certify_err));

		}

	}
    /*
     * 　保存するフォルダを記憶する
     */
    public void SaveInitialDir()
    {
        Editor e = m_preferences.edit();
        try
        {
        	e.putString("InitialDir_key2", m_strInitialDir);
            e.commit();
        }
		catch (Exception f)
		{
			String	msg;
			msg = getString(R.string.param_err);
			MessageBox(msg);
        }
    }
    /*
     * 　ゲインレベルを記録する
     */
    public void WriteGainLevel(int ch, int nGainLevel)
    {
    	String	str;

        Editor e = m_preferences.edit();
        try
        {
        	if(ch == 0)
        	{
        		m_View.m_nGainLevel[0] = nGainLevel;
        		str = Integer.toString(m_View.m_nGainLevel[0]);
        		e.putString("GainLevel_key", str);
            	e.commit();
        	}
        	else if(ch == 1)
        	{
        		m_View.m_nGainLevel[1] = nGainLevel;
        		str = Integer.toString(m_View.m_nGainLevel[1]);
        		e.putString("GainLevel2_key", str);
            	e.commit();
        	}
        }
        catch (Exception f)
        {
        	String	msg;
        	msg = getString(R.string.param_err);
        	MessageBox(msg);
        }
    }

    /*
     * 　サンプリング周波数が変更した場合、デジタルフィルタを初期化
     */
    void ChangeSamplingFreq()
    {
		m_View.SetFilterFreq();

/*
        int		i;
		for(i = 0; i < 2; i++)
		{
	        m_View.m_IntegralObj[i].InitIntegral(m_View.m_nSamplingRate, m_View.m_fLowCutFreq);
		}

*/
    }
    /*
     * 　ボタンサイズが変更された場合の処理
     */
	void ChangeButtonSize(Button btn)
	{
       	int	nSize = 40 + (int)(m_nBtnSize * 8);	//タブレット
       	ViewGroup.LayoutParams params = btn.getLayoutParams();
       	params.height = (int)(nSize);
       	params.width = (int)(nSize);
       	
	}
	//表示前に呼ばれる　ボタンの大きさ、フォントの大きさ設定
	@Override
	protected void onResume() {
		// TODO 自動生成されたメソッド・スタブ
//		m_View.m_nMeasMode = 0;
		nstep = 0;		// adtex 筋弛緩の深さのシミュレート用

		super.onResume();
//		readPreferences();
		boolean bMultiFlg = false;

        if(0 < m_nComKind2)
        	bMultiFlg = true;
		
		m_View.SetFilterFreq();
		int			i;
		m_nBtnSize = 6;
		
		
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point(0, 0);
/*  ADTEX
       display.getRealSize(point);
       int		nWidth = (point.x < point.y)? point.y : point.x;
       
		if(bMultiFlg)
			m_nBtnSize = nWidth / m_nNoOfButton;
		else
			m_nBtnSize = nWidth / (m_nNoOfButton - 2);

		m_nBtnSize /= 13;
		for(i = 0; i < m_nNoOfButton; i++)
		{
			m_Button[i].setTextSize((int)(m_nBtnSize * 1.3));
	        ChangeButtonSize(m_Button[i]);
		}
		int	nSize = 40 + (int)(m_nBtnSize * 8);	//タブレット
		if(bMultiFlg)
			m_nCurrentBtnPos = (int)(nSize * 13.5);
		else
			m_nCurrentBtnPos = (int)(nSize * 11.5);
*/
		if(m_bADStartFlg)
			WritePreferences();

        if(!m_bADStartFlg && m_nFileReadFlg == 0)
        {
			m_View.InitMeas(false, false);
            if(!m_View.m_bDebugModeFlg)
            	m_BLEObj.EndBLEMeas();
            else
            	SendMeasEndUART();
            m_View.m_nMeasMode = 0;
        }

    	if(m_SView == null || m_SView.m_surfaceView == null)
    	{
    		m_surfaceView = null;
    		m_SView = null;

    		m_surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
    		m_SView = new MySurfaceView(this);
    		m_SView.SetSurface(m_surfaceView);
    	}
		if(m_View.m_nMeasMode == 0) {
			m_Button[0].setBackground(m_btn_play_circle);
		}
        else {
			m_Button[0].setBackground(m_btn_pause_circle);
		}

		m_View.SetMaxMin();
		m_View.m_bDrawFlg = true;
	}

	// プログラムを終了させるかどうかの問
	public boolean dispatchKeyEvent(KeyEvent event) {
	    // TODO Auto-generated method stub
	    if (event.getAction()==KeyEvent.ACTION_DOWN) {
	        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
	             AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
	             alertDialogBuilder.setTitle(getString(R.string.app_name));
	             alertDialogBuilder.setMessage(getString(R.string.is_end));
	             alertDialogBuilder.setPositiveButton(getString(R.string.end),
	                     new DialogInterface.OnClickListener() {
	                         public void onClick(DialogInterface dialog, int which) {
	                         	m_bPrgEndFlg = true;
	                         }
	                     });
	             alertDialogBuilder.setNegativeButton(getString(R.string.cancel),
	                     new DialogInterface.OnClickListener() {
	                         public void onClick(DialogInterface dialog, int which) {
	                         }
	                     });
	             alertDialogBuilder.setCancelable(true);
	             AlertDialog alertDialog = alertDialogBuilder.create();
	             alertDialog.show();
		            return false;
	        }
		    }
	    return super.dispatchKeyEvent(event);
	}

    /*
     *  タッチされた場合のイベント
     */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// TODO 自動生成されたメソッド・スタブ
	    int eventAction = event.getActionMasked();
	    int pointerIndex = event.getActionIndex();
	    int pointerId = event.getPointerId(pointerIndex);
	    if(m_surfaceView == null)
	    		return super.onTouchEvent(event);
	    m_surfaceView.getLocationOnScreen(m_View.m_GlobalPos);
		switch (eventAction)
		{
		case MotionEvent.ACTION_DOWN:	//一本目の指が押される
			m_bTouchFlg = true;
			m_View.m_zsx1 = m_View.m_zsx2 = event.getX() - m_View.m_GlobalPos[0];
			m_View.m_zsy1 = m_View.m_zsy2 = event.getY() - m_View.m_GlobalPos[1];
//			m_View.onActionDown();
			break;

		case MotionEvent.ACTION_POINTER_DOWN:	//二本目の指が押される
			m_View.m_zsx1 = event.getX(0) - m_View.m_GlobalPos[0];
			m_View.m_zsx2 = event.getX(1) - m_View.m_GlobalPos[0];
			m_View.m_zsy1 = event.getY(0) - m_View.m_GlobalPos[1];
			m_View.m_zsy2 = event.getY(1) - m_View.m_GlobalPos[1];
			m_bTouchFlg = false;
			m_View.onMultiDown();
			break;

		case MotionEvent.ACTION_POINTER_UP:	//二本目の指が離れる
			m_bTouchFlg = false;
			m_View.m_zex1 = event.getX(0) - m_View.m_GlobalPos[0];
			m_View.m_zex2 = event.getX(1) - m_View.m_GlobalPos[0];
			m_View.m_zey1 = event.getY(0) - m_View.m_GlobalPos[1];
			m_View.m_zey2 = event.getY(1) - m_View.m_GlobalPos[1];
			m_View.TouchAction(true);
			m_bTouchFlg = false;
			break;

		case MotionEvent.ACTION_CANCEL:	//最後の指が離れる
		case MotionEvent.ACTION_UP:
			if(m_bTouchFlg)
			{
				m_View.m_zex1 = m_View.m_zex2 = event.getX() - m_View.m_GlobalPos[0];
				m_View.m_zey1 = m_View.m_zey2 = event.getY() - m_View.m_GlobalPos[1];
				m_View.TouchShift();
				m_bTouchFlg = false;
			}
			break;

		case MotionEvent.ACTION_MOVE:	//一本指操作
			if(m_bTouchFlg)
			{
				m_View.m_zex1 = m_View.m_zex2 = event.getX() - m_View.m_GlobalPos[0];
				m_View.m_zey1 = m_View.m_zey2 = event.getY() - m_View.m_GlobalPos[1];
//				m_View.onActionMove();
			}
			else if(m_View.m_bZoomFlg)
			{
				m_View.m_zex1 = event.getX(0) - m_View.m_GlobalPos[0];
				m_View.m_zex2 = event.getX(1) - m_View.m_GlobalPos[0];
				m_View.m_zey1 = event.getY(0) - m_View.m_GlobalPos[1];
				m_View.m_zey2 = event.getY(1) - m_View.m_GlobalPos[1];
//				m_View.onMultiMove();
			}
			break;
		}
		return super.onTouchEvent(event);
	}


    /*
     * 　コメントダイアログを表示
     */

	public void showInputComment()
	{
		m_dialog = new InputComment(this, new ResultListener());
		m_dialog.m_Comment = "";
		m_dialog.show();
	}


    /*
     * 　コメントダイアログからの結果を受け取る
     */

	public class ResultListener implements InputComment.DialogListener
	{
		public void onRegistSelected()
		{
				// 登録を選択した際の処理.
			m_View.m_Comment = m_dialog.m_Comment;
			m_View.AddMarker();
		}

		public void onCancel()
		{
			// キャンセルした際の処理.
			;
		}
	}

	public boolean GetVoltData(byte data1, byte data2)
	{
		// TODO: ここに実装コードを追加します.
		short	sDummy1, sDummy2, sDummy3;
		int		vdata;
		float	fVoltage, fSub;
		sDummy2 = (short)(data2 & 0x000f);
		sDummy1 = (short)(data1 & 0x00ff);
		sDummy3 = (short)(data2 & 0x00f0);
		if(sDummy3 == 0) {
			vdata = sDummy1 + sDummy2 * 256;
			if ( 0 < vdata && vdata < 4096 ) {
				fVoltage = (float) vdata * 1.2F / 1024.0F;
				m_View.m_nBatteryLevel = GetBatteryLevel(fVoltage, m_View.m_nBatteryType);
				fSub = fVoltage - m_View.m_fBatteryVolt;
				if(0.5F < fSub)			//0.5V以上上昇するとバッテリーが交換されたとみなす
					BatteryParamReset();
				m_View.m_fBatteryVolt = fVoltage;

				return true;
			}
		}
		return false;
	}

	public void BatteryParamReset()
	{

	}
	public int GetBatteryLevel(float fVoltage, int nBatteryType)
	{
		int nLevel = m_BatteryObj.GetBatteryLevel(fVoltage, nBatteryType, 2);
		return nLevel;
	}

	public int GetBLEData(int nSize, byte data[], short sData1[], short sData2[])
	{
		int		i, j, k;
		j = 0;
		if ( m_nComKind == 0 ) {
			for (i = 0; i < 18; i += 3) {
				DataBit12ToShort(data[i], data[i + 1], data[i + 2], m_sWork1);
				for (k = 0; k < 2; k++) {
					sData1[j] = m_sWork1[k];
					sData2[j] = 0;
					j++;
				}
			}
		} else {
			for (i = 0; i < 9; i += 3) {
				DataBit12ToShort(data[i], data[i + 1], data[i + 2], m_sWork1);
				DataBit12ToShort(data[i + 9], data[i + 10], data[i + 11], m_sWork2);
				for (k = 0; k < 2; k++) {
					sData1[j] = m_sWork1[k];
					sData2[j] = m_sWork2[k];
					j++;
				}
			}
		}
		SetData(j, sData1, sData2);
		if(20 <= nSize)
			GetVoltData(data[18], data[19]);
		else {
			m_View.m_fBatteryVolt = 0.0F;
			m_View.m_nBatteryLevel = 0;
		}
		return j;
	}

	@Override
	protected void onDestroy() {
		// TODO 自動生成されたメソッド・スタブ
		WritePreferences();
		if(!m_View.m_bDebugModeFlg)
		{
			m_BLEObj.EndBLEMeas();
			m_BLEObj.Destroy();
		}
		else {
			SendMeasEndUART();
			m_UsbCom.onUsbStop();
		}
		super.onDestroy();

		android.os.Process.killProcess(android.os.Process.myPid());
	}

	void DspToastPulse()
	{
		String msg = "";
		if(m_View.m_nPulseMode == 1 || m_View.m_nPulseMode == 2)
			msg = "Output Calibration Pulse";
		else if(m_View.m_nPulseMode == 3 || m_View.m_nPulseMode == 4)
			msg = "Output TOF Pulse";
		else if(m_View.m_nPulseMode == 5 || m_View.m_nPulseMode == 6)
			msg = "Output Twitch Pulse";
		else if(m_View.m_nPulseMode == 7 || m_View.m_nPulseMode == 8)
			msg = "Output TET Pulse";
		else if(m_View.m_nPulseMode == 9 || m_View.m_nPulseMode == 10)
			msg = "Output DBS Pulse";
		else if(m_View.m_nPulseMode == 11 || m_View.m_nPulseMode == 12)
			msg = "Output PTC Pulse";
		else if(m_View.m_nPulseMode == 13)
			msg = "Output TOF Pulse";
		else if(m_View.m_nPulseMode == 14)
			msg = "Output PTC Pulse";
		if(0 < m_View.m_nPulseMode) {
			long lCurrent = System.currentTimeMillis();
			long lSub = lCurrent - m_lPrevToastTime;
			if(3000 < lSub || m_View.m_nPulseMode != m_nOldToastPulseMode) {	//連続してパルスのToast表示しない　５秒開ける　CAL終了後もしばらく Toast 表示が続くため
				Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
				toast.show();
				toast.setGravity(Gravity.TOP, 50, 110);
				m_lPrevToastTime = lCurrent;
				m_nOldToastPulseMode = m_View.m_nPulseMode;
			}
		}
	}
    public void DataBit12ToShort(byte d0, byte d1, byte d2, short sData[])	//BLE送信データ3byte から12bitデータを２個作成
    {
        int		dummy, s0, s1, s2;
        s0 = (int)d0;
        if(s0 < 0)
            s0 = 256 + s0;
        s1 = (int)d1;
        if(s1 < 0)
            s1 = 256 + s1;
        s2 = (int)d2;
        if(s2 < 0)
            s2 = 256 + s2;

        dummy = 0xf0 & s2;
        dummy = dummy << 4;
        sData[0] = (short)(s0 + dummy);

        dummy = 0x0f & s2;
        dummy = dummy << 8;
        sData[1] = (short)(s1 + dummy);
    }


}
