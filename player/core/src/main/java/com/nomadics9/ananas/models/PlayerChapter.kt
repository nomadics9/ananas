package com.nomadics9.ananas.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerChapter(
    /**
     * The start position.
     */
    val startPosition: Long,
    /**
     * The name.
     */
    val name: String? = null,
) : Parcelable
