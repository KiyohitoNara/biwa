package io.github.kiyohitonara.biwa.presentation.settings

import io.github.kiyohitonara.biwa.domain.model.AppTheme

/** Represents the UI state for the settings screen. */
data class SettingsUiState(
    /** App-wide color scheme. */
    val theme: AppTheme = AppTheme.SYSTEM,
)
