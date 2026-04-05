package io.github.kiyohitonara.biwa.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Manages UI state for the media library screen.
 *
 * Collects the [GetAllMediaUseCase] stream and exposes it as a [StateFlow].
 */
class LibraryViewModel(
    getAllMediaUseCase: GetAllMediaUseCase,
) : ViewModel() {
    /**
     * Current state of the library.
     *
     * Starts as [LibraryUiState.Loading] until the first emission arrives,
     * then transitions to [LibraryUiState.Success] on every update.
     * The upstream flow is kept active for 5 seconds after the last subscriber
     * disappears to survive configuration changes.
     */
    val uiState: StateFlow<LibraryUiState> = getAllMediaUseCase.execute()
        .map { items -> LibraryUiState.Success(items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState.Loading,
        )
}
