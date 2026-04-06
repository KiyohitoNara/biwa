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
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manages UI state for the media library screen.
 *
 * Collects the full media list from [GetAllMediaUseCase] and reactively applies
 * the current [SortOrder] and [MediaFilter] via [combine] to produce [uiState].
 * Thumbnail generation for VIDEO items without a cached path is triggered
 * automatically on each library update.
 */
class LibraryViewModel(
    private val getAllMediaUseCase: GetAllMediaUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val getMediaByIdUseCase: GetMediaByIdUseCase,
    private val updateLastViewedAtUseCase: UpdateLastViewedAtUseCase,
    private val generateThumbnailUseCase: GenerateThumbnailUseCase,
    private val reorderMediaUseCase: ReorderMediaUseCase,
) : ViewModel() {
    // IDs for which thumbnail generation has already been scheduled this session.
    private val generatingIds = mutableSetOf<String>()

    private val _sortOrder = MutableStateFlow(SortOrder.ADDED_AT_DESC)

    /** Currently selected sort order. */
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _mediaFilter = MutableStateFlow(MediaFilter.ALL)

    /** Currently selected media-type filter. */
    val mediaFilter: StateFlow<MediaFilter> = _mediaFilter

    /**
     * Current state of the library, reflecting the active [sortOrder] and [mediaFilter].
     *
     * Starts as [LibraryUiState.Loading] until the first DB emission arrives.
     * The upstream flow is kept active for 5 seconds after the last subscriber
     * disappears to survive configuration changes.
     */
    val uiState: StateFlow<LibraryUiState> = combine(
        getAllMediaUseCase.execute(),
        _sortOrder,
        _mediaFilter,
    ) { items, sortOrder, filter ->
        val filtered = items.applyFilter(filter)
        val sorted = filtered.applySort(sortOrder)
        LibraryUiState.Success(items = sorted, sortOrder = sortOrder, mediaFilter = filter)
    }.stateIn(
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
     * Moves the item at [fromIndex] to [toIndex] within the currently displayed list
     * and persists the new [SortOrder.MANUAL] ordering via [ReorderMediaUseCase].
     *
     * No-op if [uiState] is not [LibraryUiState.Success].
     */
    fun reorderMedia(fromIndex: Int, toIndex: Int) {
        val state = uiState.value as? LibraryUiState.Success ?: return
        val items = state.items.toMutableList()
        val item = items.removeAt(fromIndex)
        items.add(toIndex.coerceIn(0, items.size), item)
        viewModelScope.launch { reorderMediaUseCase.execute(items.map { it.id }) }
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
