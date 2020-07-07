package com.adtex.NeuromusclarMonitor;

public class IIRFilterObj {

	// IIRFilterQCObj.cpp: CIIRFilterQCObj クラスのインプリメンテーション
	//
	//////////////////////////////////////////////////////////////////////
	double m_FsampP;
	double m_a0[];
	double m_a1[];
	double m_a2[];
	double m_b1[];
	double m_b2[];

	int m_FilType;
	int m_OrderN;
	double m_AC;
	double m_fc;
	double m_fc2;
	double m_Pi;
	double m_PD1, m_PD0;
	double m_SN2, m_SN1, m_SN0, m_SD2, m_SD1, m_SD0, m_SH;
	double m_TN2, m_TN1, m_TN0, m_TD2, m_TD1, m_TD0, m_TH;
	double m_FC1;
	double m_FC2;
	double m_fcp1;
	double m_fcp2;
	double	m_x1[], m_x2[], m_x3[];
	float	m_pfWork[];
	int		m_nNoOfAlloc;
	int		m_BiquadN;
	float	m_fIn[], m_fOut[];

	public void InitObj()
	{
		m_fIn = new float[2];
		m_fOut = new float[2];

		m_x1 = new double[3];
		m_x2 = new double[3];
		m_x3 = new double[3];
		m_a0 = new double[8];
		m_a1 = new double[8];
		m_a2 = new double[8];
		m_b1 = new double[8];
		m_b2 = new double[8];
		int i;
		for(i = 0; i < 2; i++)
			m_fIn[i] = m_fOut[i] = 0.0F;
		for (i = 0; i < 8; i++) {
			m_a0[i] = 1.0;
			m_a1[i] = 0.0;
			m_a2[i] = 0.0;
			m_b1[i] = 0.0;
			m_b2[i] = 0.0;
		}
		m_Pi = 3.14159265358979323846264;
		m_FilType = 0;
		m_FC1 = 2000;
		m_FC2 = 5000;
		m_BiquadN = 0;
		m_nNoOfAlloc = 1000;
		m_pfWork = new float[m_nNoOfAlloc];
	}

	void InitFilter()
	{
		int		i;
		for(i = 0; i < 3; i++)
			m_x1[i] = m_x2[i] = m_x3[i] = 0.0;
		for(i = 0; i < 2; i++)
			m_fIn[i] = m_fOut[i] = 0.0F;
	}
/*----
	void CutSignalArray(int nNoOfData, float pfDataIn[], float pfDataOut[])
	{
		int		i, filn;
		if(m_nNoOfAlloc < nNoOfData)
		{
			m_pfWork = null;
			m_nNoOfAlloc = nNoOfData;
			m_pfWork = new float[m_nNoOfAlloc];
		}
		for( filn = 0; filn < m_BiquadN; filn++ )
		{
			if(filn == 0)
			{
				for(i = 0; i < nNoOfData; i++)
				{
					m_pfWork[i] = pfDataIn[i];
					pfDataOut[i] = 0.0F;
				}
			}
			else
			{
				for(i = 0; i < nNoOfData; i++)
				{
					m_pfWork[i] = pfDataOut[i];
					pfDataOut[i] = 0.0F;
				}
			}
			for(i = 2; i < nNoOfData; i++)
			{
				pfDataOut[i] = (float)(m_a2[filn] * m_pfWork[i - 2]
						+ m_a1[filn] * m_pfWork[i - 1]
						+ m_a0[filn] * m_pfWork[i - 0]
						- m_b2[filn] * pfDataOut[i - 2]
						- m_b1[filn] * pfDataOut[i - 1]);
			}
		}
	}

--------------*/
	void SetParamSub(float fFreq1, float fFreq2, float fSamplingFreq, double dLevel_, double dCutLevel)
	{
		float	dummy;
		float	fHalfFreq = fSamplingFreq / 2.0F;
		int	FilterType = -1;
		int		i;
		for(i = 0; i < 3; i++)
			m_x1[i] = m_x2[i] = m_x3[i] = 0.0;
		for(i = 0; i < 2; i++)
			m_a0[i] = m_a1[i] = m_a2[i] = m_b2[i] = m_b1[i] = 0.0F;
		for(i = 0; i < 2; i++)
			m_fIn[i] = m_fOut[i] = 0.0F;
		m_BiquadN = 1;
		if(fFreq1 == fFreq2)
			return;
		if(0.0 < fFreq1 && fFreq1 < fHalfFreq && 0.0 < fFreq2 && fFreq2 < fHalfFreq)
		{
			if(fFreq1 < fFreq2)			//バンドカット
				FilterType = 3;
			else if(fFreq2 < fFreq1)	//バンドパス
				FilterType = 2;
		}
		if(fFreq2 < fFreq1)
		{
			dummy = fFreq1;
			fFreq1 = fFreq2;
			fFreq2 = dummy;
		}
		if(fFreq1 <= 0.0 && 0.0 < fFreq2 && fFreq2 < fHalfFreq)	//LowPass
		{
			FilterType = 0;
			IirDesign(FilterType, 2, fSamplingFreq, fFreq2, fFreq2);
			return;
		}
		else if(fHalfFreq <= fFreq2 && 0.0 < fFreq1 && fFreq1 < fHalfFreq)	//HiPass
		{
			FilterType = 1;
			IirDesign(FilterType, 2, fSamplingFreq, fFreq1, fFreq1);
			return;
		}
		if(FilterType == -1)
		{
			if(fFreq1 == 0.0 && fHalfFreq < fFreq2)
				m_a0[0] = m_a0[1] = 1.0F;
			return;
		}
		IirDesign(FilterType, 2, fSamplingFreq, fFreq1, fFreq2);
	}

	void IirDesign(int filterType, int i, double Fs, double Fc1, double Fc2)
	{
		m_FsampP = Fs;

		m_fc = Fc1;
		m_fc2 = Fc2;
		m_AC = 3.0;
		m_FilType = filterType;
		m_OrderN = i;

		BasicFilCal();
		PreWarp();
		FreqTrans();
		SZTrans();
	}

	//////////////////////////////////////////////
	// Basic Analog LPF
	void BasicFilCal() // Butterworth
	{
		int i;
		double d0, bi, ai;
		double eps, dDummy;
		if(m_OrderN == 0)
			m_OrderN = 1;
		dDummy = Math.pow(10.0, m_AC / 10.0) - 1.0;
		if(0.0 <= dDummy)
			eps = Math.sqrt(dDummy);
		else
			eps = 1.0;
		d0 = Math.pow(eps, (-1.0 / m_OrderN));
		bi = d0 * d0;
		i = 1;
		ai = 2.0 * d0 * Math.sin( i * m_Pi / (2.0 * m_OrderN) );

		if( m_OrderN == 1 ) {
			m_PD1 = 1.0; m_PD0 = d0;
		} else {
			m_PD1 = ai; m_PD0 = bi;
		}
	}

	//////////////////////////////////////////////
	// Pre Warping
	void	PreWarp()
	{
		double tmp_wc1, tmp_wc2;
		if(m_FsampP == 0.0)
			m_FsampP = 44100;
		if(m_Pi == 0.0)
			m_Pi = 3.1415926535;
		m_FC1 = (float)m_fc;
		tmp_wc1 = (2.0 * m_FsampP) * Math.tan(2.0 * m_Pi * m_FC1 / (2.0 * m_FsampP));
		m_fcp1 = (float)(tmp_wc1 / (2.0 * m_Pi));

		m_FC2 = (float)m_fc2;
		tmp_wc2 = (2.0 * m_FsampP) * Math.tan(2.0 * m_Pi * m_FC2 / (2.0 * m_FsampP));
		m_fcp2 = (float)(tmp_wc2 / (2.0 * m_Pi));

	}

	//////////////////////////////////////////////
	// Frequency Transformation
	void FreqTrans()
	{
		double wc1, wc2;
		double wb, w0;
		double xx, dd, ww, zz, uu, vv;
		double	dDummy;
		wc1 = 2.0 * m_Pi * m_fcp1;
		wc2 = 2.0 * m_Pi * m_fcp2;
		wb = wc2 - wc1;
		dDummy = wc1 * wc2;
		if(0.0 <= dDummy)
			w0 = Math.sqrt(dDummy);
		else
			w0 = 1.0;
		if(w0 == 0.0)
			w0 = 1.0;
		if(m_PD0 == 0.0)
			m_PD0 = 1.0;
		if( m_FilType == 0 ) { // LPF
			if( m_OrderN == 1 ) {
				m_SH  = m_PD0 * wc1;
				m_SN2 = 0.0; m_SN1 = 0.0; m_SN0 = 1.0;
				m_SD2 = 0.0; m_SD1 = 1.0; m_SD0 = m_PD0 * wc1;
			} else {
				m_SH  = m_PD0 * wc1 * wc1;
				m_SN2 = 0.0; m_SN1 = 0.0; m_SN0 = 1.0;
				m_SD2 = 1.0; m_SD1 = m_PD1 * wc1; m_SD0 = m_PD0 * wc1 * wc1;
			}
		} else if( m_FilType == 1 ) { // HPF
			if( m_OrderN == 1 ) {
				m_SH  = 1.0;
				m_SN2 = 0.0; m_SN1 = 1.0; m_SN0 = 0.0;
				m_SD2 = 0.0; m_SD1 = 1.0; m_SD0 = wc1 / m_PD0;
			} else {
				m_SH  = 1.0;
				m_SN2 = 1.0; m_SN1 = 0.0; m_SN0 = 0.0;
				m_SD2 = 1.0; m_SD1 = m_PD1 * wc1 / m_PD0; m_SD0 = wc1 * wc1 / m_PD0;
			}
		} else if( m_FilType == 2 ) { // BPF
			xx = wb / w0;
			dd = m_PD1 * xx / 2.0;
			ww = xx * Math.sqrt(4.0 * m_PD0 - m_PD1 * m_PD1 ) / 2.0;
			zz = 4.0 - dd * dd + ww * ww;
			uu = Math.sqrt(zz / 2 + Math.sqrt(zz * zz / 4.0 + dd * dd * ww * ww));
			if(uu == 0.0)
				uu = 1.0;
			vv = dd * ww / uu;
			if( m_OrderN == 1 ) {
				m_SH  = m_PD0 * wb;
				m_SN2 = 0.0; m_SN1 = 1.0; m_SN0 = 0.0;
				m_SD2 = 1.0; m_SD1 = m_PD0 * wb; m_SD0 = w0 * w0;
			} else {
				m_SH = Math.sqrt(wb * wb * m_PD0);
				m_SN2 = 0.0; m_SN1 = 1.0; m_SN0 = 0.0;
				m_SD2 = 1.0; m_SD1 = w0 * (dd + vv);
				m_SD0 = w0 * w0 * ((dd + vv) * (dd + vv)
					+ (ww + uu) * (ww + uu)) / 4.0;

				m_TH  = Math.sqrt(wb * wb * m_PD0);
				m_TN2 = 0.0; m_TN1 = 1.0; m_TN0 = 0.0;
				m_TD2 = 1.0; m_TD1 = w0 * (dd - vv);
				m_TD0 = w0 * w0 * ((dd - vv) * (dd - vv)
					+ (ww - uu) * (ww - uu)) / 4.0;
			}
		} else if( m_FilType == 3 ) { // BRF
			xx = wb / w0;
			dd = (m_PD1 / m_PD0) * xx / 2.0;
			ww = xx * Math.sqrt(4.0 * (1.0 / m_PD0) - (m_PD1 / m_PD0) * (m_PD1 / m_PD0) ) / 2.0;
			zz = 4.0 - dd * dd + ww * ww;
			uu = Math.sqrt(zz / 2 + Math.sqrt(zz * zz / 4.0 + dd * dd * ww * ww));
			if(uu == 0.0)
				uu = 1.0;
			vv = dd * ww / uu;
			if( m_OrderN == 1 ) {
				m_SH  = 1.0;
				m_SN2 = 1.0; m_SN1 = 0.0; m_SN0 = w0 * w0;
				m_SD2 = 1.0; m_SD1 = wb / m_PD0; m_SD0 = w0 * w0;
			} else {
				m_SH  = 1.0;
				m_SN2 = 1.0; m_SN1 = 0.0; m_SN0 = w0 * w0;
				m_SD2 = 1.0; m_SD1 = w0 * (dd + vv);
				m_SD0 = w0 * w0 * ((dd + vv) * (dd + vv)
					+ (ww + uu) * (ww + uu)) / 4.0;
				m_TH  = 1.0;
				m_TN2 = 1.0; m_TN1 = 0.0; m_TN0 = w0 * w0;
				m_TD2 = 1.0; m_TD1 = w0 * (dd - vv);
				m_TD0 = w0 * w0 * ((dd - vv) * (dd - vv)
					+ (ww - uu) * (ww - uu)) / 4.0;
			}
		}
	}

	//////////////////////////////////////////////
	// s-z Transformation
	void SZTrans()
	{
		double alpha, beta, gg;
		int i;
		for (i = 0; i < 8; i++) {
			m_a0[i] = 1.0; m_a1[i] = 0.0; m_a2[i] = 0.0; m_b1[i] = 0.0; m_b2[i] = 0.0;
		}
		if(m_FsampP == 0.0)
			m_FsampP = 44100;
		alpha = 2.0 * m_FsampP;
		m_BiquadN = 1;
		if(m_SD0 == 0.0)
			m_SD0 = 1.0;
		if( m_FilType == 0 || m_FilType == 1 ) {
			if( m_OrderN == 1 ) {
				gg = 1.0 / (m_SD0 + alpha);
				m_a0[0] = (m_SN1 * alpha + m_SN0) * m_SH * gg;
				m_a1[0] = (m_SN0 - m_SN1 * alpha) * m_SH * gg;
				m_a2[0] = 0.0 * m_SH;
				m_b1[0] = (m_SD0 - alpha) / (m_SD0 + alpha);
				m_b2[0] = 0.0;
			} else {
				beta = m_SN2 * alpha * alpha;
				gg  = 1.0 / (alpha * alpha + m_SD1 * alpha + m_SD0);
				m_a0[0] = (beta + m_SN1 * alpha + m_SN0) * m_SH * gg;
				m_a1[0] = 2.0 * (m_SN0 - beta) * m_SH * gg;
				m_a2[0] = (beta - m_SN1 * alpha + m_SN0) * m_SH * gg;
				m_b1[0] = 2.0 * gg * (m_SD0 - alpha * alpha);
				m_b2[0] = gg * (alpha * alpha - m_SD1 * alpha + m_SD0);
			}
		} else {
			beta = m_SN2 * alpha * alpha;
			gg  = 1.0 / (alpha * alpha + m_SD1 * alpha + m_SD0);
			m_a0[0] = (beta + m_SN1 * alpha + m_SN0) * m_SH * gg;
			m_a1[0] = 2.0 * (m_SN0 - beta) * m_SH * gg;
			m_a2[0] = (beta - m_SN1 * alpha + m_SN0) * m_SH * gg;
			m_b1[0] = 2.0 * gg * (m_SD0 - alpha * alpha);
			m_b2[0] = gg * (alpha * alpha - m_SD1 * alpha + m_SD0);
			if( m_OrderN == 2 ) {
				beta = m_TN2 * alpha * alpha;
				gg  = 1.0 / (alpha * alpha + m_TD1 * alpha + m_TD0);
				m_a0[1] = (beta + m_TN1 * alpha + m_TN0) * m_TH * gg;
				m_a1[1] = 2.0 * (m_TN0 - beta) * m_TH * gg;
				m_a2[1] = (beta - m_TN1 * alpha + m_TN0) * m_TH * gg;
				m_b1[1] = 2.0 * gg * (m_TD0 - alpha * alpha);
				m_b2[1] = gg * (alpha * alpha - m_TD1 * alpha + m_TD0);
				m_BiquadN = 2;
			}
		}
	}
/*--------------
	void SaveCoeff()
	{
		FILE *fpo;
		double tmpcoeff;
		int i, filco;
		CString	filenamesv;
		errno_t	error;
		CFileDialog dlgF(FALSE, NULL, "coeff1.txt");
		if( dlgF.DoModal() == IDOK)	{
			//filenameに選択したファイルのフルパスが入る
			filenamesv = dlgF.GetPathName();
			error = fopen_s(&fpo, filenamesv, "w" );
			//ここ以降ファイルにデータを書き込む
			if (error == 0) {
				if( (m_FilType == 2 || m_FilType == 3) && (m_OrderN == 2) ) {
					filco = 2;
				} else {
					filco = 1;
				}
//				m_BiquadN = filco;
				for( i = 0; i < filco; i++ ) {
					tmpcoeff = m_a0[i];
					fprintf(fpo, "%12.7f\n", tmpcoeff);
					tmpcoeff = m_a1[i];
					fprintf(fpo, "%12.7f\n", tmpcoeff);
					tmpcoeff = m_a2[i];
					fprintf(fpo, "%12.7f\n", tmpcoeff);
					tmpcoeff = m_b1[i];
					fprintf(fpo, "%12.7f\n", tmpcoeff);
					tmpcoeff = m_b2[i];
					fprintf(fpo, "%12.7f\n", tmpcoeff);
				}
			}
			fclose(fpo);
		}
//		InvalidateRect(NULL, TRUE);
	}
-----------------------------*/

	void IirFresCal(double Xg[], double Xt[])
	{
		int i;
		double	omega;
		double	AReal;
		double	AImag;
		double	AAbs_2;
		double	BReal;
		double	BImag;
		double	BAbs_2;
		double	HAbs_2;
		double	HzdB;
		double	HThita;
		double	dummy;

		double pai = 3.14159265358979323846264;
		int k;

		int int_th;

		for(i = 0; i < 360; i++) {
			Xg[i] = 0.0;
			Xt[i] = 0.0;
			for(k = 0; k < 2; k++) {
				omega = i * pai / 180.0 + 0.001;

				AReal = 0.0; AImag = 0.0;

				AReal =  m_a0[k] + m_a1[k] * Math.cos(1.0 * omega) + m_a2[k] * Math.cos(2.0 * omega);
				AImag = 0.0 - m_a1[k] * Math.sin(1.0 * omega) - m_a2[k] * Math.sin(2.0 * omega);
				AAbs_2 = AReal * AReal + AImag * AImag;

				BReal = 0.0; BImag = 0.0;

				BReal = 1.0 + m_b1[k] * Math.cos(1.0 * omega) + m_b2[k] * Math.cos(2.0 * omega);
				BImag = 0.0 - m_b1[k] * Math.sin(1.0 * omega) - m_b2[k] * Math.sin(2.0 * omega);
				BAbs_2 = BReal * BReal + BImag * BImag;
				if(BAbs_2 == 0.0)
					BAbs_2 = 1.0;
				HAbs_2 = AAbs_2 / BAbs_2;
				if(0 < HAbs_2)
					HzdB = 10 * Math.log10(HAbs_2);
				else
					HzdB = 0.0;
				dummy = AReal*BReal + AImag*BImag;
				if(dummy != 0.0)
					HThita = Math.atan((AImag*BReal - AReal*BImag) / dummy);
				else
					HThita = m_Pi / 2.0;

				Xg[i] = Xg[i] + HzdB;
				Xt[i] = Xt[i] + HThita;
				if( Xt[i] < -3.15 ) {
					int_th = (int)(Xt[i] * 100);
					int_th = int_th % 314;
					Xt[i] = ((double)(int_th)) / 314;
				}
				if( Xt[i] > 3.15 ) {
					int_th = (int)(Xt[i] * 100);
					int_th = int_th % 314;
					Xt[i] = ((double)(int_th)) / 314;
				}
			}
		}
	}
	void CutSignalArray(int nNoOfData, float pfDataIn[], float pfDataOut[])
	{
		int			i;
		for(i = 0; i < nNoOfData; i++)
		{
			m_x1[2] = m_x1[1];
			m_x1[1] = m_x1[0];
			m_x1[0] =pfDataIn[i];

			m_x2[2] = m_x2[1];
			m_x2[1] = m_x2[0];
			m_x2[0] = m_a2[0] * m_x1[2] + m_a1[0] * m_x1[1] + m_a0[0] * m_x1[0] - m_b2[0] * m_x2[2] - m_b1[0] * m_x2[1];

			if(m_BiquadN == 1)
				pfDataOut[i] = (float) m_x2[0];
			else
			{
				m_x3[2] = m_x3[1];
				m_x3[1] = m_x3[0];
				m_x3[0] = m_a2[1] * m_x2[2] + m_a1[1] * m_x2[1] + m_a0[1] * m_x2[0] - m_b2[1] * m_x3[2] - m_b1[1] * m_x3[1];
				pfDataOut[i] = (float) m_x3[0];
			}
		}
	}
	
	double CutSignal(double dData)
	{
		m_x1[2] = m_x1[1];
		m_x1[1] = m_x1[0];
		m_x1[0] = dData;

		m_x2[2] = m_x2[1];
		m_x2[1] = m_x2[0];
		m_x2[0] = m_a2[0] * m_x1[2] + m_a1[0] * m_x1[1] + m_a0[0] * m_x1[0] - m_b2[0] * m_x2[2] - m_b1[0] * m_x2[1];

		if(m_BiquadN == 1)
			return m_x2[0];

		m_x3[2] = m_x3[1];
		m_x3[1] = m_x3[0];
		m_x3[0] = m_a2[1] * m_x2[2] + m_a1[1] * m_x2[1] + m_a0[1] * m_x2[0] - m_b2[1] * m_x3[2] - m_b1[1] * m_x3[1];

		return m_x3[0];
	}
}
