package com.nomadics9.ananas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.nomadics9.ananas.destinations.PlayerActivityDestination
import com.nomadics9.ananas.models.EpisodeItem
import com.nomadics9.ananas.models.FindroidEpisode
import com.nomadics9.ananas.ui.components.EpisodeCard
import com.nomadics9.ananas.ui.dummy.dummyEpisodeItems
import com.nomadics9.ananas.ui.theme.FindroidTheme
import com.nomadics9.ananas.ui.theme.spacings
import com.nomadics9.ananas.utils.ObserveAsEvents
import com.nomadics9.ananas.viewmodels.PlayerItemsEvent
import com.nomadics9.ananas.viewmodels.PlayerViewModel
import com.nomadics9.ananas.viewmodels.SeasonViewModel
import java.util.UUID

@Destination
@Composable
fun SeasonScreen(
    navigator: DestinationsNavigator,
    seriesId: UUID,
    seasonId: UUID,
    seriesName: String,
    seasonName: String,
    seasonViewModel: SeasonViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    LaunchedEffect(true) {
        seasonViewModel.loadEpisodes(
            seriesId = seriesId,
            seasonId = seasonId,
            offline = false,
        )
    }

    ObserveAsEvents(playerViewModel.eventsChannelFlow) { event ->
        when (event) {
            is PlayerItemsEvent.PlayerItemsReady -> {
                navigator.navigate(PlayerActivityDestination(items = ArrayList(event.items)))
            }
            is PlayerItemsEvent.PlayerItemsError -> Unit
        }
    }

    val delegatedUiState by seasonViewModel.uiState.collectAsState()

    SeasonScreenLayout(
        seriesName = seriesName,
        seasonName = seasonName,
        uiState = delegatedUiState,
        onClick = { episode ->
            playerViewModel.loadPlayerItems(item = episode)
        },
    )
}

@Composable
private fun SeasonScreenLayout(
    seriesName: String,
    seasonName: String,
    uiState: SeasonViewModel.UiState,
    onClick: (FindroidEpisode) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    when (uiState) {
        is SeasonViewModel.UiState.Loading -> Text(text = "LOADING")
        is SeasonViewModel.UiState.Normal -> {
            val episodes = uiState.episodes
            Row(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = MaterialTheme.spacings.extraLarge,
                            top = MaterialTheme.spacings.large,
                            end = MaterialTheme.spacings.large,
                        ),
                ) {
                    Text(
                        text = seasonName,
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Text(
                        text = seriesName,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                TvLazyColumn(
                    contentPadding = PaddingValues(
                        top = MaterialTheme.spacings.large,
                        bottom = MaterialTheme.spacings.large,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                    modifier = Modifier
                        .weight(2f)
                        .padding(end = MaterialTheme.spacings.extraLarge)
                        .focusRequester(focusRequester),
                ) {
                    items(episodes) { episodeItem ->
                        when (episodeItem) {
                            is EpisodeItem.Episode -> {
                                EpisodeCard(episode = episodeItem.episode, onClick = { onClick(episodeItem.episode) })
                            }

                            else -> Unit
                        }
                    }
                }

                LaunchedEffect(true) {
                    focusRequester.requestFocus()
                }
            }
        }
        is SeasonViewModel.UiState.Error -> Text(text = uiState.error.toString())
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun SeasonScreenLayoutPreview() {
    FindroidTheme {
        SeasonScreenLayout(
            seriesName = "86 EIGHTY-SIX",
            seasonName = "Season 1",
            uiState = SeasonViewModel.UiState.Normal(dummyEpisodeItems),
            onClick = {},
        )
    }
}
