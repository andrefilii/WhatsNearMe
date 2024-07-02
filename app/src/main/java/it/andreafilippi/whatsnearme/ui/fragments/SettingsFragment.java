package it.andreafilippi.whatsnearme.ui.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import java.io.Serializable;

import it.andreafilippi.whatsnearme.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    public static SettingsFragment newInstance() {
        SettingsFragment sf = new SettingsFragment();
        return sf;
    }

    public SettingsFragment() {

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}