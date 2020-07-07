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


public class SettingGraph extends PreferenceActivity {
    int m_nInitFlg = 0;
    MainActivity	m_ma;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_ma = GlobalVariable.m_ma;
        addPreferencesFromResource(R.layout.setting_graph);
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


        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Heart_Amp_Mag_key");
        str = edittext_preference.getText();
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Respiratory_Amp_Mag_key");
        str = edittext_preference.getText();
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("TOF1_Amp_Mag_key");
        str = edittext_preference.getText();
        edittext_preference.setSummary(str);

        list = (ListPreference)getPreferenceScreen().findPreference("calc_graph_ave_key");
        str = GetGraphAveType(list.getValue());
        list.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("average_time_key");
        str = edittext_preference.getText() + " points.";
        edittext_preference.setSummary(str);

    }

    public String GetGraphAveType(String str1)
    {
        String	ret;
        if(str1.equals("0"))
            ret = getString(R.string.calc_graph_ave_select0);
        else
            ret = getString(R.string.calc_graph_ave_select1);
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
