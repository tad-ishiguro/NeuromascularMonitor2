package com.adtex.NeuromusclarMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
//import com.ratocsystems.usb60sample.USB60BCR.Main.readThread;
//import com.ratocsystems.usb60sample.USB60BCR.R;
//import com.ratocsystems.usb60sample.USB60BCR.Main.readThread;

public class CUsbCom {
    public MainActivity		m_ma;
	boolean	m_bConnectFlg = false;
	private static final String TAG = "Usb60BCR.10161530";
	private static boolean D = true;
	int	m_nFlg = 0;
	int	m_nCount = 0;
	char	m_buf[] = new char[100];
	int		m_nNoOfData = 0;
	public static final int readLength = 8192;		//ORG 8192
	short		m_ReadData[] = new short[readLength];
	byte		m_buf2[] = new byte[65536];
	short		m_ReadData2[][] = new short[8][65536];
	int			m_nBuffIndex = 0;
	int			m_nLastIndex = 0;
	int		m_nComCount;
	int		m_nDataCh;
	int		m_bComInitFlg;
	short	m_sData1[], m_sData2[];


	static Context DeviceUARTContext;

	private UsbManager mUsbManager = null;

	public static D2xxManager ftdid2xx = null;
	FT_Device ftDev = null;
	D2xxManager.DriverParameters d2xxDrvParameter;

	int iVid = 0x0584;	// Vendor ID of RATOC
	int iPid = 0xb020;	// Defualt:REX-USB60F
	int iPidTb[] = {
			0xb020,	// REX-USB60F
			0xb02f,	// REX-USB60MI
			0xb03B,	// REX-USB60MB
			0xffff};
	int iDevid = 0;	// DeviceId
	Context m_Context;
	static int iEnableReadFlag = 1;

	// local variables
	int baudRate = 115200;	// baud rate
	byte dataBit = 8;	// 8:8bit, 7:7bit
	byte parity = 0;	// 0:none, 1:odd, 2:even, 3:mark, 4:space
	byte stopBit = 1;	// 1:1 stop bits, 2:2 stop bits
	byte flowControl = 0;	// 0:none, 1:flow control (CTS/RTS)
	int portNumber = 1;

	ArrayList<CharSequence> portNumberList;

	public int readcount = 0;
	public int iavailable = 0;
	public int savePosition = 0;
	public int saveLength = 0;
	byte[] readData;
	char[] readDataToText;
	byte[] readDataToBin;
	public boolean bReadThreadGoing = false;	// Thread flag
	public readThread read_thread;

	boolean uart_configured = false;
	boolean		m_bUSBInitFlg = false;

	AlertDialog alertDlg = null;
	private String mBCRtext;

	// BCR Header & Terminate Code for KEYENCE BL-N60
	private static char StxCode = 0x02, EscCode = 0x1B,
			ExtCode = 0x03, EotCode = 0x04,
			LFCode = 0x0a, CRCode = 0x0d;

//	Handler mHandler = new Handler();

	private static final String ACTION_USB_PERMISSION =
			"com.android.example.USB_PERMISSION";
	private Intent mIntent;
	private PendingIntent mPermissionIntent;

	CUsbCom(MainActivity ma)
	{
		m_ma = ma;
	}

    public void InitObj(Context context) {        //�Ăяo���Ƃ��� m_UsbCom.InitObj(this);
    	if(m_bUSBInitFlg)
    		return;
		m_sData1 = new short[20];
		m_sData2 = new short[20];

        readData = new byte[readLength];
        readDataToText = new char[readLength+2];
        readDataToBin = new byte[readLength+2];
        m_Context = context;

        mUsbManager = (UsbManager) m_Context.getSystemService(Context.USB_SERVICE);
    	if (mUsbManager == null)
            if (D) Log.e(TAG, "+++ Cnanot getSystemService for USBMnager +++");


    	try {
    		ftdid2xx = D2xxManager.getInstance(DeviceUARTContext = m_Context);
    	} catch (D2xxManager.D2xxException ex) {
    		ex.printStackTrace();
    	}

    	if (ftdid2xx != null) {
    		int i;
    		for(i = 0; iPidTb[i] != 0xffff; i++) {
    			if(!ftdid2xx.setVIDPID(iVid,iPidTb[i]))		// Set VID/PID of target USB device
    				Log.i("ftd2xx-java","setVIDPID Error");
			}
			if(!ftdid2xx.setVIDPID(0x557,0x2008))		// Set VID/PID of target USB device sanwa
				Log.i("ftd2xx-java","setVIDPID Error");
			if(!ftdid2xx.setVIDPID(0x7aa, 0x2a))		// Set VID/PID of target USB device corega
				Log.i("ftd2xx-java","setVIDPID Error");
			if(!ftdid2xx.setVIDPID(0x403, 0x6001))		// Set VID/PID of target USB device bluetooth dongru
				Log.i("ftd2xx-java","setVIDPID Error");

    	}

    	mIntent = new Intent(ACTION_USB_PERMISSION);
        mPermissionIntent = PendingIntent.getBroadcast(m_Context, 0, mIntent, 0);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        m_Context.registerReceiver(mUsbReceiver, filter);		// register for USB60 unpluged
        m_bUSBInitFlg = true;
    	m_nNoOfData = 0;
    	m_nBuffIndex = 0;
    	m_nLastIndex = 0;
		m_bConnectFlg = false;
    }

    // ******************************************************************************************************************************************************************
    private UsbDevice Search_MyUsbSerial() {
    	UsbDevice mUsbDevice = null;
    	int openIndex = 0;	// searched device at first

		int tempDevCount = ftdid2xx.createDeviceInfoList(DeviceUARTContext);
		Log.i("Misc Function Test ",
				"Device number = " + Integer.toString(tempDevCount));

		if (tempDevCount == 0) {
			return null;
		}

		D2xxManager.FtDeviceInfoListNode DevInfoNode = ftdid2xx.getDeviceInfoListDetail(openIndex);
    	int MyDeviceId = DevInfoNode.location / 16;
    	HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
    	Iterator<UsbDevice> deviceIterator  = deviceList.values().iterator();
    	// Search UsbDevice object
    	while(deviceIterator.hasNext()){
    	      UsbDevice mUsbDev = deviceIterator.next();
    	      if (MyDeviceId == mUsbDev.getDeviceId()) {
    	    	  mUsbDevice = mUsbDev;	// get UsbDevice object
    	    	  break;
    	      }
    	}

		if (D) {
			iVid = mUsbDevice.getVendorId();
			iPid = mUsbDevice.getProductId();
			iDevid = mUsbDevice.getDeviceId();
			Log.i(TAG, "VenderId: " + Integer.toHexString(iVid) +
    	           "  ProductId: " + Integer.toHexString(iPid) +
    	           "  Device Id: " + Integer.toHexString(iDevid));
		}

		return mUsbDevice;
    }


    private boolean Start_MyUsbSerial(UsbDevice MyUsbDevice) {
/*----*/
    	if (connectFunction(MyUsbDevice) == false) {
//			mTvStatus.setText(R.string.Notconnected);
			return false;
    	}
/*-------------*/
    	SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
//    	mTvStatus.setText(R.string.Connected);

       	if (D) Log.e(TAG, "+++ Start_MyUsbSerial OK !!! +++");
		m_bConnectFlg = true;
		return true;
    }

	public void End_MyUsbSerial() {

		bReadThreadGoing = false;
		try {
			Thread.sleep(50);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}

		if(ftDev != null) {
			synchronized(ftDev) {
				if( true == ftDev.isOpen()) {
					ftDev.close();
				}
				ftDev = null;
			}
		}
		m_bConnectFlg = false;
	}

	public void onUsbResume() {

	    if (D) Log.e(TAG, "+++ onResume +++");

	    if (ftDev != null)
	    	return;

	    UsbDevice mUsbDevice = Search_MyUsbSerial();

		if(mUsbDevice != null) {

			mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);

			return;
		}

		MyUsbSerialErrDlg();
	}

	public void onUsbStop() {
        if (D) Log.e(TAG, "+++ onStop +++");
		End_MyUsbSerial();
	}

    /**
     * Alert Dialog for USB-Serial error */
	public void Reconnect()
	{
		if(!m_bConnectFlg) {
			End_MyUsbSerial();
			Start_MyUsbSerial(Search_MyUsbSerial());
		}
    	m_nNoOfData = 0;
	}


    private void MyUsbSerialErrDlg()
    {
    	if (alertDlg != null)
    		return;

	    alertDlg = new AlertDialog.Builder(m_Context)
	    .setTitle(R.string.app_name)
	    .setMessage(R.string.alert_NotConnect)
	    .setPositiveButton(R.string.btn_ok,
	    	new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int which) {
	    		}
	    	}
	    )
	    .show();
    }

	public boolean connectFunction(UsbDevice MyUsbDevice)
	{

		if (MyUsbDevice == null)
			return false;

		if(null == ftDev) {
			ftDev = ftdid2xx.openByUsbDevice(DeviceUARTContext, MyUsbDevice);
		} else {
			synchronized(ftDev)
			{
				ftDev = ftdid2xx.openByUsbDevice(DeviceUARTContext, MyUsbDevice);
			}
		}

		if(ftDev == null) {
//			Toast.makeText(DeviceUARTContext,"open device port NG, ftDev == null", Toast.LENGTH_LONG).show();
			Toast toast = Toast.makeText(DeviceUARTContext,"open device port NG, ftDev == null", Toast.LENGTH_SHORT);
			toast.show();
			toast.setGravity(Gravity.TOP, 50, 110);

			return false;
		}

		if (true == ftDev.isOpen()) {
//			Toast.makeText(DeviceUARTContext, "open device port OK", Toast.LENGTH_SHORT).show();
			Toast toast = Toast.makeText(DeviceUARTContext, "open device port OK", Toast.LENGTH_SHORT);
			toast.show();
			toast.setGravity(Gravity.TOP, 50, 110);

			m_ma.m_lConnectTime2 = System.currentTimeMillis();

			if(false == bReadThreadGoing) {
				read_thread = new readThread(handler);
				read_thread.start();
				bReadThreadGoing = true;
			}
		} else {
//			Toast.makeText(DeviceUARTContext, "open device port NG", Toast.LENGTH_LONG).show();
			Toast toast = Toast.makeText(DeviceUARTContext, "open device port NG", Toast.LENGTH_SHORT);
			toast.show();
			toast.setGravity(Gravity.TOP, 50, 110);

			return false;
		}

		return true;

	}

	public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl)
	{
		if (ftDev.isOpen() == false) {
			Log.e("j2xx", "SetConfig: device not open");
			return;
		}

		// configure our port
		// reset to UART mode for 232 devices
		ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

		ftDev.setBaudRate(baud);

		switch (dataBits) {
		case 7:
			dataBits = D2xxManager.FT_DATA_BITS_7;
			break;
		case 8:
			dataBits = D2xxManager.FT_DATA_BITS_8;
			break;
		default:
			dataBits = D2xxManager.FT_DATA_BITS_8;
			break;
		}

		switch (stopBits) {
		case 1:
			stopBits = D2xxManager.FT_STOP_BITS_1;
			break;
		case 2:
			stopBits = D2xxManager.FT_STOP_BITS_2;
			break;
		default:
			stopBits = D2xxManager.FT_STOP_BITS_1;
			break;
		}

		switch (parity) {
		case 0:
			parity = D2xxManager.FT_PARITY_NONE;
			break;
		case 1:
			parity = D2xxManager.FT_PARITY_ODD;
			break;
		case 2:
			parity = D2xxManager.FT_PARITY_EVEN;
			break;
		case 3:
			parity = D2xxManager.FT_PARITY_MARK;
			break;
		case 4:
			parity = D2xxManager.FT_PARITY_SPACE;
			break;
		default:
			parity = D2xxManager.FT_PARITY_NONE;
			break;
		}

		ftDev.setDataCharacteristics(dataBits, stopBits, parity);

		short flowCtrlSetting;
		switch (flowControl) {
		case 0:
			flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
			break;
		case 1:
			flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
			break;
		case 2:
			flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
			break;
		case 3:
			flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
			break;
		default:
			flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
			break;
		}

		// flow ctrl: XOFF/XOM
		ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);

		uart_configured = true;
//		Toast.makeText(DeviceUARTContext, "Config done", Toast.LENGTH_SHORT).show();

 	}

 	public void ShowSerailInfo() {

    	String sSerialInfo, sParity, sFlowControl;

		switch (parity) {
		case 0:
			sParity = "NONE";
			break;
		case 1:
			sParity = "ODD";
			break;
		case 2:
			sParity = "EVEN";
			break;
		case 3:
			sParity = "MARK";
			break;
		case 4:
			sParity = "SPACE";
			break;
		default:
			sParity = "NONE";
			break;
		}

		switch (flowControl) {
		case 0:
			sFlowControl = "NONE";
			break;
		case 1:
			sFlowControl = "RTS/CTS";
			break;
		case 2:
			sFlowControl = "DTR/DSR";
			break;
		case 3:
			sFlowControl = "XON/XOFF";
			break;
		default:
			sFlowControl = "NONE";
			break;
		}

        sSerialInfo = m_Context.getString(R.string.h_BaudRate) + " : " + String.valueOf(baudRate) + "\n"
        + m_Context.getString(R.string.h_DataBit) + " : " + String.valueOf(dataBit) + "\n"
        + m_Context.getString(R.string.h_ParityBit) + " : " + sParity + "\n"
        + m_Context.getString(R.string.h_StopBit) + " : " + String.valueOf(stopBit) + "\n"
        + m_Context.getString(R.string.h_FlowControl) + " : " + sFlowControl;

    	alertDlg = new AlertDialog.Builder(m_Context)
    	.setTitle(m_Context.getString(R.string.app_name) + " - Serial Setting")
    	.setMessage(sSerialInfo)
    	.setPositiveButton(R.string.btn_ok,
    			new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    			}
    		}
    	)
    	.show();
    }

	final Handler handler =  new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		try
    		{
    			if (alertDlg != null)
    				alertDlg.dismiss();

    			if(0 < saveLength)
    			{
    				GetUARTData();
/*---------------------------
    				if(m_ma.m_nComKind2 == 0)
    					ReciveA30();
    				else if(m_ma.m_nComKind2 == 2)
    					ReciveA40(0);
    				else if(m_ma.m_nComKind2 == 3)
    					ReciveA40(1);
 ------------------------*/
    				saveLength = 0;
    			}
    		}
    		catch (Exception e)
    		{
    			int 	a = 0;
    			a = 1;
    		}
    	}
    };
	public int GetUARTData()	//BLEj受信データの処理
	{
		int i;
		int		ret = 0;
		int nLength = saveLength;
		if ( nLength <= 0 )
			return 0;
		for (i = 0; i < nLength; i++) {
			m_buf2[m_nBuffIndex] = readDataToBin[i];
			m_nBuffIndex++;
		}
		if(3 <= m_nBuffIndex && m_buf2[0] == 'R')
		{
			if(m_buf2[1] == '0' && m_buf2[2] == '1') {
				m_ma.m_bMsgReq = true;
				m_ma.m_Msg = m_ma.getString(R.string.P06SS_NOT_CONNECT);
				m_ma.m_View.m_nMeasMode = 0;
				m_nBuffIndex = 0;
			}
			if(m_buf2[1] == '0' && m_buf2[2] == '2') {
				m_ma.m_bMsgReq = true;
				m_ma.m_Msg = m_ma.getString(R.string.P06SS_NOT_CONNECT2);
				m_ma.m_View.m_nMeasMode = 0;
				m_nBuffIndex = 0;
			}
		}
		if(20 <= m_nBuffIndex) {
			if (m_buf2[0] == 'E' && m_buf2[1] == 'N' && m_buf2[2] == 'C' && m_buf2[3] == 'R')
				m_ma.CheckABSCommand(20, m_buf2);
			else
				ret = m_ma.GetBLEData(20, m_buf2, m_sData1, m_sData2);
			m_nBuffIndex = 0;
		}
		return ret;
	}


	private class readThread  extends Thread {
		Handler mHandler;

		readThread(Handler h){
			mHandler = h;
//			setPriority(Thread.MIN_PRIORITY);
			setPriority(Thread.NORM_PRIORITY);
		}

		@Override
		public void run()
		{
			int i;

			while(true == bReadThreadGoing) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
                    e.printStackTrace();
				}

				synchronized(ftDev)
				{
					iavailable = ftDev.getQueueStatus();
					if (0 < iavailable) {

						if(readLength < iavailable){
							iavailable = readLength;
						}
						if(readLength < (savePosition + iavailable))
							iavailable = readLength - savePosition;
/*------------
						if(D)
						{
							String	logstr;
							logstr = String.format("read data %d", iavailable);

							Log.e("ReadData", logstr);
						}
------------*/
						ftDev.read(readData, iavailable);
						/*---
						if(m_ma.m_nComKind2 == 0)
						{
							for (i = 0; i < iavailable; i++) {
								readDataToText[savePosition+i] = (char) readData[i];
							}
							savePosition += i;
							readDataToText[savePosition] = '\0';
						}
						else
						----*/
						{
							for (i = 0; i < iavailable; i++)
							{
								readDataToBin[savePosition] = readData[i];
								savePosition++;
							}
						}

						if (savePosition == readLength) {	// overflow
							saveLength = savePosition;
							Message msg = mHandler.obtainMessage();
							mHandler.sendMessage(msg);
							savePosition = 0;
						}

					}
					if (0 < savePosition) {
						saveLength = savePosition;
						Message msg = mHandler.obtainMessage();
						mHandler.sendMessage(msg);
						savePosition = 0;
					}
				}
			}
		}

	}
	//
    // Broardcast Receiver for USB60 Remove
    //
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();

			Log.d(TAG, "onReceive action : " + action);

    		if (ACTION_USB_PERMISSION.equals(action)) {
    			synchronized (m_Context) {			//�����Ŗ�蔭��
    				UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

    				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
    					if (device != null) {
    						if (alertDlg !=null) {
    							alertDlg.dismiss();
    							alertDlg = null;
    						}
//    						Log.d(TAG, "Permission granted " + device);
    						Start_MyUsbSerial(device);
    					}
    				} else {
    					Log.d(TAG, "permission denied for device " + device);
    				}
    			}
    		} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            	End_MyUsbSerial();
//    			mTvStatus.setText(R.string.Notconnected);
            }
    	}
    };
	public boolean sendData(int numBytes, byte[] buffer)
	{
		if(ftDev == null)
			return false;
		if (ftDev.isOpen() == false)
			return false;
		if (0 < numBytes)
		{
			ftDev.write(buffer, numBytes);
		}
		return true;
	}
	public boolean sendString(String str)
	{
		boolean bRet;
		byte[] sbyte = str.getBytes();
		int nLength = sbyte.length;
		bRet = sendData(nLength, sbyte);
		return bRet;
	}

}



