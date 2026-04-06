package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.AbPoint
import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import io.github.kiyohitonara.biwa.domain.model.SetAbPointResult
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository

/**
 * Sets the A or B point of the AB-repeat range for a video.
 *
 * Enforces that A-point < B-point. If the resulting range would be invalid,
 * no state is saved and [SetAbPointResult.InvalidRange] is returned.
 *
 * @param repository Persistence layer for playback state.
 * @param clock Provides the current time as Unix epoch seconds.
 */
class SetAbPointUseCase(
    private val repository: PlaybackStateRepository,
    private val clock: () -> Long,
) {
    /**
     * Applies [point] at [positionMs] for [videoId].
     *
     * Returns [SetAbPointResult.Success] when the state is saved, or
     * [SetAbPointResult.InvalidRange] when the update would produce B ≤ A.
     */
    suspend fun execute(
        videoId: String,
        point: AbPoint,
        positionMs: Long,
    ): SetAbPointResult {
        val current = repository.getPlaybackState(videoId)
        val newAbStart = if (point == AbPoint.A) positionMs else current?.abStartMs
        val newAbEnd = if (point == AbPoint.B) positionMs else current?.abEndMs

        if (newAbStart != null && newAbEnd != null && newAbEnd <= newAbStart) {
            return SetAbPointResult.InvalidRange
        }

        repository.savePlaybackState(
            PlaybackState(
                videoId = videoId,
                positionMs = current?.positionMs ?: 0L,
                abStartMs = newAbStart,
                abEndMs = newAbEnd,
                playbackSpeed = current?.playbackSpeed ?: 1.0f,
                updatedAt = clock(),
            )
        )
        return SetAbPointResult.Success
    }
}
