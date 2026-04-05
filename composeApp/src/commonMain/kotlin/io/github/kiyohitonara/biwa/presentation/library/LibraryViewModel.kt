package io.github.kiyohitonara.biwa.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manages UI state for the media library screen.
 *
 * Collects the [GetAllMediaUseCase] stream and exposes it as a [StateFlow].
 * Deletion is delegated to [DeleteMediaUseCase]; the list updates reactively.
 */
class LibraryViewModel(
    getAllMediaUseCase: GetAllMediaUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
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

    private val _deleteError = MutableSharedFlow<String>()

    /** Emits an error message when a deletion fails. One-shot event. */
    val deleteError: SharedFlow<String> = _deleteError.asSharedFlow()

    /**
     * Deletes the media item identified by [id] along with its file.
     *
     * On success the item disappears from [uiState] automatically.
     * On failure an error message is emitted on [deleteError].
     */
    fun deleteMedia(id: String) {
        viewModelScope.launch {
            try {
                deleteMediaUseCase.execute(id)
            } catch (e: Exception) {
                _deleteError.emit(e.message ?: "Failed to delete")
            }
        }
    }
}
