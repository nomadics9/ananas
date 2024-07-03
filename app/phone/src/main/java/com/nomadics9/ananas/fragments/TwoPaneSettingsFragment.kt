package com.nomadics9.ananas.fragments

import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceHeaderFragmentCompat

class TwoPaneSettingsFragment : PreferenceHeaderFragmentCompat() {
    override fun onCreatePreferenceHeader(): PreferenceFragmentCompat {
        return SettingsFragment()
    }
}
