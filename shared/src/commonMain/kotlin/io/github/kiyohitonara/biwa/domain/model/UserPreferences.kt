package io.github.kiyohitonara.biwa.domain.model

/** User-configurable application preferences. */
data class UserPreferences(
    /** Color scheme applied to the entire app. */
    val theme: AppTheme = AppTheme.SYSTEM,
)
