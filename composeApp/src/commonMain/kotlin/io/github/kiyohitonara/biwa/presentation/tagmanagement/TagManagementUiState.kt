package io.github.kiyohitonara.biwa.presentation.tagmanagement

import io.github.kiyohitonara.biwa.domain.model.Tag

/** Represents the UI state for the tag management screen or bottom sheet. */
sealed interface TagManagementUiState {
    /** Initial state while tags are being loaded. */
    data object Loading : TagManagementUiState

    /** Tags are ready to display. */
    data class Ready(
        /** All tags in the library, ordered alphabetically. */
        val allTags: List<Tag>,
        /**
         * Tags currently attached to the target media item.
         * Empty when the view is in global (non-media-specific) management mode.
         */
        val mediaTags: List<Tag> = emptyList(),
    ) : TagManagementUiState
}
