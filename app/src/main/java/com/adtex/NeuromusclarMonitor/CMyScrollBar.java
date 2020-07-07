package com.adtex.NeuromusclarMonitor;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class CMyScrollBar
{
	Paint	m_bkPaint = new Paint();	//�o�b�N�O���E���h
	Paint	m_barPaint = new Paint();	//�o�[�̐F
	Paint m_btnPaint = new Paint();		//�{�^���̐F
	Paint m_btnPaint2 = new Paint();		//�{�^���̐F
	int	m_stax, m_stay, m_endx, m_endy;
	float	m_fStartPos, m_fEndPos, m_fStartDspPos, m_fDspWidth;
	int		m_bar_sx, m_bar_sy, m_bar_ex, m_bar_ey;
	int		m_btn_sx, m_btn_sy, m_btn_ex, m_btn_ey;
	double	m_ax, m_bx;
	public CMyScrollBar()
	{
		m_fStartPos = m_fStartDspPos = 0.0F;
		m_fEndPos = 100.0F;
		m_fDspWidth = 100.0F;

		m_bkPaint.setStyle(Style.FILL);
		m_barPaint.setStyle(Style.STROKE);
		m_btnPaint.setStyle(Style.FILL);
		m_btnPaint2.setStyle(Style.STROKE);
		m_bkPaint.setColor(Color.WHITE);
		m_barPaint.setColor(Color.BLACK);
		m_btnPaint.setColor(Color.WHITE);
		m_btnPaint2.setColor(Color.BLACK);
	}

	public void SetSize(int stax, int stay, int endx, int endy)
	{
		m_stax = stax;
		m_endx = endx;
		m_stay = stay;
		m_endy = endy;
		int		dummy;
		if(m_endx < m_stax)
		{
			dummy = m_stax;
			m_stax = m_endx;
			m_endx = dummy;
		}
		if(m_endy < m_stay)
		{
			dummy = m_stay;
			m_stay = m_endy;
			m_endy = dummy;
		}
		m_bar_sx = m_stax;
		m_bar_ex = m_endx;
		int		nHight, nMid;
		nHight = (m_endy - m_stay) / 10;
		if(nHight < 2)
			nHight = 2;
		nMid = (m_endy + m_stay) / 2;
		m_bar_sy = nMid - nHight;
		m_bar_ey = nMid + nHight;
		m_btn_sy = nMid - nHight * 2;
		m_btn_ey = nMid + nHight * 2;
	}

	public void SetMaxMin(float fStart, float fEnd)
	{
		m_fStartPos = fStart;
		m_fEndPos = fEnd;
	}

	public void SetWidth(float fWidth)
	{
		m_fDspWidth = fWidth;
	}

	public void SetStartDspPos(float fDspStart)
	{
		m_fStartDspPos = fDspStart;
	}

	public void CalcBtnX()
	{
		if(m_fEndPos == m_fStartPos)
		{
			m_btn_sx = m_stax;
			m_btn_ex = m_endx;
		}
		else
		{
			m_ax = (double)(m_endx - m_stax) / (double)(m_fEndPos - m_fStartPos);
			m_bx = (double)m_stax - (double)m_fStartPos * m_ax;
			m_btn_sx = (int)(m_ax * m_fStartDspPos + m_bx + 0.5);
			m_btn_sx = (m_btn_sx < m_stax)? m_stax : (m_endx < m_btn_sx)? m_endx : m_btn_sx;
			m_btn_ex = (int)(m_ax * (m_fStartDspPos + m_fDspWidth) + m_bx + 0.5);
			m_btn_ex = (m_btn_ex < m_stax)? m_stax : (m_endx < m_btn_ex)? m_endx : m_btn_ex;
		}
	}

	public void DrawObj(Canvas canvas, boolean bReverseFlg)
	{
		if(!bReverseFlg)
		{
			m_bkPaint.setColor(Color.WHITE);
			m_barPaint.setColor(Color.BLACK);
			m_btnPaint.setColor(Color.LTGRAY);
			m_btnPaint2.setColor(Color.BLACK);
		}
		else
		{
			m_bkPaint.setColor(Color.WHITE);
			m_barPaint.setColor(Color.BLACK);
			m_btnPaint.setColor(Color.DKGRAY);
			m_btnPaint2.setColor(Color.BLACK);
		}
		CalcBtnX();
		canvas.drawRect(m_stax, m_stay, m_endx, m_endy, m_bkPaint);
		canvas.drawRect(m_bar_sx,  m_bar_sy, m_bar_ex, m_bar_ey, m_barPaint);
		canvas.drawRect(m_btn_sx,  m_btn_sy, m_btn_ex, m_btn_ey, m_btnPaint);
		canvas.drawRect(m_btn_sx,  m_btn_sy, m_btn_ex, m_btn_ey, m_btnPaint2);
	}

	public float ShiftScroll(float sx, float ex)
	{
		double	A, B, C, D;
		double	Bx, Cx, dSub;
		float	fSub, fRet;
		if(0 == m_ax)
			return -1;
		A = m_stax;
		D = m_endx;
		B = sx;
		C = ex;
		Bx = (B - m_bx) / m_ax;
		Cx = (C - m_bx) / m_ax;
		dSub = Cx - Bx;
		fSub = (float)dSub;
		fRet = m_fStartDspPos + fSub;

		return fRet;
	}

	public float ShiftPointScroll(float ex)
	{
		float	fP = m_fStartDspPos + m_fDspWidth / 2.0F;
		float	sx = (float)(m_ax * fP + m_bx);
		float	ret = ShiftScroll(sx, ex);
		return ret;
	}

}
