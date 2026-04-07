package io.github.kiyohitonara.biwa.domain.model

/** User-configurable application preferences. */
data class UserPreferences(
    /** Sort order applied to the library on launch. */
    val defaultSortOrder: SortOrder = SortOrder.ADDED_AT_DESC,
    /** Color scheme applied to the entire app. */
    val theme: AppTheme = AppTheme.SYSTEM,
)
