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


public class SettingPulseRate extends PreferenceActivity {
    int m_nInitFlg = 0;
    MainActivity	m_ma;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_ma = GlobalVariable.m_ma;
        addPreferencesFromResource(R.layout.setting_pulserate);
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


        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Heart_Rate_Upper_key");
        str = edittext_preference.getText() + " bpm";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Heart_Rate_Lower_key");
        str = edittext_preference.getText() + " bpm";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Respiratory_Rate_Upper_key");
        str = edittext_preference.getText() + " bpm";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Respiratory_Rate_Lower_key");
        str = edittext_preference.getText() + " bpm";
        edittext_preference.setSummary(str);

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
