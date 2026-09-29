package io.github.kiyohitonara.biwa.domain.model

/** Persisted playback state for a single video or GIF. */
data class PlaybackState(
    /** ID of the associated [MediaItem]. */
    val videoId: String,
    /** Last known playback position in milliseconds. */
    val positionMs: Long,
    /** A-point of the AB-repeat range in milliseconds, or null if unset. */
    val abStartMs: Long?,
    /** B-point of the AB-repeat range in milliseconds, or null if unset. */
    val abEndMs: Long?,
    /** Last used playback speed multiplier (e.g. 0.5, 1.0, 2.0). */
    val playbackSpeed: Float,
    /** Unix epoch seconds when this record was last written. */
    val updatedAt: Long,
)
