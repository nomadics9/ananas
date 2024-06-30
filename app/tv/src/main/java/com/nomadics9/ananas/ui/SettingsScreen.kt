package com.nomadics9.ananas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.nomadics9.ananas.destinations.ServerSelectScreenDestination
import com.nomadics9.ananas.destinations.SettingsSubScreenDestination
import com.nomadics9.ananas.destinations.UserSelectScreenDestination
import com.nomadics9.ananas.models.Preference
import com.nomadics9.ananas.models.PreferenceCategory
import com.nomadics9.ananas.models.PreferenceSelect
import com.nomadics9.ananas.models.PreferenceSwitch
import com.nomadics9.ananas.ui.components.SettingsCategoryCard
import com.nomadics9.ananas.ui.components.SettingsSelectCard
import com.nomadics9.ananas.ui.components.SettingsSwitchCard
import com.nomadics9.ananas.ui.theme.FindroidTheme
import com.nomadics9.ananas.ui.theme.spacings
import com.nomadics9.ananas.utils.ObserveAsEvents
import com.nomadics9.ananas.viewmodels.SettingsEvent
import com.nomadics9.ananas.viewmodels.SettingsViewModel
import com.nomadics9.ananas.core.R as CoreR

@Destination
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(true) {
        settingsViewModel.loadPreferences(intArrayOf())
    }

    ObserveAsEvents(settingsViewModel.eventsChannelFlow) { event ->
        when (event) {
            is SettingsEvent.NavigateToSettings -> {
                navigator.navigate(SettingsSubScreenDestination(event.indexes, event.title))
            }
            is SettingsEvent.NavigateToUsers -> {
                navigator.navigate(UserSelectScreenDestination)
            }
            is SettingsEvent.NavigateToServers -> {
                navigator.navigate(ServerSelectScreenDestination)
            }
        }
    }

    val delegatedUiState by settingsViewModel.uiState.collectAsState()

    SettingsScreenLayout(delegatedUiState) { preference ->
        when (preference) {
            is PreferenceSwitch -> {
                settingsViewModel.setBoolean(preference.backendName, preference.value)
            }
            is PreferenceSelect -> {
                settingsViewModel.setString(preference.backendName, preference.value)
            }
        }
        settingsViewModel.loadPreferences(intArrayOf())
    }
}

@Composable
private fun SettingsScreenLayout(
    uiState: SettingsViewModel.UiState,
    onUpdate: (Preference) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    when (uiState) {
        is SettingsViewModel.UiState.Normal -> {
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default * 2, vertical = MaterialTheme.spacings.large),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
            ) {
                item(span = { TvGridItemSpan(this.maxLineSpan) }) {
                    Text(
                        text = stringResource(id = CoreR.string.title_settings),
                        style = MaterialTheme.typography.displayMedium,
                    )
                }
                items(uiState.preferences) { preference ->
                    when (preference) {
                        is PreferenceCategory -> SettingsCategoryCard(preference = preference)
                        is PreferenceSwitch -> {
                            SettingsSwitchCard(preference = preference) {
                                onUpdate(preference.copy(value = !preference.value))
                            }
                        }
                        is PreferenceSelect -> {
                            val options = stringArrayResource(id = preference.options)
                            SettingsSelectCard(preference = preference) {
                                val currentIndex = options.indexOf(preference.value)
                                val newIndex = if (currentIndex == options.count() - 1) {
                                    0
                                } else {
                                    currentIndex + 1
                                }
                                onUpdate(preference.copy(value = options[newIndex]))
                            }
                        }
                    }
                }
            }
            LaunchedEffect(true) {
                focusRequester.requestFocus()
            }
        }
        is SettingsViewModel.UiState.Loading -> {
            Text(text = "LOADING")
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun SettingsScreenLayoutPreview() {
    FindroidTheme {
        SettingsScreenLayout(
            uiState = SettingsViewModel.UiState.Normal(
                listOf(
                    PreferenceCategory(
                        nameStringResource = CoreR.string.settings_category_language,
                        iconDrawableId = CoreR.drawable.ic_languages,
                    ),
                    PreferenceCategory(
                        nameStringResource = CoreR.string.settings_category_appearance,
                        iconDrawableId = CoreR.drawable.ic_palette,
                    ),
                ),
            ),
            onUpdate = {},
        )
    }
}
