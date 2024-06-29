package org.askartv.phone.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.askartv.phone.core.R as CoreR

class SettingsDownloadsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(CoreR.xml.fragment_settings_downloads, rootKey)
    }
}
