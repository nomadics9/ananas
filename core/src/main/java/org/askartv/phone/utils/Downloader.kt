package org.askartv.phone.utils

import org.askartv.phone.models.FindroidItem
import org.askartv.phone.models.FindroidSource
import org.askartv.phone.models.UiText

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
