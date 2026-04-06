package io.github.kiyohitonara.biwa.presentation.photoviewer

import io.github.kiyohitonara.biwa.domain.model.MediaItem

/** Represents the UI state for the photo viewer screen. */
sealed interface PhotoViewerUiState {
    /** Loading the photo list and resolving the initial position. */
    data object Loading : PhotoViewerUiState

    /** Photos are ready to display. */
    data class Ready(
        /** All photos in the library available for swipe navigation. */
        val photos: List<MediaItem>,
        /** Index into [photos] that is currently visible. */
        val currentIndex: Int,
        /** Whether the top toolbar is visible. */
        val isToolbarVisible: Boolean,
    ) : PhotoViewerUiState

    /** An unrecoverable error occurred (e.g. the photo was not found). */
    data class Error(
        /** Human-readable description of the error. */
        val message: String,
    ) : PhotoViewerUiState
}
