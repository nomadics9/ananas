package com.nomadics9.ananas.models

interface FindroidSources {
    val sources: List<FindroidSource>
    val runtimeTicks: Long
    val trickplayInfo: Map<String, FindroidTrickplayInfo>?
}
