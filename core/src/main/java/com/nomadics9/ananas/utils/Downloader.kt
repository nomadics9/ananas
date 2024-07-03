package com.nomadics9.ananas.utils

import com.nomadics9.ananas.models.FindroidItem
import com.nomadics9.ananas.models.FindroidSource
import com.nomadics9.ananas.models.UiText

interface Downloader {
    suspend fun downloadItem(
        item: FindroidItem,
        sourceId: String,
        storageIndex: Int = 0,
    ): Pair<Long, UiText?>

    suspend fun cancelDownload(item: FindroidItem, source: FindroidSource)

    suspend fun deleteItem(item: FindroidItem, source: FindroidSource)

    suspend fun getProgress(downloadId: Long?): Pair<Int, Int>
}
