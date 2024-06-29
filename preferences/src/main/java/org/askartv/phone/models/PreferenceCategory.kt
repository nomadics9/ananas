package org.askartv.phone.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class PreferenceCategory(
    @StringRes override val nameStringResource: Int,
    @StringRes override val descriptionStringRes: Int? = null,
    @DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<String> = emptyList(),
    val onClick: (Preference) -> Unit = {},
    val nestedPreferences: List<Preference> = emptyList(),
) : Preference
