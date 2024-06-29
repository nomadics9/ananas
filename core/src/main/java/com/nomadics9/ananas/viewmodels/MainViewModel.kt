package com.nomadics9.ananas.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.nomadics9.ananas.AppPreferences
import com.nomadics9.ananas.database.ServerDatabaseDao
import com.nomadics9.ananas.models.Server
import com.nomadics9.ananas.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    private val database: ServerDatabaseDao,
) : ViewModel() {
    var startDestinationChanged = false

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val server: Server?, val user: User?) : UiState()
        data object Loading : UiState()
    }

    init {
        loadServerAndUser()
    }

    private fun loadServerAndUser() {
        viewModelScope.launch {
            val serverId = appPreferences.currentServer
            serverId?.let { id ->
                database.getServerWithAddressAndUser(id)?.let { data ->
                    _uiState.emit(
                        UiState.Normal(data.server, data.user),
                    )
                }
            }
        }
    }
}
