package com.adtex.NeuromusclarMonitor;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.os.Environment;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.widget.Toast;
import android.widget.TextView;

import android.hardware.usb.UsbManager;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;


class SignalObj
{
	int     m_nOutputPulseValue = 0;        //出力した電流値
	boolean	m_bCorrectOutFlg = false;		//ピーク出力ありなら　true
	int		m_nCorrectionStartCount = 0;	//	ピーク出力開始位置
	float	m_fCorrectionSum = 0.0F;	//平均値を求めるための積算値
	int		m_nCorrectionAveCount = 0;	//平均値を出すためのカウント

	boolean m_bCorrectionLogFlg = false;	//true筋収縮信号値計算ログを記録する
	float	m_fCorrectionValue = 0.0F;		//筋収縮信号出力 電流値 mA
	boolean m_bCorrectPulseMode = false;	//筋収縮信号出力中 true;
	boolean m_bCorrectADFlg = false;		//筋収縮信号出力中 ADFlg
	int		m_nCorrectInterval = 100;	//筋収縮信号出力中 パルス間隔(msec)
	int		m_nCorrectNoOfPulse = 1;	//筋収縮信号出力中 パルス数
	int		m_nCorrectOutputCount = 0;	//筋収縮信号出力中出力したパルスのカウント
	long	m_lCorrectOutputTime;	//筋収縮信号出力 TET　出力時間

	int		m_nCorrectionCount;			//測定開始から計算を何回行ったか？
    int		m_nCorrectionAllCount = 0;			//アプリ起動時から計算を何回行ったか？
	float	m_fNoOfUnblockedReceptors;	//筋弛緩剤投与下でもブロックされていない受容体数(万)
	float	m_fNoOfBlocksDueStim;		//神経刺激によるACh受容体ブロック数(万)
	float 	m_fNoOfUnblockedACh;	//神経刺激後の非ブロックACh受容体数(万)
	float	m_fAChRecoveryRate;	//神経刺激によるACh受容体ブロック回復率
	float	m_fAChRecovery;		//ACh受容体ブロック回復量
	float	m_fPTP_Stim;	//神経刺激によるACh増加量(PTP)万
	float 	m_fNoOfUnblockedReceptors2;	//増加量を加えた非ブロックACh受容体数(万)
	float	m_fInitReceptor;	//増加量を加えた非ブロックACh受容体数(万)　の初期値-1  演算をやめるための判定値

	int		m_nCorrectionKind;	//電流値補正　0:無し　1:あり
	float	m_fAChReceptor;		//ACh 受容体数の初期値(万)
	float	m_fAChBlockRate;	//	筋弛緩深度(ACh 受容体ブロック率 0-100%)
	float	m_fAChBlockRateX;	//	筋弛緩深度(ACh 受容体ブロック率 0-100%)	/* adtex */
	float	m_adtex_AChBlockRate[] = new float[10];	//	筋弛緩深度(ACh 受容体ブロック率 0-100%)
	float	m_fBlockOneStim;	//	一回の神経刺激によるＡCh 受容体ブロック数(万)
	float	m_fRecoveryHalfLife;	//	ACh 受容体回復量の半減期 t1 / 2(sec)
	float	m_fPTP;				//		1回の神経刺激による増加量(PTP)
	float	m_fPTP_HalfLife;	//	ACh 増加量(PTP)の半減期 t1 / 2(sec)
//	float	m_fMinimumSensitivity;	//	最小感度(%)

	int		m_nFileVersion = 0;		//File Version
	int     m_nNoOfCalAve = 3;      //キャリブレーション時の移動平均数
	int		m_nOldPulseOutNo;	//PulseOutNo を何度も出力するのをふせぐための変数
	float	m_fCalPeakAve;	//CAL ピークの移動平均値　移動平均は３固定
	int		m_nCalCount;	//キャリブレーションのパルス送信　回数
	boolean	m_bCalFlg;	//キャリブレーション　山が見つかったら　ＴＲＵＥ　　見つからない場合はＦＡＬＳＥ
	float	m_fCalResultCurrentValue;	//キャリブレーション 結果の電流値
    int     m_nBatteryType;         //0アルカリ電池　1ニッケル水素　　2Eneloop
	int		m_nAutoPilotPTCLevel;	//AutoPilot で　PTC から　TOFにもどるための　PTCしきい値
    int     m_nPTCPhase = 0;	//PTC の段階　0：開始前　　1:Twitch1　　2:TET   3:Twitch2
    long	m_lPTCPhaseTime = 0;			//PTC 次の段階に移行する時間

	boolean m_bDBSSecondPulse = false;	//DBS の第二パターンを出力する
	long	m_lDBSSecondTime;			//DBS の第二パターンを出力する時間

	long	m_lLastPulseTime = 0;		//最後にパルスを出力した時間    msec   中止時の中止コマンド出力判定　に用いる
	long	m_lLastPulseOutTime = 0;	//最後に出力したパルスの出力時間 msec
	float	m_fWork[] = new float[40];
	int		m_nResultPeakPos[] = new int[40];
	float	m_fResultPeakValue[] = new float[40];
	long	m_nPrevPulseOutTime = 0;		//前回パルスを送った時間
	int		m_nAutoCalibrationCount;	//キャリブレーションで何回パルスを出したかをカウント
	int		m_nPulseMode = 0;		//0:無し0   1:CAL 単押し  2:CAL 長押し  3:TOF単押し　4:TOF長押し　　5:TWITCH単押し  6:TWITCh 長押し
	//　　　　　7:TET単押し　　8:TET長押し  9:DBS単押し　 10:DBS長押し　 11:PTC単押し　 12:PTC長押し  13 AutoPilot TOF  14AutoPilot PTC
	int		m_nOldPulseMode = -1;	//ボタンの色の変更
    int     m_nOldMeasMode = -1;     //ボタンの色の変更
	int		m_nPulseOutNo = 0;		//Pulse 出力時のADデータ番号を取得
	boolean	m_bPulseOutFlg = false;	//Pulse を出力したらtrue
	boolean m_bStopSaveFlg;			//[STOP]で自動保存
	int		m_nNoOfGain;			//GainTableの数
	float	m_fCalibrationStart;	//キャリブレーション　スタート電流値 (mA)
	float	m_fCalibrationStep;		//キャリブレーション　ステップ電流値 (mA)
	int		m_nCalibrationInterval;	//キャリブレーション　パルス　出力間隔　(msec)
	float	m_fCurrentValue;		//電流値(mA)
	float	m_fDetectionThreshold;	//検出閾値　　コントロール値の％で指定
	float	m_fControlValue;		//コントロール値　補正 μV
	float	m_fControlRawValue;		//生のコントロール値 　　頭打ちになる値
	int		m_nPulseWidth;			//パルス幅(μsec)
	int		m_nTwitchInterval;		//Twitch間隔(sec)
	float	m_fTOFInterval;			//パルスの間隔 T1-T1 sec
	float	m_fTOFStimInterval;		//パルスの間隔 T1-T2 sec
	float	m_fTOFTimeLimit;	//TOFマニュアル制限時間(sec)
	int		m_nPTCTwitch1Num;	//PTC Twitch1 刺激回数
	int		m_nPTC_TETStimFreq;	//PTC TET 刺激周波数(Hz)
	float	m_fPTC_TETStimTime;	//PTC TET 刺激時間(sec)
	int		m_nPTCTwitch2Num;	//PTC Twitch2 刺激回数
	float	m_fPTCAutoInterval;	//PTC 自動繰り返し時間(sec)
	float	m_fPTCTimeLimit;	//PTC マニュアル制限時間(sec)
	float	m_fDBSStimInterval;	//DBS 刺激間隔(msec)
	float	m_fDBS_1_2_Interval;	//DBS 1-2 間隔(sec)
	float	m_fDBS_1_1_Interval;	//DBS 1-1 間隔(sec)
	int		m_nDBSPattern;		//DBS バーストパターン   0:3.3    1:3.1   2:2.3
	float	m_fDBSTimeLimit;	//DBS マニュアル制限時間(sec)
	int		m_nTETStimFreq;		//TET 刺激周波数(Hz)
	float	m_fTETStimTime;		//TET 刺激時間(sec)
	float	m_fTETTimeLimit;	//TET 制限時間(sec)

	String		m_PatientID;
	float		m_fBatteryVolt = 0.0F;
	int         m_nBatteryLevel = 0;
	boolean		m_bDebugModeFlg;
	boolean		m_bAutoGainFlg;
	float		m_fHeartAmpMag;
	float		m_fRespiratoryAmpMag;
	float		m_fTOF1AmpMag;
	byte		m_byGainArray[][];
	byte		m_byDAPulseKind[];
	byte		m_byDACurrent[];
	int         m_nPulseOutArray[];

	float		m_fGainAmp[] = new float[20];	//Gain 増幅率
	float		m_fGainConst[] = new float[20];	//Gain 増幅率
	int			m_nKind;	//タッチした場所　0 WAVE  1:Calc  2:Pulse  3:Calc Scroll  4:WaveScroll
	boolean		m_bZoomFlg, m_bShiftFlg;	//タッチ操作　ズーム、シフト
	int m_GlobalPos[] = new int[2];	//surface view の座標
	CMyScrollBar	m_ScrollBar[];		//[連続解析]　結果グラフ下のスクロールバーのオブジェクト
	float		m_zsx1, m_zsy1, m_zex1, m_zey1;	//タッチした開始座標と終了座標
	float		m_zsx2, m_zsy2, m_zex2, m_zey2;	//２本目のタッチした開始座標と終了座標
	char	m_char[];			//データ保存に用いられるワークバッファ
//	char    m_line[];        //USBシリアル通信用1行バッファ
	String  m_line;
	ProgressDialog m_ProgressDlg;	//保存中のダイアログ
	String	m_str;				//データ保存に用いられる文字列
	MarkerData	m_Marker[];		//[脈波表示] で測定中タップされた場合のマーカー情報を保持する
	int		m_nNoOfMarker;		//記録されたマーカーの数
	int		m_nNoOfMarkerAlloc;		//確保されているマーカーのバッファ数
	float		m_fMarkerTime;		//タップされたマーカーの位置
	Paint	m_MarkerPaint = new Paint();	//マーカーの色
	String	m_Comment;			//マーカー位置のコメント

	int			m_nDialogKind;		//0:Save   1:Read  2:Calc
	boolean		m_bDlgCancelFlg;	//データ保存中のダイアログでキャンセルがタップされた場合 true
	boolean m_bProgressInDlg;		//データ処理中は　true
	boolean		m_bGraphCheck[];	//結果グラフを表示するか否か
	long m_nPrevDataTime;	//最後にサンプリングした時刻　測定可能残り時間の計算に用いられる
	float	m_fHeartRateUpper, m_fHeartRateLower, m_fRespiratoryRateUpper, m_fRespiratoryRateLower;
	int		m_nCalcGraphAveFlg, m_nAverageTime;
	float	m_fCalcMax, m_fCalcMin;	//結果グラフの縦軸　範囲
	float	m_fPulseMax, m_fPulseMin;	//結果グラフ　心拍数グラフの縦軸範囲

	int		m_nDspStartTime;	// m_bLastDspFlgがfalse の時の結果グラフ表示開始時刻 sec
	int		m_nCalcDataRange =600;	//結果グラフの表示幅sec
	boolean m_bLastDspFlg;		//結果グラフ true 常に最新の結果を表示　false スクロールバーがずらされて　古いデータを表示
	float	m_fNormalBPM[] = new float[4];	//BPM のチェックで使う　正常なBPM を記憶
	int		m_nNoOfCheck[] = new int[4];	//脈拍数のチェックに用いられる。
	float		m_fCheckPulse[][];	//脈拍数のチェックに用いられる配列
	int		m_nChPeakPos[][];	//時間差　算出をおこなうピークのポジション
	int		m_nPeakCount[][];	//脈波ピークの配列
	int		m_nNoOfPeak[] = new int[4];	//検出した脈波ピーク数
	int		m_nLastPeak[] = new int[2];	//検出したピークの内、時間差に用いた最後のピーク位置
	int		m_nOldCh1[] = new int[2];	//最後に時間差に用いたピークの位置

	int		m_nAWork[][];			//Aピークの位置を記録するためのワークエリア
	int		m_nACount[];	//Aピークを記録した数
	int		m_nLastCalcPos[];		//最後に計算した位置
	public IIRFilterObj	m_Filter[];	//心弾波　包絡線処理　ローパスフィルター
	float	m_fPaketBuf[];		//受信データをフロートに
	float	m_fPaketBuf2[];		//受信データをフロートに
	float	m_fPaketBuf3[];		//受信データをフロートに
	float	m_fPaketBuf4[];		//受信データをフロートに
	final  int		m_nPacketSize = 100;	//一度に転送される　パケットの最大サイズ

	long	m_lSignalTime;		//最後にデータカット閾値以上の信号が検出された時間
	boolean m_bSignalFlg2 = true;		//シグナルが無い場合、心拍数を０にするためのフラグ
	long		m_lLastPulseLessTime = 0;	//最後に脈波がなくなった時間、（正常値に戻って三秒後にアラームを消す）
	boolean		m_bPulseLessFlg = false;	//脈波がないとfalse
	public HartObj		m_HartObj;		//測定時のハートマークを描画するオブジェクト
	boolean				m_bHartFlg;		//ハートマークを鼓動させるためのフラグ
	int		m_bOverRangeErrFlg[] = new int[2];	//オーバーレンジを検出するための変数
	long	m_nOverRangeTime[] = new long[2];	//オーバーレンジを検出するための変数
	int m_nHartWidth;				//測定時画面右上に表示されるハートの幅
	boolean m_bAbsoluteTimeFlg = true;		//[詳細設定]-[その他の設定] [グラフの横軸絶対時刻表示]
	double	m_ax[], m_ay[], m_by[], m_bx[];	//グラフをビューに描画するための係数
	int		m_sx[], m_ex[], m_sy[], m_ey[];	//グラフの四隅の座標
	int		m_Text_ey;			//結果の数値を表示する位置の高さ

	float	m_fDspRawData;
	float	m_fDspRawStart;
	float	m_fDspRawEnd;
	long		m_nPrevDspTime;
	Paint m_RangePaint = new Paint();	//WAVE グラフの表示範囲をCalcグラフに示す
	Paint m_winPaint = new Paint();		//グラフ下地の色
	Paint m_RawPaint = new Paint();	//生波形のグラフ色
	Paint m_frPaint = new Paint();		//フレーム、目盛りの色
	Paint m_Text = new Paint();			//計算値文字表示
	Paint m_Bold = new Paint();		//ハートマークの中の脈拍数のテキスト文字の大きさ指定
	Paint m_ErrText	= new Paint();	//オーバーレンジした場合の表示テキスト　右上赤
	Paint m_OverRangeText = new Paint();	//オーバーレンジした場合の表示テキスト　中央　大きな赤
	Paint m_OKText	= new Paint();		//オーバーレンジしてない場合の表示テキスト　右上　黒
	Paint m_memPaint = new Paint();		//目盛りの色
	Paint m_bkPaint = new Paint();		//全体の下地の色
	Paint	m_WavePaint[];	//波形グラフの色　0:筋弛緩, 1:心拍数, 2:呼吸波形
	Paint m_CalcPaint[];	//結果グラフの色　0:脈拍  1:呼吸数　2:T1アンプリチュード  3:T4/T1  4:TOF Count   5:PTC  6:CtrlValue   7 Gain
	float	m_fResult[][][];	//  結果配列  0瞬時値　1:平均　0:脈拍  1:呼吸数　2:T1アンプリチュード  3:T1アンプリチュード（CtrlVal比）4:T4/T1   5:TOF Count   6:PTC  7 Gain
	float	m_fResultVal[][];		//結果 0瞬時値　1:平均　2 平均値　０の時を除く  0:脈拍  1:呼吸数　2:T1アンプリチュード  3:T1アンプリチュード（CtrlVal比）4:T4/T1  5:TOF Count 6 PTC    7Gain
	float	m_fTime[][];	//脈波結果の時間　msec    0:脈波　1:呼吸　2:筋弛緩データ
	int		m_nNoOfData[];			//計算結果の数 0:脈波　1:呼吸　2:筋弛緩データ


	int		m_nNoOfCalc = 7;	//計算結果の種類の数
	int		m_nNoOfWave = 5;	//WAVE波形の種類の数
	long	m_nGainTime;		//オートゲインで１秒毎に最大値を調べるための時間
	short	m_sUpperLimitLevel, m_sUpperLevel, m_sLowerLevel, m_sLowerLimitLevel;	//AUTO GAIN のためのレベル定義
	int		m_nLastPos;				//RawDataのデータ数
	int		m_nOldLastPos;		//オートゲインで１秒毎に最大値を調べるための最初の範囲


	int		m_nSamplingRate = 200;
	int		m_nNoOfFileData = 0;//[ファイル開く]　で選択されたファイルにふくまれたデータの数
	int		m_nReadPos, m_nReadSize;	//ファイルから一行分の文字列を取り出すための変数
	String		m_SelectFileName;	//[ファイル開く]　で選択されたファイル名
	int		m_nGainLevel[];
	int		m_nCharSize = 15;	//[詳細設定]-[フォントサイズ] [メモリのフォントサイズ]
	int		m_nCharSize2 = 20;	//[詳細設定]-[フォントサイズ] [計算値のフォントサイズ]
	float	m_fLowBandPassFreq1;
	float	m_fLowBandPassFreq2;
	float	m_fHighPassFreq;
	float	m_fBandPassFreq1;
	float	m_fBandPassFreq2;

	boolean m_bDrawFlg;
	int			m_nWidth, m_nHeight;
	int			m_nCount, m_nOldCount;
	MainActivity	m_ma;		//MainActivity
	int		m_nMeasMode = 0; //測定モード　0:測定していない　1:測定中
	long m_lMeasStartTime;
	String	m_DataFolder;
	float		m_fWaveMax, m_fWaveMin;
	float	m_fWaveData[][];		//0:Ch1生波形 1:Ch2生波形　2:筋弛緩波形　3:心拍波形　4:呼吸波形
	int		m_nNoOfAllocRawData, m_nNoOfAllocCalcData;
	float	m_fDays;
	float	m_fDataTime[];			//生波形の時間
//adtex USB
	private FT_Device ftDev = null;
	private static Context mContext;
	private D2xxManager ftdid2xx;
	private static final String TAG = "USB";
	private int iavailable = 0;
	private static final int readLength = 512;

	SignalObj()
	{
		int		i;
		for(i = 0; i < 2; i++)
		{
			m_bOverRangeErrFlg[i] = 0;
			m_nOverRangeTime[i] = System.currentTimeMillis();
		}
/*---------
		m_fGainAmp[0] = 94.4F;
		m_fGainAmp[1] = 162.0F;
		m_fGainAmp[2] = 272.5F;
		m_fGainAmp[3] = 458.5F;
		m_fGainAmp[4] = 744.3F;
		m_fGainAmp[5] = 1251.7F;
		m_fGainAmp[6] = 2105.7F;
		m_fGainAmp[7] = 3522.8F;
		m_fGainAmp[8] = 5924.8F;
		m_fGainAmp[9] = 9967.1F;
		m_nNoOfGain = 10;
		SetGainConst();
		-----------*/
		m_nOldPulseOutNo = 0;
		m_nKind = -1;
		m_bZoomFlg = m_bShiftFlg = false;
		m_MarkerPaint.setColor(Color.RED);
		m_MarkerPaint.setTextAlign(Paint.Align.LEFT);
		m_MarkerPaint.setStrokeWidth(8.0F);

		m_RangePaint.setColor(Color.GRAY);
		m_RangePaint.setStrokeWidth(8.0F);
		m_nNoOfMarkerAlloc = 100;
		m_Marker = new MarkerData[m_nNoOfMarkerAlloc];
		for(i = 0; i < m_nNoOfMarkerAlloc; i++)
			m_Marker[i] = new MarkerData();
		m_nNoOfMarker = 0;
		m_char = new char[4000];
		m_line  = "";
		m_ProgressDlg = null;
		m_bProgressInDlg = false;
		m_fResultVal = new float[3][m_nNoOfCalc + 1];
		m_nLastCalcPos = new int[4];
		m_ScrollBar = new CMyScrollBar[2];
		m_ScrollBar[0] = new CMyScrollBar();
		m_ScrollBar[1] = new CMyScrollBar();
		m_ax = new double[2];
		m_bx = new double[2];
		m_ay = new double[3];	//0:WAVE   1:b/a, c/a    2:Pulse
		m_by = new double[3];
		m_sx = new int[4];	//0 WAVE,HISTORY  1:Calc  2:CalcScroll  3:WaveScroll
		m_ex = new int[4];
		m_sy = new int[4];
		m_ey = new int[4];

		m_fWaveData = null;
		m_byGainArray = null;
		m_byDACurrent = null;
		m_byDAPulseKind = null;
		m_nPulseOutArray = null;
		m_fDataTime = null;
		m_nGainLevel = new int[2];
		m_nGainLevel[0] = m_nGainLevel[1] = 0;
		m_SelectFileName = "";

		m_winPaint.setStyle(Style.FILL);
		m_RawPaint.setColor(Color.BLACK);
		m_RawPaint.setStrokeWidth(2.0F);
		m_bkPaint.setStyle(Style.FILL);

		m_memPaint.setColor(Color.BLACK);
		m_frPaint.setColor(Color.BLACK);
		m_frPaint.setStrokeWidth(2.0F);
		m_winPaint.setColor(Color.WHITE);

		m_OverRangeText.setColor(Color.RED);
		m_OverRangeText.setStrokeWidth(4.0F);

		m_ErrText.setColor(Color.RED);
		m_ErrText.setStrokeWidth(2.0F);
		m_OKText.setColor(Color.argb(255, 74, 180, 44));
		m_OKText.setStrokeWidth(2.0F);
		m_Text.setColor(Color.BLACK);
		m_bkPaint.setColor(Color.WHITE);
		m_bkPaint.setStrokeWidth(2.0F);
		m_Bold.setColor(Color.argb(255, 0, 128, 0));
		m_Bold.setTextAlign(Paint.Align.LEFT);

		m_CalcPaint = new Paint[9];
		m_bGraphCheck = new boolean[9];
		for(i = 0; i < m_nNoOfCalc; i++)
		{
			m_CalcPaint[i] = new Paint();
			m_CalcPaint[i].setStrokeWidth(4.0F);
		}
		m_CalcPaint[0].setStrokeWidth(6.0F);

		m_CalcPaint[0].setColor(Color.BLUE);
		m_CalcPaint[1].setColor(Color.GREEN);
		m_CalcPaint[2].setColor(Color.RED);
		m_CalcPaint[3].setColor(Color.CYAN);
		m_CalcPaint[4].setColor(Color.MAGENTA);
		m_CalcPaint[5].setColor(Color.DKGRAY);
		m_CalcPaint[6].setColor(Color.rgb(64, 128,64));

		m_WavePaint = new Paint[m_nNoOfWave];
		for(i = 0; i < m_nNoOfWave; i++)
		{
			m_WavePaint[i] = new Paint();
			m_WavePaint[i].setStrokeWidth(4.0F);
		}
		m_WavePaint[0].setColor(Color.RED);
		m_WavePaint[1].setColor(Color.BLUE);
		m_WavePaint[2].setColor(Color.GREEN);
		m_WavePaint[3].setColor(Color.CYAN);
		m_WavePaint[4].setColor(Color.MAGENTA);
//		m_WavePaint[3].setColor(Color.CYAN);

		m_Filter = new IIRFilterObj[3];	//0筋弛緩モニターハイパス　　1:心拍　　2:呼吸　　3:Ch2 筋弛緩   4:点滴ローパス
		for(i = 0; i < 3; i++)
		{
			m_Filter[i] = new IIRFilterObj();
			m_Filter[i].InitObj();
		}
		m_HartObj = new HartObj();
		m_HartObj.InitObj();
		m_bHartFlg = false;
		m_fPaketBuf = new float[m_nPacketSize];
		m_fPaketBuf2 = new float[m_nPacketSize];
		m_fPaketBuf3 = new float[m_nPacketSize];
		m_fPaketBuf4 = new float[m_nPacketSize];
		m_nAWork = new int[4][512];
		m_nACount = new int[4];

		m_fCheckPulse = new float[4][4];
		m_nNoOfData = new int[3];
	}


	public void InitObj(MainActivity ma)
	{
		m_ma = ma;
		m_bDrawFlg = false;
		SetMaxMin();
		m_nReadPos = m_nReadSize = 0;
		int	dummy[];
		dummy = null;
		int		nNoOfRawDataPerDay;	//一日の測定に必要なサンプリングデータ数
		int		nNoOfCalcDataPerDay;	//一日の測定に必要な　結果データ数
		m_fDays = 1.0F;
		int		nMemCount = 0;
		nNoOfRawDataPerDay = 200 * 60 * 60 * 24;
		nNoOfCalcDataPerDay = 60 * 60 * 24;
		m_fCalcMax = 1000.0F;
		m_fCalcMin = 0.0F;
		m_fPulseMin = 0.0F;
		m_fPulseMax = 20.0F; //120から変更
		dummy = new int[25000000];	//全てのメモリを使い切らないように100Mbyte 余分に確保してから解放している

/*
		m_fNoOfUnblockedReceptors = 0.0F;	//筋弛緩剤投与下でもブロックされていない受容体数(万)
		m_fNoOfBlocksDueStim = 0.0F;		//神経刺激によるACh受容体ブロック数(万)
		m_fNoOfUnblockedACh = 0.0F;	//神経刺激後の非ブロックACh受容体数(万)
		m_fAChRecoveryRate = 0.0F;	//神経刺激によるACh受容体ブロック回復率
		m_fAChRecovery = 0.0F;		//ACh受容体ブロック回復量
		m_fPTP_Stim = 0.0F;	//神経刺激によるACh増加量(PTP)万
		m_fNoOfUnblockedReceptors2 = 0.0F;	//増加量を加えた非ブロックACh受容体数(万)
*/
		while(true)	//[詳細設定]-[その他の設定] [最大測定日数] m_fDays　で指定された量のデータを確保する
		{
			m_nNoOfAllocRawData = (int)(nNoOfRawDataPerDay * m_fDays);
			m_nNoOfAllocCalcData = (int)(nNoOfCalcDataPerDay * m_fDays);
			m_fResult = null;
			m_fWaveData = null;
			m_byGainArray = null;
			m_byDACurrent = null;
			m_byDAPulseKind = null;
            m_nPulseOutArray = null;

			m_fTime = null;
			m_nPeakCount = null;
			m_nChPeakPos = null;
			m_fDataTime = null;
			try{
				m_byGainArray = new byte[2][m_nNoOfAllocRawData];
				m_byDACurrent = new byte[m_nNoOfAllocRawData];
				m_byDAPulseKind = new byte[m_nNoOfAllocRawData];
                m_nPulseOutArray = new int[m_nNoOfAllocRawData];
                m_fDataTime = new float[m_nNoOfAllocRawData];
				m_fTime = new float[3][m_nNoOfAllocCalcData];
				m_nChPeakPos = new int[2][m_nNoOfAllocCalcData];
				m_nPeakCount = new int[4][m_nNoOfAllocCalcData * 4];
				m_fResult = new float[2][m_nNoOfCalc + 1][m_nNoOfAllocCalcData];
				m_fWaveData = new float[5][m_nNoOfAllocRawData];
			}
			catch (OutOfMemoryError e) {      //正常に確保できなかった場合は8割りにして再試行
				m_fDays *= 0.8F;
			}
			nMemCount++;
			if(m_fResult != null && m_fWaveData != null)
				break;
		}
		dummy = null;
	}
	/*
	 * 　測定前、データ解析前の変数の初期化
	 */
	public void InitMeas(boolean bInitIntegralFlg, boolean bReCalcFlg)
	{	//bInitIntegralFlg   true 積分を初期化　false 初期化しない
		SetMaxMin();
        long	lCurrentTime = System.currentTimeMillis();
		m_nPulseOutNo = 0;
		m_bPulseOutFlg = false;
		m_fCorrectionValue = 0.0F;
        if(!bReCalcFlg)
        {
    		m_lMeasStartTime = lCurrentTime;
    		m_nCorrectionCount = 0;
    		m_nNoOfMarker = 0;
            m_bLastDspFlg = true;
			m_bCorrectOutFlg = false;
		}
		m_nLastPos = 0;
        m_fDspRawData = 60.0F; //10から変更
        m_fDspRawStart = 0.0F;
        m_fDspRawEnd = 60.0F; //10から変更

		m_lSignalTime = m_lMeasStartTime;
		m_nPrevDspTime = m_lMeasStartTime;
		m_nPrevDataTime = m_lMeasStartTime;
		m_nOldCount = m_nCount = 0;
		m_nNoOfData[0] = m_nNoOfData[1] = m_nNoOfData[2] = 0;
		m_nLastCalcPos[0] = m_nLastCalcPos[1] = m_nLastCalcPos[2] = m_nLastCalcPos[3] = 0;
		m_nNoOfPeak[0] = m_nNoOfPeak[1] = m_nNoOfPeak[2] = m_nNoOfPeak[3] = 0;
    	m_nLastPeak[0] = m_nLastPeak[1] = 0;
		int		i, j;
		for(j = 0; j < 4; j++)
		{
			for(i = 0; i < 4; i++)
				m_fCheckPulse[j][i] = 0.0F;
			m_nNoOfCheck[j] = 0;
			m_fNormalBPM[j] = 0.0F;
		}
		for(j = 0; j < 3; j++)
		{
			for(i = 0; i < m_nNoOfCalc + 1; i++)
				m_fResultVal[j][i] = 0.0F;
		}

		if(bInitIntegralFlg)
			SetFilterFreq();
	}

	public void SetFilterFreq()
	{
		m_Filter[0].SetParamSub(m_fHighPassFreq, (float)m_nSamplingRate, (float)m_nSamplingRate,  0.1F, 30.0F);
//		m_Filter[3].SetParamSub(m_fHighPassFreq, (float)m_nSamplingRate, (float)m_nSamplingRate,  0.1F, 30.0F);
		m_Filter[1].SetParamSub(m_fBandPassFreq2, m_fBandPassFreq1, (float)m_nSamplingRate,  0.1F, 30.0F);
		m_Filter[2].SetParamSub(m_fLowBandPassFreq2, m_fLowBandPassFreq1, (float)m_nSamplingRate, 0.1F, 30.0F);
	}

	//縦軸スケールのデフォルト値をセットする
	void SetMaxMin()
	{
		m_fWaveMax = 4000.0F;
		m_fWaveMin = 0.0F; //-4000から変更
	}

	//脈波表示範囲が正常かチェック
	public void CheckWaveDspRange()
	{
		float	dummy;
		int		nNoOfRangeData;

		if(m_fDspRawEnd < m_fDspRawStart)
		{
			dummy = m_fDspRawStart;
			m_fDspRawStart = m_fDspRawEnd;
			m_fDspRawEnd = dummy;
		}

		m_fDspRawData = m_fDspRawEnd - m_fDspRawStart;
		if(300.0F < m_fDspRawData)
			m_fDspRawData = 300.0F;
		if(m_fDspRawData < 1.0F)
			m_fDspRawData = 1.0F;


		m_fDspRawEnd = m_fDspRawStart + m_fDspRawData;
		if(m_nLastPos < 2)
			return;
		float	fLastTime = m_fDataTime[m_nLastPos - 1];
		if(fLastTime < m_fDspRawEnd)
		{
			m_fDspRawEnd = fLastTime;
			m_fDspRawStart = m_fDspRawEnd - m_fDspRawData;
		}
		if(m_fDspRawStart < 0.0F || m_fDspRawEnd < 0.0F)
		{
			m_fDspRawStart = 0.0F;
			m_fDspRawEnd = m_fDspRawData;
		}
	}
	//測定開始からの時間に相当する　生データ位置を得る　　
	//シグナルが無い場合データカットされているので
	//サンプリング周波数から求めることができない
	int TimeToWavePos(float fTime)
	{
		if (m_nLastPos < 1)
			return 0;
		int		i;
		boolean	bFlg;
		int		nStartNo = 0;
		int		nStep = m_nLastPos / 10;
		float	fDataTime;
		while (true)
		{
			bFlg = false;
			if (nStep < 1)
				nStep = 1;
			if (nStartNo < 0)
				nStartNo = 0;
			for (i = nStartNo; i < m_nLastPos; i += nStep)
			{
				fDataTime = m_fDataTime[i];
				if (fTime < fDataTime)
				{
					if (nStep == 1)
						return i;
					nStartNo = i - nStep;
					nStep /= 10;
					bFlg = true;
					break;
				}
			}
			if (!bFlg)
			{
				if (nStep == 1)
					return m_nLastPos - 1;
				nStartNo = m_nLastPos - 1 - nStep;
				nStep /= 10;
			}
		}
	}

	public void DrawObj(Canvas canvas)
	{
		canvas.drawRect(0, 0, m_nWidth, m_nHeight, m_winPaint);
		CheckWaveDspRange();
		boolean	bScrollFlg = !m_ma.m_bADStartFlg;
		m_Text_ey = m_nCharSize2 * 2 + m_nCharSize * 2;
		DrawValu(canvas);
		DrawCalcData(canvas);
		DrawWave(canvas, m_fDspRawStart, m_fDspRawEnd, bScrollFlg, false, 0, m_fTOF1AmpMag);
		if(m_bGraphCheck[0] && 0 < m_ma.m_nComKind)
			DrawWave(canvas, m_fDspRawStart, m_fDspRawEnd, false, true, 1, m_fHeartAmpMag);
		if(m_bGraphCheck[1] && 0 < m_ma.m_nComKind)
			DrawWave(canvas, m_fDspRawStart, m_fDspRawEnd, false, true, 2, m_fRespiratoryAmpMag);
		if(m_bGraphCheck[7])
			DrawWave(canvas, m_fDspRawStart, m_fDspRawEnd, false, true, 3, 1.0F);	//Raw Ch1
		if(m_bGraphCheck[8] && 0 < m_ma.m_nComKind)
			DrawWave(canvas, m_fDspRawStart, m_fDspRawEnd, false, true, 4, 1.0F);	//Raw Ch2
		DrawMarker(canvas);
	}

	// コメントマーカーを表示する
	public void DrawMarker(Canvas canvas)
	{
		int		i, nCount, nKind;
		int		x, y;
		float	fTime;

		for(nKind = 0; nKind < 2; nKind++)
		{
			nCount = 1;
			for(i = 0; i < m_nNoOfMarker; i++)
			{
				fTime = m_Marker[i].fTime;
				x = (int) (m_ax[nKind] * fTime + m_bx[nKind] + 0.5);
				if(m_sx[nKind] < x && x < m_ex[nKind])
				{
					canvas.drawLine(x, m_sy[nKind], x, m_ey[nKind], m_MarkerPaint);
					y = nCount * m_nCharSize + m_sy[nKind];
					if(m_ey[nKind] < (m_nCharSize + y))
					{
						y = m_sy[nKind];
						nCount = 0;
					}
					y += m_nCharSize;
					canvas.drawText(m_Marker[i].Comment, x + m_nCharSize / 2, y, m_MarkerPaint);
					nCount++;
				}
			}
		}
	}

	/*---
	 * [連続解析]　で測定された結果をグラフ表示
		0:脈拍数　1:b/a  2:c/a  3:d/a  4:e/a  5 d/b 6:A.I.  7:血管年齢 8:PTT  9:PTT S/N-R 10: AoPWV
		11:Envelop   12:時間差 13:時間差S/N-R  14:PWV 15:LF   16:HF  17:LF/HF  18:Envelop SNR
		19 呼吸数　20 呼吸数エンベロープ　21 体動     22:SV  23 CO
	-----*/
	public void DrawCalcData(Canvas canvas)
	{//m_nCalcDataRange
		int		i;
		if(!m_ma.m_bADStartFlg)
			m_bLastDspFlg = false;
		m_sx[1] = m_nCharSize * 4;
		m_ex[1] = m_nWidth - m_nCharSize * 4;

		int	nMidY;
		nMidY = (m_nHeight - m_Text_ey) / 2 + m_Text_ey;
		int		nScrollSY, nScrollEY;
		nScrollSY = nMidY - (int)(m_nCharSize * 2.0);
		nScrollEY = nMidY;
		m_sy[1] = m_Text_ey;
		m_ey[1] = nScrollSY - m_nCharSize;
//		m_ey[1] = nMidY - (int)(m_nCharSize * 3.5);
		canvas.drawRect(0, m_Text_ey, m_nWidth, nMidY, m_winPaint);
		canvas.drawRect(m_sx[1], m_sy[1], m_ex[1], m_ey[1], m_bkPaint);
		canvas.drawLine(m_sx[1], m_sy[1], m_ex[1], m_sy[1], m_frPaint);
		canvas.drawLine(m_ex[1], m_ey[1], m_sx[1], m_ey[1], m_frPaint);
		canvas.drawLine(m_ex[1], m_sy[1], m_ex[1], m_ey[1], m_frPaint);
		canvas.drawLine(m_sx[1], m_ey[1], m_sx[1], m_sy[1], m_frPaint);

		float	Time;
    	float	fSta, fEnd;
		if(m_bLastDspFlg && 0 < m_nLastPos)
		{
			fEnd = m_fDataTime[m_nLastPos - 1];
			fSta = fEnd - (float)m_nCalcDataRange;
			if(fSta < 0.0F)
				fSta = 0.0F;
		}
		else
			fSta = (float)m_nDspStartTime;

		fEnd = fSta + (float)m_nCalcDataRange;

    	m_ax[1] = (double)(m_ex[1] - m_sx[1]) / (double)(fEnd - fSta);
    	m_bx[1] = (double)m_sx[1] - (double)fSta * m_ax[1];

		/*
				nStartPos = TimeToPos(m_lDspStartTime, true);
				lTime = m_fTime[nStartPos] + (m_nCalcDataRange * 60 * 1000);
				nEndPos = TimeToPos(lTime, false);
				*/
		double		nMax, nMin;

    	nMax = (double)m_fCalcMax;
    	nMin = (double)m_fCalcMin;
    	if(nMax == nMin)
    		nMax++;
    	m_ay[1] = (double)(m_sy[1] - m_ey[1]) / (nMax - nMin);
    	m_by[1] = (double)m_ey[1] - nMin * m_ay[1];

		if(m_bAbsoluteTimeFlg)
	    	DrawXMemory2(canvas, m_memPaint, m_frPaint, fSta, fEnd, 1);
		else
			DrawXMemory(canvas, m_memPaint, m_frPaint, fSta, fEnd, 1);

//		DrawYMemory(1, canvas, m_memPaint, m_frPaint, nMax, nMin);
    	nMax = (double)m_fPulseMax;
    	nMin = (double)m_fPulseMin;
    	if(nMax == nMin)
    		nMax++;
    	m_ay[2] = (double)(m_sy[1] - m_ey[1]) / (nMax - nMin);
    	m_by[2] = (double)m_ey[1] - nMin * m_ay[2];
		DrawYMemory(2, canvas, m_memPaint, m_frPaint, nMax, nMin);
   		DrawCalcGraph(0, canvas, fSta, fEnd, nMin, nMax);
   		DrawCalcGraph(1, canvas, fSta, fEnd, nMin, nMax);
		DrawCalcGraph(2, canvas, fSta, fEnd, nMin, nMax);
   		DrawCalcGraph(3, canvas, fSta, fEnd, nMin, nMax);
   		DrawCalcGraph(4, canvas, fSta, fEnd, nMin, nMax);
		DrawCalcGraph(5, canvas, fSta, fEnd, nMin, nMax);
		DrawCalcGraph(6, canvas, fSta, fEnd, nMin, nMax);
    	if(0 < m_nLastPos)
    	{
    		int		x;
    		x = (int)(m_ax[1] * m_fDspRawStart + m_bx[1]);
    		x = (x < m_sx[1])?  m_sx[1] : (m_ex[1] < x)? m_ex[1] : x;
   			canvas.drawLine(x, m_sy[1], x, m_ey[1], m_RangePaint);
    		x = (int)(m_ax[1] * m_fDspRawEnd +  m_bx[1]);
    		x = (x < m_sx[1])?  m_sx[1] : (m_ex[1] < x)? m_ex[1] : x;
   			canvas.drawLine(x, m_sy[1], x, m_ey[1], m_RangePaint);
    	}
    	DrawScrollBar(canvas, m_sx[1], nScrollSY, m_ex[1], nScrollEY, fSta, fEnd, 1);
	}


/*---
 * 結果グラフを描画
float	m_fResult[][];
* 0:脈拍  1:呼吸数　2:T1アンプリチュード  3:T1アンプリチュード比率  4:T4/T1   5:TOF Count   6:PTC
----*/
	public void DrawCalcGraph(int nKind, Canvas canvas, float fStartSec,  float fEndSec, double dMin, double dMax)
	{
		if(!m_bGraphCheck[nKind])
			return;
		int		ox, oy, x, y, i;
		int		nKind2;
		float	fTime;
//		if(nKind == 2)	//TOF グラフのみスケール違う
//			nKind2 = 1;
//		else
			nKind2 = 2;
		ox = oy = -1;

		int		no = 2;	//時間　刺激
		if(nKind == 0)
			no = 0;//時間　脈拍
		else if(nKind == 1)
			no = 1;//時間　呼吸

		int		nEndPos, nStaPos;
		nStaPos = TimeToPos(no, fStartSec, false);
		nEndPos = TimeToPos(no, fEndSec, true);
// 比率を変更 adtex
	    int		nSub;
	    nSub = nEndPos - nStaPos + 1;
	    double	fConst = 0.1; //1.0から変更
		if(nKind == 5 || nKind == 6)
			fConst = 1.0;	//TOF  PTC 10.0から変更
		else if(nKind == 2)
			fConst = 0.01;	//ピーク高さ絶対値
	    if(m_nNoOfData[no] < 1)
	    	return;
	    if(nSub < 4000) {
			for (i = nStaPos; i <= nEndPos; i++) {
				fTime = m_fTime[no][i];
				x = (int) (m_ax[1] * (double) fTime + m_bx[1]);
				x = (x < m_sx[1]) ? m_sx[1] : (m_ex[1] < x) ? m_ex[1] : x;
				y = (int) (m_ay[nKind2] * (double) m_fResult[m_nCalcGraphAveFlg][nKind][i] * fConst + m_by[nKind2]);
				y = (y < m_sy[1]) ? m_sy[1] : (m_ey[1] < y) ? m_ey[1] : y;
				if ( ox != -1 && oy != -1 && no < 2 )
					canvas.drawLine(ox, oy, x, y, m_CalcPaint[nKind]);
//				if ( 1 < no && m_sx[1] < x && x < m_ex[1] && m_sy[1] < y && y < m_ey[1] )
				if ( 1 < no && m_sx[1] < x && x < m_ex[1])
					DrawRect(canvas, x, y, m_CalcPaint[nKind]);
				ox = x;
				oy = y;
			}
		}
	    else {
			int oldmaxy, oldminy, maxy, miny;
			oldmaxy = oldminy = maxy = miny = 0;
			x = 0;
			for (i = nStaPos; i <= nEndPos; i++) {
				fTime = m_fTime[no][i];
				x = (int) (m_ax[1] * (double) fTime + m_bx[1]);
				if ( x < m_sx[1] )
					continue;
				if ( m_ex[i] < x )
					break;
				y = (int) (m_ay[nKind2] * (double) m_fResult[m_nCalcGraphAveFlg][nKind][i] * fConst + m_by[nKind2]);
				y = (y < m_sy[1]) ? m_sy[1] : (m_ey[1] < y) ? m_ey[1] : y;

				if ( i == nStaPos ) {
					oldmaxy = oldminy = maxy = miny = y;
					ox = x;
				}
				maxy = (maxy < y) ? y : maxy;
				miny = (y < miny) ? y : miny;
				if ( x != ox ) {
					if ( m_sx[1] <= ox ) {
						if ( oldmaxy < miny )
							canvas.drawLine(ox, oldmaxy, x, maxy, m_CalcPaint[nKind]);
						else if ( maxy < oldminy )
							canvas.drawLine(ox, oldminy, x, miny, m_CalcPaint[nKind]);
						else {
							canvas.drawLine(ox, maxy, ox, miny, m_CalcPaint[nKind]);
							canvas.drawLine(ox, maxy, x, y, m_CalcPaint[nKind]);
						}
					}
					oldmaxy = maxy;
					oldminy = miny;
					maxy = miny = y;
					ox = x;
				}
			}
			canvas.drawLine(x, maxy, x, miny, m_CalcPaint[nKind]);
		}
	}

	private void DrawRect(Canvas canvas, int x, int y, Paint paint)
	{
		int		nWidth = 6;
		canvas.drawRect(x - nWidth, y - nWidth, x + nWidth, y + nWidth, paint);
	}

	//測定開始からの時間に相当する結果の位置を取得
	public int TimeToPos(int no, float fTime, boolean bAfterFlg)
	{
		if(m_nNoOfData[no] < 2)
			return 0;
		float	fSta, fEnd, fDummy;
		fSta = m_fTime[no][0];
		fEnd = m_fTime[no][m_nNoOfData[no] - 1];
		if(fSta == fEnd)
			return 0;
		int	nPos;
		nPos = (int)((double)(fTime - fSta) * (double)(m_nNoOfData[no] - 1) / (double)(fEnd - fSta) + 0.5);
		if(nPos < 0)
			nPos = 0;
		else if((m_nNoOfData[no] - 1) < nPos)
			nPos = m_nNoOfData[no] - 1;
		fDummy = m_fTime[no][nPos];
		int		i;
		if(fDummy < fTime)
		{
			for(i = nPos; i < m_nNoOfData[no]; i++)
			{
				if(fTime <= m_fTime[no][i])
				{
					if(!bAfterFlg && 0 < i)
						return i - 1;
					else
						return i;
				}
			}
			return m_nNoOfData[no] - 1;
		}
		else
		{
			for(i = nPos; 0 <= i; i--)
			{
				if(m_fTime[no][i] <= fTime)
				{
					if(bAfterFlg && i < (m_nNoOfData[no] - 1))
						return i + 1;
					else
						return i;
				}
			}
		}
		return 0;
	}
	//横軸目盛りを描画　　絶対時刻の場合
	private void DrawXMemory2(Canvas canvas, Paint memPaint, Paint frPaint, float fSta, float fEnd, int no)
	{
		long	lSta = m_lMeasStartTime + (long)(fSta * 1000.0);
		long	lEnd = m_lMeasStartTime + (long)(fEnd * 1000.0);
		double	dStep[] = new double[1];
		double	dStart[] = new double[1];
		double	fMax, fMin, fInitVal;
		long	lVal;
		fMin = (double)lSta / 1000.0;
		fMax = (double)lEnd / 1000.0;
		long	lDummy;
		lDummy = 3600000 * 24;
		lVal = (lSta / lDummy) * lDummy;
		fInitVal = (double)lVal / 1000.0;

		GetMemoryStepTime(fMax, fMin, dStep, dStart, false, fInitVal);
		if(dStep[0] == 0.0)
			return;
		double	d;
		int		x, oldx;
		String	str;
		frPaint.setTextAlign(Align.CENTER);
		oldx = 0;
		for(d = dStart[0]; d <= fMax; d += dStep[0])
		{
			lVal = (long)(d * 1000.0);
			str = GetTimeString(lVal, lSta, lEnd);
			lVal -= m_lMeasStartTime;
			x = (int)(m_ax[no] * (double)lVal / 1000.0 + m_bx[no]);
			if(m_sx[no] <= x && x <= m_ex[no])
			{
				canvas.drawLine(x,  m_sy[no], x,  m_ey[no], memPaint);
				if(oldx < x)
				{
					canvas.drawText(str, x, m_ey[no] + m_nCharSize, frPaint);
					oldx = x + m_nCharSize * 5;
				}
			}
		}

	}


	//脈波を表示する
	public void DrawWave(Canvas canvas, float fWaveStaTime, float fWaveEndTime, boolean bScrollFlg, boolean bOverWriteFlg, int no, float fAmpMag)
	{
		String	str;
		float		fSize;
		int			nSize;
		int		nWaveEndPos, nWaveStaPos;
		nWaveStaPos = TimeToWavePos(fWaveStaTime);
		nWaveEndPos = TimeToWavePos(fWaveEndTime);
		nSize = nWaveEndPos - nWaveStaPos + 1;
		fSize = fWaveEndTime - fWaveStaTime;
		int		nScrollSY, nScrollEY;
		float	 fSta, fEnd;
		fSta = fEnd = 0.0F;
		nScrollSY = nScrollEY = 0;
		if(!bOverWriteFlg)
		{
			m_sx[0] = m_nCharSize * 4;
			m_ex[0] = m_nWidth - m_nCharSize * 2;
			int	nMidY;
			nMidY = (m_nHeight - m_Text_ey) / 2 + m_Text_ey;	//m_Text_ey 値表示の高さ
			m_sy[0] = nMidY + (int)(m_nCharSize * 0.5);
			if(bScrollFlg)
				m_ey[0] = m_nHeight - (int)(m_nCharSize * 4.0);
			else
				m_ey[0] = m_nHeight - (int)(m_nCharSize * 2.0);
			canvas.drawRect(m_sx[0], m_sy[0], m_ex[0], m_ey[0], m_bkPaint);
			canvas.drawLine(m_sx[0], m_sy[0], m_ex[0], m_sy[0], m_frPaint);
			canvas.drawLine(m_ex[0], m_ey[0], m_sx[0], m_ey[0], m_frPaint);
			canvas.drawLine(m_ex[0], m_sy[0], m_ex[0], m_ey[0], m_frPaint);
			canvas.drawLine(m_sx[0], m_ey[0], m_sx[0], m_sy[0], m_frPaint);
		}
		if(fSize < 0.1F)
		{
			fWaveStaTime = 0.0F;
			fWaveEndTime = 10.0F;
		}
		double		nMax, nMin;
    	nMax = (double)m_fWaveMax;
    	nMin = (double)m_fWaveMin;
    	if(nMax == nMin)
    		nMax++;
    	if(!bOverWriteFlg)
    	{
    		m_ax[0] = (double)(m_ex[0] - m_sx[0]) / (double)(fWaveEndTime - fWaveStaTime);
    		m_bx[0] = (double)m_sx[0] - (double)fWaveStaTime * m_ax[0];
    		m_ay[0] = (double)(m_sy[0] - m_ey[0]) / (nMax - nMin);
    		m_by[0] = (double)m_ey[0] - nMin * m_ay[0];
    		if(m_bAbsoluteTimeFlg)
    		{
    			long	lStartTime, lEndTime;
    			lStartTime = m_lMeasStartTime + (long)(fWaveStaTime * 1000.0F);
    			lEndTime = m_lMeasStartTime + (long)(fWaveEndTime * 1000.0F);
    			DrawXMemory2(canvas, m_memPaint, m_frPaint, lStartTime, lEndTime, 0);
    		}
    		else
    			DrawXMemory(canvas, m_memPaint, m_frPaint, fWaveStaTime, fWaveEndTime, 0);
    		DrawYMemory(0, canvas, m_memPaint, m_frPaint, nMax, nMin);
   			String	tag;
			m_frPaint.setTextAlign(Paint.Align.LEFT);
			int		yLine, yText;
			yLine = m_sy[0] + m_nCharSize2 * 1 / 2;
			yText = m_sy[0] + m_nCharSize2 * 1;

			tag = m_ma.getString(R.string.MuscleWave);
			canvas.drawLine(m_ex[0] - (int)(m_nCharSize2 * 8.0), yLine,
   							m_ex[0] - (int)(m_nCharSize2 * 5.5), yLine, m_WavePaint[0]);

			canvas.drawText(tag, m_ex[0] - (int)(m_nCharSize2 * 5.0), yText, m_frPaint);
			if(m_bGraphCheck[0] && 0 < m_ma.m_nComKind)
			{
				yLine += m_nCharSize2;
				yText += m_nCharSize2;
				tag = m_ma.getString(R.string.HeartWave);
				canvas.drawLine(m_ex[0] - (int)(m_nCharSize2 * 8.0), yLine,
						m_ex[0] - (int)(m_nCharSize2 * 5.5), yLine, m_WavePaint[1]);
				canvas.drawText(tag, m_ex[0] - (int)(m_nCharSize2 * 5.0), yText, m_frPaint);
			}
			if(m_bGraphCheck[1] && 0 < m_ma.m_nComKind)
			{
				tag = m_ma.getString(R.string.BreathingWave);
				yLine += m_nCharSize2;
				yText += m_nCharSize2;
				canvas.drawLine(m_ex[0] - (int)(m_nCharSize2 * 8.0), yLine,
						m_ex[0] - (int)(m_nCharSize2 * 5.5), yLine, m_WavePaint[2]);
				canvas.drawText(tag, m_ex[0] - (int)(m_nCharSize2 * 5.0), yText, m_frPaint);
			}
			if(m_bGraphCheck[7])
			{
				tag = "Ch1 Raw Wave";
				yLine += m_nCharSize2;
				yText += m_nCharSize2;
				canvas.drawLine(m_ex[0] - (int)(m_nCharSize2 * 8.0), yLine,
						m_ex[0] - (int)(m_nCharSize2 * 5.5), yLine, m_WavePaint[3]);
				canvas.drawText(tag, m_ex[0] - (int)(m_nCharSize2 * 5.0), yText, m_frPaint);
			}
			if(m_bGraphCheck[8] && 0 < m_ma.m_nComKind)
			{
				tag = "Ch2 Raw Wave";
				yLine += m_nCharSize2;
				yText += m_nCharSize2;
				canvas.drawLine(m_ex[0] - (int)(m_nCharSize2 * 8.0), yLine,
						m_ex[0] - (int)(m_nCharSize2 * 5.5), yLine, m_WavePaint[4]);
				canvas.drawText(tag, m_ex[0] - (int)(m_nCharSize2 * 5.0), yText, m_frPaint);
			}
    	}
		if(1 < nSize)
		{
			Paint	paint;
			paint = m_WavePaint[no];
			if(m_nLastPos <= nWaveEndPos)
				nWaveEndPos = m_nLastPos - 1;
			if(m_nLastPos <= nWaveStaPos)
				nWaveStaPos = m_nLastPos - 1;
			if(nWaveStaPos < 0)
				nWaveStaPos = 0;
			if(nWaveEndPos < 0)
				nWaveEndPos = 0;
			DrawGraphSub(canvas, paint, nWaveStaPos, nWaveEndPos, no, fAmpMag);
		}
		if(bOverWriteFlg)
			return;
		if(bScrollFlg)
		{
			nScrollSY = m_nHeight - (int)(m_nCharSize * 2.0);
			nScrollEY = m_nHeight;
			fSta = fWaveStaTime;
			fEnd = fWaveEndTime;
	    	DrawScrollBar(canvas, m_sx[0], nScrollSY, m_ex[0], nScrollEY, fSta, fEnd, 0);
		}
		else
			m_sx[3] = m_sy[3] = m_ex[3] = m_ey[3] = -1;
		m_nHartWidth = 0;
//		DrawHart(canvas);
		DrawTitle(canvas);
		String		msg = "";
		if(m_bOverRangeErrFlg[0] == 1)
		{
			if(m_ma.m_nComKind < 1)
				msg = "Over range.";
			else
				msg = "Over range. Ch1";
		}
		else if(m_bOverRangeErrFlg[1] == 1)
			msg = "Over range. Ch2";
		if(0 < m_nMeasMode && (m_bOverRangeErrFlg[0] == 1 || m_bOverRangeErrFlg[1] == 1))
		{
			int		nCharSize;
			if(m_nWidth < m_nHeight)
				nCharSize = (m_ey[1] - m_sy[1]) / 10;
			else
				nCharSize = (m_ey[1] - m_sy[1]) / 3;
			m_OverRangeText.setTextSize(nCharSize);
			m_OverRangeText.setTextAlign(Align.CENTER);
			canvas.drawText(msg, (m_sx[0] + m_ex[0]) / 2.0F, (m_sy[0] + m_ey[0]) / 2 + nCharSize / 2, m_OverRangeText);
		}
    }


	//画面上部の結果の数値の表示
	public void DrawValu(Canvas canvas)
	{
		canvas.drawRect(0, 0, m_nWidth, m_Text_ey, m_winPaint);
		String	str;
		int		x1, x2;
		int		y1, y2, y3, y4;
		int		half = m_nCharSize2 / 2;
		str = "TOF";
		x1 = 0;
		x2 = x1 + m_nCharSize2 * 2;
		y1 = m_nCharSize2;
		y2 = y1 + m_nCharSize2;
		y3 = y2 + half;
		y4 = y3 + m_nCharSize2;
		canvas.drawText(str, x1, y1, m_Text);
		str = String.format("%.0f",  m_fResultVal[m_nCalcGraphAveFlg][5]);
		canvas.drawText(str, x1, y2 , m_Text);
		canvas.drawLine(x1,  y3,  x2,  y3, m_CalcPaint[5]);

		x1 = x1 + m_nCharSize2 * 2;
		x2 = x1 + m_nCharSize2 * 2;
		str = "T1μV";
		canvas.drawText(str, x1, y1, m_Text);
		str = String.format("%.0f",  m_fResultVal[m_nCalcGraphAveFlg][2]);
		canvas.drawText(str, x1, y2 , m_Text);
		canvas.drawLine(x1,  y3,  x2,  y3, m_CalcPaint[2]);

		x1 = x1 + m_nCharSize2 * 3;
		x2 = x1 + m_nCharSize2 * 2;
		str = "T1%";
		canvas.drawText(str, x1, y1, m_Text);
		str = String.format("%3.0f",  m_fResultVal[m_nCalcGraphAveFlg][3]);
		canvas.drawText(str, x1, y2 , m_Text);
		canvas.drawLine(x1,  y3,  x2,  y3, m_CalcPaint[3]);

		x1 = x1 + m_nCharSize2 * 2;
		x2 = x1 + m_nCharSize2 * 2;
		str = "T4/T1";
		canvas.drawText(str, x1, y1, m_Text);
		str = String.format("%3.0f",  m_fResultVal[m_nCalcGraphAveFlg][4]);
		canvas.drawText(str, x1, y2 , m_Text);
		canvas.drawLine(x1,  y3,  x2,  y3, m_CalcPaint[4]);

		x1 = x1 + m_nCharSize2 * 3;
		x2 = x1 + m_nCharSize2 * 2;
		str = "PTC";
		canvas.drawText(str, x1, y1, m_Text);
		str = String.format("%.0f",  m_fResultVal[m_nCalcGraphAveFlg][6]);
		canvas.drawText(str, x1, y2 , m_Text);
		canvas.drawLine(x1,  y3,  x2,  y3, m_CalcPaint[6]);

		x1 = x1 + m_nCharSize2 * 2;
		x2 = x1 + m_nCharSize2 * 2;
		str = "HR";
		canvas.drawText(str, x1, y1, m_Text);
		str = String.format("%.0f",  m_fResultVal[m_nCalcGraphAveFlg][0]);
		canvas.drawText(str, x1, y2 , m_Text);
		canvas.drawLine(x1,  y3,  x2,  y3, m_CalcPaint[0]);

		x1 = x1 + m_nCharSize2 * 2;
		x2 = x1 + m_nCharSize2 * 2;
		str = "RR";
		canvas.drawText(str, x1, y1, m_Text);
		str = String.format("%.0f",  m_fResultVal[m_nCalcGraphAveFlg][1]);
		canvas.drawText(str, x1, y2 , m_Text);
		canvas.drawLine(x1,  y3,  x2,  y3, m_CalcPaint[1]);

//		x1 = m_ma.m_nCurrentBtnPos;
//		x1 = x1 + m_nCharSize2 * 2;
//		str = m_ma.getString(R.string.Current_Value2);
//		canvas.drawText(str, x1, y1, m_Text);
//		str = String.format("%.0f mA",  m_fCurrentValue);
//		canvas.drawText(str, x1, y2 , m_Text);

		x1 = x1 + m_nCharSize2 * 2;
		str = m_ma.getString(R.string.ctrl_value);
		canvas.drawText(str, x1, y1, m_Text);
		if(1000 < m_fControlValue)
			str = String.format("%4.2f mV",  m_fControlValue / 1000.0F);
		else
			str = String.format("%.0f μV",  m_fControlValue);
		canvas.drawText(str, x1, y2 , m_Text);

        x1 = x1 + m_nCharSize2 * 5;
        str = m_ma.getString(R.string.battery_voltage);
        canvas.drawText(str, x1, y1, m_Text);
        str = String.format("%4.2f V",  m_fBatteryVolt);
        canvas.drawText(str, x1, y2 , m_Text);


		if(m_nCorrectionKind == 1) {

			x1 = x1 + m_nCharSize2 * 5;
			str = m_ma.getString(R.string.neuromuscular_depth);
			canvas.drawText(str, x1, y1, m_Text);
			str = String.format("%4.1f %%", m_fAChBlockRateX);
			canvas.drawText(str, x1, y2, m_Text);

			x1 = x1 + m_nCharSize2 * 6;
			str = "Step";
			canvas.drawText(str, x1, y1, m_Text);
			str = String.format("%1d", m_ma.nstep);
			canvas.drawText(str, x1, y2, m_Text);
		}

		     x1 = 630;
		     str = m_line;
		     canvas.drawText(str, x1, y4 , m_Text);
		     m_ma.m_UsbCom.sendString(str);


//        x1 = x1 + m_nCharSize2 * 4;
//        str = m_ma.getString(R.string.battery_level);
//        canvas.drawText(str, x1, y1, m_Text);
//        str = String.format("%d %%",  m_nBatteryLevel);
//        canvas.drawText(str, x1, y2 , m_Text);



		DrawValueMeasTime(canvas, y4);
	}

	//残り測定可能時間の表示
	public void DrawValueMeasTime(Canvas canvas, int y4)
	{
		String	str2, str3, str;

		int		nSub = (int)((m_nPrevDataTime - m_lMeasStartTime) / 1000L);
		str = SecToString(nSub);
		nSub = GetMaxMeasTime(nSub);
		str2 = SecToString(nSub);
		str3 = str + " / " + str2;
		canvas.drawText(str3, m_nCharSize * 5, y4 - 2, m_frPaint);
		str = "";
		if(m_ma.m_nFileReadFlg == 2)
		{
			str2 = "　　File name : ";
			str += str2;
			str2 = GetOnlyFileName(m_SelectFileName);
			str += str2;
			canvas.drawText(str, m_nCharSize * 18, y4 - 2, m_frPaint);
		}
	}

//パス名からファイル名を取り出す
	String	GetOnlyFileName(String source)
	{
		int	length = source.length();
		int i;
		for(i = length - 1; 0 <= i; i--)
		{
			char	c;
			c = source.charAt(i);
			if(c == '/')
				break;
		}
		String	ret = "";
		if(0 <= i && i < length - 1)
			ret = source.substring(i + 1);
		else
			ret = source;
		return ret;
	}

//最大測定時間の算出
	public int GetMaxMeasTime(int nSub)
	{	//nSub 測定開始からの秒数
		int		nRawSec, nCalcSec, nRet;
		if(m_nLastPos == 0)
			nRawSec = (int)(m_fDays * 60.0F * 60.0F * 24.0F);
		else if(m_nLastPos < 10000)
		{
			if(m_nSamplingRate == 0)
				m_nSamplingRate = 200;
			nRawSec = m_nNoOfAllocRawData / m_nSamplingRate;
		}
		else
			nRawSec = (int)((float)m_nNoOfAllocRawData * (float)nSub / (float)m_nLastPos);

		if(m_nNoOfData[0] < 100)
			nCalcSec = (int)(m_fDays * 60.0F * 60.0F * 24.0F);
		else
			nCalcSec = (int)((float)m_nNoOfAllocCalcData * (float)nSub / (float)m_nNoOfData[0]);

		nRet = (nRawSec < nCalcSec)? nRawSec : nCalcSec;
		nRet = (nRet / 60) * 60;
		return nRet;
	}
//秒数を　日、時間、分、秒の文字列にする
	public String SecToString(int nSub)
	{
		String	str;
		int		nDay, nHour, nMin, nSec, nDummy, nDummy2;
		nDummy = 60 * 60 * 24;
		nDay = nSub / nDummy;
		nDummy2 = nSub - nDay * nDummy;
		nDummy = 60 * 60;
		nHour = nDummy2 / nDummy;
		nDummy2 = nDummy2 - nHour * nDummy;
		nDummy = 60;
		nMin = nDummy2 / nDummy;
		nSec = nDummy2 - nMin * nDummy;
		str = String.format("%d:%02d:%02d:%02d", nDay, nHour, nMin, nSec);
		return str;
	}

	//脈波グラフ左上にゲインや、表示波形情報を表示
	private void DrawTitle(Canvas canvas)
	{
		String	msg;

		m_ErrText.setTextAlign(Paint.Align.LEFT);
		m_Text.setTextAlign(Paint.Align.LEFT);
		if(m_bAutoGainFlg)
		{
			if(0 < m_ma.m_nComKind)
				msg = String.format("Auto Gain...Ch1:%d   Ch2:%d",  m_nGainLevel[0],  m_nGainLevel[1]);
			else
				msg = String.format("Auto Gain...%d",  m_nGainLevel[0]);
			canvas.drawText(msg, m_sx[0] + 2, m_sy[0] + 5 + m_nCharSize2, m_Text);
		}
		else if(m_bOverRangeErrFlg[0] == 1)
		{
			if(0 < m_ma.m_nComKind)
				msg = String.format("Over range. Ch1 Gain level:%d", m_nGainLevel[0]);
			else
				msg = String.format("Over range. Gain level:%d", m_nGainLevel[0]);
			canvas.drawText(msg, m_sx[0] + 2, m_sy[0] + 5 + m_nCharSize2, m_ErrText);
		}
		else if(0 < m_ma.m_nComKind && m_bOverRangeErrFlg[1] == 1)
		{
			msg = String.format("Over range. Ch2 Gain level:%d", m_nGainLevel[1]);
			canvas.drawText(msg, m_sx[0] + 2, m_sy[0] + 5 + m_nCharSize2, m_ErrText);
		}
		else
		{
			int		offset = 14;
			if(0 < m_ma.m_nComKind)
			{
				msg = String.format("Gain Level Ch1:%d   Ch2:%d",  m_nGainLevel[0], m_nGainLevel[1]);
				offset = 28;
			}
			else
				msg = String.format("Gain Level:%d",  m_nGainLevel[0]);
			canvas.drawText(msg, m_sx[0] + 2, m_sy[0] + 5 + m_nCharSize2, m_Text);
		}
	}
	//測定中、画面右上に鼓動するハートを表示
/*
	private void DrawHart(Canvas canvas)
	{
		String	str;
		int	xorg, yorg, nWidth, nHeight;
		if(m_nHeight < m_nWidth)
			nHeight = nWidth = m_nWidth / 10;
		else
			nHeight = nWidth = m_nHeight / 10;
		m_nHartWidth = nWidth;
		yorg = 0;
		xorg = m_nWidth - nWidth;
		m_bPulseLessFlg = false;
		if(m_nMeasMode == 1)
		{
			if(!m_bSignalFlg2)	//シグナルがなければ０にする
				m_bPulseLessFlg = true;
		}
		if(m_bPulseLessFlg)
			str = "0";
		else
			str = String.format("%d", (int)(m_fResultVal[m_nCalcGraphAveFlg][0] + 0.5));
		if(m_bHartFlg)
		{
			xorg = xorg + (int)(nWidth * 0.1 + 0.5);
			nWidth = (int)(nWidth * 0.8 + 0.5);
			m_bHartFlg = false;
		}
		m_HartObj.DrawHart(canvas, xorg, yorg, nWidth, nHeight, str);
	}
-*/
	//結果グラフの下にスクロールバーを表示
	public void DrawScrollBar(Canvas canvas, int sx, int sy, int ex, int ey, float fSta, float fEnd, int nKind)
	{	//nKind  0:Wave  1:Calc
		int		nArray = 2;
		int		nBarNo = 0;
		if(nKind == 0)
		{
			nArray = 3;
			nBarNo = 1;
		}
		m_sx[nArray] = sx;
		m_sy[nArray] = sy;
		m_ex[nArray] = ex;
 		m_ey[nArray] = ey;
		if(m_nLastPos < 2)
			return;
		float	fWidth;
		fWidth = fEnd - fSta + 1;
		m_ScrollBar[nBarNo].SetSize(sx,  sy,  ex,  ey);
		m_ScrollBar[nBarNo].SetMaxMin(m_fDataTime[0], m_fDataTime[m_nLastPos - 1]);
/*----
		if(nKind == 0)
			m_ScrollBar[nBarNo].SetMaxMin((long)(m_fDataTime[0] * 1000.0F), (long)(m_fDataTime[m_nLastPos - 1] * 1000.0F));
		else if(nKind == 1)
			m_ScrollBar[nBarNo].SetMaxMin(m_fTime[0][0], m_fTime[0][m_nNoOfData[0] - 1]);
----------*/
		m_ScrollBar[nBarNo].SetWidth(fWidth);
		m_ScrollBar[nBarNo].SetStartDspPos(fSta);
		m_ScrollBar[nBarNo].DrawObj(canvas, false);
	}


	//脈波グラフを描画
	public void DrawGraphSub(Canvas canvas, Paint paint, int nStaPos, int nEndPos, int no, float fAmpMag)
	{	//no 0 筋弛緩波形  1:心拍波形  2:呼吸波形　3:RawCh1  4:RawCh2
		int		ox, oy, x, y, i, offset_no;
		int		nOffsetUnit;
		float	buf[];
		if(no < 3)
			buf = m_fWaveData[no + 2];
		else
			buf = m_fWaveData[no - 3];
		nOffsetUnit = -(m_ey[1] - m_sy[1]) / 6;
		offset_no = 1;//オフセットなし
		if(no == 1)//オフセット下
			offset_no = 0;
		else if(no == 2)//オフセット上
			offset_no = 2;
		float	fGainConst = 1.0F;
		int		nGainLevel;
		int		nGainCh;
    	ox = -1;
    	oy = -1;
	    if((nEndPos - nStaPos + 1) < 4000)
	    {
	    	for(i = nStaPos; i <= nEndPos; i++)
	    	{
	    		x = (int)(m_ax[0] * m_fDataTime[i] + m_bx[0]);
				if(m_ex[0] < x )
					break;
				if(no < 3) {
					if ( (no == 2 || no == 1) && m_ma.m_nComKind == 1 )
						nGainCh = 1;
					else
						nGainCh = 0;
					nGainLevel = (int) m_byGainArray[nGainCh][i];
					fGainConst = m_fGainConst[nGainLevel];
					y = (int)(m_ay[0] * buf[i] * fAmpMag * fGainConst + m_by[0]);
					y = y + nOffsetUnit * offset_no - nOffsetUnit;
				}
				else if(no == 3)
					y = (int)(m_ay[0] * buf[i] + m_by[0]);
				else// if(no == 4)
					y = (int)(m_ay[0] * (buf[i] - 2048) + m_by[0]);

	    		y = (y < m_sy[0])? m_sy[0] : (m_ey[0] < y)? m_ey[0] : y;
				if(m_sx[0] <= ox)
					canvas.drawLine(ox,  oy,  x,  y, paint);
	    		ox = x;
	    		oy = y;
	    	}
		}
	    else
	    {
	    	int		maxy, miny;
			int	oldmaxy, oldminy;
			x = y = ox = maxy = miny = oldmaxy = oldminy = 0;
			for(i = nStaPos; i <= nEndPos; i++)	//描画ﾎﾟｲﾝﾄが多い場合はMAX、MINで描く
			{
				x = (int)(m_ax[0] * m_fDataTime[i] + m_bx[0]);
				if(m_ex[0] < x )
					break;

                if(no < 3) {

                    if ( no == 1 || no == 2 && m_ma.m_nComKind == 1 )
                        nGainCh = 1;
                    else
                        nGainCh = 0;
                    nGainLevel = (int) m_byGainArray[nGainCh][i];
                    fGainConst = m_fGainConst[nGainLevel];
                    y = (int) (m_ay[0] * buf[i] * fAmpMag * fGainConst + m_by[0]);
                    y = y + nOffsetUnit * offset_no - nOffsetUnit;
                }
				else if(no == 3)
					y = (int)(m_ay[0] * buf[i] + m_by[0]);
                else
					y = (int)(m_ay[0] * (buf[i] - 2048) + m_by[0]);
				y = (y < m_sy[0])? m_sy[0] : (m_ey[0] < y)? m_ey[0] : y;
				if(i == nStaPos)
				{
					oldmaxy = oldminy = maxy = miny = y;
					ox = x;
				}
				maxy = (maxy < y)? y : maxy;
				miny = (y < miny)? y : miny;
				if(x != ox)
				{
					if(m_sx[0] <= ox)
					{
						if(oldmaxy < miny)
							canvas.drawLine(ox, oldmaxy, x, maxy, paint);
						else if(maxy < oldminy)
							canvas.drawLine(ox, oldminy, x, miny, paint);
						else
						{
							canvas.drawLine(ox, maxy, ox, miny, paint);
							canvas.drawLine(ox, maxy, x, y, paint);
						}
					}
					oldmaxy = maxy;
					oldminy = miny;
					maxy = miny = y;
					ox = x;
				}
			}
			canvas.drawLine(x, maxy, x, miny, paint);
	    }
	    
	}


	//脈波検出の最上位関数　別スレッドから　500　msec 毎に呼び出す
	public void CalcMain()
	{	// nChNo チャンネル数
		float	ptr[];
		int		bRet;
		float fData;
		int		i, k;
		float	fMax[] = new float[4];
		float	fMin[] = new float[4];//, sDummy;
		float	fSum[] = new float[4];
		float	fOffset[] = new float[4];
		int		ch;
		int		nUnit = m_nSamplingRate;
		int			no;		//m_fWaveData[no] 0:生Ch1　１：生Ch2   2:筋弛緩ch1  3:心拍  4:呼吸
		int		nLoop = 1;
		if(0 < m_ma.m_nComKind)
			nLoop = 3;

		for(ch = 0; ch < nLoop; ch++)	//ch   0:筋弛緩　　1:心拍波形　　2:呼吸
		{
			fMax[ch] = -1.0e10F;
			fMin[ch] = 1.0e10F;
			if(ch == 0)	//筋弛緩モニター
			{
//				nUnit = (int)(m_nSamplingRate * m_fTOFInterval);
				no = 2;
			}
			else if(ch == 2)	//呼吸
			{
				if(m_fRespiratoryRateLower <= 0.0F)
					m_fRespiratoryRateLower = 8.0F;
				nUnit = (int)(m_nSamplingRate * 60 / m_fRespiratoryRateLower);
				no = 4;
			}
			else	//if(ch == 1)　心拍
			{
				if(m_fHeartRateLower <= 0.0F)
					m_fHeartRateLower = 40.0F;
				nUnit = (int)(m_nSamplingRate * 60 / m_fHeartRateLower);
				no = 3;
			}
			ptr	= m_fWaveData[no];

			if(ch == 0 && m_nPulseMode != 0)
			{
				int		nSearchLength = 0;
				if(m_nPulseMode == 1 || m_nPulseMode == 2 || m_nPulseMode == 5 || m_nPulseMode == 6)   //Calibration   Twitch
					nSearchLength = m_nSamplingRate / 2;
				else if(m_nPulseMode == 3 || m_nPulseMode == 4 || m_nPulseMode == 13) //TOF
					nSearchLength = (int)((m_fTOFStimInterval * 3.0 + 0.5) * m_nSamplingRate);
                else if(m_nPulseMode == 11 || m_nPulseMode == 12 || m_nPulseMode == 14) //PTC
                    nSearchLength = (int)(((float)m_nPTCTwitch2Num - 0.5F) * (float)m_nSamplingRate);
				if(m_nPulseOutNo != 0 && (m_nPulseOutNo + nSearchLength) < m_nLastPos) {
					GetControlValue(ptr, nSearchLength);
					m_nPulseOutNo = 0;
;					m_fCorrectionValue = 0.0F;
					continue;
				}
			}

			if(ch == 0)
				continue;
			if((m_nLastPos - m_nLastCalcPos[ch]) < nUnit)
				continue;

			fOffset[ch] = 0;
			fSum[ch] = 0;
			k = 0;
			for(i = m_nLastCalcPos[ch]; i < m_nLastPos; i++)
			{
				fSum[ch] += ptr[i];
				k++;
			}
			if(0 < k)
				fOffset[ch] = fSum[ch] / k;
			fMax[ch] = fMin[ch] = ptr[m_nLastCalcPos[ch]];
			for(i = m_nLastCalcPos[ch] + 1; i < m_nLastPos; i++)
			{
				if(fMax[ch] < ptr[i])
					fMax[ch] = ptr[i];
				if(ptr[i] < fMin[ch])
					fMin[ch] = ptr[i];
			}
			fMax[ch] -= fOffset[ch];
			fMin[ch] -= fOffset[ch];

			float	fLevel = 0.0F;		//最小ノイズレベル
			if(fLevel < fMax[ch])
				bRet = CalcPeakPich(ptr, ch, fMax, fMin, fOffset);
			else
				bRet = 0;
			float	fLimitTime;
			float fSec = (float)(m_nLastPos - m_nLastCalcPos[ch]) / (float)m_nSamplingRate;
			fLimitTime = (nUnit * 1.5F) / m_nSamplingRate;	//ユニットの1.5倍
			if(fLimitTime < fSec)
				m_nLastCalcPos[ch] = m_nLastPos - m_nSamplingRate * (int)fLimitTime;
		}
	}

	public int SearchPulsePeak(float ptr[], int nNoOfPeak, int nStartNo, int nEndNo, int nInterval, int nResultPos[], float fResultValue[])
	{	//返り値　見つけたピークの数　ptr[] 配列　nNoOfPeak:見つけるピークの数、nStartNo, nEndNo 見つける範囲　　nInterval ピークの間隔（データ数)　　nResultPos ピーク位置　　fResultValu ピーク値
		int	nSPos, nEPos;
		int		i, j;
		float	fData, fMax = 0.0F;
		nResultPos[0] = nStartNo;
		fResultValue[0] = ptr[nStartNo];
		int		nOffset = nInterval / 2;
		for(i = 0; i < nNoOfPeak; i++)
		{
			nSPos = nStartNo + nInterval * i - nOffset;
			nEPos = nSPos + nInterval + nOffset;
			nSPos = (nSPos < 0)? 0 : (m_nLastPos <= nSPos)? m_nLastPos - 1 : nSPos;
			nEPos = (nEPos < 0)? 0 : (m_nLastPos <= nEPos)? m_nLastPos : nEPos;
			nResultPos[i] = nSPos;
			fResultValue[i] = ptr[nSPos];
			for(j = nSPos; j < nEPos; j++)
			{
				fData = ptr[j];
				if(fResultValue[i] < fData)
				{
					fResultValue[i] = fData;
					nResultPos[i] = j;
				}
				if(fMax < fData)
				    fMax = fData;
			}
		}
        AutoGainCh1(fMax);
        return nNoOfPeak;
	}

	public void GetControlValue(float ptr[], int nSearchLength)
	{
	    float       fValue, fRatio;
		int		nStartNo, nEndNo;
		nStartNo = m_nPulseOutNo;
		if(nStartNo < 0)
			nStartNo = 0;
		int		nNoOfPeak;
		int		nInterval;
		if(m_nPulseMode == 1 || m_nPulseMode == 2 || m_nPulseMode == 5 || m_nPulseMode == 6)
		{
			nNoOfPeak = 1;
			nInterval = m_nSamplingRate / 2;
		}
		else if(m_nPulseMode == 3 || m_nPulseMode == 4 || m_nPulseMode == 13)
		{
			nNoOfPeak = 4;
			nInterval = (int)(m_fTOFStimInterval * m_nSamplingRate);
		}
		else if(m_nPulseMode == 11 || m_nPulseMode == 12 || m_nPulseMode == 14)
        {
            nNoOfPeak = m_nPTCTwitch2Num;
            nInterval = (int)(1 * m_nSamplingRate);
        }
        else
            return;
		if(nStartNo < 0)
			nStartNo = 0;
		nEndNo = nStartNo + nSearchLength;
		if(m_nLastPos < nEndNo)
			nEndNo = m_nLastPos;
		SearchPulsePeak(ptr, nNoOfPeak, nStartNo, nEndNo, nInterval, m_nResultPeakPos, m_fResultPeakValue);
		int		nGainLevel = m_byGainArray[0][m_nResultPeakPos[0]];
		int		nPeakPos = m_nResultPeakPos[0];
		float		fDummy;
		float	fCurrentValue = (float)m_byDACurrent[m_nResultPeakPos[0]];

		if(m_nPulseMode == 2) {
			m_fControlRawValue = m_fResultPeakValue[0];
			fDummy = m_fControlRawValue * m_fGainConst[nGainLevel];
			if(m_nCalCount < 3)
				m_fCalPeakAve = fDummy;
			else
				m_fCalPeakAve = CalcAve(m_nCalCount, m_nNoOfCalAve, m_fCalPeakAve, fDummy);
			m_nCalCount++;
			if ( m_fCalPeakAve <= m_fControlValue && m_bCalFlg == false && 3 < m_nCalCount ) {
					m_fCalResultCurrentValue = (fCurrentValue - m_fCalibrationStep) * 1.1F;
					if(60.0F < m_fCalResultCurrentValue)
						m_fCalResultCurrentValue = 60.0F;
					m_bCalFlg = true;
			}
			m_fControlValue = m_fCalPeakAve;
			SetPulseData(nPeakPos, m_fControlValue, 100.0F, 0.0F, 0, 0, (int) m_byGainArray[0][nPeakPos]);
		}
		else if(m_nPulseMode == 1){
			m_fControlRawValue = m_fResultPeakValue[0];
			m_fControlValue = m_fResultPeakValue[0] * m_fGainConst[nGainLevel];
			SetPulseData(nPeakPos, m_fControlValue, 100.0F, 0.0F, 0, 0, (int)m_byGainArray[0][nPeakPos]);
			m_ma.WritePreferencesCtrl();
			m_ma.WritePreferencesGain();
		}
		else if(m_nPulseMode == 5 || m_nPulseMode == 6) {
            fValue = m_fResultPeakValue[0] * m_fGainConst[nGainLevel];
            if(0.0F < m_fControlValue)
                fRatio = 100.0f * fValue / m_fControlValue;
            else
                fRatio = 0.0F;
            SetPulseData(nPeakPos, fValue, fRatio, 0.0F, 0, 0, (int) m_byGainArray[0][nPeakPos]);
        }
		else if(m_nPulseMode == 3 || m_nPulseMode == 4 || m_nPulseMode == 13)
			CalcTOFPeak();
		else if(m_nPulseMode == 11 || m_nPulseMode == 12 || m_nPulseMode == 14)
		    CalcPTCPeak();
		if(m_nPulseMode == 1 || m_nPulseMode == 3 || m_nPulseMode == 5 || m_nPulseMode == 11)
			m_nPulseMode = 0;
	}

	public void SetPulseData(int nPos, float fT1, float fT1Ratio, float fT4_T1, int nTOFCount, int nPTCCount, int nGainLevel)
	{

		// adtex
//		Context mContext = context;
//		UsbManager mUsbManager = (UsbManager)mContext.getSystemService(Context.USB_SERVICE);

		//UsbSerialDriver usb = UsbSerialProper.acquire(mUsbManager);


		m_fTime[2][m_nNoOfData[2]] = m_fDataTime[nPos];
		m_fWork[2] = fT1;
		m_fWork[3] = fT1Ratio;
		m_fWork[4] = fT4_T1;
		m_fWork[5] = (float) nTOFCount;
		m_fWork[6] = (float) nPTCCount;
		m_fWork[7] = (float) nGainLevel;

		m_line = String.format("%.5f",  m_fTime[2][m_nNoOfData[2]]);
		m_line = m_line + " " + String.format("%.1f",  m_fWork[2]);
		m_line = m_line + " " + String.format("%.1f",  m_fWork[3]);
		m_line = m_line + " " + String.format("%.1f",  m_fWork[4]);
		m_line = m_line + " " + String.format("%.1f",  m_fWork[5]);
		m_line = m_line + " " + String.format("%.1f",  m_fWork[7]);

//		m_ma.m_UsbCom.sendString(m_line);
//		byte[] strByte = m_line.getBytes();
//		int length = m_line.length();
//		m_ma.m_UsbCom.sendData(length, strByte);
//		m_ma.m_UsbCom.sendString(m_line);

		int i;
		for (i = 2; i < 8; i++) {
			m_fResult[0][i][m_nNoOfData[2]] = m_fWork[i];
			m_fResultVal[0][i] = m_fWork[i];
			m_fResultVal[1][i] = CalcAve(m_nNoOfData[2], m_nAverageTime, m_fResultVal[1][i], m_fResultVal[0][i]);
			if ( m_fResultVal[0][i] != 0.0F )
				m_fResultVal[2][i] = CalcAve(m_nNoOfData[2], m_nAverageTime, m_fResultVal[1][i], m_fResultVal[0][i]);
			else
				m_fResultVal[2][i] = 0.0F;
			m_fResult[1][i][m_nNoOfData[2]] = m_fResultVal[1][i];
		}
		m_nNoOfData[2]++;
	}



	public void CalcTOFPeak()
	{
		int		i;
		int		nPeakPos = m_nResultPeakPos[0];
		int		nGainLevel = m_byGainArray[0][m_nResultPeakPos[0]];

//		m_fControlValue = fMaxValue * m_fGainConst[nGainLevel];
		float	fLevel = m_fControlValue * m_fDetectionThreshold / 100.0F;
		float	fValue;
		int		nNoOfTOF = 0;
		boolean bT1PeakFlg = false;
		for(i = 0; i < 4; i++)
		{
			fValue = m_fResultPeakValue[i] * m_fGainConst[nGainLevel];
			m_fWork[i] = fValue;
			if(fLevel < fValue) {
				if(i == 0)
					bT1PeakFlg = true;
				nNoOfTOF++;
			}
		}
		float		fRatio1, fRatio2;
		if(0.0F < m_fControlValue)
			fRatio1 = 100.0F * m_fWork[0] / m_fControlValue;
		else
			fRatio1 = 0.0F;
		if(bT1PeakFlg && 0.0F < m_fWork[0])
			fRatio2 = 100.0F * m_fWork[3] / m_fWork[0];
		else
			fRatio2 = 0.0F;
		SetPulseData(nPeakPos, m_fWork[0], fRatio1, fRatio2, nNoOfTOF, 0, (int)m_byGainArray[0][nPeakPos]);
		if(!bT1PeakFlg && m_nPulseMode == 13) {
			m_nPulseMode = 14;    //AutoPilot の時　TOFが0になったら PTCへ
			m_nPrevPulseOutTime = System.currentTimeMillis();
		}

	}

    public void CalcPTCPeak()
    {
        int		i;
        int		nPeakPos = m_nResultPeakPos[0];
		int		nGainLevel = m_byGainArray[0][m_nResultPeakPos[0]];

		float	fLevel = m_fControlValue * m_fDetectionThreshold / 100.0F;
        float	fValue;
        int		nNoOfPTC = 0;
        for(i = 0; i < m_nPTCTwitch2Num; i++)
        {
            fValue = m_fResultPeakValue[i] * m_fGainConst[nGainLevel];
            m_fWork[i] = fValue;
            if(fLevel < fValue)
                nNoOfPTC++;
        }
        float		fRatio1;
        if(0.0F < m_fControlValue)
            fRatio1 = 100.0F * m_fWork[0] / m_fControlValue;
        else
            fRatio1 = 0.0F;
        SetPulseData(nPeakPos, m_fWork[0], fRatio1, 0.0F, 0, nNoOfPTC, (int)m_byGainArray[0][nPeakPos]);
		if(m_nAutoPilotPTCLevel <= nNoOfPTC && m_nPulseMode == 14) {
			m_nPulseMode = 13;    //AutoPilot の時　PTCがm_nAutoPilotPTCLevel以上になったら TOFへ
			m_nPrevPulseOutTime = System.currentTimeMillis();
		}
	}

    //波形の一番たかいピーク位置を検出する
	public int CalcPeakPich(float ptr[], int ch, float fMax[], float fMin[], float fOffset[])
	{
		float	fALevel, fSub;
		float	fBLevel;
		float	fData, fAPeak;
		int		i, nAPoint;
		boolean bAFlg;
		boolean bInitFlg;
		int		bRet;
		nAPoint = 0;
		fAPeak = 0.0F;
		float	fHLevel = 0.7F;
		float	fLLevel = 0.4F;
		if(fMax[ch] < 0.0F)
			return 0;
/*---
		if(ch == 0 || ch == 3)
		{
			fHLevel = 0.7F;
			fLLevel = 0.3F;
		}
-------*/
		bAFlg = false;
		bInitFlg = false;
		fSub = (fMax[ch] - fMin[ch]);
		fALevel = fHLevel * fSub + fMin[ch];
		fBLevel = fLLevel * fSub + fMin[ch];
		fAPeak = fALevel;
		for(i = m_nLastCalcPos[ch]; i < m_nLastPos; i++)
		{
			fData = ptr[i] - fOffset[ch];
			if(bInitFlg && fAPeak < fData)
			{
				bAFlg = true;
				fAPeak = fData;
				nAPoint = i;
			}
			else if(fData < fBLevel)
			{
				/*----
				if(ch == 0 || ch == 3)	//筋弛緩パルスの場合　ピークから　2sec たってから処理
				{		//T2,T3,T4 を見つけないといけないから
					int		nNeedPoint = nAPoint + (int)(m_nSamplingRate * m_fTOFStimInterval * 4);
					if(m_nLastPos < nNeedPoint)
						break;
				}
				------*/
				bInitFlg = true;
				if(bAFlg)//a ピークの右肩にピークがあると不具合を起こすため
				{
					m_nAWork[ch][m_nACount[ch]] = nAPoint;	//a ピークの位置を記録
					m_nACount[ch]++;
					if(m_nACount[ch] == 512)
						m_nACount[ch] = 0;
					bAFlg = false;
					fAPeak = fALevel;
					m_nLastCalcPos[ch] = nAPoint;
				}
			}
		}
		bRet = CheckPeakPich2(ch);
		return bRet;
	}

	//一番高いピークが脈波かどうかをチェックする
	private int CheckPeakPich2(int ch)
	{	//m_nLastCalcPos, m_nLastPos
		int	bRet;
		bRet = 0;
		int		i, nSub;
//		int		nSub_half;
		int		nMinSub;
		float	fBPM, fSec;
		float	fTime = 0.0F;
		if(m_fHeartRateUpper <= 0.0F)
			m_fHeartRateUpper = 200.0F;

		nMinSub = (int)(60 * m_nSamplingRate / m_fHeartRateUpper);	//600bpm を最大心拍数として　間隔の最小値とする
		if(ch == 2)	//呼吸時
		{
			if(m_fRespiratoryRateUpper <= 0.0F)
				m_fRespiratoryRateUpper = 50.0F;
			nMinSub = (int)(60 * m_nSamplingRate / m_fRespiratoryRateUpper);	//60bpm を最大心拍数として　間隔の最小値とする
		}
		if(nMinSub < 10)
			nMinSub = 10;
		if(m_nACount[ch] < 2)
			return 0;
		else
		{
			for(i = 1; i < m_nACount[ch]; i++)
			{
				nSub = m_nAWork[ch][i] - m_nAWork[ch][i - 1];
				if(nMinSub < nSub)
				{
					fSec = (float)nSub / (float)m_nSamplingRate;
					if(fSec != 0.0F)
					{
						fBPM = 60.0F / fSec;
						if(CheckPulseRate(ch, fBPM) && m_nNoOfPeak[ch] < m_nNoOfAllocCalcData * 2)
						{
//							int		nPoint = m_nAWork[ch][i - 1];
							int		nPoint = m_nAWork[ch][i];
							if(0 == m_nNoOfPeak[ch] || m_nPeakCount[ch][m_nNoOfPeak[ch] - 1] < nPoint)
							{
								fTime = m_fDataTime[nPoint];
								m_nPeakCount[ch][m_nNoOfPeak[ch]] = nPoint;
								m_nNoOfPeak[ch]++;
								if(ch == 1)	//心拍数
								{
									m_fResult[0][0][m_nNoOfData[0]] = fBPM;
									m_fResultVal[0][0] = m_fResult[0][0][m_nNoOfData[0]];
									m_fResult[1][0][m_nNoOfData[0]] = CalcAve(m_nNoOfData[0], m_nAverageTime, m_fResultVal[2][0], m_fResult[0][0][m_nNoOfData[0]]);
									m_fResultVal[1][0] = m_fResultVal[2][0] = m_fResult[1][0][m_nNoOfData[0]];
									m_fTime[0][m_nNoOfData[0]] = fTime;
									m_nNoOfData[0]++;
									m_bHartFlg = true;
								}
								else if(ch == 2)	//呼吸数
								{
									m_fResult[0][1][m_nNoOfData[1]] = fBPM;
									m_fResultVal[0][1] = m_fResult[0][1][m_nNoOfData[1]];
									m_fResult[1][1][m_nNoOfData[1]] = CalcAve(m_nNoOfData[1], m_nAverageTime, m_fResultVal[2][1], m_fResult[0][1][m_nNoOfData[1]]);
									m_fResultVal[1][1] = m_fResultVal[2][1] = m_fResult[1][1][m_nNoOfData[1]];
									m_fTime[1][m_nNoOfData[1]] = fTime;
									m_nNoOfData[1]++;
								}
							}
						}
					}
				}
			}
		}
		if(1 < m_nACount[ch])
		{
			m_nAWork[ch][0] = m_nAWork[ch][m_nACount[ch] - 1];
			m_nACount[ch] = 1;
		}
		return 1;
	}
	//移動平均を計算する
	public float CalcAve(int nCount, int nAverageTime, float fAve, float fVal)
	{
		float	fRet;
		int		nNoOfAve;
		int		nAveTime = nAverageTime;
		nCount++;
		nNoOfAve = (nCount < nAveTime)? nCount : nAveTime;
		if(nNoOfAve == 0)
			nNoOfAve = 1;

		fRet = (fAve * (float)(nNoOfAve - 1) + fVal) / (float)nNoOfAve;
		return fRet;
	}
	// 脈拍数が前二つの値と同程度か調べる
	public boolean CheckPulseRate(int ch, float fBPM)
	{
		int		i;
		float	fSub1, fSub2;
		boolean	bRet;
		bRet = false;
		if(fBPM == 0.0F)
			return false;
		if(m_fTOFStimInterval == 0.0F)
			m_fTOFStimInterval = 0.5F;
		if(ch == 1)
		{
			float	fBPM2 = 60.0F / m_fTOFStimInterval;
			if(fBPM2 - 2.5 < fBPM && fBPM < fBPM2 + 2.5)
				return false;	//TOF信号を脈波として検出した
			if(fBPM < m_fHeartRateLower || m_fHeartRateUpper < fBPM)
				return false;
		}
		else if(ch == 2)
		{
			if(fBPM < m_fRespiratoryRateLower || m_fRespiratoryRateUpper < fBPM)
				return false;
		}
		int		debug = 0;
		if(ch == 3)
			debug = 1;
		m_fCheckPulse[ch][0] = m_fCheckPulse[ch][1];
		m_fCheckPulse[ch][1] = m_fCheckPulse[ch][2];
		m_fCheckPulse[ch][2] = fBPM;
		fSub2 = Math.abs(m_fCheckPulse[ch][2] - m_fCheckPulse[ch][0]);
		fSub1 = Math.abs(m_fCheckPulse[ch][2] - m_fCheckPulse[ch][1]);
		m_nNoOfCheck[ch]++;

		float fError;
		float	fLevel = 0.25F; //0.4F
		if (m_fNormalBPM[ch] != 0.0F)
			fError = m_fNormalBPM[ch] * fLevel;
		else
			fError = fBPM * fLevel;

		if(m_nNoOfCheck[ch] < 2)
		{
			m_fNormalBPM[ch] = fBPM;
			bRet = true;
		}
		else if(fSub2 < fError && fSub1 < fError)	//bpm の変化が30%以内なら正常それ以上なら異常
		{
			bRet = true;
			m_fNormalBPM[ch] = fBPM;
		}
		else
		{
			bRet = false;
			if(m_fNormalBPM[ch] != 0.0)
			{
				float	fSub = Math.abs(m_fNormalBPM[ch] - fBPM);
				if(fSub < fError)
					bRet = true;
			}
		}
		return bRet;
	}


	//測定時の描画を行う　surfaceview　から呼び出し
	public void RunObj(SurfaceHolder surfaceHolder)
	{
		long	nTime, nSub;
		Canvas canvas = null;

		int		nPos, nNoOfData, i, j;
		float	fTime = 0.0F;

		try{
			if(0 < m_nLastPos)
				fTime = m_fDataTime[m_nLastPos - 1];
			else
				fTime = 0.0F;
		}
		catch(Exception e){
			int a = 0;
		}
		float	fStartTime = fTime - m_fDspRawData;
		m_fDspRawStart = fStartTime;
		m_fDspRawEnd = fTime;
		nTime = System.currentTimeMillis();
		nSub = nTime - m_nPrevDspTime;
		if(100 < nSub)
		{
			canvas = surfaceHolder.lockCanvas();
			if(canvas != null)
			{
				try{
					DrawObj(canvas);
				}
				catch(Exception e){}
				surfaceHolder.unlockCanvasAndPost(canvas);
				m_nPrevDspTime = nTime;

			}
		}
	}

	//デバイスの縦横が変更になった場合の処理　View の高さ、幅の変更
	public void ViewChanged(int width, int height)
	{
		m_nWidth = width;
		m_nHeight = height;
		m_bDrawFlg = true;
	}

	public void SetDataView(int nNoOfData, short ReadData[], short ReadData2[])
	{
		int			i;
		if(m_ma.m_nCertifyFlg != 2 && m_nMeasMode != 0) //測定時　認証完了していなければ　処理しない
			return;

        if ( m_nMeasMode == 0)          //ファイル読み込み時
        {
            for(i = 0; i < nNoOfData; i++)
            {
//                m_fCurrentValue = (float)m_byDACurrent[i + m_nLastPos];
//                if(m_nPulseMode == 0 && m_byDAPulseKind[i + m_nLastPos] != 0)
                if(m_nPulseMode != m_byDAPulseKind[i + m_nLastPos] && m_byDAPulseKind[i + m_nLastPos] == 2)
                    AutoCalibrationStartSub();  //キャリブレーションパラメータ初期化
				if(m_nPulseMode == 2 && m_byDAPulseKind[i + m_nLastPos] == 0) {
					m_fCurrentValue = m_fCalResultCurrentValue;		//キャリブレーション終了時　カレントを設定する
					m_ma.WritePreferencesCtrl();
				}
				m_nPulseMode = m_byDAPulseKind[i + m_nLastPos];
                if(m_nPulseOutNo == 0 && m_nPulseOutArray[i + m_nLastPos] != m_nOldPulseOutNo) {
					m_nPulseOutNo = m_nPulseOutArray[i + m_nLastPos];
					m_nOldPulseOutNo = m_nPulseOutNo;
				}
                m_nGainLevel[0] = m_byGainArray[0][m_nLastPos + i];
                if(0 < m_ma.m_nComKind)
                    m_nGainLevel[1] = m_byGainArray[1][m_nLastPos + i];
            }
        }
		else if(m_bPulseOutFlg && m_nPulseOutNo == 0)   //測定時
		{
			m_nPulseOutNo = m_nLastPos;
			m_bPulseOutFlg = false;
		}
		if(m_bProgressInDlg && m_nDialogKind == 0)	//保存中
			return;
		if(m_nNoOfAllocRawData < m_nLastPos + nNoOfData)	//確保したデータバッファより多くのデータが来たら処理しない
				return;
		else if(m_nNoOfAllocCalcData < m_nNoOfData[0] + 10)	//確保した結果バッファより多くのデータが来たら処理しない
				return;
		for(i = 0; i < nNoOfData; i++)	//ゴミの除去
		{
			if(ReadData[i] < 0 || 4096 <= ReadData[i])
			{
				if(i != 0)
					ReadData[i] = ReadData[i - 1];
				else if(0 < m_nLastPos)
					ReadData[i] = (short)m_fWaveData[0][m_nLastPos - 1];
				else
					ReadData[i] = 0;
			}
		}
		if(0 < m_ma.m_nComKind)	//ゴミの除去
		{
			for(i = 0; i < nNoOfData; i++)
			{
				if(ReadData2[i] < 0 || 4096 <= ReadData2[i])
				{
					if(i != 0)
						ReadData2[i] = ReadData2[i - 1];
					else if(0 < m_nLastPos)
						ReadData2[i] = (short)m_fWaveData[1][m_nLastPos - 1];
					else
						ReadData2[i] = 0;
				}
			}
		}
		if(0 < m_nMeasMode)	//測定のみ　ファイル読み込み時は処理しない
		{
			float		fCurrentValue;
			int		debug1 = 0;
			if(m_nCorrectionKind == 1)
				fCurrentValue = m_fCorrectionValue;
			else
				fCurrentValue = m_fCurrentValue;
			for(i = 0; i < nNoOfData; i++)
			{
				m_fPaketBuf[i] = (float)ReadData[i];
				m_fWaveData[0][m_nLastPos + i] = m_fPaketBuf[i];
				m_byGainArray[0][m_nLastPos + i] = (byte)m_nGainLevel[0];
				m_fDataTime[m_nLastPos + i] = (float)(m_nLastPos + i) / (float)m_nSamplingRate;
                m_byDACurrent[m_nLastPos + i] = (byte)fCurrentValue;
                m_byDAPulseKind[m_nLastPos + i] = (byte)m_nPulseMode;
                m_nPulseOutArray[m_nLastPos + i] = m_nPulseOutNo;
			}
		}
		else
		{
			for(i = 0; i < nNoOfData; i++)
				m_fPaketBuf[i] = (float)ReadData[i];
		}
//		for(i = 0; i < nNoOfData; i++)
//			m_fPaketBuf[i] -= 2048.0;
//		m_Filter[0].CutSignalArray(nNoOfData, m_fPaketBuf, m_fPaketBuf2);	//筋弛緩
		for(i = 0; i < nNoOfData; i++)
			m_fWaveData[2][m_nLastPos + i] = m_fPaketBuf[i];	//筋弛緩は

		if(0 < m_ma.m_nComKind)
		{
			if(0 < m_nMeasMode)	//測定のみ　ファイル読み込み時は処理しない
			{
				for(i = 0; i < nNoOfData; i++)
				{
					m_fPaketBuf[i] = (float)ReadData2[i];
					m_fWaveData[1][m_nLastPos + i] = m_fPaketBuf[i];
					m_byGainArray[1][m_nLastPos + i] = (byte)m_nGainLevel[1];
				}
			}
			else
			{
				for(i = 0; i < nNoOfData; i++)
					m_fPaketBuf[i] = (float)ReadData2[i];
			}
			for(i = 0; i < nNoOfData; i++)
				m_fPaketBuf[i] -= 2048.0;
			m_Filter[1].CutSignalArray(nNoOfData, m_fPaketBuf, m_fPaketBuf3);	//心拍
			m_Filter[2].CutSignalArray(nNoOfData, m_fPaketBuf, m_fPaketBuf4);	//呼吸
			for(i = 0; i < nNoOfData; i++)
			{
				m_fWaveData[3][m_nLastPos + i] = m_fPaketBuf3[i];
				m_fWaveData[4][m_nLastPos + i] = m_fPaketBuf4[i];
			}
		}
		else
		{
			for(i = 0; i < nNoOfData; i++) {
				m_fWaveData[1][m_nLastPos + i] = 0.0F;
				m_fWaveData[3][m_nLastPos + i] = 0.0F;
				m_fWaveData[4][m_nLastPos + i] = 0.0F;
			}
		}
		m_nLastPos += nNoOfData;
		m_nCount++;
		m_nPrevDataTime = System.currentTimeMillis();
	}

	public void MeasStop()
	{
		if(m_ma.m_bADStartFlg)
			m_ma.WritePreferences();
		m_ma.m_bADStartFlg = false;
		m_nPulseMode = 0;
		if(!m_bDebugModeFlg)
//		if(m_bDebugModeFlg)   //adtex
        	m_ma.m_BLEObj.EndBLEMeas();
        else
        	m_ma.SendMeasEndUART();

	}
	//整数を文字列に変換　（javaのAPIは時間がかかるため使っていない）
	public int IntToString(int val, char buf[], int nZeroPoint)		//nZeroPoint 先頭に0をつけて何桁にしたいか
	{
		int	ret, i, nPos, nCount, nTime, n;
		nPos = 0;
		if(val == 0)
		{
			buf[nPos] = '0';
			nPos++;
			buf[nPos] = '\0';
			return nPos;
		}
		else if(val < 0)
		{
			buf[nPos] = '-';
			nPos++;
			val *= -1;
		}
		nCount = 0;
		while(true)
		{
			nTime = (int)Math.pow(10.0, (double)nCount);
			if(val < nTime)
			{
				nCount--;
				break;
			}
			nCount++;
		}
		if(nCount < nZeroPoint)
			nCount = nZeroPoint - 1;
		for(i = nCount; 0 <= i; i--)
		{
			nTime = (int)Math.pow(10.0, (double)i);
			n = val / nTime;
			if(n == 0)
				buf[nPos] = '0';
			else if(n == 1)
				buf[nPos] = '1';
			else if(n == 2)
				buf[nPos] = '2';
			else if(n == 3)
				buf[nPos] = '3';
			else if(n == 4)
				buf[nPos] = '4';
			else if(n == 5)
				buf[nPos] = '5';
			else if(n == 6)
				buf[nPos] = '6';
			else if(n == 7)
				buf[nPos] = '7';
			else if(n == 8)
				buf[nPos] = '8';
			else
				buf[nPos] = '9';
			nPos++;
			val = val - (n * nTime);
		}
		buf[nPos] = '\0';
		return nPos;
	}
	//日時からファイル名を生成する
	public String GetDefaultName(int nKind)
	{
		String	str = "";
		Calendar cal = Calendar.getInstance();
	    cal.setTimeInMillis(m_lMeasStartTime);
	    int sec = cal.get(Calendar.SECOND);
	    int min = cal.get(Calendar.MINUTE);
	    int hour = cal.get(Calendar.HOUR_OF_DAY);
    	int		day = cal.get(Calendar.DATE);
    	int	month = cal.get(Calendar.MONTH) + 1;
    	int	year = cal.get(Calendar.YEAR);
		if(nKind == 1)
			str = String.format("%s/%d-%02d-%02d/%02d-%02d-%02d-neuro-raw.csv", m_PatientID, year, month, day, hour, min, sec);
		else if(nKind == 2)
			str = String.format("%s/%d-%02d-%02d/%02d-%02d-%02d-neuro-multi_raw.csv", m_PatientID, year, month, day, hour, min, sec);
    	else if(nKind == 3)
    		str = String.format("%s/%d-%02d-%02d/%02d-%02d-%02d-neuro-calc.csv", m_PatientID, year, month, day, hour, min, sec);
    	else if(nKind == 4)
    		str = String.format("%s/%d-%02d-%02d/%02d-%02d-%02d-neuro-ble.csv", m_PatientID, year, month, day, hour, min, sec);	//BLE LOGファイル
    	return str;
	}
	//外部ストレージのがあるかどうかの検出
	public boolean IsExternalStrage(){
		boolean externalStorageAvailable = false;
		boolean externalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if(Environment.MEDIA_MOUNTED.equals(state)){
		//---you can read and write the media---
			externalStorageAvailable = externalStorageWriteable = true;
		}else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)){
			//---you can only read the media---
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		}else{
			//---you cannot read nor write the media---
			externalStorageAvailable = externalStorageWriteable = false;
		}
		return externalStorageAvailable && externalStorageWriteable;
	}

	//ファイル保存フォルダを得る
	public String GetSaveDir()
	{
		String	filepath;
//		int nAPILevel = Build.VERSION.SDK_INT;

//		if(nAPILevel < 29 && IsExternalStrage())
		{
			filepath = Environment.getExternalStorageDirectory().getPath();
			filepath = filepath + "/" + m_DataFolder;
		}
//		else
//			filepath = m_ma.getExternalFilesDir(null).getPath();	//   /android/data/package name/files   保存される
		return filepath;
	}


	//シングルセンサーの生波形データファイルを読み込む
	boolean ReadSPAFile(BufferedInputStream in, byte buf1[], byte buf2[])
	{
		m_nNoOfMarker = 0;
		boolean bTimeFlg = false;
		int		no;
		int		nFlg = 0;
		m_nNoOfFileData = 0;
		byte buf3[] = new byte[100];
		m_lMeasStartTime = System.currentTimeMillis();
		String		str;
		int			nNoOfData = 0;
		while(true)
		{
			no = GetLineToFile(in, buf1, buf2);
			if(no == 0)
				break;
			if(nFlg == 0)
				str = GetLineRowStr(no, 0, buf1, buf3);
			else
				str = "";
			if(buf1[0] == 'd' && buf1[1] == 'a' && buf1[2] == 't' && buf1[3] == 'e' && buf1[4] == ',' && buf1[5] == 'h')
			{
				m_ma.MessageBox(m_ma.getString(R.string.Cannot_DEAL));
				return false;
			}
			else if(buf1[0] == 'S' && buf1[1] == 'a' && buf1[2] == 'm' && buf1[3] == 'p' && buf1[4] == 'l' && buf1[5] == 'i' && buf1[6] == 'n' && buf1[7] == 'g')
			{
				m_nSamplingRate = (int)GetLineRowToDouble(no, 1, buf1, buf3);
				m_ma.ChangeSamplingFreq();
			}
			else if(buf1[0] == 'T' && buf1[1] == 'O' && buf1[2] == 'F' && buf1[3] == '_' && buf1[4] == 'I' && buf1[5] == 'N' && buf1[6] == 'T' && buf1[7] == 'E' && buf1[8] == 'R')
				m_fTOFInterval = (float)GetLineRowToDouble(no, 1, buf1, buf3);
			else if(buf1[0] == 'T' && buf1[1] == 'O' && buf1[2] == 'F' && buf1[3] == '_' && buf1[4] == 'S' && buf1[5] == 'T' && buf1[6] == 'I' && buf1[7] == 'M' && buf1[8] == '_')
				m_fTOFStimInterval = (float)GetLineRowToDouble(no, 1, buf1, buf3);
			else if(buf1[0] == 'T' && buf1[1] == 'y' && buf1[2] == 'p' && buf1[3] == 'e' && buf1[4] == ' ' && buf1[5] == 'o' && buf1[6] == 'f' && buf1[7] == ' '  && buf1[8] == 'S'  && buf1[9] == 'e'  && buf1[10] == 'n')
				m_ma.m_SensorName = GetLineRowStr(no, 1, buf1, buf3);
			else if(!bTimeFlg && buf1[0] == 'S' && buf1[1] == 't' && buf1[2] == 'a' && buf1[3] == 'r' && buf1[4] == 't' && buf1[5] == ' ' && buf1[6] == 'T' && buf1[7] == 'i')
			{
				int		nYear, nMonth, nDay, nHour, nMin, nSec;
				nYear = (int)GetLineRowToDouble(no, 1, buf1, buf3);
				if(1970 < nYear && nYear < 10000)
				{
					nMonth = (int)GetLineRowToDouble(no, 2, buf1, buf3);
					nDay = (int)GetLineRowToDouble(no, 3, buf1, buf3);
					nHour = (int)GetLineRowToDouble(no, 4, buf1, buf3);
					nMin = (int)GetLineRowToDouble(no, 5, buf1, buf3);
					nSec = (int)GetLineRowToDouble(no, 6, buf1, buf3);
					Calendar calen = new GregorianCalendar(nYear, nMonth - 1, nDay, nHour, nMin, nSec);
					m_lMeasStartTime = calen.getTimeInMillis();

				}
			}
			else if(buf1[0] == 'N' && buf1[1] == 'o' && buf1[2] == '.' && buf1[3] == ' ' && buf1[4] == 'O' && buf1[5] == 'f')
				nFlg = 1;
			else if(str.equals("No. Of Data"))
				nNoOfData = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			else if(nFlg == 1)
			{
				m_fWaveData[0][m_nNoOfFileData] = (float)GetLineToDouble(no, buf1, buf3);
				if(m_fWaveData[0][m_nNoOfFileData] < 0.0F || 4100.0F < m_fWaveData[0][m_nNoOfFileData])
				{
					m_fWaveData[0][m_nNoOfFileData] = (float)GetLineToDouble(no, buf1, buf3);
				}
				if(m_nSamplingRate == 0)
					m_nSamplingRate = 200;
				m_fDataTime[m_nNoOfFileData] = (float)m_nNoOfFileData / (float)m_nSamplingRate;

				str = GetLineRowStr(no, 4, buf1, buf3);
				if(0 < str.length())
				{
					m_Comment = str;
					m_fMarkerTime = m_fDataTime[m_nNoOfFileData];
					AddMarker();
				}

				m_nNoOfFileData++;

				if(m_nNoOfAllocRawData <= m_nNoOfFileData)
					break;

				int nPer = 0;
				int	nOld = 0;
				if(0 < nNoOfData)
					nPer = 100 * m_nNoOfFileData / nNoOfData;
				if(nOld != nPer)
				{
					if(m_ProgressDlg != null)
						m_ProgressDlg.setProgress(nPer);
					if(m_bDlgCancelFlg)
						break;
					nOld = nPer;
				}
			}
		}
		return true;
	}

	//文字列からダブル値を得る
	double	GetLineToDouble(int no, byte buf2[], byte buf[])
	{
		double		ret = 0.0;
		try
		{
			int		i, j, k;
			for(i = 0; i < no; i++)
			{
				buf[i] = buf2[i];
				if(buf[i] == ',')
					break;
			}
			int		nFlg = 0;
			int		nCount = 0;
			int		nPos = -1;
			k = 0;
			for(j = 0; j < i; j++)
			{
				if('-' == buf[j])
				{
					nFlg = 1;
					k++;
				}
				else if('.' == buf[j])
				{
					nPos = k;
					k++;
				}
				else if('0' <= buf[j] && buf[j] <= '9')
				{
					ret = ret * 10.0 + (double)(buf[j] - '0');
					k++;
				}
			}
			if(0 <= nPos)
			{
				double	dTime = Math.pow(10.0, (double)(k - 1 - nPos));
				ret = ret / dTime;
			}
			if(nFlg == 1)
				ret *= -1.0;
		}
		catch (Exception e)
		{
			int	err;
			err = 0;
		}
		return ret;
	}

	//一行の文字列から２つのダブル値を得る
	void GetLineToDouble2(int no, byte buf2[], byte buf[], byte buf3[], double fValue[])
	{
		fValue[0] = GetLineToDouble(no, buf2, buf);
		int		i, j, k;
		k = 0;
		for(i = 0; i < no; i++)
		{
			if(buf2[i] == ',')
			{
				for(j = i + 1; j < no; j++)
				{
					buf3[k] = buf2[j];
					k++;
				}
				break;
			}
		}
		fValue[1] = GetLineToDouble(k, buf3, buf);
	}

	//一行の文字列から３つのダブル値を得る
	void GetLineToDouble3(int no, byte buf2[], byte buf[], byte buf3[], double fValue[])
	{
		fValue[0] = GetLineToDouble(no, buf2, buf);
		int		i, j, k;
		k = 0;
		for(i = 0; i < no; i++)
		{
			if(buf2[i] == ',')
			{
				for(j = i + 1; j < no; j++)
				{
					buf3[k] = buf2[j];
					k++;
				}
				break;
			}
		}
		fValue[1] = GetLineToDouble(k, buf3, buf);
		no = k;
		k = 0;
		for(i = 0; i < no; i++)
		{
			if(buf3[i] == ',')
			{
				for(j = i + 1; j < no; j++)
				{
					buf3[k] = buf3[j];
					k++;
				}
				break;
			}
		}
		fValue[2] = GetLineToDouble(k, buf3, buf);
	}

	//文字列から,で区切られた　nRaw　列目のデータを読み込む
	double GetLineRowToDouble(int no, int nRow, byte buf2[], byte buf[])
	{
		int		i, j, k, m, n;
		byte	buf3[] = new byte[no];
		n = 0;
		k = no;
		for(m = 0; m < nRow; m++)
		{
			k = 0;
			for(i = n; i < no; i++)
			{
				if(buf2[i] == ',')
				{
					n = i + 1;
					for(j = i + 1; j < no; j++)
					{
						buf3[k] = buf2[j];
						k++;
					}
					break;
				}
			}
		}
		double	fValue;
		fValue = GetLineToDouble(k, buf3, buf);
		return fValue;
	}

	//buf2 の文字列のnRow列目の文字列を得る
	String GetLineRowStr(int no, int nRow, byte buf2[], byte buf[])
	{
		int		i, p, j;

		p = j = 0;
		for(i = 0; i < no; i++)
		{
			if(buf2[i] == ',')
			{
				if(j == nRow)
					break;
				j++;
				p = 0;
			}
			else if(buf2[i] != 0)
			{
				buf[p] = buf2[i];
				p++;
			}
		}
		if(j == nRow && 0 < p)
		{
			byte	name[] = new byte[p];
			for(i = 0; i < p; i++)
				name[i] = buf[i];
			String ret = "";
			try {
				ret = new String(name, "Shift-JIS");
				int debug = 0;
			} catch (UnsupportedEncodingException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			return ret;
		}
		String	ret2 = "";
		return ret2;
	}


//マルチセンサー形式のデータファイル (AYAPMulti と共通)　を読み込む
	void ReadAYAPMultiFile(BufferedInputStream in, byte buf1[], byte buf2[])
	{
		int		no;
		double	fValue[] = new double[3];
		int		nFlg = 0;
		m_nNoOfFileData = 0;
		byte buf3[] = new byte[100];
		byte buf4[] = new byte[100];
		int		nCount = 0;
		int		nVersion = 1;
		m_nNoOfMarker = 0;
		String str;
		int		nNoOfData = 0;
		while(true)
		{
			no = GetLineToFile(in, buf1, buf2);
			if(nFlg == 0)
				str = GetLineRowStr(no, 0, buf1, buf3);
			else
				str = "";
			if(no == 0)
				break;
			if(buf1[0] == 'C' && buf1[1] == 'h' && buf1[2] == '1' && buf1[3] == ',' && buf1[4] == 'C' && buf1[5] == 'h' && buf1[6] == '2')
				nFlg = 1;
			else if(buf1[0] == 'T' && buf1[1] == 'i' && buf1[2] == 'm' && buf1[3] == 'e' && buf1[4] == '(' && buf1[5] == 's' && buf1[6] == 'e')
				nFlg = 1;
			else if(buf1[0] == 'V' && buf1[1] == 'e' && buf1[2] == 'r' && buf1[3] == 's' && buf1[4] == 'i' && buf1[5] == 'o' && buf1[6] == 'n' && buf1[7] == ',')
				nVersion = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			else if(nCount == 1 || (buf1[0] == 'S' && buf1[1] == 'a' && buf1[2] == 'm' && buf1[3] == 'p' && buf1[4] == 'l' && buf1[5] == 'i' && buf1[6] == 'n' && buf1[7] == 'g'))
			{
				m_nSamplingRate = (int)GetLineRowToDouble(no, 1, buf1, buf3);
				m_ma.ChangeSamplingFreq();
			}
			else if(buf1[0] == 'S' && buf1[1] == 't' && buf1[2] == 'a' && buf1[3] == 'r' && buf1[4] == 't' && buf1[5] == ' ' && buf1[6] == 'T' && buf1[7] == 'i')
				m_lMeasStartTime = (long)GetLineRowToDouble(no, 1, buf1, buf3);
			else if(buf1[0] == 'S' && buf1[1] == 't' && buf1[2] == 'a' && buf1[3] == 'r' && buf1[4] == 't' && buf1[5] == ' ' && buf1[6] == 'D' && buf1[7] == 'a')
			{
				int		nYear, nMonth, nDay, nHour, nMin, nSec;
				nYear = (int)GetLineRowToDouble(no, 1, buf1, buf3);
				nMonth = (int)GetLineRowToDouble(no, 2, buf1, buf3);
				nDay = (int)GetLineRowToDouble(no, 3, buf1, buf3);
				nHour = (int)GetLineRowToDouble(no, 4, buf1, buf3);
				nMin = (int)GetLineRowToDouble(no, 5, buf1, buf3);
				nSec = (int)GetLineRowToDouble(no, 6, buf1, buf3);
				Calendar calen = new GregorianCalendar(nYear, nMonth - 1, nDay, nHour, nMin, nSec);
				m_lMeasStartTime = calen.getTimeInMillis();
			}
			else if(buf1[0] == 'T' && buf1[1] == 'y' && buf1[2] == 'p' && buf1[3] == 'e' && buf1[4] == ' ' && buf1[5] == 'o' && buf1[6] == 'f' && buf1[7] == ' '  && buf1[8] == 'S'  && buf1[9] == 'e'  && buf1[10] == 'n')
				m_ma.m_SensorName = GetLineRowStr(no, 1, buf1, buf3);
			else if(str.equals("No. Of Data"))
				nNoOfData = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			else if(nFlg == 1 || 20 < nCount )
			{
				if(nVersion < 4)
				{
					GetLineToDouble2(no, buf1, buf3, buf4, fValue);
					m_fDataTime[m_nNoOfFileData] = (float)m_nNoOfFileData / (float)m_nSamplingRate;
					m_fWaveData[0][m_nNoOfFileData] = (float)fValue[0];
					m_fWaveData[1][m_nNoOfFileData] = (float)fValue[1];
					m_nNoOfFileData++;
					if(m_nNoOfAllocRawData <= m_nNoOfFileData)
						break;
				}
				else
				{
					str = GetLineRowStr(no, 7, buf1, buf3);		//GetLineToDouble3 で buf1 の内容が書き換わるので注意
					GetLineToDouble3(no, buf1, buf3, buf4, fValue);
					m_fDataTime[m_nNoOfFileData] = (float)fValue[0];
					m_fWaveData[0][m_nNoOfFileData] = (float)fValue[1];
					m_fWaveData[1][m_nNoOfFileData] = (float)fValue[2];

					if(0 < str.length())
					{
						m_Comment = str;
						m_fMarkerTime = m_fDataTime[m_nNoOfFileData];
						AddMarker();
					}

					m_nNoOfFileData++;
					if(m_nNoOfAllocRawData <= m_nNoOfFileData)
						break;
				}
				int nPer = 0;
				int		nOld = 0;
				if(0 < nNoOfData)
					nPer = 100 * m_nNoOfFileData / nNoOfData;
				if(nOld != nPer)
				{
					if(m_ProgressDlg != null)
						m_ProgressDlg.setProgress(nPer);
					if(m_bDlgCancelFlg)
						break;
					nOld = nPer;
				}
			}
			nCount++;
		}
	}
	boolean ReadGainTable(BufferedInputStream in, byte buf1[], byte buf2[])
	{
		boolean bRet = true;
		try
		{
//			String str;
			int			no;
//			int			 nArrayNo;
			m_nNoOfGain = 0;
			float		fGain;
			byte buf3[] = new byte[100];
			byte buf4[] = new byte[100];
			double	fValue[] = new double[2];
			while(true)
			{
				no = GetLineToFile(in, buf1, buf2);
				if(no == 0)
					break;
				GetLineToDouble2(no, buf1, buf3, buf4, fValue);
//				nArrayNo = (int)fValue[0];
				fGain = (float)fValue[1];
//				if(0 <= fGain && fGain < 16)
				m_fGainAmp[m_nNoOfGain] = fGain;
				m_nNoOfGain++;
			}
		}
		catch (Exception e)
		{
			bRet = false;
		}
		return bRet;
	}

	//マルチセンサー形式のデータファイル (AYAPMulti と共通)　を読み込む
	void ReadBioSignalFile(BufferedInputStream in, byte buf1[], byte buf2[])
	{
		int			nNoOfData = 0;
		String		str;
		int		no, dummy;
		double	fValue[] = new double[3];
		int		nFlg = 0;
		m_nNoOfFileData = 0;
		byte buf3[] = new byte[100];
		byte buf4[] = new byte[100];
		int		nCount = 0;
		m_nFileVersion = 1;
		m_nNoOfMarker = 0;
//		Toast.makeText(m_ma, "Loading file", Toast.LENGTH_SHORT).show();
		while(true)
		{
			no = GetLineToFile(in, buf1, buf2);
			if(nFlg == 0)
				str = GetLineRowStr(no, 0, buf1, buf3);
			else
				str = "";
			if(no == 0)
				break;

			if(nFlg == 0)
			{
                if(buf1[0] == 't' && buf1[1] == 'i' && buf1[2] == 'm' && buf1[3] == 'e' && buf1[4] == ',' && buf1[5] == 'r' && buf1[6] == 'a')
                    nFlg = 1;
				else if ( buf1[0] == 'V' && buf1[1] == 'e' && buf1[2] == 'r' && buf1[3] == 's' && buf1[4] == 'i' && buf1[5] == 'o' && buf1[6] == 'n' && buf1[7] == ',' )
					m_nFileVersion = (int) GetLineRowToDouble(no, 1, buf1, buf3);
//				else if(buf1[0] == 'S' && buf1[1] == 'a' && buf1[2] == 'm' && buf1[3] == 'p' && buf1[4] == 'l' && buf1[5] == 'i' && buf1[6] == 'n' && buf1[7] == 'g')
//				{
//					m_nSamplingRate = (int)GetLineRowToDouble(no, 1, buf1, buf3);
//					m_ma.ChangeSamplingFreq();
//				}
				else if ( buf1[0] == 'S' && buf1[1] == 't' && buf1[2] == 'a' && buf1[3] == 'r' && buf1[4] == 't' && buf1[5] == ' ' && buf1[6] == 'T' && buf1[7] == 'i' )
					m_lMeasStartTime = (long) GetLineRowToDouble(no, 1, buf1, buf3);
				else if ( buf1[0] == 'S' && buf1[1] == 't' && buf1[2] == 'a' && buf1[3] == 'r' && buf1[4] == 't' && buf1[5] == ' ' && buf1[6] == 'D' && buf1[7] == 'a' ) {
					int nYear, nMonth, nDay, nHour, nMin, nSec;
					nYear = (int) GetLineRowToDouble(no, 1, buf1, buf3);
					nMonth = (int) GetLineRowToDouble(no, 2, buf1, buf3);
					nDay = (int) GetLineRowToDouble(no, 3, buf1, buf3);
					nHour = (int) GetLineRowToDouble(no, 4, buf1, buf3);
					nMin = (int) GetLineRowToDouble(no, 5, buf1, buf3);
					nSec = (int) GetLineRowToDouble(no, 6, buf1, buf3);
					Calendar calen = new GregorianCalendar(nYear, nMonth - 1, nDay, nHour, nMin, nSec);
					m_lMeasStartTime = calen.getTimeInMillis();
				}
				else if ( str.equals("No. Of Data") )
					nNoOfData = (int) GetLineRowToDouble(no, 1, buf1, buf3);
				else
					ParamAnalyze(str, no, buf1, buf3);
			}
			else if(1 < m_nFileVersion && (nFlg == 1 || 200 < nCount ))
			{
				if(m_ma.m_nComKind < 1)
				{
					str = GetLineRowStr(no, 9, buf1, buf3);		//コメントの読み込み、GetLineToDouble3 で buf1 の内容が書き換わるので注意
					GetLineToDouble2(no, buf1, buf3, buf4, fValue);
					if(m_nSamplingRate == 0)
						m_fDataTime[m_nNoOfFileData] = (float)fValue[0];
					else
						m_fDataTime[m_nNoOfFileData] = (float)m_nNoOfFileData / m_nSamplingRate;
					m_fWaveData[0][m_nNoOfFileData] = (float)fValue[1];
					m_byGainArray[0][m_nNoOfFileData] = (byte)GetLineRowToDouble(no, 3, buf1, buf3);
					m_byDAPulseKind[m_nNoOfFileData] = (byte)GetLineRowToDouble(no, 4, buf1, buf3);
					m_byDACurrent[m_nNoOfFileData] = (byte)GetLineRowToDouble(no, 5, buf1, buf3);
                    m_nPulseOutArray[m_nNoOfFileData] = (int)GetLineRowToDouble(no, 6, buf1, buf3);
				}
				else
				{
					str = GetLineRowStr(no, 11, buf1, buf3);		//コメントの読み込み、GetLineToDouble3 で buf1 の内容が書き換わるので注意
					GetLineToDouble3(no, buf1, buf3, buf4, fValue);
					if(m_nSamplingRate == 0)
						m_fDataTime[m_nNoOfFileData] = (float)fValue[0];
					else
						m_fDataTime[m_nNoOfFileData] = (float)m_nNoOfFileData / m_nSamplingRate;
					m_fWaveData[0][m_nNoOfFileData] = (float)fValue[1];
					m_fWaveData[1][m_nNoOfFileData] = (float)fValue[2];
					m_byGainArray[0][m_nNoOfFileData] = (byte)GetLineRowToDouble(no, 6, buf1, buf3);
					m_byGainArray[1][m_nNoOfFileData] = (byte)GetLineRowToDouble(no, 7, buf1, buf3);
					m_byDAPulseKind[m_nNoOfFileData] = (byte)GetLineRowToDouble(no, 8, buf1, buf3);
					m_byDACurrent[m_nNoOfFileData] = (byte)GetLineRowToDouble(no, 9, buf1, buf3);
                    m_nPulseOutArray[m_nNoOfFileData] = (int)GetLineRowToDouble(no, 10, buf1, buf3);
				}
				if(0 < str.length())
				{
					m_Comment = str;
					m_fMarkerTime = m_fDataTime[m_nNoOfFileData];
					AddMarker();
				}
				if(m_nNoOfFileData == 0 || m_fDataTime[m_nNoOfFileData - 1] < m_fDataTime[m_nNoOfFileData])
					m_nNoOfFileData++;
				if(m_nNoOfAllocRawData <= m_nNoOfFileData)
					break;

				int nPer = 0;
				int		nOld = 0;
				if(0 < nNoOfData)
					nPer = 100 * m_nNoOfFileData / nNoOfData;
				if(nOld != nPer)
				{
					if(m_ProgressDlg != null)
						m_ProgressDlg.setProgress(nPer);
					if(m_bDlgCancelFlg)
						break;
					nOld = nPer;
				}
			}
			nCount++;
		}
		SetGainConst();
		SetFilterFreq();
		m_ma.WritePreferences();
	}

	void ParamAnalyze(String str, int no, byte buf1[], byte buf3[])
	{
		int		dummy;
		if(buf1[0] == 'C' && buf1[1] == 'o' && buf1[2] == 'm' && buf1[3] == 'K' && buf1[4] == 'i' && buf1[5] == 'n' && buf1[6] == 'd')
		{
			m_ma.m_nComKind = (int) GetLineRowToDouble(no, 1, buf1, buf3);
			m_ma.m_nComKind2 = m_ma.m_nComKind;
		}

		else if(CompString("CalibrationStart", buf1))
			m_fCalibrationStart = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("CalibrationStep", buf1))
			m_fCalibrationStep = (float)GetLineRowToDouble(no, 1, buf1, buf3);
        else if(CompString("CalibrationInterval", buf1))
            m_nCalibrationInterval = (int)GetLineRowToDouble(no, 1, buf1, buf3);
        else if(CompString("CalibrationAve", buf1))
            m_nNoOfCalAve = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("CurrentValue", buf1))
			m_fCurrentValue = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("DetectionThreshold", buf1))
			m_fDetectionThreshold = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("ControlValue", buf1))
			m_fControlValue = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("PulseWidth", buf1))
			m_nPulseWidth = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("TwitchInterval", buf1))
			m_nTwitchInterval = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("TOFInterval", buf1))
			m_fTOFInterval = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("TOFStimInterval", buf1))
			m_fTOFStimInterval = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("TOFTimeLimit", buf1))
			m_fTOFTimeLimit = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("PTCTwitch1Num", buf1))
			m_nPTCTwitch1Num = (int) GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("PTC_TETStimFreq", buf1))
			m_nPTC_TETStimFreq = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("PTC_TETStimTime", buf1))
			m_fPTC_TETStimTime = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("PTCTwitch2Num", buf1))
			m_nPTCTwitch2Num = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("PTCAutoInterval", buf1))
			m_fPTCAutoInterval = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("PTCTimeLimit", buf1))
			m_fPTCTimeLimit = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("AutoPilotPTCLevel", buf1))
			m_nAutoPilotPTCLevel = (int) GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("DBSStimInterval", buf1))
			m_fDBSStimInterval = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("DBS_1_2_Interval", buf1))
			m_fDBS_1_2_Interval = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("DBS_1_1_Interval", buf1))
			m_fDBS_1_1_Interval = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("DBSPattern", buf1))
			m_nDBSPattern = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("DBSTimeLimit", buf1))
			m_fDBSTimeLimit = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("TETStimFreq", buf1))
			m_nTETStimFreq = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("TETStimTime", buf1))
			m_fTETStimTime = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(CompString("TETTimeLimit", buf1))
			m_fTETTimeLimit = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(buf1[0] == 'T' && buf1[1] == 'y' && buf1[2] == 'p' && buf1[3] == 'e' && buf1[4] == ' ' && buf1[5] == 'o' && buf1[6] == 'f' && buf1[7] == ' '  && buf1[8] == 'S'  && buf1[9] == 'e'  && buf1[10] == 'n')
			m_ma.m_SensorName = GetLineRowStr(no, 1, buf1, buf3);
		else if(buf1[0] == 'H' && buf1[1] == 'i' && buf1[2] == 'g' && buf1[3] == 'h' && buf1[4] == 'P' && buf1[5] == 'a' && buf1[6] == 's' && buf1[7] == 's' && buf1[8] == 'F')
			m_fHighPassFreq = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(buf1[0] == 'B' && buf1[1] == 'a' && buf1[2] == 'n' && buf1[3] == 'd' && buf1[4] == 'P' && buf1[5] == 'a' && buf1[6] == 's' && buf1[7] == 's' && buf1[8] == '1')
			m_fBandPassFreq1 = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(buf1[0] == 'B' && buf1[1] == 'a' && buf1[2] == 'n' && buf1[3] == 'd' && buf1[4] == 'P' && buf1[5] == 'a' && buf1[6] == 's' && buf1[7] == 's' && buf1[8] == '2')
			m_fBandPassFreq2 = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(buf1[0] == 'L' && buf1[1] == 'o' && buf1[2] == 'w' && buf1[3] == 'B' && buf1[4] == 'a' && buf1[5] == 'n' && buf1[6] == 'd' && buf1[7] == 'P' && buf1[8] == 'a' && buf1[9] == 's' && buf1[10] == 's' && buf1[11] == '1')
			m_fLowBandPassFreq1 = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(buf1[0] == 'L' && buf1[1] == 'o' && buf1[2] == 'w' && buf1[3] == 'B' && buf1[4] == 'a' && buf1[5] == 'n' && buf1[6] == 'd' && buf1[7] == 'P' && buf1[8] == 'a' && buf1[9] == 's' && buf1[10] == 's' && buf1[11] == '2')
			m_fLowBandPassFreq2 = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("Auto Gain"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bAutoGainFlg = (dummy == 1)? true : false;
		}
		else if(str.equals("Heart Amp Mag"))
			m_fHeartAmpMag = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("Respiratory Amp Mag"))
			m_fRespiratoryAmpMag = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("TOF1 Amp Mag"))
			m_fTOF1AmpMag = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("Heart Rate Upper"))
			m_fHeartRateUpper = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("Heart Rate Lower"))
			m_fHeartRateLower = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("Respiratory Rate Upper"))
			m_fRespiratoryRateUpper = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("Respiratory Rate Lower"))
			m_fRespiratoryRateLower = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("Calc Graph Ave Flg"))
			m_nCalcGraphAveFlg = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("Average Time"))
			m_nAverageTime = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("Disconnect Time"))
			m_ma.m_nDisConnectTime = (int)GetLineRowToDouble(no, 1, buf1, buf3);

		else if(str.equals("Current Correction"))
			m_nCorrectionKind = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("ACh receptors"))
			m_fAChReceptor = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("ACh receptor block rate")) {
			m_fAChBlockRate = (float) GetLineRowToDouble(no, 1, buf1, buf3);  //ADTEX 変数化
			m_fAChBlockRateX = m_fAChBlockRate;
		}
		else if(str.equals("blocks by one stimulation"))
			m_fBlockOneStim = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("recovery half-life"))
			m_fRecoveryHalfLife = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("PTP"))
			m_fPTP = (float)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(str.equals("Half-life of PTP"))
			m_fPTP_HalfLife = (float)GetLineRowToDouble(no, 1, buf1, buf3);
//		else if(str.equals("Minimum sensitivity"))
//			m_fMinimumSensitivity = (float)GetLineRowToDouble(no, 1, buf1, buf3);

		else if(str.equals("Absolute Time Flg"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bAbsoluteTimeFlg = (dummy == 1)? true : false;
		}
		else if(str.equals("HR Graph Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bGraphCheck[0] = (dummy == 1)? true : false;
		}
		else if(str.equals("RR Graph Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bGraphCheck[1] = (dummy == 1)? true : false;
		}
		else if(str.equals("T1 Graph Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bGraphCheck[2] = (dummy == 1)? true : false;
		}
		else if(str.equals("T1 Ratio Graph Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bGraphCheck[3] = (dummy == 1)? true : false;
		}
		else if(str.equals("T4/T1 Graph Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bGraphCheck[4] = (dummy == 1)? true : false;
		}
		else if(str.equals("TOF Graph Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bGraphCheck[5] = (dummy == 1)? true : false;
		}
		else if(str.equals("PTC Graph Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bGraphCheck[6] = (dummy == 1)? true : false;
		}
		else if(str.equals("Ch1 Raw Graph Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bGraphCheck[7] = (dummy == 1)? true : false;
		}
		else if(str.equals("Ch2 Raw Graph Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bGraphCheck[8] = (dummy == 1)? true : false;
		}
		else if(str.equals("Device Name"))
			m_ma.m_DeviceName = GetLineRowStr(no, 1, buf1, buf3);		//GetLineToDouble3 で buf1 の内容が書き換わるので注意
		else if(str.equals("BLE Log Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_ma.m_bBLELogFlg = (dummy == 1)? true : false;
		}
		else if(str.equals("Stop Save Check"))
		{
			dummy = (int)GetLineRowToDouble(no, 1, buf1, buf3);
			m_bStopSaveFlg = (dummy == 1)? true : false;
		}
		else if(str.equals("No Of Gain"))
			m_nNoOfGain = (int)GetLineRowToDouble(no, 1, buf1, buf3);
		else if(0 < m_nNoOfGain) {
			int i;
			String	str2;
			for (i = 0; i < m_nNoOfGain; i++) {
				str2 = String.format("GainAmp%d", i);
				if ( str.equals(str2) )
					m_fGainAmp[i] = (float) GetLineRowToDouble(no, 1, buf1, buf3);
			}
		}
	}

	public boolean CompString(String str, byte buf[])
	{
		int length = str.length();
		int	i;
		for(i = 0; i < length; i++)
		{
			if(str.charAt(i) != buf[i])
				return false;
		}
		return true;
	}
	//ファイルから一行分のデータを得る
	int GetLineToFile(BufferedInputStream in, byte buf1[], byte buf2[])
	{
		int		no, i;
		i = 0;
		try
		{
			while(true)
			{
				if(m_nReadPos < m_nReadSize)
				{
					buf1[i] = buf2[m_nReadPos];
					m_nReadPos++;
					if(buf1[i] == 13)	//CR
					{
						buf1[i] = 0;
						m_nReadPos++;
						break;
					}
					if(buf1[i] != 0xa)
						i++;
				}
				else
				{
					no = in.read(buf2, 0, 1024);
					m_nReadSize = no;
					m_nReadPos = 0;
					if(no <= 0)
					{
						m_nReadSize = 0;
						buf1[i] = 0;
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			int	err;
			err = 0;
		}

		return i;
	}

	//生データファイルを読み込む
	void ReadCSVFile(String filename)
	{
		int		i;
		File file = new File(filename);
		FileInputStream fin;
		BufferedInputStream in = null;
		byte	buf1[] = new byte[1024];
		byte	buf2[] = new byte[1024];
		int		no;
		m_nReadPos = 0;
		m_nReadSize = 0;
		InitMeas(true, false);
		try
		{
			fin = new FileInputStream(file);
			in = new BufferedInputStream(fin);
			no = GetLineToFile(in, buf1, buf2);
			byte buf3[] = new byte[no];
			for(i = 0; i < no; i++)
				buf3[i] = buf1[i];
			String str;
			str = GetLineRowStr(no, 0, buf1, buf3);
			if("Smart Pulse Analyzer".equals(str))
			{
				if(ReadSPAFile(in, buf1, buf2))
					m_ma.m_nComKind = 0;
			}
			else if("AYAPMulti DATA File".equals(str))
			{
				ReadAYAPMultiFile(in, buf1, buf2);
				m_ma.m_nComKind = 1;
			}
			else if("Adtex Neuromascular Monitor".equals(str))
				ReadBioSignalFile(in, buf1, buf2);
			else if("gain table".equals(str))
			{
				boolean bRet = ReadGainTable(in, buf1, buf2);
				fin.close();
				String str1, str2;
				if(bRet)
				{
					str1 = "Loaded gain table\n";
					for(i = 0; i < m_nNoOfGain; i++)
					{
						str2 = String.format("%d, %.1f\r\n", i, m_fGainAmp[i]);
						str1 += str2;
					}
					SetGainConst();
					m_ma.WritePreferences();

				}
				else
					str1 = "Failed to read gain table.";
				m_ma.m_Msg = str1;
				m_ma.m_bMsgReq = true;
				return;
			}
			else
			{
				fin.close();
				m_ma.m_Msg = m_ma.getString(R.string.Cannot_DEAL);
				m_ma.m_bMsgReq = true;
				return;
			}
			fin.close();
			SetMaxMin();
			m_ma.m_nFileReadFlg = 2;
//			String msg;
//			msg = String.format("%d points of data were read form the file", m_nNoOfFileData);
//			Toast.makeText(m_ma, msg, Toast.LENGTH_SHORT).show();

			m_bDrawFlg = true;
			m_ma.WritePreferences();
		}
		catch (Exception e)
		{
			int	err;
			err = 0;
		}
	}

	void SetGainConst()
	{
		int		i;
		for(i = 0; i < m_nNoOfGain; i++)
		{
			if(0.0F < m_fGainAmp[i])
				m_fGainConst[i] = 3000000.0F / (4096.0F * m_fGainAmp[i]);
			else
				m_fGainConst[i] = 1.0F;
		}
	}
	//配列の直流成分の算出　bFlg true の場合は除去
	public float OffsetCorrect(int nNoOfData, float pData[], boolean bFlg)
	{
		float	sum1 = 0.0F;
		float	offset1;
		int		i;
		if(nNoOfData == 0)
			return 0.0F;
		for(i = 0; i < nNoOfData; i++)
			sum1 += pData[i];
		offset1 = sum1 / (float)nNoOfData;
		if(bFlg)
		{
			for(i = 0; i < nNoOfData; i++)
				pData[i] -= offset1;
		}
		return offset1;
	}

	//読み込んだデータを再計算する
	void ReCalcData()
	{

		int			nFlg;
		if(m_ma.m_nComKind < 1)
			nFlg = 1;
		else
			nFlg = 2;
		int nPer = 0;
		int		nOld = 0;

		int		i, j, k, nStep;
		nStep = 16;
		short	sData1[] = new short[nStep];
		short	sData2[] = new short[nStep];

		InitMeas(true, true);
		for(i = 0; i < (m_nNoOfFileData - nStep); i += nStep)
		{
			try
			{
				if(nFlg == 1) {
					for (j = 0; j < nStep; j++)
						sData1[j] = (short) m_fWaveData[0][i + j];
					SetDataView(nStep, sData1, sData1);
				}
				else {
					for(j = 0; j < nStep; j++)
					{
						sData1[j] = (short)m_fWaveData[0][i + j];
						sData2[j] = (short)m_fWaveData[1][i + j];
					}
					SetDataView(nStep, sData1, sData2);
				}
				CalcMain();

				if(0 < m_nNoOfFileData)
					nPer = 100 * i / m_nNoOfFileData;
				if(nPer != nOld)
				{
					if(m_ProgressDlg != null)
						m_ProgressDlg.setProgress(nPer);
					if(m_bDlgCancelFlg)
						break;
					nOld = nPer;
				}
			}
			catch (Exception e)
			{
				int	err;
				err = 0;
			}
		}
		m_nPulseMode = 0;
	}

	//指定したチャンネルのゲインを一つ上げる
	void GainUp(int ch)
	{
		m_nGainLevel[ch]++;
		if(m_nNoOfGain <= m_nGainLevel[ch])
			m_nGainLevel[ch] = m_nNoOfGain - 1;
		SendGainCommand(ch, m_nGainLevel[ch]);
		m_bDrawFlg = true;
		m_ma.WritePreferencesGain();

	}

	//指定したチャンネルのゲインを一つ下げる
	void GainDown(int ch)
	{
		m_nGainLevel[ch]--;
		if(m_nGainLevel[ch] < 0)
			m_nGainLevel[ch] = 0;
		SendGainCommand(ch, m_nGainLevel[ch]);
		m_bDrawFlg = true;
		m_ma.WritePreferencesGain();
	}

	//ゲインレベルの設定コマンドを送る
	void SendGainCommand(int ch, int nGainLevel)
	{
		if(!m_bDebugModeFlg)
//		if(m_bDebugModeFlg)   //adtex
			m_ma.m_BLEObj.SendGainCommandBLE(ch, nGainLevel);
		else
			m_ma.SendGainCommandUART(ch, nGainLevel);
		m_ma.WriteGainLevel(ch, nGainLevel);
	}



	public void AutoGain()
	{       //Ch2 の場合だけ、Ch1 の場合はパルスの時にチェックする
	    if(m_ma.m_nComKind < 1)
	        return;
		if(m_nLastPos <= 1)
			return;
		int		i, ch;
		int		chno;
		int		nLength = 3 * m_nSamplingRate;
		int		sta = m_nLastPos - nLength;
		if(sta < 0)
			sta = 0;
//		if(m_ma.m_nComKind < 1)
//			chno = 1;
//		else
			chno = 2;
		float	fData;
		float	fMax[] = new float[2];
		fMax[0] = fMax[1] = 0.0F;
		for(ch = 1; ch < chno; ch++)
		{
			m_bOverRangeErrFlg[ch] = 0;
			for(i = sta; i < m_nLastPos; i++)
			{
//				if(ch == 0)
//					fData = m_fWaveData[ch][i];
//				else
					fData = Math.abs(m_fWaveData[ch][i] - 2048.0F);
				if(fMax[ch] < fData || i == sta)
				{
					fMax[ch] = fData;
					if(1800.0F < fMax[ch])
					{
						m_bOverRangeErrFlg[ch] = 1;
						break;
					}
				}
			}
			if(m_bAutoGainFlg)
			{
				if(m_bOverRangeErrFlg[ch] == 1)
					GainDown(ch);
				else if(fMax[ch] < 300.0F)
					GainUp(ch);
			}
		}
	}

    public void AutoGainCh1(float fValue)
    {   //fValue ピークの高さ
        if(1800.0F < fValue)
            m_bOverRangeErrFlg[0] = 1;
        else
            m_bOverRangeErrFlg[0] = 0;
        if(m_bAutoGainFlg && 0 < m_nOutputPulseValue)
        {
            if(m_bOverRangeErrFlg[0] == 1)
                GainDown(0);
            else if(fValue < 300.0F)
                GainUp(0);
        }
    }


	//縦軸目盛りを描画
	private void DrawYMemory(int nKind, Canvas canvas, Paint memPaint, Paint frPaint, double fMax, double fMin)
	{	//0 WAVE    1:Calc   2:Pulse    (1は使っていない）
		double	dStep[] = new double[1];
		double	dStart[] = new double[1];
		double	dOffset;
		int		nKind2;
		nKind2 = (nKind == 2)? 1 : nKind;
		if(nKind == 0)
			dOffset = 1.0;		//327.67;
		else
			dOffset = 1.0;
		fMax /= dOffset;
		fMin /= dOffset;
		GetMemoryStep1(fMax, fMin, dStep, dStart, true, 0.0);
		double	d = 0;
		int		y;
		String	str;
		frPaint.setTextAlign(Align.RIGHT);
		int		yLimit = m_ey[nKind2] - m_nCharSize / 2;
		for(d = dStart[0]; d <= fMax; d += dStep[0])
		{
			str = GetVertScale(d, dStep[0]);
			y = (int)(m_ay[nKind] * d * dOffset + m_by[nKind]);
			canvas.drawLine(m_sx[nKind2], y,  m_ex[nKind2], y, memPaint);
			if(yLimit < y)
				y = yLimit;
			canvas.drawText(str, m_sx[nKind2] - m_nCharSize / 4, y + m_nCharSize / 2, frPaint);
		}
	}
//横軸目盛りを描画　　周波数　or 測定開始からの秒数
	private void DrawXMemory(Canvas canvas, Paint memPaint, Paint frPaint, float fStaPos, float fEndPos, int nFlg)
	{	//nFlg 0:Wave Time   1:FFT Point  2:Analyze Wave Point
		double	dStep[] = new double[1];
		double	dStart[] = new double[1];
		double	fMax, fMin;
		fMax = 1.0;
		fMin = 0.0;
    	float	fOffset = 0.0F;
		fMin = (double)fStaPos;
		fMax = (double)fEndPos;
		GetMemoryStep1(fMax, fMin, dStep, dStart, false, 0.0);
		double	d, pos;
		int		x;
		String	str;
		frPaint.setTextAlign(Align.CENTER);
		for(d = dStart[0]; d <= fMax; d += dStep[0])
		{
			if(1.0 < dStep[0])
				str = String.format("%.1f", d);
			else
				str = String.format("%.2f", d);
			x = (int)(m_ax[nFlg] * d + m_bx[nFlg]);
			if(m_sx[nFlg] <= x && x <= m_ex[nFlg])
			{
				canvas.drawLine(x,  m_sy[nFlg], x,  m_ey[nFlg], memPaint);
				canvas.drawText(str, x, m_ey[nFlg] + m_nCharSize, frPaint);
			}
		}
		str = "sec";
		canvas.drawText(str, (m_sx[nFlg] + m_ex[nFlg]) / 2, m_ey[nFlg] + m_nCharSize * 2, frPaint);
	}
	//横軸目盛りを描画　　絶対時刻の場合
	private void DrawXMemory2(Canvas canvas, Paint memPaint, Paint frPaint, long lSta, long lEnd, int no)
	{
		double	dStep[] = new double[1];
		double	dStart[] = new double[1];
		double	fMax, fMin, fInitVal;
		long	lVal;
		fMin = (double)lSta / 1000.0;
		fMax = (double)lEnd / 1000.0;
		long	lDummy;
		lDummy = 3600000 * 24;
		lVal = (lSta / lDummy) * lDummy;
		fInitVal = (double)lVal / 1000.0;
		GetMemoryStepTime(fMax, fMin, dStep, dStart, false, fInitVal);
		if(dStep[0] == 0.0)
			return;
		double	d;
		int		x, oldx;
		String	str;
		frPaint.setTextAlign(Align.CENTER);
		oldx = 0;
		for(d = dStart[0]; d <= fMax; d += dStep[0])
		{
			lVal = (long)(d * 1000.0);
			str = GetTimeString(lVal, lSta, lEnd);
			if(no == 0)
			{
				lVal -= m_lMeasStartTime;
				x = (int)(m_ax[no] * (double)lVal / 1000.0 + m_bx[no]);
			}
			else
				x = (int)(m_ax[no] * (double)lVal + m_bx[no]);
			if(m_sx[no] <= x && x <= m_ex[no])
			{
				canvas.drawLine(x,  m_sy[no], x,  m_ey[no], memPaint);
				if(oldx < x)
				{
					canvas.drawText(str, x, m_ey[no] + m_nCharSize, frPaint);
					oldx = x + m_nCharSize * 5;
				}
			}
		}
	}

	//秒数を日、時間、分、秒の文字列にする　
	public String GetTimeString(long lTime, long lSta, long lEnd)
	{
		long	lSub = lEnd - lSta;
		long	l2Days = 2 * 24 * 60 * 60 * 1000;
		String	str;
		str = "";
		Calendar cal = Calendar.getInstance();
	    cal.setTimeInMillis(lTime);
	    int sec = cal.get(Calendar.SECOND);
	    int min = cal.get(Calendar.MINUTE);
	    int hour = cal.get(Calendar.HOUR_OF_DAY);
	    if(lSub < l2Days)
	    	str = String.format("%d:%02d:%02d", hour, min, sec);
	    else
	    {
	    	int		day = cal.get(Calendar.DATE);
	    	str = String.format("%d:%d:%02d:%02d", day, hour, min, sec);
	    }
		return str;
	}
	//推移グラフ用　秒数から　年、月、日、時間　の文字列を取得
	//目盛りをうつ位置を切りのよい位置にする　日単位か　分単位
	public long GetDayString(long lTime, long lSta, long lEnd, String strA[])
	{	//
		long	lSub = lEnd - lSta;
		long	l7Day = 7 * 24 * 60 * 60 * 1000;
		Calendar cal = Calendar.getInstance();
	    cal.setTimeInMillis(lTime);
	    int sec = cal.get(Calendar.SECOND);
	    int min = cal.get(Calendar.MINUTE);
	    int hour = cal.get(Calendar.HOUR_OF_DAY);
    	int day = cal.get(Calendar.DATE);
	    int	month = cal.get(Calendar.MONTH) + 1;
	    int	year = cal.get(Calendar.YEAR);
		if(l7Day < lSub)	//時間差が大きい場合は　（一週間以上）　一日単位にする
		{
			cal.set(year, month - 1, day, 0, 0, 0);
			hour = min = sec = 0;
			lTime = cal.getTimeInMillis();
		}
		else if(sec != 0)	//分単位にする
		{
			cal.set(year, month - 1, day, hour, min, 0);
			sec = 0;
			lTime = cal.getTimeInMillis();
		}
	    strA[0] = String.format("%d", year);
	    strA[1] = String.format("%02d/%02d", month, day);
    	strA[2] = String.format("%d:%02d", hour, min);
		return lTime;
	}
//絶対時刻の場合　何分毎に目盛りを描画するかを決める　　　pStart は描画開始目盛り　　pStep は目盛り間隔
	void GetMemoryStepTime(double fMax, double fMin, double pStep[], double pStart[], boolean bVertFlg, double dInitVal)
	{
		if(fMax < -1e16 || 1e16 < fMax || fMin < -1e16 || 1e16 < fMin)
			return;
//		CheckSwapDouble(&fMin, &fMax);
		double	dDummy;
		if(fMax < fMin)
		{
			dDummy = fMax;
			fMax = fMin;
			fMin = dDummy;
		}
		double	fSub = fMax - fMin;
		if(fSub == 0.0)
		{
			pStep[0] = 1.0;
			pStart[0] = fMin;
			return;
		}
		int		nCount = 0;
		double	dDummy2;
		dDummy = fSub;
		while(dDummy < 1.0)
		{
			dDummy *= 10.0;
			nCount--;
			if(nCount < -10)
			{
				dDummy = 10.0;
				break;
			}
		}
		while(60.0 < dDummy)
		{
			dDummy /= 60.0;
			nCount++;
			if(10 < nCount)
			{
				dDummy = 10.0;
				break;
			}
		}
		if(dDummy < 2.0)
			dDummy2 = 0.2;
		else if(dDummy < 6.0)
			dDummy2 = 0.5;
		else if(dDummy < 13.0)
			dDummy2 = 1.0;
		else if(dDummy < 33.0)
			dDummy2 = 5.0;
		else
			dDummy2 = 10.0;
		if(bVertFlg)
			dDummy2 /= 2.0;
		pStep[0] = dDummy2 * Math.pow(60.0, nCount);
		double	fStep = pStep[0];
		double	fStart = 0.0;
			pStart[0] = fMin;
		if(fMin < 0.0)
		{
			for(double i = dInitVal; (fMin - fStep) <= i; i -= fStep)
			{
				if(i < fMin)
				{
					fStart = (i + fStep);
					break;
				}
			}
		}
		else
		{
			for(double i = dInitVal; i < fMax; i += fStep)
			{
				if(fMin <= i)
				{
					fStart = (double)i;
					break;
				}
			}
		}
		pStart[0] = fStart;
		pStep[0] = fStep;
	}

	//目盛りが秒数、周波数の場合、開始目盛りと目盛り間隔を取得　　　pStart は描画開始目盛り　　pStep は目盛り間隔
	void GetMemoryStep1(double fMax, double fMin, double pStep[], double pStart[], boolean bVertFlg, double dInitVal)
	{
		if(fMax < -1e10 || 1e10 < fMax || fMin < -1e10 || 1e10 < fMin)
			return;
		double	dDummy;
		if(fMax < fMin)
		{
			dDummy = fMax;
			fMax = fMin;
			fMin = dDummy;
		}
		double	fSub = fMax - fMin;
		if(fSub == 0.0)
		{
			pStep[0] = 1.0;
			pStart[0] = fMin;
			return;
		}
		int		nCount = 0;
		double	dDummy2;
		dDummy = fSub;
		while(dDummy < 1.0)
		{
			dDummy *= 10.0;
			nCount--;
			if(nCount < -10)
			{
				dDummy = 10.0;
				break;
			}
		}
		while(10.0 < dDummy)
		{
			dDummy /= 10.0;
			nCount++;
			if(10 < nCount)
			{
				dDummy = 10.0;
				break;
			}
		}
		if(dDummy < 1.9)
			dDummy2 = 0.2;
		else if(dDummy < 3.9)
			dDummy2 = 0.5;
		else if(dDummy < 6.9)
			dDummy2 = 1.0;
		else
			dDummy2 = 2.0;
//		if(bVertFlg)
//			dDummy2 /= 2.0;
		pStep[0] = dDummy2 * Math.pow(10.0, nCount);
		double	fStep = pStep[0];
		double	fStart = 0.0;
		pStart[0] = fMin;
		if(fMin < 0.0)
		{
			for(double i = dInitVal; (fMin - fStep) <= i; i -= fStep)
			{
				if(i < fMin)
				{
					fStart = (i + fStep);
					break;
				}
			}
		}
		else
		{
			for(double i = dInitVal; i < fMax; i += fStep)
			{
				if(fMin <= i)
				{
					fStart = (double)i;
					break;
				}
			}
		}
		pStart[0] = fStart;
		pStep[0] = fStep;
	}

//目盛りの文字列を取得
	double MakeMemString(double dVal, String str1[], int nSpaceFlg)
	{
		double	dRet = dVal;
		String	str = " ";
		double	dFreq;
		dFreq = dVal;
		if(dFreq < 0.0000001)
		{
			dRet = (double)((int)(dFreq * 100000000.0)) / 100000000.0;
			str = String.format("%.8f", dRet);
		}
		else if(dFreq < 0.000001)
		{
			dRet = (double)((int)(dFreq * 10000000.0)) / 10000000.0;
			str = String.format("%.7f", dRet);
		}
		else if(dFreq < 0.00001)
		{
			dRet = (double)((int)(dFreq * 1000000.0)) / 1000000.0;
			str = String.format("%.6f", dRet);
		}
		else if(dFreq < 0.0001)
		{
			dRet = (double)((int)(dFreq * 100000.0)) / 100000.0;
			str = String.format("%.5f", dRet);
		}
		else if(dFreq < 0.001)
		{
			dRet = (double)((int)(dFreq * 10000.0)) / 10000.0;
			str = String.format("%.4f", dRet);
		}
		else if(dFreq < 0.01)
		{
			dRet = (double)((int)(dFreq * 1000.0)) / 1000.0;
			str = String.format("%.3f", dRet);
		}
		else if(dFreq < 0.1)
		{
			dRet = (double)((int)(dFreq * 100.0)) / 100.0;
			str = String.format("%.2f", dRet);
		}
		else if(dFreq < 1.0)
		{
			dRet = (double)((int)(dFreq * 10.0)) / 10.0;
			str = String.format("%.1f", dRet);
		}
		else if(dFreq < 10.0)
		{
			dRet = (double)((int)dFreq);
			str = String.format("%1.0f", dRet);
		}
		else if(dFreq < 100.0)
		{
			dRet = (double)((int)(dFreq / 10.0) * 10);
			str = String.format("%2.0f", dRet);
		}
		else if(dFreq < 1000.0)
		{
			dRet = (double)((int)(dFreq / 100.0) * 100);
			str = String.format("%3.0f", dRet);
		}
		else if(dFreq < 10000.0)
		{
			dRet = (double)((int)(dFreq / 1000.0) * 1000);
			str = String.format("%1.0fK", dRet / 1000.0);
		}
		else if(dFreq < 100000.0)
		{
			dRet = (double)((int)(dFreq / 10000.0) * 10000);
			str = String.format("%2.0fK", dRet / 1000.0);
		}
		else if(dFreq < 1000000.0)
		{
			dRet = (double)((int)(dFreq / 100000.0) * 100000);
			str = String.format("%3.0fK", dRet / 1000000.0);
		}
		else
		{
			dRet = (double)((int)(dFreq / 1000000.0) * 1000000);
			str = String.format("%1.0fM", dRet / 1000000.0);
		}
		if(dRet <= 0.0)
			dRet = 0.01;
		if(nSpaceFlg != 0)
			str1[0] = " " + str + " ";
		else
			str1[0] = str;
		return dRet;
	}

	//シグナルがあるかどうかのチェック　一定時間以上　指定閾値以上の値がなければ　m_bSignalFlg をfalse
	void CheckSignal(int nNoOfData, short psData1[], short psData2[])
	{
		m_bSignalFlg2 = true;
		if (0 < nNoOfData)
		{
			int		i, j;
			boolean	bFlg = false;
			long	lCurrentTime = System.currentTimeMillis();
			float	fSub[] = new float[2];
			float	fOffset[] = new float[2];
			int		nSum[] = new int[2];
			short	ptr[];
			for (j = 0; j < 2; j++)
			{
				if(j == 0)
					ptr = psData1;
				else
					ptr = psData2;
					nSum[j] = 0;
				for (i = 0; i < nNoOfData; i++)
					nSum[j] += (int)ptr[i];
				fOffset[j] = (float)nSum[j] / (float)nNoOfData;
			}
			for (i = 0; i < nNoOfData; i++)
			{
				fSub[0] = Math.abs((float)psData1[i] - fOffset[0]);
				fSub[1] = Math.abs((float)psData2[i] - fOffset[1]);
				if (20.0 < fSub[0] || 20.0 < fSub[1])
				{
					bFlg = true;
					m_lSignalTime = lCurrentTime;
					break;
				}
			}
			if (!bFlg)
			{
				if ((m_lSignalTime + 3000L) < lCurrentTime)
					m_bSignalFlg2 = false;
			}
		}
	}

	//縦軸目盛りの数字文字列を取得
	String GetVertScale(double dVal, double dStep)
	{
		double	dRet;
		String	step, str;
		if(Math.abs(dStep) < 0.00001)
			step = "%.6f ";
		else if(Math.abs(dStep) < 0.0001)
			step = "%.5f ";
		else if(Math.abs(dStep) < 0.001)
			step = "%.4f ";
		else if(Math.abs(dStep) < .01)
			step = "%.3f ";
		else if(Math.abs(dStep) < .1)
			step = "%.2f ";
		else if(Math.abs(dStep) < 1.0)
			step = "%.1f ";
		else
			step = "%.0f ";
		str = String.format(step, dVal);
		dRet = dVal;
		if(str == "-0.0 ")
			str = "0.0 ";
		return str;
	}



	//結果データの保存の上位関数
	public void ReCalcDlg(ProgressDialog dlg)
	{	//bAutoFlg 終了時の保存		bCalcDataSaveFlg	終了時か連続測定の場合TRUE : 脈波測定の場合 FALSE
		m_bDlgCancelFlg = false;
		m_bProgressInDlg = true;
		m_ProgressDlg = dlg;
//		if(m_ProgressDlg != null)
//			m_ProgressDlg.setProgress(50);
		ReCalcData();

		m_bProgressInDlg = false;
		m_bDrawFlg = true;
	}



	//結果データの保存の上位関数
	public void ReadDataDlg(ProgressDialog dlg)
	{	//bAutoFlg 終了時の保存		bCalcDataSaveFlg	終了時か連続測定の場合TRUE : 脈波測定の場合 FALSE
		m_bDlgCancelFlg = false;
		m_bProgressInDlg = true;
		m_ProgressDlg = dlg;
//		if(m_ProgressDlg != null)
//			m_ProgressDlg.setProgress(50);
		ReadCSVFile(m_SelectFileName);
		m_bProgressInDlg = false;
	}


/*
	//結果データの保存の上位関数
	public void SaveData(ProgressDialog dlg)
	{	//bAutoFlg 終了時の保存		bCalcDataSaveFlg	終了時か連続測定の場合TRUE : 脈波測定の場合 FALSE
		m_bDlgCancelFlg = false;
		m_bProgressInDlg = true;
		m_ProgressDlg = dlg;
//		if(m_ProgressDlg != null)
//			m_ProgressDlg.setProgress(50);
		SaveRawData();		//生データの保存
		SaveCalcData();		//計算データの保存
		m_bProgressInDlg = false;
	}
*/

	public void SaveData(ProgressDialog dlg, Context context)
	{	//bAutoFlg 終了時の保存		bCalcDataSaveFlg	終了時か連続測定の場合TRUE : 脈波測定の場合 FALSE
		m_bDlgCancelFlg = false;
		m_bProgressInDlg = true;
		m_ProgressDlg = dlg;
//		if(m_ProgressDlg != null)
//			m_ProgressDlg.setProgress(50);
		SaveRawData();		//生データの保存
		SaveCalcData(context);		//計算データの保存
		m_bProgressInDlg = false;
	}


	//[連続解析] で測定した　結果データを -calc.csv ファイルに保存する
	public void SaveCalcData(Context context)	//calc
	{
//		if(m_ma.m_nFileReadFlg == 2)
//			return;
		if(m_nNoOfData[0] == 0 && m_nNoOfData[1] == 0 && m_nNoOfData[2] == 0)
			return;
		if(m_nLastPos < 1)
			return;
		int		nPer;
		String filename;
		filename = GetDefaultName(3);	//計算値データの保存

		String	filepath2;
		filepath2 = GetSaveDir() + "/" + filename;

		File file = new File(filepath2);
		file.getParentFile().mkdirs();
		String str2;
		FileOutputStream fos;
		try
		{
			fos = new FileOutputStream(file, false);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "SJIS");
			BufferedWriter bw = new BufferedWriter(osw);

			m_str = "Adtex Neuromascular Monitor Result\r\n";
			bw.write(m_str);
			m_str = "Version,1\r\n";
			bw.write(m_str);
//			m_str = String.format("Sampling Freq,%d\r\n", m_nSamplingRate);
//			bw.write(m_str);
			m_str = GetSmoothingCondition();
			bw.write(m_str);
			m_str = String.format("Start Time,%d\r\n", m_lMeasStartTime);
			bw.write(m_str);
			str2 = GetDayTimeString();
			m_str = String.format("Start Time,%s\r\n", str2);
			bw.write(m_str);
//			m_str = String.format("No. Of Data,%d\r\n", m_nNoOfData[0] + m_nNoOfData[0]);
//			bw.write(m_str);
			int		nNoOfResult = 7;
			m_str = "Hour,Minute,Second,Time,Heart Rate,Respiratory Rate,T1(μV),T1(%),T4/T1,TOF,PTC,Heart Rate Ave,Respiratory Rate Ave,T1 Ave(μV),T1 Ave(%),T4/T1 Ave,TOF Ave,PTC Ave,Gain\r\n";
			bw.write(m_str);

			Calendar cal = Calendar.getInstance();
			int		msec, sec, min, hour, day;

			int		i;
			m_str = "";
			char	buf[];
			buf = new char[10];
			int		nCount, nCount2,  nLength, j;
			nCount = nCount2 = nLength = 0;
			float	fDataTime;
			int		no[] = new int[3];
			int		oldno[] = new int[3];
			oldno[0] = oldno[1] = oldno[2] = no[0] = no[1] = no[2] = -1;
			float		fMaxTime = m_fDataTime[m_nLastPos - 1];
			float		fT, fVal;
			boolean		bChangeFlg;
			int			nChangeNo, m, n, k, nResultNo;
			long		lTime;

			int		nMaxNo = 0;
			if(m_nNoOfData[nMaxNo] < m_nNoOfData[1])
				nMaxNo = 1;
			if(m_nNoOfData[nMaxNo] < m_nNoOfData[2])
				nMaxNo = 2;
			nChangeNo = 0;
			int		nKeta = 1;

			int		nMarkerPos = 0;
			float	fNextMarkerTime, fTime;
//			byte	utf8[] = null;
			int		nLength3 = 0;
			String	Comment = "";

			fNextMarkerTime = -1.0F;
			if(nMarkerPos < m_nNoOfMarker)
			{
				fNextMarkerTime = m_Marker[nMarkerPos].fTime;
				Comment = m_Marker[nMarkerPos].Comment;
				nLength3 = Comment.length();
				nMarkerPos++;
			}
			else
				fNextMarkerTime = m_fDataTime[m_nLastPos - 1] + 100.0F;

			for(fT = 0.0F; fT < fMaxTime; fT += 0.1F)	//0.1秒毎に時間を進める
			{
				for(j = 0; j < 3; j++)
				{
					for(i = no[j] + 1; i < m_nNoOfData[j]; i++)
					{
						if(m_fTime[j][i] < fT)
						{
							no[j] = i;
							break;
						}
					}
				}
				bChangeFlg = false;
				for(j = 0; j < 3; j++)
				{
					if(oldno[j] != no[j])
					{
						bChangeFlg = true;
						nChangeNo = j;
						oldno[j] = no[j];
					}
				}
				if(!bChangeFlg)
					continue;
				lTime = m_lMeasStartTime + (long)(fT * 1000.0F);
			    cal.setTimeInMillis(lTime);
			    msec = cal.get(Calendar.MILLISECOND);
			    sec = cal.get(Calendar.SECOND);
			    min = cal.get(Calendar.MINUTE);
			    hour = cal.get(Calendar.HOUR_OF_DAY);
		    	fDataTime = fT;

				nLength = IntToString(hour, buf, 0);
				buf[nLength] = ',';
				nLength++;
				for(j = 0; j < nLength; j++)
					m_char[j + nCount] = buf[j];
				nCount += nLength;

				nLength = IntToString(min, buf, 0);
				buf[nLength] = ',';
				nLength++;
				for(j = 0; j < nLength; j++)
					m_char[j + nCount] = buf[j];
				nCount += nLength;

				nLength = IntToString(sec, buf, 0);
				buf[nLength] = ',';
				nLength++;
				for(j = 0; j < nLength; j++)
					m_char[j + nCount] = buf[j];
				nCount += nLength;

				nLength = FloatToString(fDataTime, buf, 3);
				buf[nLength] = ',';
				nLength++;
				for(j = 0; j < nLength; j++) {
					m_char[j + nCount] = buf[j];
				}
				nCount += nLength;
				nCount2 += nLength;

				for(m = 0; m < 2; m++)
				{
					for(n = 0; n < nNoOfResult; n++)
					{
						if(n == 0)
							nResultNo = 0;
						else if(n == 1)
							nResultNo = 1;
						else
							nResultNo = 2;

						if(no[nResultNo] < 0)
							fVal = 0.0F;
						else
							fVal = m_fResult[m][n][no[nResultNo]];

						nKeta = 1;

						nLength = FloatToString(fVal, buf, nKeta);
						buf[nLength] = ',';
						nLength++;
						for(j = 0; j < nLength; j++)
							m_char[j + nCount] = buf[j];
						nCount += nLength;
						nCount2 += nLength;
					}
				}

				fVal = m_fResult[0][7][no[2]];		//GAIN
				nLength = FloatToString(fVal, buf, 0);
				for(j = 0; j < nLength; j++)
					m_char[j + nCount] = buf[j];
				nCount += nLength;
				nCount2 += nLength;

				nLength = 0;

				// メモ:ここで書き込んでいそう
				fTime = fT;
				if(fNextMarkerTime <= fTime && 0 < nLength3)
				{
					buf[nLength] = ',';
					nLength++;
					for(j = 0; j < nLength; j++)
						m_char[j + nCount] = buf[j];
					nCount += nLength;
					nCount2 += nLength;

					m_char[nCount] = '\0';
					bw.write(m_char, 0, nCount);
					nCount = 0;
					nLength = 0;
					bw.write(Comment);

					if(nMarkerPos < m_nNoOfMarker)
					{
						fNextMarkerTime = m_Marker[nMarkerPos].fTime;
						Comment = m_Marker[nMarkerPos].Comment;
						nLength3 = Comment.length();
						nMarkerPos++;
					}
					else
						fNextMarkerTime = m_fDataTime[m_nLastPos - 1] + 100.0F;
				}

				buf[nLength] = '\r';
				nLength++;
				buf[nLength] = '\n';
				nLength++;
				for(j = 0; j < nLength; j++)
					m_char[j + nCount] = buf[j];
				nCount += nLength;
				nCount2 += nLength;

				m_char[nCount] = '\0';
				bw.write(m_char, 0, nCount);


				nPer = 90 + (10 * no[nMaxNo]) / m_nNoOfData[nMaxNo];
				if(m_ProgressDlg != null)
					m_ProgressDlg.setProgress(nPer);
				nCount = 0;
				if(m_bDlgCancelFlg)
					break;
			}
			bw.flush();
			bw.close();
			if(m_ProgressDlg != null)
				m_ProgressDlg.setProgress(100);
        }
		catch (Exception e)
		{
			int	err;
			err = 0;
        }
	}

	//生データファイルを保存
	public void SaveRawData()
	{
		if(m_nLastPos == 0)
			return;
		SaveBioSignalData();
	}



	//マーカー情報を位置でソートする
	public void SortMarker()
	{
		int		i, j;
		boolean bFlg;
		bFlg = true;
		MarkerData	dummy;
		dummy = new MarkerData();
		while(bFlg)
		{
			bFlg = false;
			for(i = 0; i < m_nNoOfMarker - 1; i++)
			{
				for(j = i + 1; j < m_nNoOfMarker; j++)
				{
					if(m_Marker[j].fTime < m_Marker[i].fTime)
					{
						dummy = m_Marker[j];
						m_Marker[j] = m_Marker[i];
						m_Marker[i] = dummy;
						bFlg = true;
					}
				}
			}
		}
	}

	//マーカー情報を追加
	public void AddMarker()
	{
		if(m_nNoOfMarkerAlloc <= m_nNoOfMarker)
			return;
		m_Marker[m_nNoOfMarker].Comment = m_Comment;
		m_Marker[m_nNoOfMarker].fTime = m_fMarkerTime;
		m_nNoOfMarker++;
		m_bDrawFlg = true;
	}

	// float 値が異常値か調べる
	public float CheckFloatValue(float fVal)
	{
		if(fVal < -1e10F || 1e10F < fVal)
			fVal = 0.0F;
		else if(-1e-10 < fVal && fVal < 1e-10)
			fVal = 0.0F;
		return fVal;
	}


	//float 値を文字列に変換
	public int FloatToString(float fVal, char buf[], int nPoint)
	{
		fVal = CheckFloatValue(fVal);
		int	nPos, val, nLength, i;
		int	val2;
		char	buf2[];
		buf2 = new char[10];
		nPos = 0;
		if(fVal < 0.0F)
		{
			buf[nPos] = '-';
			nPos++;
			fVal *= -1.0;
		}
		val = (int)fVal;
		nLength = IntToString(val, buf2, 0);
		for(i = 0; i < nLength; i++)
		{
			buf[nPos] = buf2[i];
			nPos++;
		}
		if(nPoint == 0)
		{
			buf[nPos] = '\0';
			return nPos;
		}
		buf[nPos] = '.';
		nPos++;
		val2 = (int)((fVal - (float)val) * (float)Math.pow(10.0, (double)nPoint) + 0.5F);
		nLength = IntToString(val2, buf2, nPoint);
		for(i = 0; i < nLength; i++)
		{
			buf[nPos] = buf2[i];
			nPos++;
		}
		buf[nPos] = '\0';
		return nPos;
	}

	//データに書き込む　処理条件を書き込むための文字列を得る
	public String GetSmoothingCondition()
	{
		String	str, str2;
		int			nFlg;

		str = String.format("Sampling Freq,%d\r\n", m_nSamplingRate);

		str2 = String.format("Ch1 Gain Level,%d\r\n",  m_nGainLevel[0]);
		str += str2;

		str2 = String.format("CalibrationStart,%.3f\r\n",  m_fCalibrationStart);
		str += str2;

		str2 = String.format("CalibrationStep,%.3f\r\n",  m_fCalibrationStep);
		str += str2;

		str2 = String.format("CalibrationInterval,%d\r\n",  m_nCalibrationInterval);
		str += str2;

        str2 = String.format("CalibrationAve,%d\r\n",  m_nNoOfCalAve);
        str += str2;

        str2 = String.format("CurrentValue,%.3f\r\n",  m_fCurrentValue);
		str += str2;

		str2 = String.format("DetectionThreshold,%.3f\r\n",  m_fDetectionThreshold);
		str += str2;

		str2 = String.format("ControlValue,%.3f\r\n",  m_fControlValue);
		str += str2;

		str2 = String.format("PulseWidth,%d\r\n",  m_nPulseWidth);
		str += str2;

		str2 = String.format("TwitchInterval,%d\r\n",  m_nTwitchInterval);
		str += str2;

		str2 = String.format("TOFInterval,%.3f\r\n",  m_fTOFInterval);
		str += str2;

		str2 = String.format("TOFStimInterval,%.3f\r\n",  m_fTOFStimInterval);
		str += str2;

		str2 = String.format("TOFTimeLimit,%.3f\r\n",  m_fTOFTimeLimit);
		str += str2;

		str2 = String.format("PTCTwitch1Num,%d\r\n",  m_nPTCTwitch1Num);
		str += str2;

		str2 = String.format("PTC_TETStimFreq,%d\r\n",  m_nPTC_TETStimFreq);
		str += str2;

		str2 = String.format("PTC_TETStimTime,%.3f\r\n",  m_fPTC_TETStimTime);
		str += str2;

		str2 = String.format("PTCTwitch2Num,%d\r\n",  m_nPTCTwitch2Num);
		str += str2;

		str2 = String.format("PTCAutoInterval,%.3f\r\n",  m_fPTCAutoInterval);
		str += str2;

		str2 = String.format("PTCTimeLimit,%.3f\r\n",  m_fPTCTimeLimit);
		str += str2;

		str2 = String.format("AutoPilotPTCLevel,%d\r\n",  m_nAutoPilotPTCLevel);
		str += str2;

		str2 = String.format("DBSStimInterval,%.3f\r\n",  m_fDBSStimInterval);
		str += str2;

		str2 = String.format("DBS_1_2_Interval,%.3f\r\n",  m_fDBS_1_2_Interval);
		str += str2;

		str2 = String.format("DBS_1_1_Interval,%.3f\r\n",  m_fDBS_1_1_Interval);
		str += str2;

		str2 = String.format("DBSPattern,%d\r\n",  m_nDBSPattern);
		str += str2;

		str2 = String.format("DBSTimeLimit,%.3f\r\n",  m_fDBSTimeLimit);
		str += str2;

		str2 = String.format("TETStimFreq,%d\r\n",  m_nTETStimFreq);
		str += str2;

		str2 = String.format("TETStimTime,%.3f\r\n",  m_fTETStimTime);
		str += str2;

		str2 = String.format("TETTimeLimit,%.3f\r\n",  m_fTETTimeLimit);
		str += str2;

		str2 = String.format("ComKind,%d\r\n",  m_ma.m_nComKind);
		str += str2;
		str2 = String.format("HighPassFreq,%.3f\r\n",  m_fHighPassFreq);
		str += str2;
		str2 = String.format("LowBandPass1Freq,%.3f\r\n",  m_fLowBandPassFreq1);
		str += str2;
		str2 = String.format("LowBandPass2Freq,%.3f\r\n",  m_fLowBandPassFreq2);
		str += str2;
		str2 = String.format("BandPass1Freq,%.3f\r\n",  m_fBandPassFreq1);
		str += str2;
		str2 = String.format("BandPass2Freq,%.3f\r\n",  m_fBandPassFreq2);
		str += str2;

		nFlg = (m_bAutoGainFlg)? 1 : 0;
		str2 = String.format("Auto Gain,%d\r\n",  nFlg);
		str += str2;
		str2 = String.format("Heart Amp Mag,%.3f\r\n",  m_fHeartAmpMag);
		str += str2;
		str2 = String.format("Respiratory Amp Mag,%.3f\r\n",  m_fRespiratoryAmpMag);
		str += str2;
		str2 = String.format("TOF1 Amp Mag,%.3f\r\n",  m_fTOF1AmpMag);
		str += str2;
		str2 = String.format("Heart Rate Upper,%.3f\r\n",  m_fHeartRateUpper);
		str += str2;
		str2 = String.format("Heart Rate Lower,%.3f\r\n",  m_fHeartRateLower);
		str += str2;
		str2 = String.format("Respiratory Rate Upper,%.3f\r\n",  m_fRespiratoryRateUpper);
		str += str2;
		str2 = String.format("Respiratory Rate Lower,%.3f\r\n",  m_fRespiratoryRateLower);
		str += str2;
		str2 = String.format("Calc Graph Ave Flg,%d\r\n",  m_nCalcGraphAveFlg);
		str += str2;
		str2 = String.format("Average Time,%d\r\n",  m_nAverageTime);
		str += str2;
		str2 = String.format("Disconnect Time,%d\r\n",  m_ma.m_nDisConnectTime);
		str += str2;

		str2 = String.format("Current Correction,%d\r\n",  m_nCorrectionKind);
		str += str2;
		str2 = String.format("ACh receptors,%.3f\r\n",  m_fAChReceptor);
		str += str2;
		str2 = String.format("ACh receptor block rate,%.3f\r\n",  m_fAChBlockRate);
		str += str2;
		str2 = String.format("blocks by one stimulation,%.3f\r\n",  m_fBlockOneStim);
		str += str2;
		str2 = String.format("recovery half-life,%.3f\r\n",  m_fRecoveryHalfLife);
		str += str2;
		str2 = String.format("PTP,%.3f\r\n",  m_fPTP);
		str += str2;
		str2 = String.format("Half-life of PTP,%.3f\r\n",  m_fPTP_HalfLife);
		str += str2;
		nFlg = (m_bCorrectionLogFlg)? 1 : 0;
		str2 = String.format("Correction Log Flg,%d\r\n",  nFlg);
		str += str2;

//		str2 = String.format("Minimum sensitivity,%.3f\r\n",  m_fMinimumSensitivity);
//		str += str2;

		nFlg = (m_bAbsoluteTimeFlg)? 1 : 0;
		str2 = String.format("Absolute Time Flg,%d\r\n",  nFlg);
		str += str2;
		nFlg = (m_bGraphCheck[0])? 1 : 0;
		str2 = String.format("HR Graph Check,%d\r\n",  nFlg);
		str += str2;
		nFlg = (m_bGraphCheck[1])? 1 : 0;
		str2 = String.format("RR Graph Check,%d\r\n",  nFlg);
		str += str2;
		nFlg = (m_bGraphCheck[2])? 1 : 0;
		str2 = String.format("T1 Graph Check,%d\r\n",  nFlg);
		str += str2;
		nFlg = (m_bGraphCheck[3])? 1 : 0;
		str2 = String.format("T1 Ratio Graph Check,%d\r\n",  nFlg);
		str += str2;
		nFlg = (m_bGraphCheck[4])? 1 : 0;
		str2 = String.format("T4/T1 Graph Check,%d\r\n",  nFlg);
		str += str2;
		nFlg = (m_bGraphCheck[5])? 1 : 0;
		str2 = String.format("TOF Graph Check,%d\r\n",  nFlg);
		str += str2;
		nFlg = (m_bGraphCheck[6])? 1 : 0;
		str2 = String.format("PTC Graph Check,%d\r\n",  nFlg);
		str += str2;
		nFlg = (m_bGraphCheck[7])? 1 : 0;
		str2 = String.format("Ch1 Raw Graph Check,%d\r\n",  nFlg);
		str += str2;
		nFlg = (m_bGraphCheck[8])? 1 : 0;
		str2 = String.format("Ch2 Raw Graph Check,%d\r\n",  nFlg);
		str += str2;
		str2 = String.format("Device Name,%s\r\n",  m_ma.m_DeviceName);
		str += str2;
		nFlg = (m_ma.m_bBLELogFlg)? 1 : 0;
		str2 = String.format("BLE Log Check,%d\r\n",  nFlg);
		str += str2;
		nFlg = (m_bStopSaveFlg)? 1 : 0;
		str2 = String.format("Stop Save Check,%d\r\n",  nFlg);
		str += str2;
		str2 = String.format("No Of Gain,%d\r\n",  m_nNoOfGain);
		str += str2;

		int			i;
		for(i = 0; i < m_nNoOfGain; i++)
		{
			str2 = String.format("GainAmp,%d,%.3f\r\n", i, m_fGainAmp[i]);
			str += str2;
		}
		if(0 < m_ma.m_nComKind)
		{
			str2 = String.format("Ch2 Gain Level,%d\r\n",  m_nGainLevel[1]);
			str += str2;
		}
		return str;
	}
	//日時を文字列で返す
	public String GetDayTimeString()
	{
		Calendar cal = Calendar.getInstance();
	    cal.setTimeInMillis(m_lMeasStartTime);
	    int sec = cal.get(Calendar.SECOND);
	    int min = cal.get(Calendar.MINUTE);
	    int hour = cal.get(Calendar.HOUR_OF_DAY);
    	int	day = cal.get(Calendar.DATE);
    	int	month = cal.get(Calendar.MONTH) + 1;
    	int	year = cal.get(Calendar.YEAR);
    	String str;
    	str = String.format("%d,%02d,%02d,%02d,%02d,%02d", year, month, day, hour, min, sec);
    	return str;
	}

	//シングルセンサーファイルを保存
/*--------
	private void SaveRawDataSub()
	{
		if(m_nLastPos < 1)
			return;
		SortMarker();
		int		nPer;
		String filename;
		filename = GetDefaultName(1);

		String	filepath2;
		filepath2 = GetSaveDir() + "/" + filename;

		File file = new File(filepath2);
		file.getParentFile().mkdirs();
		String str2;


		FileOutputStream fos;
		int		nNoOfData = m_nLastPos;
		try
		{
			fos = new FileOutputStream(file, false);

			OutputStreamWriter osw = new OutputStreamWriter(fos, "SJIS");
			BufferedWriter bw = new BufferedWriter(osw);
			m_str = "Smart Pulse Analyzer\r\n";
			bw.write(m_str);
			m_str = String.format("Sampling Freq,%d\r\n", m_nSamplingRate);
			bw.write(m_str);
//			bw.write(strKind);
			m_str = GetSmoothingCondition();
			bw.write(m_str);
			m_str = String.format("Start Time,%d\r\n", m_lMeasStartTime);
			bw.write(m_str);
			str2 = GetDayTimeString();
			m_str = String.format("Start Time,%s\r\n", str2);
			bw.write(m_str);

			m_str = String.format("Type of Sensor,%s\r\n", m_ma.m_SensorName);
			bw.write(m_str);

			m_str = String.format("No. Of Data,%d\r\n", m_nLastPos);
			bw.write(m_str);
			int		i, dummy;
			m_str = "";
			dummy = 0;
			char	buf[];
			buf = new char[256];
			char	buf2[];
			buf2 = new char[32];
			int		nCount, nLength, j, nLength3, k;
			nLength3 = nCount = nLength = 0;

			float		fNextMarkerTime, fTime;
			int		nMarkerPos = 0;
			String	Comment = "";
//			byte	utf8[] = null;
			int		nLength2;
			nLength2 = 0;
			fNextMarkerTime = -1.0F;
			if(nMarkerPos < m_nNoOfMarker)
			{
				fNextMarkerTime = m_Marker[nMarkerPos].fTime;
				Comment = m_Marker[nMarkerPos].Comment;
				nLength2 = Comment.length();
				nMarkerPos++;
			}
			else
				fNextMarkerTime = m_fDataTime[m_nLastPos - 1] + 100.0F;

			for(i = 0; i < nNoOfData; i++)
			{
				nLength = 0;
				for(k = 0; k < 4; k++)
				{
					if(k == 0)
						nLength3 = FloatToString(m_fWaveData[0][i], buf2, 2);
					else
						nLength3 = FloatToString(m_fWaveData[k + 1][i], buf2, 2);
					for(j = 0; j < nLength3; j++)
					{
						buf[nLength] = buf2[j];
						nLength++;
					}
					if(k < 3)
					{
						buf[nLength] = ',';
						nLength++;
					}
				}
				fTime = m_fDataTime[i];
				if(fNextMarkerTime <= fTime && 0 < nLength2)
				{
					buf[nLength] = ',';
					nLength++;
					for(j = 0; j < nLength; j++)
						m_char[j + nCount] = buf[j];
					nCount += nLength;
					m_char[nCount] = '\0';
					bw.write(m_char, 0, nCount);
					nCount = 0;
					nLength = 0;
					bw.write(Comment);

					if(nMarkerPos < m_nNoOfMarker)
					{
						fNextMarkerTime = m_Marker[nMarkerPos].fTime;
						Comment = m_Marker[nMarkerPos].Comment;
						nLength2 = Comment.length();
						nMarkerPos++;
					}
					else
						fNextMarkerTime = m_fDataTime[m_nLastPos - 1] + 100.0F;
				}
				buf[nLength] = '\r';
				nLength++;
				buf[nLength] = '\n';
				nLength++;

				for(j = 0; j < nLength; j++)
					m_char[j + nCount] = buf[j];
				nCount += nLength;
				dummy = i % 100;
				if(dummy == 0 || 3000 < nCount)
				{
					m_char[nCount] = '\0';
					bw.write(m_char, 0, nCount);
					nPer = (90 * i) / m_nLastPos;
					if(m_ProgressDlg != null)
						m_ProgressDlg.setProgress(nPer);
					nCount = 0;

				}
				if(m_bDlgCancelFlg)
					break;
			}
			if(dummy != 0)
			{
				m_char[nCount] = '\0';
				bw.write(m_char, 0, nCount);
			}
			bw.flush();
			bw.close();
        }
		catch (Exception e)
		{
			int	err;
			err = 0;
        }
	}
--------------*/
	private void SaveBioSignalData()
	{
		if(m_nLastPos < 1)
			return;
		SortMarker();
		int		nPer;
		String filename;
		if(m_ma.m_nComKind < 1)
			filename = GetDefaultName(1);
		else
			filename = GetDefaultName(2);
		String	filepath2;
		filepath2 = GetSaveDir() + "/" + filename;

		File file = new File(filepath2);
		file.getParentFile().mkdirs();
		String str2;


		FileOutputStream fos;
		int		nNoOfData = m_nLastPos;
		try
		{
			fos = new FileOutputStream(file, false);

			OutputStreamWriter osw = new OutputStreamWriter(fos, "SJIS");
			BufferedWriter bw = new BufferedWriter(osw);
			m_str = "Adtex Neuromascular Monitor\r\n";
			bw.write(m_str);
			m_str = "Version,2\r\n";
			bw.write(m_str);
//			m_str = String.format("Sampling Freq,%d\r\n", m_nSamplingRate);
//			bw.write(m_str);
//			bw.write(strKind);
			m_str = GetSmoothingCondition();
			bw.write(m_str);
			m_str = String.format("Start Time,%d\r\n", m_lMeasStartTime);
			bw.write(m_str);
			str2 = GetDayTimeString();
			m_str = String.format("Start Date,%s\r\n", str2);
			bw.write(m_str);

//			m_str = String.format("Type of Sensor,%s\r\n", m_ma.m_SensorName);
//			bw.write(m_str);

			m_str = String.format("No. Of Data,%d\r\n", m_nLastPos);
			bw.write(m_str);
			if(m_ma.m_nComKind < 1)
				m_str = "time,raw,AD pulse,gain,DA pulse,DA Current,DA Pulse Position,comment\r\n";
			else
				m_str = "time,raw ch1,raw ch2,AD pulse ch1,heart,respiratory,gain ch1,gain ch2,DA pulse,DA Current,DA Pulse Position,comment\r\n";
			bw.write(m_str);
			int		i, dummy;
			m_str = "";
			dummy = 0;
			char	buf[];
			buf = new char[256];
			char	buf2[];
			buf2 = new char[32];
			int		nCount, nLength, j, nLength3, k;
			nLength3 = nCount = nLength = 0;

			float		fNextMarkerTime, fTime;
			int		nMarkerPos = 0;
			String	Comment = "";
//			byte	utf8[] = null;
			int		nLength2;
			nLength2 = 0;
			fNextMarkerTime = -1.0F;
			if(nMarkerPos < m_nNoOfMarker)
			{
				fNextMarkerTime = m_Marker[nMarkerPos].fTime;
				Comment = m_Marker[nMarkerPos].Comment;
				nLength2 = Comment.length();
				nMarkerPos++;
			}
			else
				fNextMarkerTime = m_fDataTime[m_nLastPos - 1] + 100.0F;
			int		nMaxArray;
			nMaxArray = 10;
			if(m_ma.m_nComKind < 1)
				nMaxArray = 6;
			int		nGainLevel;
			float	fGainConst;
			for(i = 0; i < nNoOfData; i++)
			{
				nLength = 0;
				nLength3 = FloatToString(m_fDataTime[i], buf2, 4);
				for(j = 0; j < nLength3; j++)
				{
					buf[nLength] = buf2[j];
					nLength++;
				}
				buf[nLength] = ',';
				nLength++;
				nGainLevel = (int) m_byGainArray[0][i];
				fGainConst = m_fGainConst[nGainLevel];

				if(m_ma.m_nComKind < 1)
				{
					for(k = 0; k < nMaxArray; k++)
					{
						if(k == 0)
							nLength3 = FloatToString(m_fWaveData[0][i], buf2, 2);
						else if(k == 1)
							nLength3 = FloatToString(m_fWaveData[2][i] * fGainConst, buf2, 2);
						else if(k == 2)
							nLength3 = IntToString((int)m_byGainArray[0][i], buf2, 0);
						else if(k == 3)
							nLength3 = IntToString((int)m_byDAPulseKind[i], buf2, 0);
                        else if(k == 4)
                            nLength3 = IntToString((int)m_byDACurrent[i], buf2, 0);
                        else if(k == 5)
                            nLength3 = IntToString(m_nPulseOutArray[i], buf2, 0);
						for(j = 0; j < nLength3; j++)
						{
							buf[nLength] = buf2[j];
							nLength++;
						}
						if(k < 5)
						{
							buf[nLength] = ',';
							nLength++;
						}
					}
				}
				else
				{
					for(k = 0; k < nMaxArray; k++)
					{
						if(k == 2)
							nLength3 = FloatToString(m_fWaveData[2][i] * fGainConst, buf2, 2);
						else if(k < 5)
							nLength3 = FloatToString(m_fWaveData[k][i], buf2, 2);
						else if(k == 5 || k == 6)
							nLength3 = IntToString((int)m_byGainArray[k - 5][i], buf2, 0);
						else if(k == 7)
							nLength3 = IntToString((int)m_byDAPulseKind[i], buf2, 0);
						else if(k == 8)
							nLength3 = IntToString((int)m_byDACurrent[i], buf2, 0);
                        else if(k == 9)
                            nLength3 = IntToString(m_nPulseOutArray[i], buf2, 0);
						for(j = 0; j < nLength3; j++)
						{
							buf[nLength] = buf2[j];
							nLength++;
						}
						if(k < 9)
						{
							buf[nLength] = ',';
							nLength++;
						}
					}
				}

				fTime = m_fDataTime[i];
				if(fNextMarkerTime <= fTime && 0 < nLength2)
				{
					buf[nLength] = ',';
					nLength++;
					for(j = 0; j < nLength; j++)
						m_char[j + nCount] = buf[j];
					nCount += nLength;
					m_char[nCount] = '\0';
					bw.write(m_char, 0, nCount);
					nCount = 0;
					nLength = 0;
					bw.write(Comment);

					if(nMarkerPos < m_nNoOfMarker)
					{
						fNextMarkerTime = m_Marker[nMarkerPos].fTime;
						Comment = m_Marker[nMarkerPos].Comment;
						nLength2 = Comment.length();
						nMarkerPos++;
					}
					else
						fNextMarkerTime = m_fDataTime[m_nLastPos - 1] + 100.0F;
				}
				buf[nLength] = '\r';
				nLength++;
				buf[nLength] = '\n';
				nLength++;

				for(j = 0; j < nLength; j++)
					m_char[j + nCount] = buf[j];
				nCount += nLength;
				dummy = i % 100;
				if(dummy == 0 || 3000 < nCount)
				{
					m_char[nCount] = '\0';
					bw.write(m_char, 0, nCount);
					nPer = (90 * i) / m_nLastPos;
					if(m_ProgressDlg != null)
						m_ProgressDlg.setProgress(nPer);
					nCount = 0;

				}
				if(m_bDlgCancelFlg)
					break;
			}
			if(dummy != 0)
			{
				m_char[nCount] = '\0';
				bw.write(m_char, 0, nCount);
			}
			bw.flush();
			bw.close();
        }
		catch (Exception e)
		{
			int	err;
			err = 0;
        }
	}
/////////////////////Touch Action
	public void TouchAction(boolean bUpFlg)
	{
		int	nKind;
		if(!m_bZoomFlg)
			return;
		nKind = m_nKind;//タッチした場所　0 WAVE  1:Calc  2:Pulse  3:ScrollCalc  4:ScrollWave
		if(nKind == -1 || nKind == 3 || nKind == 4)
			return;
		float		nSubX, nSubY;
		nSubX = Math.abs(m_zsx1 - m_zex1) + Math.abs(m_zsx2 - m_zex2);
		nSubY = Math.abs(m_zsy1 - m_zey1) + Math.abs(m_zsy2 - m_zey2);
		if(nSubY < nSubX)
		{
			if(m_zex1 < m_zsx1 && m_zsx1 < m_zsx2 && m_zsx2 < m_zex2 ||
						m_zex2 < m_zsx2 && m_zsx2 < m_zsx1 && m_zsx1 < m_zex1)
				ZoomInX(nKind);
			else if(m_zsx1 < m_zex1 && m_zex1 < m_zex2 && m_zex2 < m_zsx2 ||
						m_zsx2 < m_zex2 && m_zex2 < m_zex1 && m_zex1 < m_zsx1)
				ZoomOutX(nKind);
		}
		else
		{
			if(m_zey1 < m_zsy1 && m_zsy1 < m_zsy2 && m_zsy2 < m_zey2 ||
					m_zey2 < m_zsy2 && m_zsy2 < m_zsy1 && m_zsy1 < m_zey1)
				ZoomInY(nKind);
			else if(m_zsy1 < m_zey1 && m_zey1 < m_zey2 && m_zey2 < m_zsy2 ||
					m_zsy2 < m_zey2 && m_zey2 < m_zey1 && m_zey1 < m_zsy1)
				ZoomOutY(nKind);
		}
		m_bDrawFlg = true;
		m_zsy1 = m_zey1;
		m_zsy2 = m_zey2;
		m_zsx1 = m_zex1;
		m_zsx2 = m_zex2;
		if(bUpFlg)
			m_bZoomFlg = false;
	}

	//タップされたのが有効な場所かどうか調べる
	public int CheckPoint()
	{
		int		ret;	//0 WAVE   1 Calc  2 Pulse  3 ScrollBar
		ret  = -1;
		if(ret == -1)
			ret = CheckPointSub(0);	//sx[0], sy[0], ex[0], ey[0]
		if(ret == -1)
		{
			ret = CheckPointSub(1);	//sx[1], sy[1], ex[1], ey[1]
			if(ret == 1)	//m_nMemSwitch 0 脈拍目盛り右側　1脈拍目盛り左側
				ret = 2;
		}
		if(ret == -1)
			ret = CheckScrollPos();		//スクロールバー　３
		return ret;
	}

	//スクロールバーがチェックされたかどうか
	public int CheckScrollPos()
	{
		int	nCount = 0;
		if(m_sx[2] < m_zsx1 && m_zsx1 < m_ex[2] && m_sy[2] - 10 < m_zsy1 && m_zsy1 < m_ey[2] + 10)
			nCount++;
		if(m_sx[2] < m_zsx2 && m_zsx2 < m_ex[2] && m_sy[2] - 10 < m_zsy2 && m_zsy2 < m_ey[2] + 10)
			nCount++;
		if(1 < nCount)
			return 3;

		nCount = 0;
		if(m_sx[3] < m_zsx1 && m_zsx1 < m_ex[3] && m_sy[3] - 10 < m_zsy1 && m_zsy1 < m_ey[3] + 10)
			nCount++;
		if(m_sx[3] < m_zsx2 && m_zsx2 < m_ex[3] && m_sy[3] - 10 < m_zsy2 && m_zsy2 < m_ey[3] + 10)
			nCount++;
		if(1 < nCount)
			return 4;
		return -1;
	}

	//結果グラフ右側の縦軸スケールがタッチされたかを調べる
	public int CheckPulseScalePos()
	{
		if(m_ex[1] < m_zsx1 && m_ex[1] < m_zsx2)
		{
			if(m_sy[1] < m_zsy1 && m_zsy1 < m_ey[1] || m_sy[1] < m_zsy2 && m_zsy2 < m_ey[1])
				return 2;
		}
		return -1;
	}

	//タップされたのがグラフ内かどうかを調べる
	public int CheckPointSub(int nKind)
	{
		int	nCount = 0;
		if(m_sx[nKind] < m_zsx1 && m_zsx1 < m_ex[nKind] && m_sy[nKind] < m_zsy1 && m_zsy1 < m_ey[nKind])
			nCount++;
		if(m_sx[nKind] < m_zsx2 && m_zsx2 < m_ex[nKind] && m_sy[nKind] < m_zsy2 && m_zsy2 < m_ey[nKind])
			nCount++;
		if(1 < nCount)
			return nKind;
		else
			return -1;
	}

	//横軸拡大操作
	public void ZoomInX(int nKind)
	{
		double	A, B, C, D, E, F;
		double	Ax, Bx, Cx, Dx, Ex, Fx;
		double	a, b;

		if(nKind == 2)
			nKind = 1;

		if(nKind < 0 || 1 < nKind)
			return;
		if(0 == m_ax[nKind])
			return;
		A = m_sx[nKind]; F = m_ex[nKind];
		if(m_zex1 < m_zsx1 && m_zsx1 < m_zsx2 && m_zsx2 < m_zex2)
		{
			B = m_zex1; C = m_zsx1; D = m_zsx2; E = m_zex2;
		}
		else
		{
			B = m_zex2; C = m_zsx2; D = m_zsx1; E = m_zex1;
		}
		if(B == E)
			return;
		Bx = (B - m_bx[nKind]) / m_ax[nKind];
		Cx = (C - m_bx[nKind]) / m_ax[nKind];
		Dx = (D - m_bx[nKind]) / m_ax[nKind];
		Ex = (E - m_bx[nKind]) / m_ax[nKind];
		a = (Cx - Dx) / (B - E);
		b = Cx - a * B;
		Ax = a * A + b;
		Fx = a * F + b;
		ZoomXSub(nKind, Ax, Fx);
	}

	private void ZoomXSub(int nKind, double Ax, double Fx)
	{
		if(nKind == 0)
		{
			float	fStaPos, fEndPos;
			fStaPos = (float)Ax;
			fEndPos = (float)Fx;
			if(!m_ma.m_bADStartFlg && 0 < m_nLastPos)
			{
				m_fDspRawStart = fStaPos;
				m_fDspRawEnd = fEndPos;
				CheckWaveDspRange();
				m_bDrawFlg = true;
			}
			else
			{
				m_fDspRawData = fEndPos - fStaPos;
				CheckWavePos();
				if(0 < m_nLastPos)
				{
					float fPos = m_fDataTime[m_nLastPos - 1] - m_fDspRawData;
					m_fDspRawStart = fPos;
					m_fDspRawEnd = m_fDataTime[m_nLastPos - 1];
				}
				else
				{
					m_fDspRawStart = 0;
					m_fDspRawEnd = m_fDspRawData;
				}
			}
		}
		else
		{
			int		nStaPos, nEndPos;
			nStaPos = (int)Ax;
			nEndPos = (int)Fx;
			m_nCalcDataRange = nEndPos - nStaPos;
			if(m_nCalcDataRange == 0)
				m_nCalcDataRange = 1;
			else if(10800 < m_nCalcDataRange)
				m_nCalcDataRange = 10800;


			if(0 < m_nLastPos)
			{
				int		nLastTime = (int)m_fDataTime[m_nLastPos - 1] + 1;
				if(nLastTime < nEndPos)
				{
					nEndPos = nLastTime;
					nStaPos = nEndPos - m_nCalcDataRange;
					m_bLastDspFlg = true;
				}
				else
					m_bLastDspFlg = false;
			}
			if(nStaPos < 0)
				nStaPos = 0;
			m_nDspStartTime = nStaPos;
		}
	}

	//横軸縮小操作
	public void ZoomOutX(int nKind)
	{
		double	A, B, C, D, E, F;
		double	Ax, Bx, Cx, Dx, Ex, Fx;
		double	a, b;

		if(nKind == 2)
			nKind = 1;
		if(nKind < 0 || 1 < nKind)
			return;

		if(0 == m_ax[nKind])
			return;
		A = m_sx[nKind]; F = m_ex[nKind];
		if(m_zsx1 < m_zex1 && m_zex1 < m_zex2 && m_zex2 < m_zsx2)
		{
			B = m_zsx1; C = m_zex1; D = m_zex2; E = m_zsx2;
		}
		else
		{
			B = m_zsx2; C = m_zex2; D = m_zex1; E = m_zsx1;
		}
		if(C == D)
			return;
		Bx = (B - m_bx[nKind]) / m_ax[nKind];
		Cx = (C - m_bx[nKind]) / m_ax[nKind];
		Dx = (D - m_bx[nKind]) / m_ax[nKind];
		Ex = (E - m_bx[nKind]) / m_ax[nKind];
		a = (Bx - Ex) / (C - D);
		b = Bx - a * C;
		Ax = a * A + b;
		Fx = a * F + b;
		ZoomXSub(nKind, Ax, Fx);
	}



		//縦軸縮小操作
	public void ZoomOutY(int nKind)
	{
		double	A, B, C, D, E, F;
		double	Ay, By, Cy, Dy, Ey, Fy;
		double	a, b;
		int		nKind2;
		nKind2 = (nKind == 2)? 1 : nKind;

		if(nKind < 0 || 2 < nKind)
			return;

		if(0 == m_ay[nKind])
			return;

		A = m_sy[nKind2]; F = m_ey[nKind2];
		if(m_zsy1 < m_zey1 && m_zey1 < m_zey2 && m_zey2 < m_zsy2)
		{
			B = m_zsy1; C = m_zey1; D = m_zey2; E = m_zsy2;
		}
		else
		{
			B = m_zsy2; C = m_zey2; D = m_zey1; E = m_zsy1;
		}
		if(B == E)
			return;
		By = (B - m_by[nKind]) / m_ay[nKind];
		Cy = (C - m_by[nKind]) / m_ay[nKind];
		Dy = (D - m_by[nKind]) / m_ay[nKind];
		Ey = (E - m_by[nKind]) / m_ay[nKind];
		a = (By - Ey) / (C - D);
		b = By - a * C;
		Ay = a * A + b;
		Fy = a * F + b;
		ZoomYSub(nKind, Ay, Fy);
	}

	private void ZoomYSub(int nKind, double Ay, double Fy)
	{
		if(nKind == 0)//タッチした場所　0 WAVE  1:Calc  2:Pulse  3:Scroll
		{
			m_fWaveMax = (float)Ay;
			m_fWaveMin = (float)Fy;
			CheckWaveScale();
			m_bDrawFlg = true;
		}
		else if(nKind == 1)
		{
			m_fCalcMax = (float)Ay;
			m_fCalcMin = (float)Fy;
			CheckCalcScale();
		}
		else if(nKind == 2)
		{
			m_fPulseMax = (float)Ay;
			m_fPulseMin = (float)Fy;
			CheckPulseScale();
		}
	}

	//縦軸拡大操作
	public void ZoomInY(int nKind)
	{
		double	A, B, C, D, E, F;
		double	Ay, By, Cy, Dy, Ey, Fy;
		double	a, b;
		int		nKind2;
		nKind2 = (nKind == 2)? 1 : nKind;
		if(nKind < 0 || 2 < nKind)
			return;

		if(0 == m_ay[nKind])
			return;
		A = m_sy[nKind2]; F = m_ey[nKind2];
		if(m_zey1 < m_zsy1 && m_zsy1 < m_zsy2 && m_zsy2 < m_zey2)
		{
			B = m_zey1; C = m_zsy1; D = m_zsy2; E = m_zey2;
		}
		else
		{
			B = m_zey2; C = m_zsy2; D = m_zsy1; E = m_zey1;
		}
		if(B == E)
			return;
		By = (B - m_by[nKind]) / m_ay[nKind];
		Cy = (C - m_by[nKind]) / m_ay[nKind];
		Dy = (D - m_by[nKind]) / m_ay[nKind];
		Ey = (E - m_by[nKind]) / m_ay[nKind];
		a = (Cy - Dy) / (B - E);
		b = Cy - a * B;
		Ay = a * A + b;
		Fy = a * F + b;
		ZoomYSub(nKind, Ay, Fy);
	}

	//脈波表示範囲が適切かチェックする
	public void CheckWavePos()
	{
		int		nSamplingRate;
		if(m_nSamplingRate != 0)
			nSamplingRate = m_nSamplingRate;
		else
			nSamplingRate = 200;
		if(300.0F < m_fDspRawData)
			m_fDspRawData = 300.0F;
		if(m_fDspRawData < 1.0F)
			m_fDspRawData = 1.0F;
	}

	//脈波縦軸範囲が適切かチェックする
	public void CheckWaveScale()
	{
		float		nDummy;
		if(m_fWaveMax < m_fWaveMin)
		{
			nDummy = m_fWaveMax;
			m_fWaveMax = m_fWaveMin;
			m_fWaveMin = nDummy;
		}
		nDummy = m_fWaveMax - m_fWaveMin;
		if(nDummy < 10)
		{
			m_fWaveMin -= 5.0F;
			m_fWaveMax += 5.0F;
		}
		if(m_fWaveMin < -100000.0F)
			m_fWaveMin = -100000.0F;
		if(100000.0F <= m_fWaveMax)
			m_fWaveMax = 100000.0F;
	}
	//結果グラフの縦軸が適切かチェック
	public void CheckCalcScale()
	{
		float	fDummy;
		if(m_fCalcMax < m_fCalcMin)
		{
			fDummy = m_fCalcMax;
			m_fCalcMax = m_fCalcMin;
			m_fCalcMin = fDummy;
		}
		fDummy = m_fCalcMax - m_fCalcMin;
		if(fDummy < 0.01F)
		{
			m_fCalcMin -= 0.01F;
			m_fCalcMax += 0.01F;
		}
		if(m_fCalcMin < -100.0F)
			m_fCalcMin = -100.0F;
		if(2000.0F <= m_fCalcMax)
			m_fCalcMax = 2000.0F;
	}
	//結果グラフの右側の縦軸が適切かチェック
	public void CheckPulseScale()
	{
		float	fDummy;
		if(m_fPulseMax < m_fPulseMin)
		{
			fDummy = m_fPulseMax;
			m_fPulseMax = m_fPulseMin;
			m_fPulseMin = fDummy;
		}
		fDummy = m_fPulseMax - m_fPulseMin;
		if(fDummy < 10.0F)
		{
			m_fPulseMin -= 10.0F;
			m_fPulseMax += 10.0F;
		}
		if(m_fPulseMin < -10000.0F)
			m_fPulseMin = -10000.0F;
		if(10000.0F <= m_fPulseMax)
			m_fPulseMax = 10000.0F;
	}
	//二本目の指がタッチされた場合の処理
	public void onMultiDown()
	{
		int		nKind;
		nKind = CheckPoint();
		if(nKind != -1)
		{
			m_bZoomFlg = true;
			m_nKind = nKind;
		}
	}

	//最後の指が離れた時の処理
	public void TouchShift()
	{
		int	nKind;
		nKind = CheckPoint();
		if(nKind == -1)
			return;
		if(Math.abs(m_zsx1 - m_zex1) < 20.0 && Math.abs(m_zsy1 - m_zey1) < 20.0)
		{	//タップして、デフォルトの表示

			float	fTime;
			if(nKind == 0 || nKind == 2)
			{
				if(m_nMeasMode == 0) {
//					Toast.makeText(m_ma, m_ma.getString(R.string.cannot_input_comment), Toast.LENGTH_LONG).show();
					Toast toast = Toast.makeText(m_ma, m_ma.getString(R.string.cannot_input_comment), Toast.LENGTH_SHORT);
					toast.show();
					toast.setGravity(Gravity.TOP, 50, 110);
				}

				else if(m_nNoOfMarkerAlloc <= m_nNoOfMarker)
					m_ma.MessageBox(m_ma.getString(R.string.Cannot_Entory_Marker));
				else
				{
					if(nKind == 2)
						nKind = 1;
					if(m_ax[nKind] == 0.0F)
						return;
					fTime = (float)(((double)m_zex1 - m_bx[nKind]) / m_ax[nKind]);
					m_fMarkerTime = fTime;
					if(0 <= m_fMarkerTime)
						m_ma.showInputComment();
				}
			}
			else if(nKind == 3 || nKind == 4)
				ShiftScrollBar(nKind, -1.0F, m_zex1);
		}
		else
		{
			float	nSubX, nSubY;
			nSubX = Math.abs(m_zsx1 - m_zex1);
			nSubY = Math.abs(m_zsy1 - m_zey1);
			if(nKind == 3 || nKind == 4)
				ShiftScrollBar(nKind, m_zsx1, m_zex1);
			else if(nSubY < nSubX)
				ShiftX(nKind);
			else
				ShiftY(nKind);
		}
		m_bDrawFlg = true;
	}

	//縦方向に指を動かしたときの処理
	public void ShiftY(int nKind)
	{
		double	A, B, C, D;
		double	Ay, By, Cy, Dy;
		double	a, b;
		int		nKind2;
		nKind2 = (nKind == 2)? 1 : nKind;

		if(nKind < 0 || 2 < nKind)
			return;

		if(0 == m_ay[nKind])
			return;
		A = m_sy[nKind2]; D = m_ey[nKind2];
		B = m_zsy1; C = m_zey1;
		By = (B - m_by[nKind]) / m_ay[nKind];
		Cy = (C - m_by[nKind]) / m_ay[nKind];
		a = m_ay[nKind];
		b = C - a * By;
		Ay = (A - b) / a;
		Dy = (D - b) / a;
		ZoomYSub(nKind, Ay, Dy);
	}

	//横方向に指を動かしたときの処理
	public void ShiftX(int nKind)
	{
		double	A, B, C, D;
		double	Ax, Bx, Cx, Dx, Axo, Dxo;
		double	a, b;
		if(nKind == 2)
			nKind = 1;
		if(nKind < 0 || 1 < nKind)
			return;

		if(0 == m_ax[nKind])
			return;
		A = m_sx[nKind];
		D = m_ex[nKind];
		B = m_zsx1;
		C = m_zex1;
		Axo = (A - m_bx[nKind]) / m_ax[nKind];
		Dxo = (D - m_bx[nKind]) / m_ax[nKind];
		Cx = (C - m_bx[nKind]) / m_ax[nKind];
		Bx = (B - m_bx[nKind]) / m_ax[nKind];
		a = m_ax[nKind];
		b = C - a * Bx;
		Ax = (A - b) / a;
		Dx = (D - b) / a;
		ZoomXSub(nKind, Ax, Dx);
	}

	//スクロールバーを動かしたときの処理
	public void ShiftScrollBar(int nKind, float sx, float ex)
	{
		float	fStaPos, fEndPos, fLastPos;
		int		nBarNo = 0;
		if(nKind == 4)
			nBarNo = 1;
		if(sx < 0)
			fStaPos = m_ScrollBar[nBarNo].ShiftPointScroll(ex);
		else
			fStaPos = m_ScrollBar[nBarNo].ShiftScroll(sx, ex);
		if(fStaPos < 0)
			fStaPos = 0;
		if(nKind == 4)//脈波範囲選択グラフ
		{
			m_fDspRawStart = fStaPos;
			m_fDspRawEnd = m_fDspRawStart + m_fDspRawData;
			CheckWaveDspRange();
			m_bDrawFlg = true;
		}
		else	//計算グラフ
		{
			fEndPos = fStaPos + (float)m_nCalcDataRange;
			if(0 < m_nLastPos)
			{
				fLastPos = m_fDataTime[m_nLastPos - 1];
				if(fLastPos < fEndPos)
				{
					m_bLastDspFlg = true;
					fStaPos = fLastPos - (float)m_nCalcDataRange;
				}
				else
					m_bLastDspFlg = false;
				if(fStaPos < 0.0F)
					fStaPos = 0.0F;
				m_nDspStartTime = (int)fStaPos;
			}
		}
	}
	void ReadParamFile()
	{
		Settings sg = GlobalVariable.m_settings;

		FileSelectionDialog dlg = new FileSelectionDialog(sg, sg, "csv; CSV" );
		dlg.show( new File( m_ma.m_ParamFileDir ) );
	}

	//パラメータを読み込む
	void ReadParamFileSub(boolean bMsgFlg, Context con)
	{
		File file = new File(m_ma.m_ParamFileName);
		FileInputStream fin;
		BufferedInputStream in = null;
		byte	buf1[] = new byte[1024];
		byte	buf2[] = new byte[1024];
		String	str2[] = new String[10];
		String	msg;
		int		no, no1;
		m_nReadPos = 0;
		m_nReadSize = 0;
		int		nFlg = 0;
		int		i = 0;
		boolean bSuccess = false;
		try
		{
			fin = new FileInputStream(file);
			in = new BufferedInputStream(fin);


			while(true)
			{
				no = GetLineToFile(in, buf1, buf2);
				if(no == 0)
					break;
				no1 = AnalyzeStr(buf1, no, str2, 10);
				if(no1 == 0)
					break;
				if(i == 0 && !str2[0].equals("NeuromascularMonitor ParamFile"))
					break;
				else {
					ParamAnalyze(str2[0], no, buf1, buf2);
					bSuccess = true;
				}
				i++;
			}
			fin.close();
			if(!bSuccess)
			{
				msg = m_ma.getString(R.string.NotParamFile);
				m_ma.MessageBox(msg, con);
			}
			else
			{
				m_ma.WritePreferences();
				SetGainConst();
				SetFilterFreq();
				if(bMsgFlg) {
					msg = m_ma.getString(R.string.ParamFileReadSuccess);
					m_ma.MessageBox(msg, con);
				}
			}

		}
		catch (Exception e)
		{
			msg = m_ma.getString(R.string.NotParamFile);
			m_ma.MessageBox(msg, con);
		}
		return;

	}
	//一行の文字列から,で区切られた文字列を取り出す　
	int AnalyzeStr(byte buf[], int nLength, String str1[], int nMaxSize)
	{
		int			i, j, k, m;
		byte		c;
		byte		buf2[] = new byte[1024];
		k = 0;
		j = 0;
		for(i = 0; i < nLength; i++)
		{
			c = buf[i];
			buf2[j] = c;
			j++;
			if(c == ',' || i == nLength - 1)
			{
				if(c == ',')
					j--;
				if(0 < j)
				{
					byte buf3[] = new byte[j];
					for(m = 0; m < j; m++)
						buf3[m] = buf2[m];
					String	str = new String(buf3);
					if(k < nMaxSize)
					{
						str1[k] = str;
						k++;
					}
					else
						break;
				}
				j = 0;
			}
		}
		return k;
	}

	//ファイル名から拡張子の部分をカットする
	String CutExtention(String source)
	{
		int	length = source.length();
		int i;
		for(i = length - 1; 0 <= i; i--)
		{
			char	c;
			c = source.charAt(i);
			if(c == '.')
				break;
		}
		String	ret = source;
		if(0 <= i && i < length - 1)
			ret = source.substring(0, i);
		return ret;
	}


	//パラメータをファイルに書き込む
	boolean SaveParamFileSub(String filename)
	{
		String	onlyfilename = GetOnlyFileName(filename);
		String filename2 = CutExtention(onlyfilename) + ".csv";

		String	filepath2, str;
		filepath2 = GetSaveDir() + "/" + filename2;
		File file = new File(filepath2);
		file.getParentFile().mkdirs();

		FileOutputStream fos;
		try
		{
			fos = new FileOutputStream(file, false);

			OutputStreamWriter osw = new OutputStreamWriter(fos, "SJIS");
			BufferedWriter bw = new BufferedWriter(osw);
			str = "NeuromascularMonitor ParamFile\r\n";
			bw.write(str);
			str = GetSmoothingCondition();
			bw.write(str);

			bw.flush();
			bw.close();
			return true;
		}
		catch (Exception e)
		{
			int	err;
			err = 0;
		}
		return false;
	}

	void AutoTwitchStart()
    {
        if(!CheckADStart(true))
            return;
        m_nPulseMode = 6;
        m_nPrevPulseOutTime = 0;
    }

    void OneTwitchPulse()
    {
        if(!CheckADStart(true))
            return;
        SendPulseCommand(m_fCurrentValue, 1, 0, true);
        m_nPulseMode = 5;
        m_ma.DspToastPulse();
    }

    boolean AutoTwitchPulse()	//TWITCH　一秒起き
    {
    	boolean bRet = false;
        long nCurrent = System.currentTimeMillis();
        long	lSum = nCurrent - m_nPrevPulseOutTime;
        if((long)(m_nTwitchInterval * 1000) < lSum) {
            SendPulseCommand(m_fCurrentValue, 1, 0, true);
            m_ma.DspToastPulse();
            m_nPrevPulseOutTime = nCurrent;
            bRet = true;
        }
        return bRet;
    }

	void AutoDBSStart()
	{
		if(!CheckADStart(true))
			return;
		m_nPulseMode = 10;
		m_nPrevPulseOutTime = 0;
	}

	void OneDBSPulse()
	{
		if(!CheckADStart(true))
			return;
		OutPutDBSPulse1();
        m_nPulseMode = 9;
        m_ma.DspToastPulse();
		m_nPulseMode = 0;
	}

	boolean AutoDBSPulse()	//DBS
	{
		boolean bRet = false;
		long nCurrent = System.currentTimeMillis();
		long	lSum = nCurrent - m_nPrevPulseOutTime;
		if((long)(m_fDBS_1_1_Interval * 1000.0F) < lSum) {
			OutPutDBSPulse1();
            m_ma.DspToastPulse();
			m_nPrevPulseOutTime = nCurrent;
			bRet = false;
		}
		return bRet;
	}

	void OutPutDBSPulse1() {
		int nNoOfPulse = 3;
		int nInterval = (int)m_fDBSStimInterval;
		if ( m_nDBSPattern == 2 )
			nNoOfPulse = 2;
		SendPulseCommand(m_fCurrentValue, nNoOfPulse, nInterval, false);
		m_bDBSSecondPulse = true;
		m_lDBSSecondTime = System.currentTimeMillis() + (long)(m_fDBS_1_2_Interval * 1000.0F) - (long)(nInterval * (nNoOfPulse - 1)) - 50;
	}

	boolean OutPutDBSPulse2()
	{
		int nNoOfPulse = 3;
		int nInterval = (int)m_fDBSStimInterval;
		if(m_nDBSPattern == 1)
			nNoOfPulse = 2;
		SendPulseCommand(m_fCurrentValue, nNoOfPulse, nInterval, false);
		m_bDBSSecondPulse = false;
		return true;
	}

	void AutoTETStart()
	{
		if(!CheckADStart(true))
			return;
		m_nPulseMode = 8;
		m_nPrevPulseOutTime = 0;
	}

	boolean CheckConection()
	{
		if(m_ma.m_nCertifyFlg != 2)
		{
			m_ma.ReConnect(false);
			m_ma.m_lLastPulseLimitTime = 0;		//パルス出力できていませんよ
			return false;
		}
		return true;
	}

	void OneTETPulse()
	{
		if(!CheckADStart(true))
			return;
		OutPutTETPulse(false, m_nTETStimFreq, m_fTETStimTime);
        m_nPulseMode = 7;
        m_ma.DspToastPulse();
		m_nPulseMode = 0;
	}

	boolean AutoTETPulse()	//TET
	{
		boolean bRet = false;
		long nCurrent = System.currentTimeMillis();
		long	lSum = nCurrent - m_nPrevPulseOutTime;
		if((long)(m_fTETTimeLimit * 1000.0F) < lSum) {
			OutPutTETPulse(false, m_nTETStimFreq, m_fTETStimTime);
            m_ma.DspToastPulse();
			m_nPrevPulseOutTime = nCurrent;
			bRet = false;
		}
		return bRet;
	}

    void OutPutTETPulse(boolean bADFlg, int nFreq, float fTETStimTime)
	{
		float	fTime = fTETStimTime;
		int		nInterval = 0;
		if(0 < nFreq)
			nInterval = 1000 / nFreq;	//20msec
		int	nNoOfPulse = 0;
		if(0 < nInterval)
			nNoOfPulse = (int)(fTime * 1000.0F / (float)nInterval);	//250
		SendPulseCommand(m_fCurrentValue, nNoOfPulse, nInterval, bADFlg);
	}


    void AutoPilotStart()
    {
        if(!CheckADStart(true))
            return;
        m_nPulseMode = 13;
        m_nPrevPulseOutTime = 0;
    }


	void AutoPTCStart()
	{
		if(!CheckADStart(true))
			return;
		m_nPulseMode = 12;
		m_nPrevPulseOutTime = 0;
	}

	boolean AutoPTCPulse()	//キャリブレーション　一秒起き
    {
        boolean bRet = false;
        long nCurrent = System.currentTimeMillis();
        long	lSum = nCurrent - m_nPrevPulseOutTime;
        if((int)(m_fPTCAutoInterval * 1000.0F) < lSum) {
            OutputPTCPulse1();
            m_ma.DspToastPulse();
            m_nPrevPulseOutTime = nCurrent;
            bRet = true;
        }
        return bRet;
    }

    void OnePTCPulse()
    {
        if(!CheckADStart(true))
            return;
        OutputPTCPulse1();
        m_nPulseMode = 11;
        m_ma.DspToastPulse();
    }


    void OutputPTCPulse1() {
        int nNoOfPulse = m_nPTCTwitch1Num;
        int nInterval = 1000;
        SendPulseCommand(m_fCurrentValue, nNoOfPulse, nInterval, false);
        m_nPTCPhase = 1;
        m_lPTCPhaseTime = System.currentTimeMillis() + (long)(nNoOfPulse * 1000.0F);
    }

    boolean OutputPTCPulse2()
    {
        OutPutTETPulse(false, m_nPTC_TETStimFreq, m_fPTC_TETStimTime);
        m_nPTCPhase = 2;
        m_lPTCPhaseTime = System.currentTimeMillis() + (long)((m_fPTC_TETStimTime + 3.0F) * 1000.0F);
        return true;
    }

    boolean OutputPTCPulse3() {
        int nNoOfPulse = m_nPTCTwitch2Num;
        int nInterval = 1000;
        SendPulseCommand(m_fCurrentValue, nNoOfPulse, nInterval, true);
        m_nPTCPhase = 3;
        m_lPTCPhaseTime = System.currentTimeMillis() + (long)((nNoOfPulse - 1) * 1000.0F);
        return true;
    }

    void AutoTOFStart()
	{
		if(!CheckADStart(true))
			return;
		m_nPulseMode = 4;
		m_nPrevPulseOutTime = 0;
	}

	boolean AutoTOFPulse()	//キャリブレーション　一秒起き
	{
		boolean bRet = false;
		long nCurrent = System.currentTimeMillis();
		long	lSum = nCurrent - m_nPrevPulseOutTime;
		if((int)(m_fTOFInterval * 1000.0F) < lSum) {
			SendPulseCommand(m_fCurrentValue, 4, (int)(m_fTOFStimInterval * 1000.0F), true);
            m_ma.DspToastPulse();
			m_nPrevPulseOutTime = nCurrent;
			bRet = true;
		}
		return bRet;
	}

	void OneTOFPulse()
	{
		if(!CheckADStart(true))
			return;
		SendPulseCommand(m_fCurrentValue, 4, (int)(m_fTOFStimInterval * 1000.0F), true);
		m_nPulseMode = 3;
        m_ma.DspToastPulse();
	}

	void AutoCalibrationStart()
	{
		if(!CheckADStart(true))
			return;
        m_nPulseMode = 2;
        AutoCalibrationStartSub();
	}

	void AutoCalibrationStartSub()
    {
        m_nAutoCalibrationCount = 0;
        m_fControlRawValue = 0.0F;
        m_nPrevPulseOutTime = 0;
        m_fCalPeakAve = 0.0F;	//CAL ピークの移動平均値　移動平均は３固定
        m_nCalCount = 0;	//キャリブレーションのパルス送信　回数
        m_bCalFlg = false;	//キャリブレーション　山が見つかったら　ＴＲＵＥ　　見つからない場合はＦＡＬＳＥ
        m_fCalResultCurrentValue = 0.0F;	//キャリブレーション 結果の電流値
    }


	boolean AutoCalibration()	//キャリブレーション　一秒起き
	{
		boolean bRet = false;
		long nCurrent = System.currentTimeMillis();
		long	lSum = nCurrent - m_nPrevPulseOutTime;
		if(m_nCalibrationInterval < lSum) {
			float fValue = m_fCalibrationStart + m_fCalibrationStep * m_nAutoCalibrationCount;
			if ( fValue <= 60.0 ) {
				SendPulseCommand(fValue, 1, 0, true);
                m_ma.DspToastPulse();
				m_fCurrentValue = fValue;
				bRet = true;
			}
			else {
				if(m_bCalFlg) {
					m_fCurrentValue = m_fCalResultCurrentValue;
					m_ma.WritePreferencesCtrl();
					m_ma.WritePreferencesGain();
				}
				m_nPulseMode = 0;
			}
			m_nAutoCalibrationCount++;
			m_nPrevPulseOutTime = nCurrent;
		}
		return bRet;
	}

	void PulseCalibration()
	{
		if(!CheckADStart(true))
			return;
		SendPulseCommand(m_fCurrentValue, 1, 0, true);
		m_nPulseMode = 1;
		m_ma.DspToastPulse();
	}

	boolean CheckADStart(boolean bNeadADMeasFlg)
	{
		if(m_nMeasMode == 0 && bNeadADMeasFlg)
		{
			String	msg = m_ma.getString(R.string.StartADMeas);
			m_ma.MessageBox(msg);
			return false;
		}
		else if(m_nMeasMode == 1 && !bNeadADMeasFlg)
		{
			String	msg = m_ma.getString(R.string.StopADMeas);
			m_ma.MessageBox(msg);
			return false;
		}
		return true;
	}

	public void SendPulseWidthCommand()
	{
		if(!m_bDebugModeFlg)
//		if(m_bDebugModeFlg)   //adtex
			m_ma.m_BLEObj.SendPulseWidth(m_nPulseWidth);
		else
			m_ma.SendPulseWidthUART(m_nPulseWidth);
	}

	public void DAPowerOn(boolean bOnFlg)
	{
		if(!m_bDebugModeFlg)
//		if(m_bDebugModeFlg)   //adtex
			m_ma.m_BLEObj.DAPowerOn(bOnFlg);
		else
			m_ma.DAPowerOnUART(bOnFlg);
	}

	public void PowerOff()
	{
		if(!m_bDebugModeFlg)
//		if(m_bDebugModeFlg)   //adtex
			m_ma.m_BLEObj.PowerOff();
		else
			m_ma.PowerOffUART();
	}

	public float GetCorrectionValueSub(int nFlg, float fSubTime)
	{
		float	fCorrectionValue;
		float	fDummy = m_fNoOfUnblockedACh;
		m_fNoOfUnblockedReceptors = m_fAChReceptor * (1.0F - m_fAChBlockRateX / 100.0F);	//筋弛緩剤投与下でもブロックされていない受容体数(万)

		m_fNoOfUnblockedACh = m_fNoOfUnblockedACh - m_fNoOfBlocksDueStim + m_fAChRecovery;	//神経刺激後の非ブロックACh受容体数(万)

		m_fNoOfBlocksDueStim = m_fBlockOneStim * nFlg;	//神経刺激によるACh受容体ブロック数(万)
		if(m_fRecoveryHalfLife <= 0.0F)
			m_fRecoveryHalfLife = 1.0F;
		m_fAChRecoveryRate = 1.0F - (float)Math.exp(-0.693F * fSubTime / m_fRecoveryHalfLife);	//神経刺激によるACh受容体ブロック回復率

		m_fAChRecovery = (m_fNoOfUnblockedReceptors - fDummy) * m_fAChRecoveryRate;		//ACh受容体ブロック回復量
		if(m_fPTP_HalfLife <= 0.0F)
			m_fPTP_HalfLife = 1.0F;
		m_fPTP_Stim = m_fPTP_Stim * (float)Math.exp(-0.693 * fSubTime / m_fPTP_HalfLife) + m_fPTP * nFlg;	//神経刺激によるACh増加量(PTP)万
		m_fNoOfUnblockedReceptors2 = m_fNoOfUnblockedACh + m_fPTP_Stim;	//増加量を加えた非ブロックACh受容体数(万)

//		float	fMinimumSensitivity = m_fControlValue * m_fDetectionThreshold / 100.0F;
		float	fMinimumSensitivity = 60.0F * m_fDetectionThreshold / 100.0F;
		if(60.0F < m_fNoOfUnblockedReceptors2)
			fCorrectionValue = 60.0F;
		else if(m_fNoOfUnblockedReceptors2 < fMinimumSensitivity)
			fCorrectionValue = 0.0F;
		else
			fCorrectionValue = m_fNoOfUnblockedReceptors2;

		int		nPulseFlg = 0;
		if(m_bCorrectPulseMode)
			nPulseFlg = 1;
		if(m_bCorrectionLogFlg) {
			String msg;        // A   B   C   D    E    F    G    H    I    J    K    L    M    N    O    P      Q
			msg = String.format("%.2f,%d,%.0f,%.1f,%.1f,%.0f,%.0f,%.0f,%.1f,%.2f,%.2f,%.2f,%.0f,%.2f,%.0f,%.0f,%.0f\r\n",
					(float) m_nCorrectionAllCount * fSubTime,    //A
					nFlg,    //B
					m_fAChReceptor,    //C
					m_fAChBlockRate,    //D
					m_fNoOfUnblockedReceptors,    //E
					m_fBlockOneStim,    //F
					m_fNoOfBlocksDueStim,    //G
					m_fNoOfUnblockedACh,    //H
					m_fRecoveryHalfLife,    //I
					m_fAChRecoveryRate * 100.0F,    //J
					m_fAChRecovery,        //K
					m_fPTP,                //L
					m_fPTP_HalfLife,        //M
					m_fPTP_Stim,            //N
					m_fNoOfUnblockedReceptors2,    //O
					m_fDetectionThreshold,        //P
					fCorrectionValue);            //Q
			SaveCorrectLog(msg);
            m_nCorrectionAllCount++;
		}
        m_nCorrectionCount++;
		return fCorrectionValue;

	}

	public float GetCorrectionValue(boolean bAveFlg, boolean bFirstFlg)
	{	//bAveFlg = true平均値を出力　TET　　false 最後の計算値を出力　その他
		//bFirstFlg 最初の一発目なら true それ以降なら　false
		if(m_bCorrectOutFlg)	//二重パルスを防ぐため
			return 0.0F;
		float	fCorrectionValue = 0.0F;
		long	lCurrentTime = System.currentTimeMillis();
		long 	lSub = (int)(lCurrentTime - m_lMeasStartTime);
		float	fAllSubTime = (float)lSub / 1000.0F;
		float	fSubTime = 0.02F;
        int		nAllLoop, nLoop;
        nAllLoop = (int)(fAllSubTime / fSubTime);
		nLoop = nAllLoop - m_nCorrectionCount;
        if(nLoop == 0)
            return 0.0F;
		int		nSubTimeMSec = (int)(fSubTime * 1000.0F);
		int		i;
		int		nNextTime = m_nCorrectOutputCount * m_nCorrectInterval;
		int		nCurrentTime;

		// 測定中
		if (m_nMeasMode == 1) {
			// 300000[ms](5分)ごとに1回
			if (lSub / 300000 > m_ma.nstep){
				// MainActivityのnstepが10未満なら
				if (m_ma.nstep < 10){
					m_fAChBlockRateX = m_adtex_AChBlockRate[m_ma.nstep];
					m_ma.nstep++;
				}
			}
		}
		else
		{
			m_ma.nstep = 0;
			m_fAChBlockRateX = m_fAChBlockRate;
		}

		if(bFirstFlg)
			nLoop--;
		for (i = 0; i < nLoop; i++) {
			if(!bFirstFlg){
				nCurrentTime = nSubTimeMSec * (m_nCorrectionCount - m_nCorrectionStartCount);
				if(!bAveFlg) {
					if (!m_bCorrectOutFlg && nNextTime <= nCurrentTime ) {
						m_bCorrectOutFlg = true;
						m_fCorrectionValue = GetCorrectionValueSub(1, fSubTime);
					} else
						GetCorrectionValueSub(0, fSubTime);
				}
				else
				{
					m_fCorrectionSum += GetCorrectionValueSub(1, fSubTime);
					m_nCorrectionAveCount++;
					if (!m_bCorrectOutFlg && nNextTime <= nCurrentTime && 0 < m_nCorrectionAveCount) {
                        m_bCorrectOutFlg = true;
						m_fCorrectionValue = m_fCorrectionSum / (float)m_nCorrectionAveCount;
						m_fCorrectionSum = 0.0F;
						m_nCorrectionAveCount = 0;
					}
				}
			}
			else
				GetCorrectionValueSub(0, fSubTime);
		}
		if(bFirstFlg && m_bCorrectPulseMode) {
			m_fCorrectionValue = GetCorrectionValueSub(1, fSubTime);
			m_fCorrectionSum = 0.0F;
			m_nCorrectionAveCount = 0;
			m_nCorrectionStartCount = m_nCorrectionCount;
			m_bCorrectOutFlg = true;
		}
		return m_fCorrectionValue;
	}


/*------*/
	public void SaveCorrectLog(String msg)
	{
		String	filepath2, str, str2;
		filepath2 = GetSaveDir() + "/CorrectLog.csv";
		File file = new File(filepath2);
		if(msg.equals("clear"))
		{
			if (file.exists())
				file.delete();
			return;
		}
		file.getParentFile().mkdirs();
		FileOutputStream fos;
		try
		{
			fos = new FileOutputStream(file, true);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "SJIS");
			BufferedWriter bw = new BufferedWriter(osw);
			bw.write(msg);
			bw.flush();
			bw.close();
		}
		catch (Exception e)
		{
			int	err;
			err = 0;
		}
	}
/*------------*/

	public void SendPulseCommand(float fValue, int nNoOfPulse, int nInterval, boolean bADFlg) {    //fValue mA      nInterval   msec		bADFlg : true AD検出必要　 false AD検出不要
		if ( nNoOfPulse <= 0 )
			return;
		if ( m_nCorrectionKind == 1 )
			SendCorrectPulseCommand(nNoOfPulse, nInterval, bADFlg);
		else
			SendPulseCommandSub(fValue, nNoOfPulse, nInterval, bADFlg, true);
	}

	public void SendPulseCommandSub(float fValue, int nNoOfPulse, int nInterval, boolean bADFlg, boolean bNormalFlg)
	{	//fValue mA      nInterval   msec		bADFlg : true AD検出必要　 false AD検出不要
		// bNormalFlg   筋弛緩補正の一発目以外の時はfalse それ以外の普通のときは　true   パルスの検出位置を調べるため
		int nValue = (int)(fValue + 0.5F);
		int		nNoOfPulse2 = nNoOfPulse - 1;
		int		nInterval2 = nInterval / 10 - 1;
		nValue = m_ma.CheckParamRange(nValue, 0, 60);
		nNoOfPulse2 = m_ma.CheckParamRange(nNoOfPulse2, 0, 4095);
		nInterval2 = m_ma.CheckParamRange(nInterval2, 0, 255);
		if(0 < nValue) {
            if ( !m_bDebugModeFlg )
//			  if(m_bDebugModeFlg)   //adtex
                m_ma.m_BLEObj.SendPulseCommand(nValue, nNoOfPulse2, nInterval2);
            else
                m_ma.SendPulseCommandUART(nValue, nNoOfPulse2, nInterval2);
        }
		m_nOutputPulseValue = nValue;
		if(bNormalFlg) {
			if ( bADFlg )
				m_bPulseOutFlg = true;
			else
				m_bPulseOutFlg = false;
			m_nPulseOutNo = 0;
			m_lLastPulseTime = System.currentTimeMillis();        //最後にパルスを出力した時間    msec   中止時の中止コマンド出力判定　に用いる
			m_lLastPulseOutTime = (nNoOfPulse - 1) * nInterval;    //最後に出力したパルスの出力時間 msec
		}
	}

	public void DAStop()
	{
		m_nPulseMode = 0;
		m_nOldPulseMode = -1;
		m_nPTCPhase = 0;
		m_lPTCPhaseTime = 0;
		SendPulseStopCommand();
	}

	public void SendPulseStopCommand()
	{	//fValue mA      nInterval   msec		bADFlg : true AD必要　 false ADだめ
		long		lTime = System.currentTimeMillis();
		long		lTime2 = m_lLastPulseTime + m_lLastPulseOutTime;	//パルス出力中なら停止コマンド出力
		if(lTime < lTime2) {
			if ( !m_bDebugModeFlg )
//			if(m_bDebugModeFlg)   //adtex
				m_ma.m_BLEObj.SendPulseStopCommand();
			else
				m_ma.SendPulseStopCommandUART();
		}
	}

	public void SendCorrectPulseCommand(int nNoOfPulse, int nInterval, boolean bADFlg)
	{
		if(nNoOfPulse == 1 || nInterval == 0)
		{
			float	fValue;
			fValue = GetCorrectionValue(false, true);
			SendPulseCommandSub(fValue, 1, 0, bADFlg, true);
		}
		else {
			m_bCorrectPulseMode = true;
			m_nCorrectNoOfPulse = nNoOfPulse;
			m_nCorrectInterval = nInterval;
			m_bCorrectADFlg = bADFlg;
			m_nCorrectOutputCount = 0;
			m_lCorrectOutputTime = m_nCorrectNoOfPulse * m_nCorrectInterval;
			m_fCorrectionSum = 0.0F;
			m_nCorrectionAveCount = 0;
			m_bCorrectOutFlg = false;
		}
	}

	public void OutputCorrectPulse()	//10msec毎に呼ばれる
	{
		if(!m_bCorrectPulseMode) {
			GetCorrectionValue(false, true);
			return;
		}
		boolean	bFirstFlg = false;
		boolean bNormalFlg = false;
		if(m_nCorrectOutputCount == 0) {
			bNormalFlg = true;        //パルスの検出位置を調べるため
			bFirstFlg = true;
		}
		if(100 <= m_nCorrectInterval)	//インターバル 100msec以上
		{
			GetCorrectionValue(false, bFirstFlg);
			if(m_bCorrectOutFlg)
			{
				m_nCorrectOutputCount++;
				SendPulseCommandSub(m_fCorrectionValue, 1, 0, m_bCorrectADFlg, bNormalFlg);
				if(m_nCorrectNoOfPulse <= m_nCorrectOutputCount) {
					m_bCorrectPulseMode = false;
					m_nCorrectOutputCount = 0;
				}
				m_bCorrectOutFlg = false;
			}
		}
		else {	//インターバル 100msec 以下 TET
			GetCorrectionValue(true, bFirstFlg);    //最初の一発は直近の値　その他は平均値
			if(m_bCorrectOutFlg) {
				//100msec 間の出力数
				if ( m_nCorrectInterval == 0 )
					m_nCorrectInterval = 20;
				int nNoOfOutput = 100 / m_nCorrectInterval;    //100msec に出力するパルス数
				if ( m_nCorrectNoOfPulse < nNoOfOutput )
					nNoOfOutput = m_nCorrectNoOfPulse;
				m_nCorrectOutputCount += nNoOfOutput;
				SendPulseCommandSub(m_fCorrectionValue, nNoOfOutput, m_nCorrectInterval, m_bCorrectADFlg, bNormalFlg);
				if ( m_nCorrectNoOfPulse <= m_nCorrectOutputCount) {
					m_bCorrectPulseMode = false;
					m_nCorrectOutputCount = 0;
				}
				m_bCorrectOutFlg = false;
			}
		}
	}
}
