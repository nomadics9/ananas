package com.nomadics9.ananas.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.nomadics9.ananas.core.R as CoreR

class SettingsDownloadsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(CoreR.xml.fragment_settings_downloads, rootKey)
    }
}
