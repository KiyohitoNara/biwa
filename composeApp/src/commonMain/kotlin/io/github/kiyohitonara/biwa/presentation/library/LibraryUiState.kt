package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.MediaFilter
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.SortOrder

/** Represents the UI state for the media library screen. */
sealed interface LibraryUiState {
    /** Initial state while the first emission from the repository is awaited. */
    data object Loading : LibraryUiState

    /** Items are ready to display. [items] may be empty. */
    data class Success(
        /** Media items after applying [mediaFilter] and [sortOrder]. */
        val items: List<MediaItem>,
        /** Active sort order. */
        val sortOrder: SortOrder = SortOrder.ADDED_AT_DESC,
        /** Active media-type filter. */
        val mediaFilter: MediaFilter = MediaFilter.ALL,
    ) : LibraryUiState
}
