package io.github.kiyohitonara.biwa.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.domain.extractor.MediaMetadataExtractor
import io.github.kiyohitonara.biwa.domain.model.AddMediaRequest
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GenerateThumbnailUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllAlbumsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaIdsInAllAlbumsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetOrderedMediaIdsForAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderAlbumMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages UI state for the media library screen.
 *
 * Items are always displayed in their persisted manual order
 * ([MediaItem.sortOrder] globally, or the album-specific order when a single album
 * is active). Selecting a [SortOrder] is a one-shot reorder action that
 * computes a new order and persists it via the reorder use cases. Thumbnail
 * generation for items without a cached path is triggered automatically on
 * each library update.
 */
class LibraryViewModel(
    private val getAllMediaUseCase: GetAllMediaUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val getMediaByIdUseCase: GetMediaByIdUseCase,
    private val updateLastViewedAtUseCase: UpdateLastViewedAtUseCase,
    private val generateThumbnailUseCase: GenerateThumbnailUseCase,
    private val reorderMediaUseCase: ReorderMediaUseCase,
    private val getAllAlbumsUseCase: GetAllAlbumsUseCase,
    private val getMediaIdsInAllAlbumsUseCase: GetMediaIdsInAllAlbumsUseCase,
    private val getOrderedMediaIdsForAlbumUseCase: GetOrderedMediaIdsForAlbumUseCase,
    private val reorderAlbumMediaUseCase: ReorderAlbumMediaUseCase,
    private val addMediaUseCase: AddMediaUseCase,
    private val metadataExtractor: MediaMetadataExtractor,
    private val libraryDisplayState: LibraryDisplayState,
    logger: Logger,
) : ViewModel() {
    private val log = logger.withTag("LibraryViewModel")

    // IDs for which thumbnail generation has already been scheduled this session.
    private val generatingIds = mutableSetOf<String>()

    private val _activeAlbumIds = MutableStateFlow<Set<String>>(emptySet())

    /** IDs of albums currently selected as filters. */
    val activeAlbumIds: StateFlow<Set<String>> = _activeAlbumIds

    /**
     * Current state of the library, reflecting the active album filter and the
     * persisted manual ordering.
     *
     * Starts as [LibraryUiState.Loading] until the first DB emission arrives.
     * The upstream flow is kept active for 5 seconds after the last subscriber
     * disappears to survive configuration changes.
     */
    val uiState: StateFlow<LibraryUiState> =
        combine(_activeAlbumIds, getAllAlbumsUseCase.execute()) { requestedIds, allAlbums ->
            requestedIds intersect allAlbums.map { it.id }.toSet()
        }.distinctUntilChanged()
            .flatMapLatest { albumIds ->
                val mediaFlow =
                    when {
                        albumIds.isEmpty() ->
                            getAllMediaUseCase
                                .execute()
                                .map { items -> items.sortedBy { it.sortOrder } }
                        albumIds.size == 1 ->
                            combine(
                                getAllMediaUseCase.execute(),
                                getOrderedMediaIdsForAlbumUseCase.execute(albumIds.first()),
                            ) { items, orderedIds ->
                                val idIndex = orderedIds.withIndex().associate { (i, id) -> id to i }
                                items.filter { it.id in idIndex }.sortedBy { idIndex[it.id] ?: Int.MAX_VALUE }
                            }
                        else ->
                            combine(
                                getAllMediaUseCase.execute(),
                                getMediaIdsInAllAlbumsUseCase.execute(albumIds.toList()),
                            ) { items, filteredIds ->
                                items.filter { it.id in filteredIds }.sortedBy { it.sortOrder }
                            }
                    }

                combine(mediaFlow, getAllAlbumsUseCase.execute()) { items, allAlbums ->
                    LibraryUiState.Success(
                        items = items,
                        availableAlbums = allAlbums,
                        activeAlbumIds = albumIds,
                    )
                }
            }.map { state ->
                libraryDisplayState.update(state.items.map { it.id })
                state
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LibraryUiState.Loading,
            )

    private val _deleteError = MutableSharedFlow<String>()

    /** Emits an error message when a deletion fails. One-shot event. */
    val deleteError: SharedFlow<String> = _deleteError.asSharedFlow()

    private val _isAdding = MutableStateFlow(false)

    /** True while one or more media files are being added to the library. */
    val isAdding: StateFlow<Boolean> = _isAdding

    private val _addMediaError = MutableSharedFlow<String>()

    /** Emits a summary message when at least one file failed during an add operation. */
    val addMediaError: SharedFlow<String> = _addMediaError.asSharedFlow()

    private val _navEffect = MutableSharedFlow<LibraryNavEffect>()

    /** Emits a one-shot navigation event when a media item is opened. */
    val navEffect: SharedFlow<LibraryNavEffect> = _navEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            getAllMediaUseCase.execute().collect { items ->
                items
                    .filter { it.thumbnailPath == null }
                    .forEach { item ->
                        if (generatingIds.add(item.id)) {
                            launch { generateThumbnailUseCase.execute(item) }
                        }
                    }
            }
        }
    }

    /**
     * Reorders the currently displayed list by [sortOrder] and persists the new ordering.
     *
     * When exactly one album is active, the order is saved as the album-specific manual order
     * via [ReorderAlbumMediaUseCase]; otherwise the global manual order is updated via
     * [ReorderMediaUseCase]. No-op when [uiState] is not [LibraryUiState.Success] or when
     * multiple album filters are active (the operation has no defined target ordering).
     */
    fun setSortOrder(sortOrder: SortOrder) {
        val state = uiState.value as? LibraryUiState.Success ?: return
        if (state.activeAlbumIds.size > 1) return
        val orderedIds = state.items.applySort(sortOrder).map { it.id }
        val singleAlbumId = state.activeAlbumIds.singleOrNull()
        log.i { "Set sort order=$sortOrder albumId=$singleAlbumId count=${orderedIds.size}" }
        viewModelScope.launch {
            if (singleAlbumId != null) {
                reorderAlbumMediaUseCase.execute(singleAlbumId, orderedIds)
            } else {
                reorderMediaUseCase.execute(orderedIds)
            }
        }
    }

    /**
     * Toggles [albumId] in the active album filter set.
     *
     * If [albumId] is already active it is removed; otherwise it is added.
     * An empty active set means no album filter is applied.
     */
    fun toggleAlbum(albumId: String) {
        _activeAlbumIds.update { ids ->
            if (albumId in ids) ids - albumId else ids + albumId
        }
        log.d { "Toggle album filter albumId=$albumId active=${_activeAlbumIds.value}" }
    }

    /**
     * Moves the item at [fromIndex] to [toIndex] within the currently displayed list
     * and persists the new ordering.
     *
     * When exactly one album is active, the ordering is saved as an album-specific sort order
     * via [ReorderAlbumMediaUseCase]. Otherwise the global manual ordering is updated via
     * [ReorderMediaUseCase].
     *
     * No-op if [uiState] is not [LibraryUiState.Success].
     */
    fun reorderMedia(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val state = uiState.value as? LibraryUiState.Success ?: return
        val items = state.items.toMutableList()
        val item = items.removeAt(fromIndex)
        items.add(toIndex.coerceIn(0, items.size), item)
        val orderedIds = items.map { it.id }
        val singleAlbumId = state.activeAlbumIds.singleOrNull()
        log.d { "Reorder media from=$fromIndex to=$toIndex albumId=$singleAlbumId" }
        viewModelScope.launch {
            if (singleAlbumId != null) {
                reorderAlbumMediaUseCase.execute(singleAlbumId, orderedIds)
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
    @Suppress("TooGenericExceptionCaught") // Translate any use-case failure into a UI error event.
    fun deleteMedia(id: String) {
        log.i { "Delete media id=$id" }
        viewModelScope.launch {
            try {
                deleteMediaUseCase.execute(id)
            } catch (e: Exception) {
                log.w(e) { "Failed to delete media id=$id" }
                _deleteError.emit(e.message ?: "Failed to delete")
            }
        }
    }

    /**
     * Adds the given list of media [uris] to the library sequentially.
     *
     * Sets [isAdding] to true for the duration of the operation. If any item
     * fails (either metadata extraction or file copy), [addMediaError] emits
     * a summary message of the form "Added N, failed M" once all items have
     * been processed. No event is emitted when every item succeeds; the
     * updated library is itself the success signal.
     *
     * No-op if [uris] is empty.
     */
    @Suppress("TooGenericExceptionCaught") // Per-item failures are tallied, not propagated.
    fun addMedia(uris: List<String>) {
        if (uris.isEmpty()) return
        log.i { "Add media count=${uris.size}" }
        viewModelScope.launch {
            _isAdding.value = true
            var success = 0
            var failure = 0
            for (uri in uris) {
                try {
                    val metadata = metadataExtractor.extract(uri)
                    addMediaUseCase.execute(
                        AddMediaRequest(
                            sourceUri = uri,
                            fileName = metadata.fileName,
                            mediaType = metadata.mediaType,
                            displayName = metadata.fileName,
                            durationMs = metadata.durationMs,
                            widthPx = metadata.widthPx,
                            heightPx = metadata.heightPx,
                            fileSizeBytes = metadata.fileSizeBytes,
                            takenAt = metadata.takenAt,
                        ),
                    )
                    success++
                } catch (e: Exception) {
                    log.w(e) { "Failed to add media uri=$uri" }
                    failure++
                }
            }
            _isAdding.value = false
            log.i { "Add media finished success=$success failure=$failure" }
            if (failure > 0) {
                _addMediaError.emit("Added $success, failed $failure")
            }
        }
    }

    /**
     * Records the view and emits a navigation effect to open the unified media viewer
     * positioned at [id]. Does nothing if [id] is not found in the library.
     */
    fun openMedia(id: String) {
        log.d { "Open media id=$id" }
        viewModelScope.launch {
            updateLastViewedAtUseCase.execute(id)
            getMediaByIdUseCase.execute(id) ?: return@launch
            _navEffect.emit(LibraryNavEffect.OpenMediaViewer(id))
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun List<MediaItem>.applySort(sortOrder: SortOrder) =
        when (sortOrder) {
            SortOrder.ADDED_AT_DESC -> sortedByDescending { it.addedAt }
            SortOrder.ADDED_AT_ASC -> sortedBy { it.addedAt }
            SortOrder.FILE_NAME -> sortedBy { it.displayName.lowercase() }
            SortOrder.LAST_VIEWED_AT -> sortedByDescending { it.lastViewedAt ?: Long.MIN_VALUE }
            SortOrder.FILE_SIZE -> sortedByDescending { it.fileSizeBytes }
        }
}
