package io.github.kiyohitonara.biwa.presentation.settings

import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.domain.model.SortOrder

/** Represents the UI state for the settings screen. */
data class SettingsUiState(
    /** Default sort order applied to the library on next launch. */
    val defaultSortOrder: SortOrder = SortOrder.ADDED_AT_DESC,
    /** App-wide color scheme. */
    val theme: AppTheme = AppTheme.SYSTEM,
)
