package org.askartv.phone.ui.dummy

import org.askartv.phone.models.CollectionType
import org.askartv.phone.models.HomeItem
import org.askartv.phone.models.HomeSection
import org.askartv.phone.models.UiText
import org.askartv.phone.models.View
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
