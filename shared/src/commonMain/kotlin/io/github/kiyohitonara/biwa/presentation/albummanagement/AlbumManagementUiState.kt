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
        /** The album currently opened during drill-down, or null at the root level. */
        val currentParentId: String? = null,
        /** Albums whose parent is [currentParentId], i.e. the entries shown at the current level. */
        val currentAlbums: List<Album> = emptyList(),
        /** Ancestor chain from the root down to the currently opened album; empty at the root. */
        val breadcrumb: List<Album> = emptyList(),
    ) : AlbumManagementUiState
}
