package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.domain.model.Tag

/** Represents the UI state for the media library screen. */
sealed interface LibraryUiState {
    /** Initial state while the first emission from the repository is awaited. */
    data object Loading : LibraryUiState

    /** Items are ready to display. [items] may be empty. */
    data class Success(
        /** Media items after applying [activeTagIds] and [sortOrder]. */
        val items: List<MediaItem>,
        /** Active sort order. */
        val sortOrder: SortOrder = SortOrder.ADDED_AT_DESC,
        /** All available tags for display in filter chips. */
        val availableTags: List<Tag> = emptyList(),
        /** IDs of tags currently selected as filters (AND logic). */
        val activeTagIds: Set<String> = emptySet(),
    ) : LibraryUiState
}
