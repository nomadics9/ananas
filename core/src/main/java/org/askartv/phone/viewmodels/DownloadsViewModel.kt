package org.askartv.phone.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import org.askartv.phone.AppPreferences
import org.askartv.phone.Constants
import org.askartv.phone.core.R
import org.askartv.phone.models.FavoriteSection
import org.askartv.phone.models.FindroidMovie
import org.askartv.phone.models.FindroidShow
import org.askartv.phone.models.UiText
import org.askartv.phone.repository.JellyfinRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    private val repository: JellyfinRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val eventsChannel = Channel<DownloadsEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    sealed class UiState {
        data class Normal(val sections: List<FavoriteSection>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    init {
        testServerConnection()
    }

    private fun testServerConnection() {
        viewModelScope.launch {
            try {
                if (appPreferences.offlineMode) return@launch
                repository.getPublicSystemInfo()
                // Give the UI a chance to load
                delay(100)
            } catch (e: Exception) {
                eventsChannel.send(DownloadsEvent.ConnectionError(e))
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)

            val sections = mutableListOf<FavoriteSection>()

            val items = repository.getDownloads()

            FavoriteSection(
                Constants.FAVORITE_TYPE_MOVIES,
                UiText.StringResource(R.string.movies_label),
                items.filterIsInstance<FindroidMovie>(),
            ).let {
                if (it.items.isNotEmpty()) {
                    sections.add(
                        it,
                    )
                }
            }
            FavoriteSection(
                Constants.FAVORITE_TYPE_SHOWS,
                UiText.StringResource(R.string.shows_label),
                items.filterIsInstance<FindroidShow>(),
            ).let {
                if (it.items.isNotEmpty()) {
                    sections.add(
                        it,
                    )
                }
            }
            _uiState.emit(UiState.Normal(sections))
        }
    }
}

sealed interface DownloadsEvent {
    data class ConnectionError(val error: Exception) : DownloadsEvent
}
