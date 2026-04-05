package io.github.kiyohitonara.biwa.domain.model

/** Raw metadata extracted from a media file before it is added to the library. */
data class MediaFileMetadata(
    /** File name including extension. */
    val fileName: String,
    /** Detected media type. */
    val mediaType: MediaType,
    /** Total playback duration in milliseconds. Null for photos. */
    val durationMs: Long? = null,
    /** Width in pixels. */
    val widthPx: Long? = null,
    /** Height in pixels. */
    val heightPx: Long? = null,
    /** File size in bytes. */
    val fileSizeBytes: Long? = null,
    /** Capture timestamp (Unix epoch seconds). */
    val takenAt: Long? = null,
)
