package io.github.kiyohitonara.biwa.presentation.library

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton holder for the ordered list of media IDs currently displayed in the library.
 *
 * Written by [LibraryViewModel] whenever its filtered + sorted list changes, and read by
 * [io.github.kiyohitonara.biwa.presentation.mediaviewer.MediaViewerViewModel] when the
 * viewer opens, so the viewer's swipe order matches what the user saw in the library
 * (active [io.github.kiyohitonara.biwa.domain.model.SortOrder] and tag filters).
 *
 * An empty list means no library state has been recorded yet (e.g. cold start via deep
 * link); consumers should fall back to all available media in that case.
 */
class LibraryDisplayState {
    private val _orderedIds = MutableStateFlow<List<String>>(emptyList())

    /** IDs of items currently shown in the library, in display order. */
    val orderedIds: StateFlow<List<String>> = _orderedIds.asStateFlow()

    /** Replaces the stored ordering with [ids]. */
    fun update(ids: List<String>) {
        _orderedIds.value = ids
    }
}
