package com.nomadics9.ananas.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.nomadics9.ananas.AppPreferences
import com.nomadics9.ananas.BuildConfig
import com.nomadics9.ananas.utils.restart
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.nomadics9.ananas.core.R as CoreR

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var appPreferences: AppPreferences

    private val updateUrl = BuildConfig.UPDATE_ADDRESS
    private var isUpdateAvailable: Boolean = false
    private var newLastModifiedDate: Date? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(CoreR.xml.fragment_settings, rootKey)

        findPreference<Preference>("switchServer")?.setOnPreferenceClickListener {
            findNavController().navigate(TwoPaneSettingsFragmentDirections.actionNavigationSettingsToServerSelectFragment())
            true
        }

        findPreference<Preference>("switchUser")?.setOnPreferenceClickListener {
            val serverId = appPreferences.currentServer!!
            findNavController().navigate(
                TwoPaneSettingsFragmentDirections.actionNavigationSettingsToUsersFragment(
                    serverId
                )
            )
            true
        }

        findPreference<Preference>("switchAddress")?.setOnPreferenceClickListener {
            val serverId = appPreferences.currentServer!!
            findNavController().navigate(
                TwoPaneSettingsFragmentDirections.actionNavigationSettingsToServerAddressesFragment(
                    serverId
                )
            )
            true
        }

        findPreference<Preference>("pref_offline_mode")?.setOnPreferenceClickListener {
            activity?.restart()
            true
        }

        findPreference<Preference>("privacyPolicy")?.setOnPreferenceClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/nomadics9/ananas/blob/main/PRIVACY"),
            )
            startActivity(intent)
            true
        }


        findPreference<Preference>("appInfo")?.setOnPreferenceClickListener {
            if (isUpdateAvailable && newLastModifiedDate != null) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                startActivity(intent)
                storeDate(newLastModifiedDate!!)
                true
            } else {
                findNavController().navigate(TwoPaneSettingsFragmentDirections.actionSettingsFragmentToAboutLibraries())
                false
            }
        }

        findPreference<Preference>("requests")?.setOnPreferenceClickListener {
            findNavController().navigate(TwoPaneSettingsFragmentDirections.actionNavigationSettingsToRequestsWebFragment())
            true
        }

        // Check for updates when the settings screen is opened
        checkForUpdates()
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            val lastModifiedDate = fetchLastModifiedDate(updateUrl)
            if (lastModifiedDate != null) {
                Timber.d("Fetched Last-Modified date: $lastModifiedDate")
                val storedDate = getStoredDate()
                Timber.d("Stored date: $storedDate")
                if (storedDate == Date(0L) || lastModifiedDate.after(storedDate)) {
                    Timber.d("Update available")
                    isUpdateAvailable = true
                    newLastModifiedDate = lastModifiedDate
                    showUpdateAvailable()
                } else {
                    Timber.d("No update available")
                    isUpdateAvailable = false
                }
            } else {
                Timber.d("Failed to fetch Last-Modified date")
                isUpdateAvailable = false
            }
        }
    }

    private suspend fun fetchLastModifiedDate(urlString: String): Date? {
        return withContext(Dispatchers.IO) {
            var urlConnection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "HEAD"
                val lastModified = urlConnection.getHeaderField("Last-Modified")
                if (lastModified != null) {
                    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                    dateFormat.parse(lastModified)
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching Last-Modified date")
                null
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun getStoredDate(): Date {
        val sharedPreferences = preferenceManager.sharedPreferences
        val storedDateString = sharedPreferences?.getString("stored_date", null)
        Timber.d("Retrieved stored date string: $storedDateString")
        return if (storedDateString != null) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(storedDateString) ?: Date(0)
            } catch (e: Exception) {
                Timber.e(e, "Error parsing stored date string")
                Date(0)
            }
        } else {
            Date(0)
        }
    }

    private fun storeDate(date: Date) {
        val dateString = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(date)
        preferenceManager.sharedPreferences?.edit()?.putString("stored_date", dateString)?.apply()
        Timber.d("Stored new date: $dateString")
    }

    private fun showUpdateAvailable() {
        val appInfoPreference = findPreference<Preference>("appInfo")
        appInfoPreference?.let {
            it.summary = "Update available!"
            it.icon = ResourcesCompat.getDrawable(resources, CoreR.drawable.ic_download, null)  // Ensure this drawable exists
            Timber.d("Update available UI shown")
        }
    }
}
