package com.nomadics9.ananas.models

interface Preference {
    val nameStringResource: Int
    val descriptionStringRes: Int?
    val iconDrawableId: Int?
    val enabled: Boolean
    val dependencies: List<String>
}
