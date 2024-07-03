package com.nomadics9.ananas.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrickplayInfo(
    val width: Int,
    val height: Int,
    val tileWidth: Int,
    val tileHeight: Int,
    val thumbnailCount: Int,
    val interval: Int,
    val bandwidth: Int,
) : Parcelable
