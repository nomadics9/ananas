package com.nomadics9.ananas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.nomadics9.ananas.destinations.MovieScreenDestination
import com.nomadics9.ananas.destinations.PlayerActivityDestination
import com.nomadics9.ananas.destinations.ShowScreenDestination
import com.nomadics9.ananas.models.FindroidEpisode
import com.nomadics9.ananas.models.FindroidItem
import com.nomadics9.ananas.models.FindroidMovie
import com.nomadics9.ananas.models.FindroidShow
import com.nomadics9.ananas.models.HomeItem
import com.nomadics9.ananas.ui.components.Direction
import com.nomadics9.ananas.ui.components.ItemCard
import com.nomadics9.ananas.ui.dummy.dummyHomeItems
import com.nomadics9.ananas.ui.theme.FindroidTheme
import com.nomadics9.ananas.ui.theme.spacings
import com.nomadics9.ananas.utils.ObserveAsEvents
import com.nomadics9.ananas.viewmodels.HomeViewModel
import com.nomadics9.ananas.viewmodels.PlayerItemsEvent
import com.nomadics9.ananas.viewmodels.PlayerViewModel
import com.nomadics9.ananas.core.R as CoreR

@Destination
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    isLoading: (Boolean) -> Unit,
) {
    LaunchedEffect(key1 = true) {
        homeViewModel.loadData()
    }

    ObserveAsEvents(playerViewModel.eventsChannelFlow) { event ->
        when (event) {
            is PlayerItemsEvent.PlayerItemsReady -> {
                navigator.navigate(PlayerActivityDestination(items = ArrayList(event.items)))
            }
            is PlayerItemsEvent.PlayerItemsError -> Unit
        }
    }

    val delegatedUiState by homeViewModel.uiState.collectAsState()

    HomeScreenLayout(
        uiState = delegatedUiState,
        isLoading = isLoading,
        onClick = { item ->
            when (item) {
                is FindroidMovie -> {
                    navigator.navigate(MovieScreenDestination(item.id))
                }
                is FindroidShow -> {
                    navigator.navigate(ShowScreenDestination(item.id))
                }
                is FindroidEpisode -> {
                    playerViewModel.loadPlayerItems(item = item)
                }
            }
        },
    )
}

@Composable
private fun HomeScreenLayout(
    uiState: HomeViewModel.UiState,
    isLoading: (Boolean) -> Unit,
    onClick: (FindroidItem) -> Unit,
) {
    var homeItems: List<HomeItem> by remember { mutableStateOf(emptyList()) }

    val focusRequester = remember { FocusRequester() }

    when (uiState) {
        is HomeViewModel.UiState.Normal -> {
            homeItems = uiState.homeItems
            isLoading(false)
        }
        is HomeViewModel.UiState.Loading -> {
            isLoading(true)
        }
        else -> Unit
    }
    TvLazyColumn(
        contentPadding = PaddingValues(bottom = MaterialTheme.spacings.large),
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester),
    ) {
        items(homeItems, key = { it.id }) { homeItem ->
            when (homeItem) {
                is HomeItem.Section -> {
                    Text(
                        text = homeItem.homeSection.name.asString(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = MaterialTheme.spacings.large),
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                        contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.large),
                    ) {
                        items(homeItem.homeSection.items, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                direction = Direction.HORIZONTAL,
                                onClick = {
                                    onClick(it)
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
                }
                is HomeItem.ViewItem -> {
                    Text(
                        text = stringResource(id = CoreR.string.latest_library, homeItem.view.name),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = MaterialTheme.spacings.large),
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                        contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.large),
                    ) {
                        items(homeItem.view.items.orEmpty(), key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                direction = Direction.VERTICAL,
                                onClick = {
                                    onClick(it)
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
                }
                else -> Unit
            }
        }
    }
    LaunchedEffect(homeItems) {
        focusRequester.requestFocus()
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun HomeScreenLayoutPreview() {
    FindroidTheme {
        HomeScreenLayout(
            uiState = HomeViewModel.UiState.Normal(dummyHomeItems),
            isLoading = {},
            onClick = {},
        )
    }
}
