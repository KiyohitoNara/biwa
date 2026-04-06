package io.github.kiyohitonara.biwa.presentation.videoplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kiyohitonara.biwa.domain.model.AbPoint
import io.github.kiyohitonara.biwa.domain.model.SetAbPointResult
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetPlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ResetAbRepeatUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SavePlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetAbPointUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages UI state for the video player screen.
 *
 * On creation, loads the [MediaItem] and any saved [PlaybackState] for [mediaId].
 * Delegates AB-repeat logic to [SetAbPointUseCase] and [ResetAbRepeatUseCase],
 * and saves state to [SavePlaybackStateUseCase] when the ViewModel is cleared.
 *
 * @param mediaId ID of the media item to play.
 */
class VideoPlayerViewModel(
    private val mediaId: String,
    private val getMediaByIdUseCase: GetMediaByIdUseCase,
    private val getPlaybackStateUseCase: GetPlaybackStateUseCase,
    private val savePlaybackStateUseCase: SavePlaybackStateUseCase,
    private val setAbPointUseCase: SetAbPointUseCase,
    private val resetAbRepeatUseCase: ResetAbRepeatUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<VideoPlayerUiState>(VideoPlayerUiState.Loading)

    /** Current state of the video player screen. */
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private val _abRepeatError = MutableSharedFlow<Unit>()

    /** Emits when an AB-point is rejected because it would produce an invalid range (B ≤ A). */
    val abRepeatError: SharedFlow<Unit> = _abRepeatError.asSharedFlow()

    init {
        viewModelScope.launch { loadMediaItem() }
    }

    private suspend fun loadMediaItem() {
        val item = getMediaByIdUseCase.execute(mediaId)
        if (item == null) {
            _uiState.value = VideoPlayerUiState.Error("Media not found")
            return
        }
        val saved = getPlaybackStateUseCase.execute(mediaId)
        _uiState.value = VideoPlayerUiState.Ready(
            mediaItem = item,
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
     * Updates the current playback position reported by the player.
     * Called continuously during playback.
     */
    fun updatePosition(positionMs: Long) {
        val state = _uiState.value as? VideoPlayerUiState.Ready ?: return
        _uiState.value = state.copy(positionMs = positionMs)
    }

    /**
     * Updates the total duration once the player has prepared the media.
     * Overrides the value sourced from metadata.
     */
    fun updateDuration(durationMs: Long) {
        val state = _uiState.value as? VideoPlayerUiState.Ready ?: return
        _uiState.value = state.copy(durationMs = durationMs)
    }

    /** Reflects the player's playing / paused state in [uiState]. */
    fun updatePlayingState(isPlaying: Boolean) {
        val state = _uiState.value as? VideoPlayerUiState.Ready ?: return
        _uiState.value = state.copy(isPlaying = isPlaying)
    }

    /** Changes the playback speed to [speed]. */
    fun setPlaybackSpeed(speed: Float) {
        val state = _uiState.value as? VideoPlayerUiState.Ready ?: return
        _uiState.value = state.copy(playbackSpeed = speed)
    }

    /**
     * Sets the A or B point of the AB-repeat range at [positionMs].
     *
     * On success the range is updated in [uiState].
     * If the resulting range would be invalid (B ≤ A), emits [abRepeatError]
     * and leaves the existing range unchanged.
     */
    fun setAbPoint(point: AbPoint, positionMs: Long) {
        val state = _uiState.value as? VideoPlayerUiState.Ready ?: return
        viewModelScope.launch {
            when (setAbPointUseCase.execute(mediaId, point, positionMs)) {
                is SetAbPointResult.Success -> {
                    val newAbStart = if (point == AbPoint.A) positionMs else state.abStartMs
                    val newAbEnd = if (point == AbPoint.B) positionMs else state.abEndMs
                    _uiState.value = state.copy(abStartMs = newAbStart, abEndMs = newAbEnd)
                }
                is SetAbPointResult.InvalidRange -> _abRepeatError.emit(Unit)
            }
        }
    }

    /** Clears the AB-repeat range and updates [uiState]. */
    fun resetAbRepeat() {
        val state = _uiState.value as? VideoPlayerUiState.Ready ?: return
        viewModelScope.launch {
            resetAbRepeatUseCase.execute(mediaId)
            _uiState.value = state.copy(abStartMs = null, abEndMs = null)
        }
    }

    /** Toggles visibility of the playback controls overlay. */
    fun toggleControls() {
        val state = _uiState.value as? VideoPlayerUiState.Ready ?: return
        _uiState.value = state.copy(isControlsVisible = !state.isControlsVisible)
    }

    /**
     * Persists the current playback state to the repository.
     * Automatically called when the ViewModel is cleared (screen leaves composition).
     */
    fun saveCurrentState() {
        val state = _uiState.value as? VideoPlayerUiState.Ready ?: return
        viewModelScope.launch {
            savePlaybackStateUseCase.execute(
                videoId = mediaId,
                positionMs = state.positionMs,
                abStartMs = state.abStartMs,
                abEndMs = state.abEndMs,
                playbackSpeed = state.playbackSpeed,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentState()
    }
}
