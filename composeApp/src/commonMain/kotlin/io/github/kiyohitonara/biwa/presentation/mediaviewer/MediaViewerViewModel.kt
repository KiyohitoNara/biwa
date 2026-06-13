package io.github.kiyohitonara.biwa.presentation.mediaviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kiyohitonara.biwa.domain.model.AbPoint
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.model.SetAbPointResult
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetPlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ResetAbRepeatUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SavePlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetAbPointUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import io.github.kiyohitonara.biwa.presentation.library.LibraryDisplayState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages UI state for the unified media viewer that swipes across photos,
 * videos and GIFs in a single pager.
 *
 * On creation, collects the entire library via [GetAllMediaUseCase] and
 * resolves the initial pager position from [mediaId]. Per-video playback
 * state ([positionMs], AB-repeat, speed) lives on [uiState] and is
 * persisted via [savePlaybackStateUseCase] whenever the user swipes to a
 * different video or the screen is dismissed.
 *
 * @param mediaId ID of the media item that should be shown first.
 */
class MediaViewerViewModel(
    private val mediaId: String,
    private val getAllMediaUseCase: GetAllMediaUseCase,
    private val updateLastViewedAtUseCase: UpdateLastViewedAtUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val getPlaybackStateUseCase: GetPlaybackStateUseCase,
    private val savePlaybackStateUseCase: SavePlaybackStateUseCase,
    private val setAbPointUseCase: SetAbPointUseCase,
    private val resetAbRepeatUseCase: ResetAbRepeatUseCase,
    private val libraryDisplayState: LibraryDisplayState,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MediaViewerUiState>(MediaViewerUiState.Loading)

    /** Current state of the media viewer screen. */
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

    private val _abRepeatError = MutableSharedFlow<Unit>()

    /** Emits when an AB-point is rejected because it would produce an invalid range (B ≤ A). */
    val abRepeatError: SharedFlow<Unit> = _abRepeatError.asSharedFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()

    /** Emits when the screen should pop back to the library (e.g. after deleting the last item). */
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    /** IDs whose media has been deleted via this ViewModel, so we skip persisting their state. */
    private val deletedIds = mutableSetOf<String>()

    // Snapshot of the library's ordering at the moment the viewer opens.
    // Held for the viewer's lifetime so reordering the library does not jolt the active view.
    // Empty means no library state was recorded — fall back to all media in raw order.
    private val displayOrderIndex: Map<String, Int>? =
        libraryDisplayState.orderedIds.value
            .takeIf { it.isNotEmpty() }
            ?.withIndex()
            ?.associate { (index, id) -> id to index }

    init {
        viewModelScope.launch { collectMedia() }
    }

    private suspend fun collectMedia() {
        getAllMediaUseCase.execute().collect { rawItems ->
            val items = applyDisplayOrder(rawItems)
            val currentState = _uiState.value

            if (items.isEmpty()) {
                _navigateBack.emit(Unit)
                return@collect
            }

            if (currentState is MediaViewerUiState.Loading) {
                val index = items.indexOfFirst { it.id == mediaId }
                if (index == -1) {
                    _uiState.value = MediaViewerUiState.Error("Media not found")
                } else {
                    _uiState.value = readyStateFor(items, index)
                }
            } else if (currentState is MediaViewerUiState.Ready) {
                val previousItem = currentState.items.getOrNull(currentState.currentIndex)
                val clampedIndex = currentState.currentIndex.coerceAtMost(items.size - 1)
                val newCurrentItem = items[clampedIndex]
                if (previousItem?.id != newCurrentItem.id) {
                    // The previously-current item was removed (e.g. via delete);
                    // reset the playback fields for whichever item now occupies that slot.
                    _uiState.value = readyStateFor(items, clampedIndex)
                        .copy(isToolbarVisible = currentState.isToolbarVisible)
                } else {
                    _uiState.value = currentState.copy(items = items, currentIndex = clampedIndex)
                }
            }
        }
    }

    private suspend fun readyStateFor(items: List<MediaItem>, index: Int): MediaViewerUiState.Ready {
        val item = items[index]
        val saved = if (item.isPlayable) getPlaybackStateUseCase.execute(item.id) else null
        return MediaViewerUiState.Ready(
            items = items,
            currentIndex = index,
            isToolbarVisible = true,
            positionMs = saved?.positionMs ?: 0L,
            durationMs = item.durationMs ?: 0L,
            isPlaying = false,
            playbackSpeed = saved?.playbackSpeed ?: 1.0f,
            abStartMs = saved?.abStartMs,
            abEndMs = saved?.abEndMs,
            isControlsVisible = true,
        )
    }

    /**
     * Called when the pager settles on a new page at [index].
     *
     * Persists the previous item's playback state if it was a video / GIF,
     * loads any saved state for the new item, records the view, and updates
     * [uiState] to reflect the new position.
     */
    fun onMediaChanged(index: Int) {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        if (index == state.currentIndex) return
        val newItem = state.items.getOrNull(index) ?: return
        viewModelScope.launch {
            saveCurrentVideoState(state)
            val saved = if (newItem.isPlayable) getPlaybackStateUseCase.execute(newItem.id) else null
            val latestState = _uiState.value as? MediaViewerUiState.Ready ?: return@launch
            _uiState.value = latestState.copy(
                currentIndex = index,
                positionMs = saved?.positionMs ?: 0L,
                durationMs = newItem.durationMs ?: 0L,
                isPlaying = false,
                playbackSpeed = saved?.playbackSpeed ?: 1.0f,
                abStartMs = saved?.abStartMs,
                abEndMs = saved?.abEndMs,
                isControlsVisible = true,
            )
            updateLastViewedAtUseCase.execute(newItem.id)
        }
    }

    /** Toggles the visibility of the top toolbar. */
    fun toggleToolbar() {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        _uiState.value = state.copy(isToolbarVisible = !state.isToolbarVisible)
    }

    /** Toggles visibility of the bottom playback controls overlay (video only). */
    fun toggleControls() {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        _uiState.value = state.copy(isControlsVisible = !state.isControlsVisible)
    }

    /** Updates the current playback position reported by the player. */
    fun updatePosition(positionMs: Long) {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        _uiState.value = state.copy(positionMs = positionMs)
    }

    /** Updates the total duration once the player has prepared the media. */
    fun updateDuration(durationMs: Long) {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        _uiState.value = state.copy(durationMs = durationMs)
    }

    /** Reflects the player's playing / paused state in [uiState]. */
    fun updatePlayingState(isPlaying: Boolean) {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        _uiState.value = state.copy(isPlaying = isPlaying)
    }

    /** Changes the playback speed of the current video to [speed]. */
    fun setPlaybackSpeed(speed: Float) {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        _uiState.value = state.copy(playbackSpeed = speed)
    }

    /**
     * Sets the A or B point of the AB-repeat range of the current video at [positionMs].
     *
     * On success the range is updated in [uiState]. Emits [abRepeatError] and leaves
     * the existing range unchanged when the resulting range would be invalid (B ≤ A).
     */
    fun setAbPoint(point: AbPoint, positionMs: Long) {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        val current = state.items.getOrNull(state.currentIndex) ?: return
        if (!current.isPlayable) return
        viewModelScope.launch {
            when (setAbPointUseCase.execute(current.id, point, positionMs)) {
                is SetAbPointResult.Success -> {
                    val newAbStart = if (point == AbPoint.A) positionMs else state.abStartMs
                    val newAbEnd = if (point == AbPoint.B) positionMs else state.abEndMs
                    _uiState.value = state.copy(abStartMs = newAbStart, abEndMs = newAbEnd)
                }
                is SetAbPointResult.InvalidRange -> _abRepeatError.emit(Unit)
            }
        }
    }

    /** Clears the AB-repeat range of the current video. */
    fun resetAbRepeat() {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        val current = state.items.getOrNull(state.currentIndex) ?: return
        if (!current.isPlayable) return
        viewModelScope.launch {
            resetAbRepeatUseCase.execute(current.id)
            _uiState.value = state.copy(abStartMs = null, abEndMs = null)
        }
    }

    /**
     * Deletes the currently visible media item along with its file.
     *
     * When the deleted item was the last remaining one, [navigateBack] is emitted.
     * Otherwise the pager advances to the next item via reactive re-emission.
     */
    fun deleteCurrentMedia() {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        val item = state.items.getOrNull(state.currentIndex) ?: return
        viewModelScope.launch {
            deletedIds.add(item.id)
            deleteMediaUseCase.execute(item.id)
        }
    }

    /**
     * Persists the current video's playback state to the repository.
     * Called when the screen leaves composition. No-op for non-video items
     * and for items already deleted via [deleteCurrentMedia].
     */
    fun saveCurrentState() {
        val state = _uiState.value as? MediaViewerUiState.Ready ?: return
        viewModelScope.launch { saveCurrentVideoState(state) }
    }

    private suspend fun saveCurrentVideoState(state: MediaViewerUiState.Ready) {
        val current = state.items.getOrNull(state.currentIndex) ?: return
        if (!current.isPlayable) return
        if (current.id in deletedIds) return
        savePlaybackStateUseCase.execute(
            videoId = current.id,
            positionMs = state.positionMs,
            abStartMs = state.abStartMs,
            abEndMs = state.abEndMs,
            playbackSpeed = state.playbackSpeed,
        )
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentState()
    }

    /**
     * Filters and reorders [rawItems] to match the library's last recorded display order.
     * Items not present in the snapshot are dropped — this honors the library's active
     * tag filter. Returns [rawItems] unchanged when no snapshot was recorded.
     */
    private fun applyDisplayOrder(rawItems: List<MediaItem>): List<MediaItem> {
        val index = displayOrderIndex ?: return rawItems
        return rawItems.filter { it.id in index }.sortedBy { index.getValue(it.id) }
    }

    /** Items whose playback state we persist (videos and GIFs). */
    private val MediaItem.isPlayable: Boolean
        get() = mediaType == MediaType.VIDEO || mediaType == MediaType.GIF
}
