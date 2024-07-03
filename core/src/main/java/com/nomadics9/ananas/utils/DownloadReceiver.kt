package com.nomadics9.ananas.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import com.nomadics9.ananas.database.ServerDatabaseDao
import com.nomadics9.ananas.models.FindroidItem
import com.nomadics9.ananas.models.toFindroidEpisode
import com.nomadics9.ananas.models.toFindroidMovie
import com.nomadics9.ananas.models.toFindroidSeason
import com.nomadics9.ananas.models.toFindroidShow
import com.nomadics9.ananas.models.toFindroidSource
import com.nomadics9.ananas.repository.JellyfinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DownloadReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: ServerDatabaseDao

    @Inject
    lateinit var downloader: Downloader

    @Inject
    lateinit var repository: JellyfinRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.DOWNLOAD_COMPLETE") {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != -1L) {
                val source = database.getSourceByDownloadId(id)
                if (source != null) {
                    val path = source.path.replace(".download", "")
                    val successfulRename = File(source.path).renameTo(File(path))
                    if (successfulRename) {
                        database.setSourcePath(source.id, path)
                    } else {
                        val items = mutableListOf<FindroidItem>()
                        items.addAll(
                            database.getMovies().map { it.toFindroidMovie(database, repository.getUserId()) },
                        )
                        items.addAll(
                            database.getShows().map { it.toFindroidShow(database, repository.getUserId()) },
                        )
                        items.addAll(
                            database.getSeasons().map { it.toFindroidSeason(database, repository.getUserId()) },
                        )
                        items.addAll(
                            database.getEpisodes().map { it.toFindroidEpisode(database, repository.getUserId()) },
                        )

                        items.firstOrNull { it.id == source.itemId }?.let {
                            CoroutineScope(Dispatchers.IO).launch {
                                downloader.deleteItem(it, source.toFindroidSource(database))
                            }
                        }
                    }
                } else {
                    val mediaStream = database.getMediaStreamByDownloadId(id)
                    if (mediaStream != null) {
                        val path = mediaStream.path.replace(".download", "")
                        val successfulRename = File(mediaStream.path).renameTo(File(path))
                        if (successfulRename) {
                            database.setMediaStreamPath(mediaStream.id, path)
                        } else {
                            database.deleteMediaStream(mediaStream.id)
                        }
                    }
                }
            }
        }
    }
}
