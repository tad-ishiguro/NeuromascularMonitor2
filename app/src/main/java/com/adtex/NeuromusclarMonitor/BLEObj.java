package com.adtex.NeuromusclarMonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class BLEObj
{
	boolean m_bInitDataFlg = false;
	OutputStreamWriter m_osw = null;
	BufferedWriter m_bw = null;
	FileOutputStream m_fos = null;
	long		m_lStartTime;
	int			m_nComKind = 0;
    public static final String TAG = "nRFUART";
    public static final int REQUEST_SELECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;
    public static final int UART_PROFILE_READY = 10;
    public static final int UART_PROFILE_CONNECTED = 20;
    public static final int UART_PROFILE_DISCONNECTED = 21;
    public static final int STATE_OFF = 10;

    public int mState = UART_PROFILE_DISCONNECTED;
    public static UartService mService = null;
    public static BluetoothDevice mDevice = null;
    public static BluetoothAdapter mBtAdapter = null;
    public MainActivity		m_ma;
    public	boolean m_bConnectFlg;
    private boolean		m_bCreateFlg;
    int		m_nDataBit, m_nSamplingRate;
    boolean		m_bMeasFlg;
    short	m_sData1[], m_sData2[];
    char	m_Buf[], m_Buf2[];
    byte	m_AsciiBuf[];
    int		m_nAsciiPos;
    BLEObj(MainActivity ma)
    {
    	m_ma = ma;
        m_ma.m_lConnectTime2 = 0;
    	m_bConnectFlg = false;
    	m_bCreateFlg = false;
    	m_nSamplingRate = 240;
    	m_nDataBit = 12;
		m_sData1 = new short[20];
		m_sData2 = new short[20];
    	m_AsciiBuf = new byte[20];
    	m_nAsciiPos = 0;
    	m_bMeasFlg = false;
    	m_Buf = new char[20];
    	m_Buf2 = new char[1024];
    }
    public	boolean OnCreate()	//BLEObj の初期化　使うときに最初にコールする
    {
    	if(m_bCreateFlg)
    		return true;
    	mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    	if (mBtAdapter == null)
    	{
//    		Toast.makeText(m_ma, "Bluetooth is not available", Toast.LENGTH_LONG).show();
    		return false;
    	}
        service_init();
    	m_bCreateFlg = true;
    	return true;
    }

    public void Connect(boolean bFlg)	//BLEとの接続(bFlg:true) または　切断((bFlg:false) を行う
    {	//bFlg TRUE:Connect   FALSE:DisConnect
    	if (!mBtAdapter.isEnabled())
    	{
    		Log.i(TAG, "onClick - BT not enabled yet");
    		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		m_ma.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    	}
    	else
    	{
    		if(!m_bConnectFlg && bFlg)		//接続する場合
    		{
    			Intent newIntent = new Intent(m_ma, DeviceListActivity.class);	//デバイスリストダイアログの表示
    			m_ma.startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
    		}
    		else if(m_bConnectFlg && !bFlg)	//切断する場合
    		{
    			if (mDevice!=null)
    			{
    				mService.disconnect();
    				m_bConnectFlg = false;
    		        m_ma.m_lConnectTime2 = 0;
    			}
    		}
    	}
    }

    public void Send(String message)	//接続したBLE機器に文字列送信
    {
    	byte[] value;
    	if(!m_bConnectFlg)
    		return;
		try
		{
			//send data to service
			value = message.getBytes("UTF-8");
			mService.writeRXCharacteristic(value);
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
            Log.e(TAG, e.toString());
        }

    }

    public void SendBin(byte value[])	//接続したBLE機器にバイト配列を送信
    {
    	if(!m_bConnectFlg)
    		return;
		try
		{
			//send data to service
			mService.writeRXCharacteristic(value);
		}
		catch (Exception e)
		{
            Log.e(TAG, e.toString());
        }

    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder rawBinder)	//サービスと接続
        {
        	mService = ((UartService.LocalBinder) rawBinder).getService();
        	Log.d(TAG, "onServiceConnected mService= " + mService);
        	if (!mService.initialize())
        	{
                   Log.e(TAG, "Unable to initialize Bluetooth");
//                   m_ma.finish();
        	}
        }

        public void onServiceDisconnected(ComponentName classname)
        {
       ////     mService.disconnect(mDevice);
        	mService = null;
        }
    };

    private void service_init() {		//サービスの初期化　詳細不明
        Intent bindIntent = new Intent(m_ma, UartService.class);	//UartService の生成
        m_ma.bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);	//生成したUartService とバインド

        LocalBroadcastManager.getInstance(m_ma).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());	//フィルターの設定
    }


    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {		//接続、切断、データ受信の処理

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;

           //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {	//接続の知らせ
            	 m_ma.runOnUiThread(new Runnable() {
                     public void run() {
                             Log.d(TAG, "UART_CONNECT_MSG");
//                           m_ma.ConnectDevice(mDevice.getName());
                             mState = UART_PROFILE_CONNECTED;
//                             m_bMeasStartFlg = false;
                     }
            	 });
            }

          //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {	//切断の知らせ
            	 m_ma.runOnUiThread(new Runnable() {
                     public void run() {
                    	 Log.d(TAG, "UART_DISCONNECT_MSG");
//                       m_ma.DisConnectDevice(mDevice.getName());
                         mState = UART_PROFILE_DISCONNECTED;
                         mService.close();
                         m_bConnectFlg = false;
                         m_ma.m_lConnectTime2 = 0;
                            //setUiState();
                     }
                 });
            }


          //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {	//BLEサービスが見つかった
             	 mService.enableTXNotification();
                 m_bConnectFlg = true;
             	 m_ma.m_lConnectTime2 = System.currentTimeMillis();
                 Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
            }
          //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                 final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);	//データ受信
                 m_ma.runOnUiThread(new Runnable() {
                     public void run() {	//txValue に受信データがあるので　別スレッドで文字列処理を行う
                         try
                         {
//                         	String text = new String(txValue, "UTF-8");
                        	 if(txValue.length == 16 && txValue[11] == 'e' && txValue[12] == 'r' && txValue[13] == 'r' && txValue[14] == 'o' && txValue[15] == 'r')
                        	 {
                        		 m_bInitDataFlg = false;
  //               				Toast.makeText(m_ma,"BLE Command error. Please reset AYAP05C", Toast.LENGTH_LONG).show();
                        	 }
                             else if(txValue.length == 3 && txValue[0] == 'R')
                             {
                                 m_bInitDataFlg = false;
                                 if(txValue[1] == '0' && txValue[2] == '1') {
                                     m_ma.m_bMsgReq = true;
                                     m_ma.m_Msg = m_ma.getString(R.string.P06SS_NOT_CONNECT);
                                     m_ma.m_View.m_nMeasMode = 0;
                                 }
                                 if(txValue[1] == '0' && txValue[2] == '2') {
                                     m_ma.m_bMsgReq = true;
                                     m_ma.m_Msg = m_ma.getString(R.string.P06SS_NOT_CONNECT2);
                                     m_ma.m_View.m_nMeasMode = 0;
                                 }
                             }
                        	 else if(m_ma.m_nCertifyFlg == 1)
                        	 {
                        		 m_bInitDataFlg = false;
                        		 GetCertifyData(txValue.length, txValue);
                        	 }
                        	 else if(m_ma.m_nCertifyFlg == 2)
                        	 {
                        		 m_bInitDataFlg = false;
                        		 GetBLEData(txValue.length, txValue);
                        	 }
                        	 else if(m_ma.m_nCertifyFlg == 3 && !m_bInitDataFlg)
                        	 {
                        		 m_bInitDataFlg = true;
//                  				Toast.makeText(m_ma,"An error occurred in AYAP05C. Please reset.", Toast.LENGTH_LONG).show();
                        	 }
//                         	m_ma.GetBLEData(text);

                         } catch (Exception e) {
                             Log.e(TAG, e.toString());
                         }
                     }
                 });
             }
           //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
//            	m_ma.showMessage("Device doesn't support UART. Disconnecting");
            	mService.disconnect();
                m_bConnectFlg = false;
                m_ma.m_lConnectTime2 = 0;
            }
        }
    };
	public boolean StartBLEMeas(int nComKind, int nSamplingRate, int nDataBit)	//測定開始コマンドの送信
	{
		m_nComKind = nComKind;
		m_nSamplingRate = nSamplingRate;
		m_nDataBit = nDataBit;
		if(!m_bConnectFlg)
			return false;
		String	str;
		str = m_ma.GetMeasCommand();
		Send(str);
		m_bMeasFlg = true;
		WriteTimeStamp(0);
		WriteLogFile("start\r\n");
		Log.e("StartBLEMeas", "StartBLEMeas");
		return true;
	}

	public void EndBLEMeas()	//測定終了コマンドの送信
	{
		if(!m_bConnectFlg)
			return;
		if(!m_bMeasFlg)
			return;
		Send("b");
		m_bMeasFlg = false;
		WriteTimeStamp(2);
		WriteLogFile("end\r\n");
		Log.e("EndBLEMeas", "EndBLEMeas");
	}

	public void SendGainCommandBLE(int ch, int nGainLevel)		//ゲインレベルのコマンドをBLE機器に転送
	{
		if(!m_bConnectFlg)
			return;
    	byte[] strByte = new byte[5];
    	strByte[0] = 'g';
    	strByte[1] = (byte)('0' + ch);		//チャンネル番号　　BLEは0で固定
    	strByte[2] = m_ma.IntToHex(nGainLevel);	//ゲインレベル
    	String result;
		try {
			result = new String(strByte, "UTF-8");
	    	Send(result);
		} catch (UnsupportedEncodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		Log.e("SendGainBLE", "SendGainBLE");
	}

	public void SendPulseWidth(int nPulseWidth)		//パルス幅のコマンドをBLE機器に転送
	{
		if(!m_bConnectFlg)
			return;
		String	str;
		str = String.format("td%03X", nPulseWidth);
		Send(str);
	}

	public void SendPulseCommand(int nValue, int nNoOfPulse, int nInterval)
	{
		if(!m_bConnectFlg)
			return;
		String	str;
		str = String.format("te%02X%03X%02X", nValue, nNoOfPulse, nInterval);
		Send(str);
	}

	public void SendPulseStopCommand()
	{
		if(!m_bConnectFlg)
			return;
		String	str;
		str = "tc";
		Send(str);
	}

	public void DAPowerOn(boolean bOnFlg)
	{
		if(!m_bConnectFlg)
			return;
		String	str;
		if(bOnFlg)
			str = "tb";
		else
			str = "ta";
		Send(str);
	}

	public void PowerOff()
	{
		if(!m_bConnectFlg)
			return;
		String	str;
		str = "p";
		Send(str);
	}

	public int HexToNum(byte c)	//デバッグで使用関数　使用していません。
	{
		int		ret;
		ret = 0;
		if('0' <= c && c <= '9')
			ret = c - '0';
		else if('A' <= c && c <= 'F')
			ret = c - 'A' + 10;
		else if('a' <= c && c <= 'f')
			ret = c - 'a' + 10;
		return ret;
	}
	public int AsciiToData(int nSize, byte data[])//デバッグで使用関数　使用していません。
	{
		int		i;
		int		d3, d2, d1;
		int		nCount = 0;
		for(i = 0; i < nSize; i++)
		{
			if(data[i] == 'P')
			{
				if(m_nAsciiPos == 3)
				{
					d1 = HexToNum(m_AsciiBuf[0]);
					d2 = HexToNum(m_AsciiBuf[1]);
					d3 = HexToNum(m_AsciiBuf[2]);
					m_sData1[nCount] = (short)(d1 * 256 + d2 * 16 + d3);
					nCount++;
				}
				m_nAsciiPos = 0;
			}
			else
			{
				m_AsciiBuf[m_nAsciiPos] = data[i];
				m_nAsciiPos++;
			}
		}
		return nCount;
	}


    public int GetBLEData(int nSize, byte data[])	//BLEj受信データの処理
    {
    	int		j, k, i;

    	j = m_ma.GetBLEData(nSize, data, m_sData1, m_sData2);
    	if(m_ma.m_bBLELogFlg)
    	{
    		int		nLength = 0;
    		int		nPos = 0;
    		k = 0;
    		for(i = 0; i < j; i++)
    		{
    			nLength = m_ma.m_View.IntToString(m_sData1[i], m_Buf, 0);
    			for(k = 0; k < nLength; k++)
    			{
    				m_Buf2[nPos] = m_Buf[k];
    				nPos++;
    			}
    			if(i != (j - 1))
    			{
    				m_Buf2[nPos] = ',';
    				nPos++;
    			}
    			if(m_nComKind == 1)
				{
					nLength = m_ma.m_View.IntToString(m_sData2[i], m_Buf, 0);
					for(k = 0; k < nLength; k++)
					{
						m_Buf2[nPos] = m_Buf[k];
						nPos++;
					}
					if(i != (j - 1))
					{
						m_Buf2[nPos] = ',';
						nPos++;
					}
				}
    		}
    		m_Buf2[nPos] = '\r';
    		nPos++;
    		m_Buf2[nPos] = '\n';
    		nPos++;
    		WriteTimeStamp(1);
    		WriteLogFile2(m_Buf2, nPos);
    	}
    	return j;
    }

    public void DataBit10ToShort(byte d0, byte d1, byte d2, byte d3, byte d4, short sData[])
    {	//BLE受信データ 5byte から　10bit データを４個作成
    	int		dummy, s0, s1, s2, s3, s4;

   		s0 = (int)d0;
   		if(s0 < 0)
   			s0 = 256 + s0;
   		s1 = (int)d1;
   		if(s1 < 0)
   			s1 = 256 + s1;
   		s2 = (int)d2;
   		if(s2 < 0)
   			s2 = 256 + s2;
   		s3 = (int)d3;
   		if(s3 < 0)
   			s3 = 256 + s3;
   		s4 = (int)d4;
   		if(s4 < 0)
   			s4 = 256 + s4;

    	dummy = 0xc0 & s4;
    	dummy = dummy << 2;
    	sData[0] = (short)(s0 + dummy);

    	dummy = 0x30 & s4;
    	dummy = dummy << 4;
    	sData[1] = (short)(s1 + dummy);

    	dummy = 0x0c & s4;
    	dummy = dummy << 6;
    	sData[2] = (short)(s2 + dummy);

    	dummy = 0x03 & s4;
    	dummy = dummy << 8;
    	sData[3] = (short)(s3 + dummy);
    }


    private static IntentFilter makeGattUpdateIntentFilter() {		//フィルターの設定　詳細不明
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    public void Destroy()		//アプリ終了処理
    {
        Log.d(TAG, "onDestroy()");

        try {
        	LocalBroadcastManager.getInstance(m_ma).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        m_ma.unbindService(mServiceConnection);
        m_bConnectFlg = false;
        m_ma.m_lConnectTime2 = 0;
        mService.stopSelf();
        mService= null;

    	m_bConnectFlg = false;
    	m_bCreateFlg = false;
    	m_bMeasFlg = false;
    	CloseLogFile();

    }
    public void Resume()	//不明　使用されていない模様
    {
    	Log.d(TAG, "onResume");
    	if (!mBtAdapter.isEnabled())
    	{
    		Log.i(TAG, "onResume - BT not enabled yet");
    		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		m_ma.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    	}
    }
    public boolean SelectDevice(int resultCode, Intent data)	//見つかったAYAP05 を選択
    {
        if (resultCode == Activity.RESULT_OK && data != null) {
            String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

            Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
            mService.connect(deviceAddress);
			return true;

        }
        else
		{
			return false;
		}
    }

	public void CreateLogFile()	//通信ログを作成
	{
		if(!m_ma.m_bBLELogFlg)
			return;
		String filepath, filename;

		filename = m_ma.m_View.GetDefaultName(4);
		filepath = m_ma.m_View.GetSaveDir() + "/log/" + filename;
		File file = new File(filepath);
		file.getParentFile().mkdirs();
		try
		{
			m_fos = new FileOutputStream(file, true);
			m_osw = new OutputStreamWriter(m_fos, "SJIS");
			m_bw = new BufferedWriter(m_osw);
		}
		catch (Exception e)
		{
			int	err;
			err = 0;
        }
	}

	public void CloseLogFile()	//通信ログを閉じる
	{
		try
		{
			if(m_bw != null)
			{
				m_bw.flush();
				m_bw.close();
			}
			m_fos = null;
			m_osw = null;
			m_bw = null;
		}
		catch (Exception e)
		{
			int	err;
			err = 0;
        }
	}


	public void WriteLogFile(String str)	//通信ログ　文字列の書き込み
	{
		if(!m_ma.m_bBLELogFlg)
			return;
		if(m_fos == null)
		{
			CreateLogFile();
		}
		try
		{
			m_bw.write(str);
		}
		catch (Exception e)
		{
			int	err;
			err = 0;
        }
	}

	public void WriteLogFile2(char buf[], int nCount)	//通信ログの書き込み　受信データの書き込み
	{
		if(!m_ma.m_bBLELogFlg)
			return;
		if(m_fos == null)
		{
			CreateLogFile();
		}
		try
		{
			m_bw.write(buf, 0, nCount);
		}
		catch (Exception e)
		{
			int	err;
			err = 0;
        }
	}

	public void WriteTimeStamp(int nFlg)	//BLE通信の時刻の書き込み  nFlg 0スタート　１：通信データ  2:END
	{
		if(!m_ma.m_bBLELogFlg)
			return;
		long		lTime;
		lTime = System.currentTimeMillis();
	    String str;
		if(nFlg == 0 || nFlg == 2)
		{
			if(nFlg == 0)
				m_lStartTime = lTime;
			int		sec, min, hour;
			Calendar cal = Calendar.getInstance();
		    cal.setTimeInMillis(lTime);
		    sec = cal.get(Calendar.SECOND);
		    min = cal.get(Calendar.MINUTE);
		    hour = cal.get(Calendar.HOUR_OF_DAY);
		    str = String.format("%d-%d-%d,",  hour, min, sec);
		    WriteLogFile(str);
		}
		else
		{
			int		nSub;
			nSub = (int)(lTime - m_lStartTime);
			int		nLength;
			nLength = m_ma.m_View.IntToString(nSub,  m_Buf,  0);
			m_Buf[nLength] = ',';
			nLength++;
			WriteLogFile2(m_Buf, nLength);
		}
	}

    public void SendCertify()	//認証コマンドの送信
    {
    	byte[] RandKey = new byte[16];
    	m_ma.m_AESObj.MakeRandKey(RandKey);
    	m_ma.m_AESObj.SetEncrypt(RandKey);
    	byte[] SendData = new byte[17];
    	int		i;
    	SendData[0] = 'x';
    	for(i = 0; i < 16; i++)
    		SendData[i + 1] = RandKey[i];
    	SendBin(SendData);
    }

    public void GetCertifyData(int nSize, byte data[])	//受信した認証データの確認
    {
		int		j, k, i;
    	byte[]	code = new byte[16];
    	j = 0;
    	if(nSize == 20)
    	{
    		if(data[0] == 'E' && data[1] == 'N' && data[2] == 'C' && data[3] == 'R')
    		{
    			for(i = 0; i < 16; i++)
    				code[i] = data[i + 4];
    			if(m_ma.m_AESObj.CheckByteArray(m_ma.m_AESObj.m_encrypt, code))
    			{
    				m_ma.m_nCertifyFlg = 2;
//    				Toast.makeText(m_ma, m_ma.getString(R.string.certify_ok), Toast.LENGTH_LONG).show();
					Toast toast = Toast.makeText(m_ma, m_ma.getString(R.string.certify_ok), Toast.LENGTH_SHORT);
					toast.show();
					toast.setGravity(Gravity.TOP, 50, 110);

				}
    			else
    			{
    				m_ma.m_nCertifyFlg = 3;
    				m_ma.MessageBox(m_ma.getString(R.string.certify_err));

    			}
    		}
    	}
    }
}

