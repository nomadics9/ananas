package org.askartv.phone.models

interface FindroidSources {
    val sources: List<FindroidSource>
    val runtimeTicks: Long
    val trickplayInfo: Map<String, FindroidTrickplayInfo>?
}
