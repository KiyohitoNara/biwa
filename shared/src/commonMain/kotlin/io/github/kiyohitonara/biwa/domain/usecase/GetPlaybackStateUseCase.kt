package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository

/**
 * Retrieves the saved playback state for a video.
 *
 * @param repository Persistence layer for playback state.
 */
class GetPlaybackStateUseCase(
    private val repository: PlaybackStateRepository,
) {
    /**
     * Returns the saved [PlaybackState] for [videoId], or null if none exists.
     */
    suspend fun execute(videoId: String): PlaybackState? {
        return repository.getPlaybackState(videoId)
    }
}
