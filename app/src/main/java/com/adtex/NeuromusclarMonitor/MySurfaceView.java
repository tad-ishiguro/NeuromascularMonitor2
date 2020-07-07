package com.adtex.NeuromusclarMonitor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MySurfaceView extends SurfaceView
implements Runnable, SurfaceHolder.Callback
{
	boolean	m_bSurfaceFlg;
	Thread m_ViewThread;
	SurfaceHolder m_surfaceHolder;
	SurfaceView m_surfaceView;
	MainActivity	m_ma;
	Bitmap	m_Bitmap;
	int 	m_nTabIndex;
	public MySurfaceView(Context context)
	{
		super(context);
		// TODO 自動生成されたコンストラクター・スタブ
		m_bSurfaceFlg = false;
        m_ma = GlobalVariable.m_ma;
		setWillNotDraw(false);

	}


	public void SetSurface(SurfaceView sv)
	{
		if(sv == null)
		{
			m_surfaceHolder.removeCallback(this);
			m_surfaceHolder = null;
			m_ViewThread = null;
			m_bSurfaceFlg = false;
			m_Bitmap = null;
		}
		else
		{
			m_surfaceView = sv;
			m_surfaceHolder = sv.getHolder();
			m_surfaceHolder.addCallback(this);
			m_bSurfaceFlg = true;
		}
	}

	public void DrawObj()
	{
		Canvas canvas = null;
		if(!m_bSurfaceFlg)
			return;
		canvas = m_surfaceHolder.lockCanvas();
		if(canvas == null)
			return;
		m_ma.m_View.DrawObj(canvas);
		m_ma.m_View.m_bDrawFlg = false;
		m_surfaceHolder.unlockCanvasAndPost(canvas);
	}

	public Bitmap GetBitmap()
	{
		int	nWidth = m_ma.m_View.m_nWidth;
		int nHeight = m_ma.m_View.m_nHeight;
		m_Bitmap = Bitmap.createBitmap(nWidth, nHeight, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(m_Bitmap);
		m_ma.m_View.DrawObj(canvas);
		return m_Bitmap;
	}

	@Override
	public void run()
	{
		while(m_ViewThread != null)
		{
			if(m_ma.m_View.m_nCount != m_ma.m_View.m_nOldCount && m_bSurfaceFlg)
			{
				m_ma.m_View.m_nOldCount = m_ma.m_View.m_nCount;
				m_ma.m_View.RunObj(m_surfaceHolder);
			}
			else if(m_ma.m_View.m_bDrawFlg)
				DrawObj();
		}
	}


	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
	// TODO 自動生成されたメソッド・スタブ
		m_ViewThread = new Thread(this);
		m_ViewThread.start();

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
	// TODO 自動生成されたメソッド・スタブ
		m_ma.m_View.ViewChanged(width, height);

	}

	public void Destroy()
	{
		if(m_bSurfaceFlg)
		{
			m_surfaceHolder.removeCallback(this);
			m_surfaceHolder = null;
			m_ViewThread = null;
			m_bSurfaceFlg = false;
			m_surfaceView = null;
			m_Bitmap = null;
			
		}
	}




	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
	// TODO 自動生成されたメソッド・スタブ
		Destroy();
	}
}
