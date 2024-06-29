package com.nomadics9.ananas.models

data class ExceptionUiText(
    val uiText: UiText,
) : Exception()

data class ExceptionUiTexts(
    val uiTexts: Collection<UiText>,
) : Exception()
