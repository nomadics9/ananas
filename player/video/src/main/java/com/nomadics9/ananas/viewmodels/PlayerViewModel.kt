package com.nomadics9.ananas.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import com.nomadics9.ananas.models.ExternalSubtitle
import com.nomadics9.ananas.models.FindroidChapter
import com.nomadics9.ananas.models.FindroidEpisode
import com.nomadics9.ananas.models.FindroidItem
import com.nomadics9.ananas.models.FindroidMovie
import com.nomadics9.ananas.models.FindroidSeason
import com.nomadics9.ananas.models.FindroidShow
import com.nomadics9.ananas.models.FindroidSourceType
import com.nomadics9.ananas.models.FindroidSources
import com.nomadics9.ananas.models.PlayerChapter
import com.nomadics9.ananas.models.PlayerItem
import com.nomadics9.ananas.models.TrickplayInfo
import com.nomadics9.ananas.repository.JellyfinRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject internal constructor(
    private val repository: JellyfinRepository,
) : ViewModel() {
    private val eventsChannel = Channel<PlayerItemsEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    fun loadPlayerItems(
        item: FindroidItem,
        mediaSourceIndex: Int? = null,
    ) {
        Timber.d("Loading player items for item ${item.id}")

        viewModelScope.launch {
            val playbackPosition = item.playbackPositionTicks.div(10000)

            try {
                val items = prepareMediaPlayerItems(item, playbackPosition, mediaSourceIndex)
                eventsChannel.send(PlayerItemsEvent.PlayerItemsReady(items))
            } catch (e: Exception) {
                Timber.d(e)
                eventsChannel.send(PlayerItemsEvent.PlayerItemsError(e))
            }
        }
    }

    private suspend fun prepareMediaPlayerItems(
        item: FindroidItem,
        playbackPosition: Long,
        mediaSourceIndex: Int?,
    ): List<PlayerItem> = when (item) {
        is FindroidMovie -> movieToPlayerItem(item, playbackPosition, mediaSourceIndex)
        is FindroidShow -> seriesToPlayerItems(item, playbackPosition, mediaSourceIndex)
        is FindroidSeason -> seasonToPlayerItems(item, playbackPosition, mediaSourceIndex)
        is FindroidEpisode -> episodeToPlayerItems(item, playbackPosition, mediaSourceIndex)
        else -> emptyList()
    }

    private suspend fun movieToPlayerItem(
        item: FindroidMovie,
        playbackPosition: Long,
        mediaSourceIndex: Int?,
    ) = listOf(item.toPlayerItem(mediaSourceIndex, playbackPosition))

    private suspend fun seriesToPlayerItems(
        item: FindroidShow,
        playbackPosition: Long,
        mediaSourceIndex: Int?,
    ): List<PlayerItem> {
        val nextUp = repository.getNextUp(item.id)

        return if (nextUp.isEmpty()) {
            repository
                .getSeasons(item.id)
                .flatMap { seasonToPlayerItems(it, playbackPosition, mediaSourceIndex) }
        } else {
            episodeToPlayerItems(nextUp.first(), playbackPosition, mediaSourceIndex)
        }
    }

    private suspend fun seasonToPlayerItems(
        item: FindroidSeason,
        playbackPosition: Long,
        mediaSourceIndex: Int?,
    ): List<PlayerItem> {
        return repository
            .getEpisodes(
                seriesId = item.seriesId,
                seasonId = item.id,
                fields = listOf(ItemFields.MEDIA_SOURCES),
            )
            .filter { it.sources.isNotEmpty() }
            .filter { !it.missing }
            .map { episode -> episode.toPlayerItem(mediaSourceIndex, playbackPosition) }
    }

    private suspend fun episodeToPlayerItems(
        item: FindroidEpisode,
        playbackPosition: Long,
        mediaSourceIndex: Int?,
    ): List<PlayerItem> {
        // TODO Move user configuration to a separate class
        val userConfig = try {
            repository.getUserConfiguration()
        } catch (_: Exception) {
            null
        }
        return repository
            .getEpisodes(
                seriesId = item.seriesId,
                seasonId = item.seasonId,
                fields = listOf(ItemFields.MEDIA_SOURCES, ItemFields.CHAPTERS, ItemFields.TRICKPLAY),
                startItemId = item.id,
                limit = if (userConfig?.enableNextEpisodeAutoPlay != false) null else 1,
            )
            .filter { it.sources.isNotEmpty() }
            .filter { !it.missing }
            .map { episode -> episode.toPlayerItem(mediaSourceIndex, playbackPosition) }
    }

    private suspend fun FindroidItem.toPlayerItem(
        mediaSourceIndex: Int?,
        playbackPosition: Long,
    ): PlayerItem {
        val mediaSources = repository.getMediaSources(id, true)
        val mediaSource = if (mediaSourceIndex == null) {
            mediaSources.firstOrNull { it.type == FindroidSourceType.LOCAL } ?: mediaSources[0]
        } else {
            mediaSources[mediaSourceIndex]
        }
        val externalSubtitles = mediaSource.mediaStreams
            .filter { mediaStream ->
                mediaStream.isExternal && mediaStream.type == MediaStreamType.SUBTITLE && !mediaStream.path.isNullOrBlank()
            }
            .map { mediaStream ->
                ExternalSubtitle(
                    mediaStream.title,
                    mediaStream.language,
                    Uri.parse(mediaStream.path!!),
                    when (mediaStream.codec) {
                        "subrip" -> MimeTypes.APPLICATION_SUBRIP
                        "webvtt" -> MimeTypes.APPLICATION_SUBRIP
                        "ass" -> MimeTypes.TEXT_SSA
                        else -> MimeTypes.TEXT_UNKNOWN
                    },
                )
            }
        val trickplayInfo = when (this) {
            is FindroidSources -> {
                this.trickplayInfo?.get(mediaSource.id)?.let {
                    TrickplayInfo(
                        width = it.width,
                        height = it.height,
                        tileWidth = it.tileWidth,
                        tileHeight = it.tileHeight,
                        thumbnailCount = it.thumbnailCount,
                        interval = it.interval,
                        bandwidth = it.bandwidth,
                    )
                }
            }
            else -> null
        }
        return PlayerItem(
            name = name,
            itemId = id,
            mediaSourceId = mediaSource.id,
            mediaSourceUri = mediaSource.path,
            playbackPosition = playbackPosition,
            parentIndexNumber = if (this is FindroidEpisode) parentIndexNumber else null,
            indexNumber = if (this is FindroidEpisode) indexNumber else null,
            indexNumberEnd = if (this is FindroidEpisode) indexNumberEnd else null,
            externalSubtitles = externalSubtitles,
            chapters = chapters.toPlayerChapters(),
            trickplayInfo = trickplayInfo,
        )
    }

    private fun List<FindroidChapter>?.toPlayerChapters(): List<PlayerChapter>? {
        return this?.map { chapter ->
            PlayerChapter(
                startPosition = chapter.startPosition,
                name = chapter.name,
            )
        }
    }
}

sealed interface PlayerItemsEvent {
    data class PlayerItemsReady(val items: List<PlayerItem>) : PlayerItemsEvent
    data class PlayerItemsError(val error: Exception) : PlayerItemsEvent
}
