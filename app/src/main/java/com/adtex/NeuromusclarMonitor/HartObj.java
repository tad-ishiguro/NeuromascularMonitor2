package com.adtex.NeuromusclarMonitor;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

public class HartObj
{
	double	m_pXArray[], m_pYArray[];
	double	m_dMaxX, m_dMinX, m_dMaxY, m_dMinY;
	int		m_nNoOfData;
	Paint	m_Paint;
	Path 	m_Path;
	Paint	m_Text;
	public void InitObj()
	{
		double x,y,a,b,c,d,k,p,q,pi;// pi は円周率
		double t,dt, t2, t3;// 媒介変数とその変分
		double tmax;// 媒介変数の最大値
//		double xx[10001],yy[10001];// メモリ容量の上限に注意
		int i,imax;
		int		nRes = 150;

		m_Path = new Path();
		m_Paint = new Paint();
		m_Paint.setColor(Color.argb(0x88,  0xff,  00,  00));
		m_Paint.setStyle(Paint.Style.FILL_AND_STROKE);
//		m_Paint.setAntiAlias(true);
		m_pXArray = new double[nRes + 10];
		m_pYArray = new double[nRes + 10];

		m_Text = new Paint();
		m_Text.setColor(Color.BLACK);
		m_Text.setTextAlign(Paint.Align.LEFT);
	//  定数設定
		a = 0.0;
		b = 0.4;
		c = d = 0.8;
		k = 0.5;
		p = 1.0;
		q = 1.2;
	/*-------------*/
		pi=3.1415926535;

	//  他のパラメータ設定
		tmax = 4.0 * pi;
//		dt = 2 * tmax / 4000;// t のプロット間隔
		dt = 2 * tmax / nRes;// t のプロット間隔
		m_dMaxX = -1.0e20;
		m_dMinX = 1.0e20;
		m_dMaxY = -1.0e20;
		m_dMinY = 1.0e20;
	//  計算実行
		i = 0;
		for(t = -tmax ; t <= tmax ; t = t + dt)
		{
			t2 = t * t;
			t3 = t2 * t;
			x = a * ((1.0 - Math.exp(-p * t2)) / (Math.exp(-p * t2) + 1.0)) + c * t3 * Math.exp(-q * t2) * Math.cos(k * t);
			y = b * ((1.0 - Math.exp(-p * t2)) / (Math.exp(-p * t2) + 1.0)) + d * t3 * Math.exp(-q * t2) * Math.sin(k * t);
			if(m_dMaxX < x)
				m_dMaxX = x;
			if(x < m_dMinX)
				m_dMinX = x;
			if(m_dMaxY < y)
				m_dMaxY = y;
			if(y < m_dMinY)
				m_dMinY = y;
			m_pXArray[i] = x;
			m_pYArray[i] = y;
			i++;
		}
		if(m_dMaxX == m_dMinX)
		{
			m_dMaxX++;
			m_dMinX--;
		}
		if(m_dMaxY == m_dMinY)
		{
			m_dMaxY++;
			m_dMinY--;
		}
		m_nNoOfData = i;
	}

	public void DrawHart(Canvas canvas, int xorg, int yorg, int nWidth, int nHeight, String str)
	{
		int		nCharSize = nHeight / 2;
		m_Path.reset();
		m_Text.setTextSize(nCharSize);
		int		i;
		float	x, y;
		for(i = 0; i < m_nNoOfData; i++)
		{
			x = (float)(((double)nWidth * (double)(m_pXArray[i] - m_dMinX)) / (m_dMaxX - m_dMinX) + (double)xorg);
			y = (float)(((double)nHeight * (double)(m_dMaxY - m_pYArray[i])) / (m_dMaxY - m_dMinY) + (double)yorg);
			if(i == 0)
				m_Path.moveTo(x,  y);
			else
				m_Path.lineTo(x, y);
		}
		canvas.drawPath(m_Path,  m_Paint);
		int		nLength;
		nLength = str.length();
		x = xorg + nWidth / 2 - (int)(nLength * nCharSize * 0.3);
		y = yorg + (int)(nHeight * 0.6);
		canvas.drawText(str,  x, y, m_Text);
	}
}

