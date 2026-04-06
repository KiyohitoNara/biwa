package io.github.kiyohitonara.biwa.presentation.photoviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kiyohitonara.biwa.domain.usecase.GetAllPhotosUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages UI state for the photo viewer screen.
 *
 * On creation, collects the photo library via [GetAllPhotosUseCase] and resolves
 * the initial pager position from [mediaId]. As the user swipes, [onPhotoChanged]
 * records the view via [UpdateLastViewedAtUseCase].
 *
 * @param mediaId ID of the photo that should be shown first.
 */
class PhotoViewerViewModel(
    private val mediaId: String,
    private val getAllPhotosUseCase: GetAllPhotosUseCase,
    private val updateLastViewedAtUseCase: UpdateLastViewedAtUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PhotoViewerUiState>(PhotoViewerUiState.Loading)

    /** Current state of the photo viewer screen. */
    val uiState: StateFlow<PhotoViewerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { collectPhotos() }
    }

    private suspend fun collectPhotos() {
        getAllPhotosUseCase.execute().collect { photos ->
            val currentState = _uiState.value

            if (currentState is PhotoViewerUiState.Loading) {
                // First emission: resolve the initial index
                val index = photos.indexOfFirst { it.id == mediaId }
                if (index == -1) {
                    _uiState.value = PhotoViewerUiState.Error("Photo not found")
                } else {
                    _uiState.value = PhotoViewerUiState.Ready(
                        photos = photos,
                        currentIndex = index,
                        isToolbarVisible = true,
                    )
                }
            } else if (currentState is PhotoViewerUiState.Ready) {
                // Subsequent emissions: update photo list, keep current index clamped
                val clampedIndex = currentState.currentIndex.coerceAtMost((photos.size - 1).coerceAtLeast(0))
                _uiState.value = currentState.copy(photos = photos, currentIndex = clampedIndex)
            }
        }
    }

    /**
     * Called when the pager settles on a new page at [index].
     * Updates [uiState] and records the view timestamp.
     */
    fun onPhotoChanged(index: Int) {
        val state = _uiState.value as? PhotoViewerUiState.Ready ?: return
        _uiState.value = state.copy(currentIndex = index)
        val photoId = state.photos.getOrNull(index)?.id ?: return
        viewModelScope.launch { updateLastViewedAtUseCase.execute(photoId) }
    }

    /** Toggles the visibility of the top toolbar. */
    fun toggleToolbar() {
        val state = _uiState.value as? PhotoViewerUiState.Ready ?: return
        _uiState.value = state.copy(isToolbarVisible = !state.isToolbarVisible)
    }
}
