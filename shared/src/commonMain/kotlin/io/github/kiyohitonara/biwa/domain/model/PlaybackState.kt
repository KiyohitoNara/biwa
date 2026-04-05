package io.github.kiyohitonara.biwa.domain.model

/**
 * Persisted playback state for a single video or GIF.
 *
 * @param videoId ID of the associated [MediaItem].
 * @param positionMs Last known playback position in milliseconds.
 * @param abStartMs A-point of the AB-repeat range in milliseconds, or null if unset.
 * @param abEndMs B-point of the AB-repeat range in milliseconds, or null if unset.
 * @param playbackSpeed Last used playback speed multiplier (e.g. 0.5, 1.0, 2.0).
 * @param updatedAt Unix epoch seconds when this record was last written.
 */
data class PlaybackState(
    val videoId: String,
    val positionMs: Long,
    val abStartMs: Long?,
    val abEndMs: Long?,
    val playbackSpeed: Float,
    val updatedAt: Long,
)
