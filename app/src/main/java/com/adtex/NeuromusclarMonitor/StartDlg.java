package com.adtex.NeuromusclarMonitor;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.Selection;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;


public class StartDlg extends Dialog {
    private static final LinearLayout.LayoutParams FILL = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
    private Context m_context;
    private DialogListener m_listener;
    private Button m_btnRegist, m_btnCancel;
    private EditText    m_EditPatientID;
    private TextView    m_TextDate;
    public String       m_PatientID;
    MainActivity	m_ma;
    /*!
     * @brief	コンストラクタ
     *
     * @param	activity	Activity
     * @param	listener	リスナー
     */
    public StartDlg(Activity activity, DialogListener listener) {
        super((Context)activity);

        m_context = (Context)activity;
        m_listener = listener;
        m_btnRegist = null;
        m_btnCancel = null;
        m_EditPatientID = null;
    }

    /*!
     * @brief	生成イベント
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        m_ma = GlobalVariable.m_ma;
        LayoutInflater factory = LayoutInflater.from(m_context);
        final View entryView = factory.inflate(R.layout.start_dlg, null);
        // 登録ボタン.
        m_btnRegist = (Button)entryView.findViewById(R.id.Start_BTN);
        m_btnRegist.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                GetItemValu();
                if(CheckParam())
                {
                    m_listener.onRegistSelected();
                    dismiss();
                }
            }
        });

        // キャンセルボタン.
        m_btnCancel = (Button)entryView.findViewById(R.id.End_BTN);
        m_btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_listener.onCancel();
                dismiss();
            }
        });
        //
        String	str;
        m_EditPatientID = (EditText)entryView.findViewById(R.id.patient_id);
        str = m_PatientID;
        m_EditPatientID.setText(str);
        m_EditPatientID.setSelection(str.length());

        m_TextDate = (TextView)entryView.findViewById(R.id.date);
        Calendar cal = Calendar.getInstance();
        long	lCurrentTime = System.currentTimeMillis();

        cal.setTimeInMillis(lCurrentTime);
        int sec = cal.get(Calendar.SECOND);
        int min = cal.get(Calendar.MINUTE);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int	day = cal.get(Calendar.DATE);
        int	month = cal.get(Calendar.MONTH) + 1;
        int	year = cal.get(Calendar.YEAR);
        str = String.format("Date : %d/%02d/%02d", year, month, day);
        m_TextDate.setText(str);

        addContentView(entryView, FILL);
    }

    public void GetItemValu()
    {
        String	str;
        try
        {
            m_PatientID = m_EditPatientID.getText().toString();
        }
        catch (Exception e)
        {
            int	err;
            err = 0;
        }


//		m_Comment = m_editComment.getText().toString();
    }


    public boolean CheckParam()
    {
        return true;
/*---
        String filename;
        String  old = m_ma.m_View.m_PatientID;
        m_ma.m_View.m_PatientID = m_PatientID;
        filename = m_ma.m_View.GetDefaultName(1);
        m_ma.m_View.m_PatientID = old;
        String	filepath2;
        filepath2 = m_ma.m_View.GetSaveDir() + "/" + filename;

        File file = new File(filepath2);
        file.getParentFile().mkdirs();

        FileOutputStream fos;
        try
        {
            fos = new FileOutputStream(file, false);
        }
        catch (Exception e)
        {
            String  msg = m_ma.getString(R.string.patient_id_error);
            m_ma.MessageBox(msg);
            return false;
        }
        return true;
--------*/

    }
    /*!
     * @brief	キー処理
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            m_listener.onCancel();
            dismiss();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /*!
     * @brief	ダイアログ処理の結果を通知するためのインターフェース
     */
    public static interface DialogListener {
        /*!
         * @brief	登録ボタン押下
         */
        public abstract void onRegistSelected();

        /*!
         * @brief	キャンセル
         */
        public abstract void onCancel();
    }


}
