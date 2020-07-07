package com.adtex.NeuromusclarMonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.Selection;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class Settings extends Activity
		implements FileSelectionDialog.OnFileSelectListener

{
	int m_nInitFlg = 0;
	MainActivity m_ma;
	FileSaveDlg	m_dialog;

	final static int REQUEST_CODE = 0;
	// 初期フォルダ
	private static final int REQUEST_FILESELECT = 0;
	int m_nMessageKind = 0;

	int m_nNoOfButton = 15;
	Button m_Button[] = new Button[m_nNoOfButton];
	boolean m_bReadParamFlg = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		m_ma = GlobalVariable.m_ma;
		GlobalVariable.m_settings = this;
//		addPreferencesFromResource(R.layout.setting_main);
		setContentView(R.layout.setting_main);
		m_Button[0] = (Button) findViewById(R.id.BLESettingBtn);
		m_Button[1] = (Button) findViewById(R.id.GraphSettingBtn);
		m_Button[2] = (Button) findViewById(R.id.FilterSettingBtn);
		m_Button[3] = (Button) findViewById(R.id.PulseRateSettingBtn);
		m_Button[4] = (Button) findViewById(R.id.PulseSettingBtn);
		m_Button[5] = (Button) findViewById(R.id.FontSettingBtn);
		m_Button[6] = (Button) findViewById(R.id.StdCh1SettingBtn);
		m_Button[7] = (Button) findViewById(R.id.StdCh2SettingBtn);
		m_Button[8] = (Button) findViewById(R.id.RabbitCh1SettingBtn);
		m_Button[9] = (Button) findViewById(R.id.RabbitCh2SettingBtn);
		m_Button[10] = (Button) findViewById(R.id.LoadParamBtn);
		m_Button[11] = (Button) findViewById(R.id.SaveParamBtn);
		m_Button[12] = (Button) findViewById(R.id.CurrentValueCorrectionBtn);
		m_Button[13] = (Button)findViewById(R.id.gain_down_btn);
		m_Button[14] = (Button)findViewById(R.id.gain_up_btn);


		m_Button[0].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				SettingBLE();
			}
		});
		m_Button[1].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				SettingGraph();
			}
		});
		m_Button[2].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				SettingFilter();
			}
		});
		m_Button[3].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				SettingPulseRate();
			}
		});
		m_Button[4].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				SettingPulse();
			}
		});
		m_Button[5].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				SettingFont();
			}
		});
		m_Button[6].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				StdCh1Setting();
			}
		});
		m_Button[7].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				StdCh2Setting();
			}
		});
		m_Button[8].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				RabbitCh1Setting();
			}
		});
		m_Button[9].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				RabbitCh2Setting();
			}
		});
		m_Button[10].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				LoadParam();
			}
		});
		m_Button[11].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				SaveParam();
			}
		});
		m_Button[12].setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				SettingCorrection();
			}
		});

		m_Button[13].setOnClickListener(
				new View.OnClickListener() {		//GAIN DOWN
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				m_ma.m_View.GainDown(0);
			}
		});
		m_Button[14].setOnClickListener(new View.OnClickListener() {		//GAIN UP
			@Override
			public void onClick(View v) {
				((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
				m_ma.m_View.GainUp(0);
				m_ma.WritePreferences();
			}
		});

	}
	private void LoadParam()
	{
		m_ma.m_View.ReadParamFile();
	}

	public void onFileSelect( File file )
	{
		m_ma.m_ParamFileDir = file.getParent();
		m_ma.m_ParamFileName = file.getPath();
		m_ma.WritePreferences();
		m_ma.m_View.ReadParamFileSub(true, this);
	}

	private void SaveParam()
	{
		m_dialog = new FileSaveDlg(this, new FileSaveListener());

		m_dialog.m_FileName = "";
		m_dialog.show();

	}
	//パラメータ保存　でファイル名を指定された後に呼び出される
	public class FileSaveListener implements FileSaveDlg.DialogListener
	{
		public void onRegistSelected()
		{
			String	filename;
			filename = m_dialog.m_FileName;
			if(m_ma.m_View.SaveParamFileSub(filename))
			{
				String msg = m_ma.getString(R.string.ParamFileSaveSuccess);
				MessageBox(msg);
			}
			else
			{
				String msg = m_ma.getString(R.string.ParamFileSaveError);
				MessageBox(msg);

			}
		}

		public void onCancel()
		{
			// キャンセルした際の処理.
			;
		}
	}
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


	public void onBackButtonClick(View v) {
		((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
		finish();
	}

	void SettingBLE() {
		Intent intent = new Intent(this, SettingBLE.class);
		startActivity(intent);
	}

	void SettingGraph() {
		Intent intent = new Intent(this, SettingGraph.class);
		startActivity(intent);
	}

	void SettingFilter() {
		Intent intent = new Intent(this, SettingFilter.class);
		startActivity(intent);
	}

	void SettingPulseRate() {
		Intent intent = new Intent(this, SettingPulseRate.class);
		startActivity(intent);
	}

	void SettingPulse() {
		Intent intent = new Intent(this, SettingPulse.class);
		startActivity(intent);
	}

	void SettingCorrection(){
		Intent intent = new Intent(this, SettingCorrection.class);
		startActivity(intent);
	}

	void SettingFont() {
		Intent intent = new Intent(this, SettingFont.class);
		startActivity(intent);
	}

	void StdCh1Setting() {
		String str = m_ma.getString(R.string.StdCh1SettigParam1);
		MessageBoxYesNo(str, 0);
	}

	void StdCh2Setting() {
		String str = m_ma.getString(R.string.StdCh2SettigParam1);
		MessageBoxYesNo(str, 1);
	}

	void RabbitCh1Setting() {
		String str = m_ma.getString(R.string.RabbitCh1SettigParam1);
		MessageBoxYesNo(str, 2);
	}

	void RabbitCh2Setting() {
		String str = m_ma.getString(R.string.RabbitCh2SettigParam1);
		MessageBoxYesNo(str, 3);
	}

	//パラメータ変更確認ダイアログ
	public void MessageBoxYesNo(String str, int nKind) {
		m_nMessageKind = nKind;
		final Context		cont = this;

		AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
		alertDlg.setTitle(R.string.app_name);
		alertDlg.setMessage(str);
		alertDlg.setNegativeButton(
				"No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// OK ボタンクリック処理
						;
					}
				});
		alertDlg.setPositiveButton(
				"Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// OK ボタンクリック処理
						if ( m_nMessageKind == 0 )
							m_ma.StdSetting(0, cont);
						else if ( m_nMessageKind == 1 )
							m_ma.StdSetting(1, cont);
						else if ( m_nMessageKind == 2 )
							m_ma.RabbitSetting(0, cont);
						else //if(m_nMessageKind == 3)
							m_ma.RabbitSetting(1, cont);
					}
				});

		// 表示
		try {
			alertDlg.create().show();
		} catch (Exception f) {
			int error;
			error = 0;
		}
	}
}
