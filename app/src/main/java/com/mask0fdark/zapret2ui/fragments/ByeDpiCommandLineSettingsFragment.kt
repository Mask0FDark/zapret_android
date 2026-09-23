package com.mask0fdark.zapret2ui.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.mask0fdark.zapret2ui.R

class ByeDpiCommandLineSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.byedpi_cmd_settings, rootKey)
    }
}
