package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository

/**
 * Clears the AB-repeat range for a video, restoring normal playback.
 *
 * Does nothing if no playback state exists for the given video.
 *
 * @param repository Persistence layer for playback state.
 * @param clock Provides the current time as Unix epoch seconds.
 */
class ResetAbRepeatUseCase(
    private val repository: PlaybackStateRepository,
    private val clock: () -> Long,
) {
    /**
     * Removes the A and B points for [videoId] and persists the updated state.
     */
    suspend fun execute(videoId: String) {
        val current = repository.getPlaybackState(videoId) ?: return
        repository.savePlaybackState(
            current.copy(abStartMs = null, abEndMs = null, updatedAt = clock())
        )
    }
}
