package io.github.kiyohitonara.biwa.presentation.albummanagement

import io.github.kiyohitonara.biwa.domain.model.Album

/** Represents the UI state for the album management screen or bottom sheet. */
sealed interface AlbumManagementUiState {
    /** Initial state while albums are being loaded. */
    data object Loading : AlbumManagementUiState

    /** Albums are ready to display. */
    data class Ready(
        /** All albums in the library, ordered alphabetically. */
        val allAlbums: List<Album>,
        /**
         * Albums currently attached to the target media item.
         * Empty when the view is in global (non-media-specific) management mode.
         */
        val mediaAlbums: List<Album> = emptyList(),
    ) : AlbumManagementUiState
}
