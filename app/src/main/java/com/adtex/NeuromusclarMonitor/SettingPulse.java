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


public class SettingPulse extends PreferenceActivity {
    MainActivity	m_ma;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_ma = GlobalVariable.m_ma;
        addPreferencesFromResource(R.layout.setting_pulse);
        DspSummary();
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

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Calibration_Start_key");
        str = edittext_preference.getText() + " mA";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Calibration_Step_key");
        str = edittext_preference.getText() + " mA";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Calibration_Interval_key");
        str = edittext_preference.getText() + " msec";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Calibration_Ave_key");
        str = edittext_preference.getText();
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Current_Value_key");
        str = edittext_preference.getText() + " mA";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("Detection_Threshold_key");
        str = edittext_preference.getText() + " %";
        edittext_preference.setSummary(str);

        list = (ListPreference)getPreferenceScreen().findPreference("Pulse_Width_key");
        str = list.getValue() + " μsec";
        list.setSummary(str);

        list = (ListPreference)getPreferenceScreen().findPreference("Twitch_Interval_key");
        str = list.getValue() + " sec";
        list.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("TOF_STIM_INTERVAL_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("TOF_INTERVAL_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("TOF_TIME_LIMIT_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("PTC_Twitch1_NUM_key");
        str2 = getString(R.string.times);
        str = edittext_preference.getText() + " " + str2;
        edittext_preference.setSummary(str);

        list = (ListPreference)getPreferenceScreen().findPreference("PTC_TET_STIM_FREQ_key");
        str = list.getValue() + " Hz";
        list.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("PTC_TET_STIM_TIME_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("PTC_Twitch2_NUM_key");
        str2 = getString(R.string.times);
        str = edittext_preference.getText() + " " + str2;
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("PTC_AUTO_INTERVAL_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("PTC_TIME_LIMIT_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("AUTO_PILOT_PTC_LEVEL_key");
        str2 = getString(R.string.times2);
        str = edittext_preference.getText() + " " + str2;
        edittext_preference.setSummary(str);


        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("DBS_STIM_INTERVAL_key");
        str = edittext_preference.getText() + " msec";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("DBS_1_2_INTERVAL_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("DBS_1_1_INTERVAL_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

        list = (ListPreference)getPreferenceScreen().findPreference("DBS_PATTERN_key");
        str = getString(R.string.h_ComKind2) + " " + GetDBSPattern(list.getValue());
        list.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("DBS_TIME_LIMIT_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

        list = (ListPreference)getPreferenceScreen().findPreference("TET_STIM_FREQ_key");
        str = list.getValue() + " Hz";
        list.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("TET_STIM_TIME_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

        edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference("TET_TIME_LIMIT_key");
        str = edittext_preference.getText() + " sec";
        edittext_preference.setSummary(str);

    }

    public String GetDBSPattern(String str1)
    {
        String	ret;
        if(str1.equals("0"))
            ret = "3.3";
        else if(str1.equals("1"))
            ret = "3.2";
        else //if(str1.equals("2"))
            ret = "2.3";
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
