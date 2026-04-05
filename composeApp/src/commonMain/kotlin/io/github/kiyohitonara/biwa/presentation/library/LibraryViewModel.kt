package io.github.kiyohitonara.biwa.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
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
 * Opening a media item is handled by [openMedia], which records the view and
 * emits a [LibraryNavEffect] to navigate to the appropriate screen.
 */
class LibraryViewModel(
    getAllMediaUseCase: GetAllMediaUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val getMediaByIdUseCase: GetMediaByIdUseCase,
    private val updateLastViewedAtUseCase: UpdateLastViewedAtUseCase,
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

    private val _navEffect = MutableSharedFlow<LibraryNavEffect>()

    /** Emits a one-shot navigation event when a media item is opened. */
    val navEffect: SharedFlow<LibraryNavEffect> = _navEffect.asSharedFlow()

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

    /**
     * Records the view and emits a navigation effect to open the media item.
     *
     * VIDEO and GIF items navigate to the video player; PHOTO items navigate
     * to the photo viewer. Does nothing if [id] is not found in the library.
     */
    fun openMedia(id: String) {
        viewModelScope.launch {
            updateLastViewedAtUseCase.execute(id)
            val item = getMediaByIdUseCase.execute(id) ?: return@launch
            val effect = when (item.mediaType) {
                MediaType.VIDEO, MediaType.GIF -> LibraryNavEffect.OpenVideoPlayer(id)
                MediaType.PHOTO -> LibraryNavEffect.OpenPhotoViewer(id)
            }
            _navEffect.emit(effect)
        }
    }
}
