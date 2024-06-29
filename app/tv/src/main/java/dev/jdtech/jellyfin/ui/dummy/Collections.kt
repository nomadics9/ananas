package org.askartv.phone.ui.dummy

import org.askartv.phone.models.CollectionType
import org.askartv.phone.models.FindroidCollection
import org.askartv.phone.models.FindroidImages
import java.util.UUID

private val dummyMoviesCollection = FindroidCollection(
    id = UUID.randomUUID(),
    name = "Movies",
    type = CollectionType.Movies,
    images = FindroidImages(),
)

private val dummyShowsCollection = FindroidCollection(
    id = UUID.randomUUID(),
    name = "Shows",
    type = CollectionType.TvShows,
    images = FindroidImages(),
)

val dummyCollections = listOf(
    dummyMoviesCollection,
    dummyShowsCollection,
)
