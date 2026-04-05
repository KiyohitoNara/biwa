package io.github.kiyohitonara.biwa.presentation.addmedia

import io.github.kiyohitonara.biwa.domain.model.MediaItem

/** Represents the UI state for the add-media flow. */
sealed interface AddMediaUiState {
    /** No operation in progress. */
    data object Idle : AddMediaUiState

    /** File copy and metadata save are in progress. */
    data object Loading : AddMediaUiState

    /** File was added successfully. */
    data class Success(
        /** The newly created media item. */
        val item: MediaItem,
    ) : AddMediaUiState

    /** An error occurred while adding the file. */
    data class Error(
        /** Human-readable description of the failure. */
        val message: String,
    ) : AddMediaUiState
}
