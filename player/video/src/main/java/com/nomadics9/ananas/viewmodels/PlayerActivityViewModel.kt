package com.nomadics9.ananas.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.nomadics9.ananas.AppPreferences
import com.nomadics9.ananas.api.JellyfinApi
import com.nomadics9.ananas.models.FindroidSegment
import com.nomadics9.ananas.models.PlayerChapter
import com.nomadics9.ananas.models.PlayerItem
import com.nomadics9.ananas.models.Trickplay
import com.nomadics9.ananas.mpv.MPVPlayer
import com.nomadics9.ananas.player.video.R
import com.nomadics9.ananas.repository.JellyfinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.extensions.hlsSegmentApi
import org.jellyfin.sdk.model.api.ClientCapabilitiesDto
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.DirectPlayProfile
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.ProfileCondition
import org.jellyfin.sdk.model.api.ProfileConditionType
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.SubtitleProfile
import org.jellyfin.sdk.model.api.TranscodeSeekInfo
import org.jellyfin.sdk.model.api.TranscodingProfile
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class PlayerActivityViewModel
@Inject
constructor(
    private val application: Application,
    private val jellyfinRepository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), Player.Listener {
    val player: Player
    private var originalHeight: Int = 0

    private val _uiState = MutableStateFlow(
        UiState(
            currentItemTitle = "",
            currentSegment = null,
            showSkip = false,
            currentTrickplay = null,
            currentChapters = null,
            fileLoaded = false,
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val eventsChannel = Channel<PlayerEvents>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    private val segments: MutableMap<UUID, List<FindroidSegment>> = mutableMapOf()

    data class UiState(
        val currentItemTitle: String,
        val currentSegment: FindroidSegment?,
        val showSkip: Boolean?,
        val currentTrickplay: Trickplay?,
        val currentChapters: List<PlayerChapter>?,
        val fileLoaded: Boolean,
    )

    private var items: Array<PlayerItem> = arrayOf()

    private val trackSelector = DefaultTrackSelector(application)
    var playWhenReady = true
    private var currentMediaItemIndex = savedStateHandle["mediaItemIndex"] ?: 0
    private var playbackPosition: Long = savedStateHandle["position"] ?: 0

    var playbackSpeed: Float = 1f

    private val handler = Handler(Looper.getMainLooper())

    init {
        if (appPreferences.playerMpv) {
            val trackSelectionParameters = TrackSelectionParameters.Builder(application)
                .setPreferredAudioLanguage(appPreferences.preferredAudioLanguage)
                .setPreferredTextLanguage(appPreferences.preferredSubtitleLanguage)
                .build()
            player = MPVPlayer(
                context = application,
                requestAudioFocus = true,
                trackSelectionParameters = trackSelectionParameters,
                seekBackIncrement = appPreferences.playerSeekBackIncrement,
                seekForwardIncrement = appPreferences.playerSeekForwardIncrement,
                videoOutput = appPreferences.playerMpvVo,
                audioOutput = appPreferences.playerMpvAo,
                hwDec = appPreferences.playerMpvHwdec,
            )
        } else {
            val renderersFactory =
                DefaultRenderersFactory(application).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON,
                )
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTunnelingEnabled(true)
                    .setPreferredAudioLanguage(appPreferences.preferredAudioLanguage)
                    .setPreferredTextLanguage(appPreferences.preferredSubtitleLanguage),
            )
            player = ExoPlayer.Builder(application, renderersFactory)
                .setTrackSelector(trackSelector)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    /* handleAudioFocus = */
                    true,
                )
                .setSeekBackIncrementMs(appPreferences.playerSeekBackIncrement)
                .setSeekForwardIncrementMs(appPreferences.playerSeekForwardIncrement)
                .build()
        }
    }

    fun initializePlayer(
        items: Array<PlayerItem>,
    ) {
        this.items = items
        player.addListener(this)

        viewModelScope.launch {
            val mediaItems = mutableListOf<MediaItem>()
            try {
                for (item in items) {
                    val streamUrl = item.mediaSourceUri
                    val mediaSubtitles = item.externalSubtitles.map { externalSubtitle ->
                        MediaItem.SubtitleConfiguration.Builder(externalSubtitle.uri)
                            .setLabel(externalSubtitle.title.ifBlank { application.getString(R.string.external) })
                            .setMimeType(externalSubtitle.mimeType)
                            .setLanguage(externalSubtitle.language)
                            .build()
                    }

                    if (appPreferences.playerIntroSkipper) {
                        jellyfinRepository.getSegmentsTimestamps(item.itemId)?.let { segment ->
                            segments[item.itemId] = segment
                        }
                        Timber.tag("SegmentInfo").d("Segments: %s", segments)
                    }

                    Timber.d("Stream url: $streamUrl")
                    val mediaItem =
                        MediaItem.Builder()
                            .setMediaId(item.itemId.toString())
                            .setUri(streamUrl)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(item.name)
                                    .build(),
                            )
                            .setSubtitleConfigurations(mediaSubtitles)
                            .build()
                    mediaItems.add(mediaItem)

                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            val startPosition = if (playbackPosition == 0L) {
                items.getOrNull(currentMediaItemIndex)?.playbackPosition ?: C.TIME_UNSET
            } else {
                playbackPosition
            }

            player.setMediaItems(
                mediaItems,
                currentMediaItemIndex,
                startPosition,
            )
            player.prepare()
            player.play()
            pollPosition(player)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun releasePlayer() {
        val mediaId = player.currentMediaItem?.mediaId
        val position = player.currentPosition
        val duration = player.duration
        GlobalScope.launch {
            delay(1000L)
            try {
                jellyfinRepository.postPlaybackStop(
                    UUID.fromString(mediaId),
                    position.times(10000),
                    position.div(duration.toFloat()).times(100).toInt(),
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        _uiState.update { it.copy(currentTrickplay = null) }
        playWhenReady = false
        playbackPosition = 0L
        currentMediaItemIndex = 0
        player.removeListener(this)
        player.release()
    }

    private fun pollPosition(player: Player) {
        val playbackProgressRunnable = object : Runnable {
            override fun run() {
                savedStateHandle["position"] = player.currentPosition
                viewModelScope.launch {
                    if (player.currentMediaItem != null && player.currentMediaItem!!.mediaId.isNotEmpty()) {
                        val itemId = UUID.fromString(player.currentMediaItem!!.mediaId)
                        try {
                            jellyfinRepository.postPlaybackProgress(
                                itemId,
                                player.currentPosition.times(10000),
                                !player.isPlaying,
                            )
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
                handler.postDelayed(this, 5000L)
            }
        }
        val segmentCheckRunnable = object : Runnable {
            override fun run() {
                val currentMediaItem = player.currentMediaItem
                if (currentMediaItem != null && currentMediaItem.mediaId.isNotEmpty()) {
                    val itemId = UUID.fromString(currentMediaItem.mediaId)
                    val seconds = player.currentPosition / 1000.0

                    val currentSegment =
                        segments[itemId]?.find { segment -> seconds in segment.startTime..<segment.endTime }
                    _uiState.update { it.copy(currentSegment = currentSegment) }
                    Timber.tag("SegmentInfo").d("currentSegment: %s", currentSegment)

                    if (currentSegment?.type == "intro") {
                        val showSkip =
                            currentSegment.let { it.skip && seconds in it.showAt..<it.hideAt }
                        _uiState.update { it.copy(showSkip = showSkip) }
                    }
                }
                handler.postDelayed(this, 1000L)
            }
        }
        handler.post(playbackProgressRunnable)
        if (segments.isNotEmpty()) handler.post(segmentCheckRunnable)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Timber.d("Playing MediaItem: ${mediaItem?.mediaId}")
        savedStateHandle["mediaItemIndex"] = player.currentMediaItemIndex
        viewModelScope.launch {
            try {
                items.first { it.itemId.toString() == player.currentMediaItem?.mediaId }
                    .let { item ->
                        val itemTitle =
                            if (item.parentIndexNumber != null && item.indexNumber != null) {
                                if (item.indexNumberEnd == null) {
                                    "S${item.parentIndexNumber}:E${item.indexNumber} - ${item.name}"
                                } else {
                                    "S${item.parentIndexNumber}:E${item.indexNumber}-${item.indexNumberEnd} - ${item.name}"
                                }
                            } else {
                                item.name
                            }
                        _uiState.update {
                            it.copy(
                                currentItemTitle = itemTitle,
                                currentSegment = null,
                                currentChapters = item.chapters,
                                fileLoaded = false,
                            )
                        }

                        jellyfinRepository.postPlaybackStart(item.itemId)

                        if (appPreferences.playerTrickplay) {
                            getTrickplay(item)
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        var stateString = "UNKNOWN_STATE             -"
        when (state) {
            ExoPlayer.STATE_IDLE -> {
                stateString = "ExoPlayer.STATE_IDLE      -"
            }

            ExoPlayer.STATE_BUFFERING -> {
                stateString = "ExoPlayer.STATE_BUFFERING -"
            }

            ExoPlayer.STATE_READY -> {
                stateString = "ExoPlayer.STATE_READY     -"
                _uiState.update { it.copy(fileLoaded = true) }
            }

            ExoPlayer.STATE_ENDED -> {
                stateString = "ExoPlayer.STATE_ENDED     -"
                eventsChannel.trySend(PlayerEvents.NavigateBack)
            }
        }
        Timber.d("Changed player state to $stateString")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Clearing Player ViewModel")
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
    }

    fun switchToTrack(trackType: @C.TrackType Int, index: Int) {
        // Index -1 equals disable track
        if (index == -1) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(trackType)
                .setTrackTypeDisabled(trackType, true)
                .build()
        } else {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(
                        player.currentTracks.groups.filter { it.type == trackType && it.isSupported }[index].mediaTrackGroup,
                        0
                    ),
                )
                .setTrackTypeDisabled(trackType, false)
                .build()
        }
    }

    fun selectSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        playbackSpeed = speed
    }

    private suspend fun getTrickplay(item: PlayerItem) {
        val trickplayInfo = item.trickplayInfo ?: return
        Timber.d("Trickplay Resolution: ${trickplayInfo.width}")

        withContext(Dispatchers.Default) {
            val maxIndex = ceil(
                trickplayInfo.thumbnailCount.toDouble()
                    .div(trickplayInfo.tileWidth * trickplayInfo.tileHeight)
            ).toInt()
            val bitmaps = mutableListOf<Bitmap>()

            for (i in 0..maxIndex) {
                jellyfinRepository.getTrickplayData(
                    item.itemId,
                    trickplayInfo.width,
                    i,
                )?.let { byteArray ->
                    val fullBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    for (offsetY in 0..<trickplayInfo.height * trickplayInfo.tileHeight step trickplayInfo.height) {
                        for (offsetX in 0..<trickplayInfo.width * trickplayInfo.tileWidth step trickplayInfo.width) {
                            val bitmap = Bitmap.createBitmap(
                                fullBitmap,
                                offsetX,
                                offsetY,
                                trickplayInfo.width,
                                trickplayInfo.height
                            )
                            bitmaps.add(bitmap)
                        }
                    }
                }
            }
            _uiState.update {
                it.copy(
                    currentTrickplay = Trickplay(
                        trickplayInfo.interval,
                        bitmaps
                    )
                )
            }
        }
    }

    /**
     * Get chapters of current item
     *
     * @return list of [PlayerChapter]
     */
    private fun getChapters(): List<PlayerChapter>? {
        return uiState.value.currentChapters
    }

    /**
     * Get the index of the current chapter
     *
     * @return the index of the current chapter
     */
    private fun getCurrentChapterIndex(): Int? {
        val chapters = getChapters() ?: return null

        for (i in chapters.indices.reversed()) {
            if (chapters[i].startPosition < player.currentPosition) {
                return i
            }
        }

        return null
    }

    /**
     * Get the index of the next chapter
     *
     * @return the index of the next chapter
     */
    private fun getNextChapterIndex(): Int? {
        val chapters = getChapters() ?: return null
        val currentChapterIndex = getCurrentChapterIndex() ?: return null

        return minOf(chapters.size - 1, currentChapterIndex + 1)
    }

    /**
     * Get the index of the previous chapter. Only use this for seeking as it
     * will return the current chapter when player position is more than 5
     * seconds past the start of the chapter
     *
     * @return the index of the previous chapter
     */
    private fun getPreviousChapterIndex(): Int? {
        val chapters = getChapters() ?: return null
        val currentChapterIndex = getCurrentChapterIndex() ?: return null

        // Return current chapter when more than 5 seconds past chapter start
        if (player.currentPosition > chapters[currentChapterIndex].startPosition + 5000L) {
            return currentChapterIndex
        }

        return maxOf(0, currentChapterIndex - 1)
    }

    fun isFirstChapter(): Boolean? = getChapters()?.let { getCurrentChapterIndex() == 0 }
    fun isLastChapter(): Boolean? =
        getChapters()?.let { chapters -> getCurrentChapterIndex() == chapters.size - 1 }

    /**
     * Seek to chapter
     *
     * @param chapterIndex the index of the chapter to seek to
     * @return the [PlayerChapter] which has been sought to
     */
    private fun seekToChapter(chapterIndex: Int): PlayerChapter? {
        return getChapters()?.getOrNull(chapterIndex)?.also { chapter ->
            player.seekTo(chapter.startPosition)
        }
    }

    /**
     * Seek to the next chapter
     *
     * @return the [PlayerChapter] which has been sought to
     */
    fun seekToNextChapter(): PlayerChapter? {
        return getNextChapterIndex()?.let { seekToChapter(it) }
    }

    /**
     * Seek to the previous chapter Will seek to start of current chapter if
     * player position is more than 5 seconds past start of chapter
     *
     * @return the [PlayerChapter] which has been sought to
     */
    fun seekToPreviousChapter(): PlayerChapter? {
        return getPreviousChapterIndex()?.let { seekToChapter(it) }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        eventsChannel.trySend(PlayerEvents.IsPlayingChanged(isPlaying))
    }

    private fun getTranscodeResolutions(preferredQuality: String): Int {
        return when (preferredQuality) {
            "1080p" -> 1080
            "720p - 2Mbps" -> 720
            "480p - 1Mbps" -> 480
            "360p - 800kbps" -> 360
            "Auto" -> 1
            else -> 1
        }
    }

    fun changeVideoQuality(quality: String) {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        val itemId = UUID.fromString(mediaId)
        val currentItem = items.firstOrNull { it.itemId.toString() == mediaId } ?: return
        val currentPosition = player.currentPosition

        viewModelScope.launch {
            try {
                val transcodingResolution = getTranscodeResolutions(quality)
                val (videoBitRate, audioBitRate) = jellyfinRepository.getVideoTranscodeBitRate(
                    transcodingResolution
                )
                val deviceProfile = jellyfinRepository.buildDeviceProfile(videoBitRate, "ts", EncodingContext.STREAMING)
                val playbackInfo = jellyfinRepository.getPostedPlaybackInfo(itemId,true,deviceProfile,videoBitRate)
                val playSessionId = playbackInfo.content.playSessionId
                if (playSessionId != null) {
                    jellyfinRepository.stopEncodingProcess(playSessionId)
                }
                val mediaSource = playbackInfo.content.mediaSources.firstOrNull()
                if (mediaSource == null) {
                    Timber.e("Media source is null")
                } else {
                    Timber.d("Media source found: $mediaSource")
                }
                val transcodingUrl = mediaSource!!.transcodingUrl
                val mediaSubtitles = currentItem.externalSubtitles.map { externalSubtitle ->
                    MediaItem.SubtitleConfiguration.Builder(externalSubtitle.uri)
                        .setLabel(externalSubtitle.title.ifBlank { application.getString(R.string.external) })
                        .setMimeType(externalSubtitle.mimeType)
                        .build()
                }

//                TODO: Embedded sub support
//                val embeddedSubtitles = mediaSource?.mediaStreams
//                    ?.filter { it.type == MediaStreamType.SUBTITLE && !it.isExternal }
//                    ?.map { mediaStream ->
//                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(mediaStream.deliveryUrl!!))
//                            .setMimeType(
//                                when (mediaStream.codec) {
//                                    "subrip" -> MimeTypes.APPLICATION_SUBRIP
//                                    "webvtt" -> MimeTypes.APPLICATION_SUBRIP
//                                    "ass" -> MimeTypes.TEXT_SSA
//                                    else -> MimeTypes.TEXT_UNKNOWN
//                                }
//                            )
//                            .setLanguage(mediaStream.language ?: "und")
//                            .setLabel(mediaStream.title ?: "Embedded Subtitle")
//                            .build()
//                    }
//                    ?.toMutableList() ?: mutableListOf()
//                val allSubtitles = embeddedSubtitles.apply { addAll(mediaSubtitles) }

                val baseUrl = jellyfinRepository.getBaseUrl()
                val cleanBaseUrl = baseUrl.removePrefix("http://").removePrefix("https://")
                val staticUrl = jellyfinRepository.getStreamUrl(itemId, currentItem.mediaSourceId)


                val uri =
                    Uri.parse(transcodingUrl).buildUpon()
                        .scheme("https")
                        .authority(cleanBaseUrl)
                        .build()

                fun Uri.Builder.setOrReplaceQueryParameter(
                    name: String,
                    value: String
                ): Uri.Builder {
                    val currentQueryParams = this.build().queryParameterNames

                    // Create a new builder for the URI
                    val newBuilder = Uri.parse(this.build().toString()).buildUpon()

                    // Track if the parameter was replaced
                    var parameterReplaced = false

                    // Re-add all parameters
                    currentQueryParams.forEach { param ->
                        val paramValue = this.build().getQueryParameter(param)
                        if (param == name) {
                            // Replace the parameter value
                            parameterReplaced = true
                            newBuilder.appendQueryParameter(name, value)
                        } else {
                            // Append the existing parameter
                            newBuilder.appendQueryParameter(param, paramValue)
                        }
                    }

                    // Append the new parameter only if it wasn't replaced
                    if (!parameterReplaced) {
                        newBuilder.appendQueryParameter(name, value)
                    }

                    return newBuilder
                }

                val uriBuilder = uri.buildUpon()
                //.setOrReplaceQueryParameter("PlaySessionId", playSessionId!!)

                if (transcodingResolution == 1) {
                    uriBuilder.setOrReplaceQueryParameter("EnableAdaptiveBitrateStreaming", "true")
                    uriBuilder.setOrReplaceQueryParameter("Static", "false")
                    uriBuilder.appendQueryParameter("MaxVideoHeight","1080" )
                } else if (transcodingResolution == 720 || transcodingResolution == 480 || transcodingResolution == 360) {
                    uriBuilder.setOrReplaceQueryParameter(
                        "MaxVideoBitRate",
                        videoBitRate.toString()
                    )
                    uriBuilder.setOrReplaceQueryParameter("VideoBitrate", videoBitRate.toString())
                    uriBuilder.setOrReplaceQueryParameter("AudioBitrate", audioBitRate.toString())
                    uriBuilder.setOrReplaceQueryParameter("Static", "false")
                    uriBuilder.appendQueryParameter("PlaySessionId", playSessionId)
                    uriBuilder.appendQueryParameter(
                        "MaxVideoHeight",
                        transcodingResolution.toString()
                    )
                    uriBuilder.appendQueryParameter("subtitleMethod", "External")
                }


                val newUri = uriBuilder.build()
                Timber.e("URI IS %s", newUri)
                val mediaItemBuilder = MediaItem.Builder()
                    .setMediaId(currentItem.itemId.toString())
                if (transcodingResolution == 1080) {
                    mediaItemBuilder.setUri(staticUrl)
                } else {
                    mediaItemBuilder.setUri(newUri)
                }
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(currentItem.name)
                            .build(),
                    )
                    .setSubtitleConfigurations(mediaSubtitles)

                player.setMediaItem(mediaItemBuilder.build())
                player.prepare()
                player.seekTo(currentPosition)
                player.play()

                val originalHeight = mediaSource.mediaStreams
                    ?.firstOrNull { it.type == MediaStreamType.VIDEO }?.height ?: -1
                // Store the original height
                this@PlayerActivityViewModel.originalHeight = originalHeight

                //isQualityChangeInProgress = true
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun getOriginalHeight(): Int {
        return originalHeight
    }
}


sealed interface PlayerEvents {
    data object NavigateBack : PlayerEvents
    data class IsPlayingChanged(val isPlaying: Boolean) : PlayerEvents
}
