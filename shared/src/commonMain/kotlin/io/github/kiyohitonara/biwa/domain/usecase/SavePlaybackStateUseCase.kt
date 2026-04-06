package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository

/**
 * Persists the current playback state for a video.
 *
 * @param repository Persistence layer for playback state.
 * @param clock Provides the current time as Unix epoch seconds.
 */
class SavePlaybackStateUseCase(
    private val repository: PlaybackStateRepository,
    private val clock: () -> Long,
) {
    /**
     * Saves the playback state for [videoId] with the given parameters.
     * An existing record for the same [videoId] is replaced.
     */
    suspend fun execute(
        videoId: String,
        positionMs: Long,
        abStartMs: Long?,
        abEndMs: Long?,
        playbackSpeed: Float,
    ) {
        repository.savePlaybackState(
            PlaybackState(
                videoId = videoId,
                positionMs = positionMs,
                abStartMs = abStartMs,
                abEndMs = abEndMs,
                playbackSpeed = playbackSpeed,
                updatedAt = clock(),
            )
        )
    }
}
