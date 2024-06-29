package com.nomadics9.ananas.models

data class FavoriteSection(
    val id: Int,
    val name: UiText,
    var items: List<FindroidItem>,
)
