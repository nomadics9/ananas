package com.nomadics9.ananas.models

enum class VideoQuality(
    val bitrate: Int,
    val qualityString: String,
    val qualityInt: Int,
) {
    PAuto(1, "Auto", 1080),
    POriginal(1000000000, "Original", 1080),
    P1080(8000000, "1080p", 1080),
    P720(2000000, "720p", 720),
    P480(1000000, "480p", 480),
    P360(700000, "360p", 360),
    ;

    companion object {
        fun fromString(quality: String): VideoQuality? = entries.find { it.qualityString == quality }

        fun getBitrate(quality: VideoQuality): Int = quality.bitrate

        fun getQualityString(quality: VideoQuality): String = quality.qualityString

        fun getQualityInt(quality: VideoQuality): Int = quality.qualityInt
    }
}
