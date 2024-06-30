package com.nomadics9.ananas.ui.dummy

import com.nomadics9.ananas.models.CollectionType
import com.nomadics9.ananas.models.HomeItem
import com.nomadics9.ananas.models.HomeSection
import com.nomadics9.ananas.models.UiText
import com.nomadics9.ananas.models.View
import java.util.UUID

val dummyHomeItems = listOf(
    HomeItem.Section(
        HomeSection(
            id = UUID.randomUUID(),
            name = UiText.DynamicString("Continue watching"),
            items = dummyMovies + dummyEpisodes,
        ),
    ),
    HomeItem.ViewItem(
        View(
            id = UUID.randomUUID(),
            name = "Movies",
            items = dummyMovies,
            type = CollectionType.Movies,
        ),
    ),
)
