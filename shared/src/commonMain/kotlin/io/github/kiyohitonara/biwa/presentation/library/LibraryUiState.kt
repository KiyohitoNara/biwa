package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.Album
import io.github.kiyohitonara.biwa.domain.model.MediaItem

/** Represents the UI state for the media library screen. */
sealed interface LibraryUiState {
    /** Initial state while the first emission from the repository is awaited. */
    data object Loading : LibraryUiState

    /** Items are ready to display. [items] may be empty. */
    data class Success(
        /** Media items after applying [activeAlbumIds], in their persisted manual order. */
        val items: List<MediaItem>,
        /** All available albums for display in filter chips. */
        val availableAlbums: List<Album> = emptyList(),
        /** IDs of albums currently selected as filters (AND logic). */
        val activeAlbumIds: Set<String> = emptySet(),
    ) : LibraryUiState
}
