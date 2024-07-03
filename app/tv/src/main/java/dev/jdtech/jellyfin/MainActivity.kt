package com.nomadics9.ananas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ramcosta.composedestinations.DestinationsNavHost
import dagger.hilt.android.AndroidEntryPoint
import com.nomadics9.ananas.database.ServerDatabaseDao
import com.nomadics9.ananas.destinations.AddServerScreenDestination
import com.nomadics9.ananas.destinations.LoginScreenDestination
import com.nomadics9.ananas.ui.theme.FindroidTheme
import com.nomadics9.ananas.viewmodels.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var database: ServerDatabaseDao

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var startRoute = NavGraphs.root.startRoute
        if (checkServersEmpty()) {
            startRoute = AddServerScreenDestination
        } else if (checkUser()) {
            startRoute = LoginScreenDestination
        }

        setContent {
            FindroidTheme {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    startRoute = startRoute,
                )
            }
        }
    }

    private fun checkServersEmpty(): Boolean {
        if (!viewModel.startDestinationChanged) {
            val nServers = database.getServersCount()
            if (nServers < 1) {
                viewModel.startDestinationChanged = true
                return true
            }
        }
        return false
    }

    private fun checkUser(): Boolean {
        if (!viewModel.startDestinationChanged) {
            appPreferences.currentServer?.let {
                val currentUser = database.getServerCurrentUser(it)
                if (currentUser == null) {
                    viewModel.startDestinationChanged = true
                    return true
                }
            }
        }
        return false
    }
}
