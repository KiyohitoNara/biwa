package io.github.kiyohitonara.biwa.presentation.videoplayer

import io.github.kiyohitonara.biwa.domain.model.MediaItem

/** Represents the UI state for the video player screen. */
sealed interface VideoPlayerUiState {
    /** Loading media item and saved playback state. */
    data object Loading : VideoPlayerUiState

    /** Media is ready to play. */
    data class Ready(
        /** The media item being played. */
        val mediaItem: MediaItem,
        /** Current playback position in milliseconds. */
        val positionMs: Long,
        /** Total duration of the media in milliseconds. */
        val durationMs: Long,
        /** Whether the player is currently playing. */
        val isPlaying: Boolean,
        /** Current playback speed multiplier. */
        val playbackSpeed: Float,
        /** A-point of the AB-repeat range in milliseconds, or null if unset. */
        val abStartMs: Long?,
        /** B-point of the AB-repeat range in milliseconds, or null if unset. */
        val abEndMs: Long?,
        /** Whether the playback controls overlay is visible. */
        val isControlsVisible: Boolean,
    ) : VideoPlayerUiState

    /** An unrecoverable error occurred while loading the media. */
    data class Error(
        /** Human-readable description of the error. */
        val message: String,
    ) : VideoPlayerUiState
}
