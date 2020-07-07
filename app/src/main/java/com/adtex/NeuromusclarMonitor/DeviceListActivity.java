package com.adtex.NeuromusclarMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceListActivity extends Activity {
    private BluetoothAdapter mBluetoothAdapter;
	MainActivity	m_ma;

   // private BluetoothAdapter mBtAdapter;
    private TextView mEmptyList;
    public static final String TAG = "DeviceListActivity";

    List<BluetoothDevice> deviceList;
    private DeviceAdapter deviceAdapter;
    private ServiceConnection onService = null;
    Map<String, Integer> devRssiValues;
    private static final long SCAN_PERIOD = 10000; //10 seconds
    private Handler mHandler;
    private boolean mScanning;
    int		m_nDevCount;
	private Timer mainTimer;					//タイマー用
	private MainTimerTask mainTimerTask;		//タイマタスククラス
	private		boolean		m_bInitFlg;
	private		String	m_Bifro = "BifrosTec";
	private		int		m_nBifroLength = 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {	//初期化処理
        super.onCreate(savedInstanceState);
        m_ma = GlobalVariable.m_ma;
        m_nDevCount = 0;
        m_bInitFlg = false;
        Log.d(TAG, "onCreate");
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        setContentView(R.layout.device_list);
        android.view.WindowManager.LayoutParams layoutParams = this.getWindow().getAttributes();
        layoutParams.gravity=Gravity.TOP;
        layoutParams.y = 200;


        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
 //           Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            Toast toast = Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT);
            toast.show();
            toast.setGravity(Gravity.TOP, 50, 110);

            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
//            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            Toast toast = Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT);
            toast.show();
            toast.setGravity(Gravity.TOP, 50, 110);

            finish();
            return;
        }
        populateList();
        mEmptyList = (TextView) findViewById(R.id.empty);
        Button cancelButton = (Button) findViewById(R.id.btn_cancel);
		mainTimer = new Timer();
		//タスククラスインスタンス生成
		mainTimerTask = new MainTimerTask();
		//タイマースケジュール設定＆開始
		mainTimer.schedule(mainTimerTask, 2000, 500);
//		mainTimer.schedule(mainTimerTask, 100, 100);

        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

            	if (mScanning==false)
            		scanLeDevice(true);
            	else
            		finish();
            }
        });

    }
	public class MainTimerTask extends TimerTask	//タイマータスク
	{
		@Override
		public void run()
		{
			//ここに定周期で実行したい処理を記述します
			mHandler.post( new Runnable()
			{
				public void run()
				{
					if(1 <= m_nDevCount && !m_bInitFlg && m_ma.m_bAddressFlg)	//デバイスが二つ以上あれば以前選択したアドレスのものを使用する
					{
						m_bInitFlg = true;
						int		i, nSize;
						nSize = deviceList.size();
						for(i = 0; i < nSize; i++)
						{
							BluetoothDevice device = deviceList.get(i);
							if(device == null)
								continue;
							String	str = device.getAddress();
							try
							{
								if(m_ma.m_Address.equals(str))
								{
									mBluetoothAdapter.stopLeScan(mLeScanCallback);

									Bundle b = new Bundle();
									b.putString(BluetoothDevice.EXTRA_DEVICE, deviceList.get(i).getAddress());

									Intent result = new Intent();
									result.putExtras(b);
									setResult(Activity.RESULT_OK, result);
									finish();
								}
							}
					        catch (Exception e)
					        {
					        	String	msg;
					    		String	str2 = device.getName();
					    		msg = "An error has occurred device causes that " + str2;
//					        	m_ma.MessageBox(msg);
					        }
						}
					}
/*---------------------------*/	///コメントにすると１個の場合でも自動で接続しない
					else if(m_nDevCount == 1 && !m_bInitFlg && !m_ma.m_bAddressFlg)
					{//AYAP-05　が見つかった場合の処理 一つだけの場合は自動で接続
						m_bInitFlg = true;
						int		i, nSize;
						nSize = deviceList.size();
						for(i = 0; i < nSize; i++)
						{
							BluetoothDevice device = deviceList.get(i);
							if(device == null)
								continue;

							m_Bifro = m_ma.m_DeviceName;
							m_nBifroLength = m_Bifro.length();
							String	str_sub = device.getName();
							String	str = str_sub.substring(0, m_nBifroLength);
							try
							{//DeviceName
				        		if(str.equals(m_Bifro))
								{
									mBluetoothAdapter.stopLeScan(mLeScanCallback);
									m_ma.m_Address = device.getAddress();
									m_ma.m_bAddressFlg = true;
									Bundle b = new Bundle();
									b.putString(BluetoothDevice.EXTRA_DEVICE, deviceList.get(i).getAddress());

									Intent result = new Intent();
									result.putExtras(b);
									setResult(Activity.RESULT_OK, result);
									finish();
								}
							}
					        catch (Exception e)
					        {
					        	String	msg;
					    		String	str2 = device.getName();
					    		msg = "An error has occurred device causes that " + str2;
//					        	m_ma.MessageBox(msg);
					        }
						}
					}
/*------------------------------------*/
				}
			});

		}
	}

    private void populateList() {	//デバイスリストの準備
        /* Initialize device list container */
        Log.d(TAG, "populateList");
        deviceList = new ArrayList<BluetoothDevice>();
        deviceAdapter = new DeviceAdapter(this, deviceList);
        devRssiValues = new HashMap<String, Integer>();

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

           scanLeDevice(true);

    }

    private void scanLeDevice(final boolean enable) {		//デバイスを探す
        final Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        m_nDevCount = 0;
        m_bInitFlg = false;
        if (enable) {
            // Stops scanning after a pre-defined scan period.
//        	if(!m_ma.m_bAutoMeasFlg)	//自動測定の場合は探し続ける
        	{
        		mHandler.postDelayed(new Runnable() {	//10sec でスキャン終了　ボタンをスキャンに変更
        			@Override
        			public void run() {
        				mScanning = false;
        				mBluetoothAdapter.stopLeScan(mLeScanCallback);

        				cancelButton.setText(R.string.scan);

        			}
        		}, SCAN_PERIOD);
        	}
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            cancelButton.setText(R.string.cancel);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            cancelButton.setText(R.string.scan);
        }

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
//BLEデバイスが見つかった時コールされる
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                	  runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                        	  addDevice(device,rssi);	//BLEデバイスが見つかった時コールされる
                          }
                      });

                }
            });
        }
    };

    private void addDevice(BluetoothDevice device, int rssi) {		//デバイスリストにデバイスを追加する
        boolean deviceFound = false;
		if(device == null)
			return;

        try
        {
        	for (BluetoothDevice listDev : deviceList)
        	{
        		if (listDev.getAddress().equals(device.getAddress()))
        		{
        			deviceFound = true;
        			break;
        		}
        	}
        	devRssiValues.put(device.getAddress(), rssi);
        	if (!deviceFound)
        	{
				m_Bifro = m_ma.m_DeviceName;			//[詳細設定]-[測定設定] で指定されたデバイス名
				m_nBifroLength = m_Bifro.length();
				String	str_sub = device.getName();		//見つかったデバイスのデバイス名
				String	str = str_sub.substring(0, m_nBifroLength);	//見つかったデバイス名とAYAP05のデバイス名が一致しているか調べる
        		if(str.equals(m_Bifro))
        		{
        			deviceList.add(device);		//一致していたらデバイスリストに追加
        			mEmptyList.setVisibility(View.GONE);
        			deviceAdapter.notifyDataSetChanged();
        			m_nDevCount++;
        		}
        	}
        }
        catch (Exception e)
        {
        	String	msg;
        	String	str = device.getName();
        	String	str2;
        	str2 = String.format("No. of devices:%d ", deviceList.size());
    		msg = str2 + " An error has occurred device causes that " + str;
//        	msg = str2 + str + "というデバイスが原因でエラーが発生しました。";
//        	m_ma.MessageBox(msg);
        }
    }

    @Override
    public void onStart() {	//デバイスリストダイアログが表示される
        super.onStart();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onStop() {	//デバイスリストが閉じられた時呼ばれる
        super.onStop();
//        mBluetoothAdapter.stopLeScan(mLeScanCallback);

    }

    @Override
    protected void onDestroy() {	//デバイスリストが破棄された時呼ばれる
        super.onDestroy();
        mBluetoothAdapter.stopLeScan(mLeScanCallback);

    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
    	//デバイスリスト　AYAP05 を複数見つかった場合は自動で選択されないのでリストをタップされたときの処理
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            BluetoothDevice device = deviceList.get(position);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            m_ma.m_Address = deviceList.get(position).getAddress();
            m_ma.m_bAddressFlg = true;

            Bundle b = new Bundle();
            b.putString(BluetoothDevice.EXTRA_DEVICE, deviceList.get(position).getAddress());

            Intent result = new Intent();
            result.putExtras(b);
            setResult(Activity.RESULT_OK, result);
            finish();

        }
    };



    protected void onPause() {
        super.onPause();
//        scanLeDevice(false);
    }

    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {	//デバイスリストにデバイス名を表示
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
            }

            BluetoothDevice device = devices.get(position);
            final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
            final TextView tvname = ((TextView) vg.findViewById(R.id.name));
            final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);
            final TextView tvrssi = (TextView) vg.findViewById(R.id.rssi);

            tvrssi.setVisibility(View.VISIBLE);
            byte rssival = (byte) devRssiValues.get(device.getAddress()).intValue();
            if (rssival != 0) {
                tvrssi.setText("Rssi = " + String.valueOf(rssival));
            }

            tvname.setText(device.getName());
            tvadd.setText(device.getAddress());
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "device::"+device.getName());
                tvname.setTextColor(Color.WHITE);
                tvadd.setTextColor(Color.WHITE);
                tvpaired.setTextColor(Color.GRAY);
                tvpaired.setVisibility(View.VISIBLE);
                tvpaired.setText(R.string.paired);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.WHITE);

            } else {
                tvname.setTextColor(Color.WHITE);
                tvadd.setTextColor(Color.WHITE);
                tvpaired.setVisibility(View.GONE);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.WHITE);
            }
            return vg;
        }
    }
    private void showMessage(String msg) {
//        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
        toast.setGravity(Gravity.TOP, 50, 110);

    }
}
