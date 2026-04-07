package io.github.kiyohitonara.biwa.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.domain.usecase.GetUserPreferencesUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetDefaultSortOrderUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetThemeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Manages UI state and user actions for the settings screen. */
class SettingsViewModel(
    private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
    private val setDefaultSortOrderUseCase: SetDefaultSortOrderUseCase,
    private val setThemeUseCase: SetThemeUseCase,
) : ViewModel() {

    /**
     * Current settings state derived from persisted user preferences.
     *
     * Emits immediately on collection. Kept alive for 5 seconds after the
     * last subscriber disappears to survive configuration changes.
     */
    val uiState: StateFlow<SettingsUiState> = getUserPreferencesUseCase.execute()
        .map { prefs -> SettingsUiState(defaultSortOrder = prefs.defaultSortOrder, theme = prefs.theme) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    /** Persists [sortOrder] as the default sort order applied on library launch. */
    fun setDefaultSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch { setDefaultSortOrderUseCase.execute(sortOrder) }
    }

    /** Persists [theme] as the app-wide color scheme. */
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { setThemeUseCase.execute(theme) }
    }
}
