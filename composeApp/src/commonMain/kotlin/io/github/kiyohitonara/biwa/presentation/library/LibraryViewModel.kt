package io.github.kiyohitonara.biwa.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kiyohitonara.biwa.domain.model.MediaFilter
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GenerateThumbnailUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllTagsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaIdsWithAllTagsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetOrderedMediaIdsForTagUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetUserPreferencesUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderTagMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages UI state for the media library screen.
 *
 * Reactively applies [SortOrder], [MediaFilter], and active tag IDs (AND logic)
 * to produce [uiState]. Thumbnail generation for VIDEO items without a cached
 * path is triggered automatically on each library update.
 */
class LibraryViewModel(
    private val getAllMediaUseCase: GetAllMediaUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val getMediaByIdUseCase: GetMediaByIdUseCase,
    private val updateLastViewedAtUseCase: UpdateLastViewedAtUseCase,
    private val generateThumbnailUseCase: GenerateThumbnailUseCase,
    private val reorderMediaUseCase: ReorderMediaUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getMediaIdsWithAllTagsUseCase: GetMediaIdsWithAllTagsUseCase,
    private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
    private val getOrderedMediaIdsForTagUseCase: GetOrderedMediaIdsForTagUseCase,
    private val reorderTagMediaUseCase: ReorderTagMediaUseCase,
) : ViewModel() {
    // IDs for which thumbnail generation has already been scheduled this session.
    private val generatingIds = mutableSetOf<String>()

    private val _sortOrder = MutableStateFlow(SortOrder.ADDED_AT_DESC)

    /** Currently selected sort order. */
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _mediaFilter = MutableStateFlow(MediaFilter.ALL)

    /** Currently selected media-type filter. */
    val mediaFilter: StateFlow<MediaFilter> = _mediaFilter

    private val _activeTagIds = MutableStateFlow<Set<String>>(emptySet())

    /** IDs of tags currently selected as filters. */
    val activeTagIds: StateFlow<Set<String>> = _activeTagIds

    /**
     * Current state of the library, reflecting the active [sortOrder], [mediaFilter],
     * and [activeTagIds].
     *
     * Starts as [LibraryUiState.Loading] until the first DB emission arrives.
     * The upstream flow is kept active for 5 seconds after the last subscriber
     * disappears to survive configuration changes.
     */
    val uiState: StateFlow<LibraryUiState> = _activeTagIds
        .flatMapLatest { tagIds ->
            val mediaFlow = when {
                tagIds.isEmpty() -> getAllMediaUseCase.execute()
                tagIds.size == 1 -> combine(
                    getAllMediaUseCase.execute(),
                    getOrderedMediaIdsForTagUseCase.execute(tagIds.first()),
                ) { items, orderedIds ->
                    val idIndex = orderedIds.withIndex().associate { (i, id) -> id to i }
                    items.filter { it.id in idIndex }.sortedBy { idIndex[it.id] ?: Int.MAX_VALUE }
                }
                else -> combine(
                    getAllMediaUseCase.execute(),
                    getMediaIdsWithAllTagsUseCase.execute(tagIds.toList()),
                ) { items, filteredIds -> items.filter { it.id in filteredIds } }
            }

            combine(
                mediaFlow,
                _sortOrder,
                _mediaFilter,
                getAllTagsUseCase.execute(),
            ) { items, sortOrder, filter, allTags ->
                // When a single tag is active and sort is MANUAL, the items are already
                // ordered by the tag-specific sort_order — skip the global applySort.
                val sortedItems = if (tagIds.size == 1 && sortOrder == SortOrder.MANUAL) {
                    items.applyFilter(filter)
                } else {
                    items.applyFilter(filter).applySort(sortOrder)
                }
                LibraryUiState.Success(
                    items = sortedItems,
                    sortOrder = sortOrder,
                    mediaFilter = filter,
                    availableTags = allTags,
                    activeTagIds = tagIds,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState.Loading,
        )

    private val _deleteError = MutableSharedFlow<String>()

    /** Emits an error message when a deletion fails. One-shot event. */
    val deleteError: SharedFlow<String> = _deleteError.asSharedFlow()

    private val _navEffect = MutableSharedFlow<LibraryNavEffect>()

    /** Emits a one-shot navigation event when a media item is opened. */
    val navEffect: SharedFlow<LibraryNavEffect> = _navEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            val prefs = getUserPreferencesUseCase.execute().first()
            _sortOrder.value = prefs.defaultSortOrder
        }
        viewModelScope.launch {
            getAllMediaUseCase.execute().collect { items ->
                items.filter { it.thumbnailPath == null }
                    .forEach { item ->
                        if (generatingIds.add(item.id)) {
                            launch { generateThumbnailUseCase.execute(item) }
                        }
                    }
            }
        }
    }

    /** Switches the active sort order to [sortOrder]. */
    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    /** Switches the active media-type filter to [filter]. */
    fun setMediaFilter(filter: MediaFilter) {
        _mediaFilter.value = filter
    }

    /**
     * Toggles [tagId] in the active tag filter set.
     *
     * If [tagId] is already active it is removed; otherwise it is added.
     * An empty active set means no tag filter is applied.
     */
    fun toggleTag(tagId: String) {
        _activeTagIds.update { ids ->
            if (tagId in ids) ids - tagId else ids + tagId
        }
    }

    /**
     * Moves the item at [fromIndex] to [toIndex] within the currently displayed list
     * and persists the new ordering.
     *
     * When exactly one tag is active, the ordering is saved as a tag-specific sort order
     * via [ReorderTagMediaUseCase]. Otherwise the global [SortOrder.MANUAL] ordering is
     * updated via [ReorderMediaUseCase].
     *
     * No-op if [uiState] is not [LibraryUiState.Success].
     */
    fun reorderMedia(fromIndex: Int, toIndex: Int) {
        val state = uiState.value as? LibraryUiState.Success ?: return
        val items = state.items.toMutableList()
        val item = items.removeAt(fromIndex)
        items.add(toIndex.coerceIn(0, items.size), item)
        val orderedIds = items.map { it.id }
        val singleTagId = state.activeTagIds.singleOrNull()
        viewModelScope.launch {
            if (singleTagId != null) {
                reorderTagMediaUseCase.execute(singleTagId, orderedIds)
            } else {
                reorderMediaUseCase.execute(orderedIds)
            }
        }
    }

    /**
     * Deletes the media item identified by [id] along with its file.
     *
     * On success the item disappears from [uiState] automatically.
     * On failure an error message is emitted on [deleteError].
     */
    fun deleteMedia(id: String) {
        viewModelScope.launch {
            try {
                deleteMediaUseCase.execute(id)
            } catch (e: Exception) {
                _deleteError.emit(e.message ?: "Failed to delete")
            }
        }
    }

    /**
     * Records the view and emits a navigation effect to open the media item.
     *
     * VIDEO and GIF items navigate to the video player; PHOTO items navigate
     * to the photo viewer. Does nothing if [id] is not found in the library.
     */
    fun openMedia(id: String) {
        viewModelScope.launch {
            updateLastViewedAtUseCase.execute(id)
            val item = getMediaByIdUseCase.execute(id) ?: return@launch
            val effect = when (item.mediaType) {
                MediaType.VIDEO, MediaType.GIF -> LibraryNavEffect.OpenVideoPlayer(id)
                MediaType.PHOTO -> LibraryNavEffect.OpenPhotoViewer(id)
            }
            _navEffect.emit(effect)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun List<MediaItem>.applyFilter(filter: MediaFilter) = when (filter) {
        MediaFilter.ALL -> this
        MediaFilter.VIDEO -> filter { it.mediaType == MediaType.VIDEO }
        MediaFilter.GIF -> filter { it.mediaType == MediaType.GIF }
        MediaFilter.PHOTO -> filter { it.mediaType == MediaType.PHOTO }
    }

    private fun List<MediaItem>.applySort(sortOrder: SortOrder) = when (sortOrder) {
        SortOrder.ADDED_AT_DESC -> sortedByDescending { it.addedAt }
        SortOrder.ADDED_AT_ASC -> sortedBy { it.addedAt }
        SortOrder.FILE_NAME -> sortedBy { it.displayName.lowercase() }
        SortOrder.LAST_VIEWED_AT -> sortedByDescending { it.lastViewedAt ?: Long.MIN_VALUE }
        SortOrder.FILE_SIZE -> sortedByDescending { it.fileSizeBytes }
        SortOrder.MANUAL -> sortedBy { it.sortOrder }
    }
}
