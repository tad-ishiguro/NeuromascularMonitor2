package com.adtex.NeuromusclarMonitor;

import android.content.Context;
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


public class SettingBLE extends PreferenceActivity {
    int m_nInitFlg = 0;
    MainActivity	m_ma;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_ma = GlobalVariable.m_ma;
        addPreferencesFromResource(R.layout.setting_ble);

        DspSummary();
        m_nInitFlg = 1;
    }

    public void onBackButtonClick(View v) {
        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200); /* adtex */
        finish();
    }



    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
    }
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
        m_ma.readPreferences();

    }

    private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener()
    {
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            DspSummary();
        }
    };

    public void DspSummary()
    {
        String	str, str2;

        ListPreference list;
        EditTextPreference	edittext_preference;

        list = (ListPreference)getPreferenceScreen().findPreference("ComKind_key");
        str = getString(R.string.h_ComKind2) + " " + GetComKindType(list.getValue());
        list.setSummary(str);

        list = (ListPreference)getPreferenceScreen().findPreference("BatteryType_key");
        str = getString(R.string.h_ComKind2) + " " + GetBatteryType(list.getValue());
        list.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("device_name_aya_key");
        str = edittext_preference.getText();
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("disconnect_time_key3");
        str = edittext_preference.getText() + " min.";
        edittext_preference.setSummary(str);


    }



    public String GetComKindType(String str1)
    {
        String	ret;
        if(str1.equals("0"))
        {
            ret = getString(R.string.com_kind_select0);
            m_ma.m_nComKind2 = 0;
        }
        else //if(str1.equals("1"))
        {
            ret = getString(R.string.com_kind_select1);
            m_ma.m_nComKind2 = 1;
        }
        return ret;
    }


    public String GetBatteryType(String str1)
    {
        String	ret;
        if(str1.equals("0"))
            ret = getString(R.string.battery_select0);
        else if(str1.equals("1"))
            ret = getString(R.string.battery_select1);
        else //if(str1.equals("2"))
            ret = getString(R.string.battery_select2);
        return ret;
    }


    //カーソルを一番後ろにするための処理
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        String str = preference.getKey();
        Preference clickedPreference = findPreference (preference.getKey ());
        if (clickedPreference instanceof EditTextPreference ) // check if EditTextPreference
        {
            Editable editable = ((EditTextPreference) clickedPreference).getEditText().getText();
            Selection.setSelection(editable, editable.length()) ; // set the cursor to last position
            return true;
        }
        return false;
    }

}
