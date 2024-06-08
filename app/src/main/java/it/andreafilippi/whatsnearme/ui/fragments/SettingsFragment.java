package it.andreafilippi.whatsnearme.ui.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import it.andreafilippi.whatsnearme.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}