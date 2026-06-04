package io.github.kiyohitonara.biwa.presentation.mediaviewer

import io.github.kiyohitonara.biwa.domain.model.MediaItem

/** Represents the UI state for the unified media viewer screen. */
sealed interface MediaViewerUiState {
    /** Loading the media list and resolving the initial position. */
    data object Loading : MediaViewerUiState

    /** Media items are ready to display. */
    data class Ready(
        /** All media in the library available for swipe navigation, in default order. */
        val items: List<MediaItem>,
        /** Index into [items] that is currently visible. */
        val currentIndex: Int,
        /** Whether the top toolbar is visible. */
        val isToolbarVisible: Boolean,
        /** Current playback position in milliseconds. Meaningful only when the current item is a video / GIF. */
        val positionMs: Long,
        /** Total duration of the current video / GIF in milliseconds. */
        val durationMs: Long,
        /** Whether the current video is playing. */
        val isPlaying: Boolean,
        /** Current playback speed multiplier (1.0 = normal). */
        val playbackSpeed: Float,
        /** Start of the AB-repeat range in milliseconds, or null if unset. */
        val abStartMs: Long?,
        /** End of the AB-repeat range in milliseconds, or null if unset. */
        val abEndMs: Long?,
        /** Whether the playback controls overlay (for video) is visible. */
        val isControlsVisible: Boolean,
    ) : MediaViewerUiState

    /** An unrecoverable error occurred (e.g. the initial media was not found). */
    data class Error(
        /** Human-readable description of the error. */
        val message: String,
    ) : MediaViewerUiState
}
