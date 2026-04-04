package io.github.kiyohitonara.biwa.domain.model

/** Input for adding a new media file to the library. */
data class AddMediaRequest(
    /** Platform-specific URI or path of the source file to copy. */
    val sourceUri: String,
    /** Destination file name used when copying to internal storage. */
    val fileName: String,
    /** Type of the media file. */
    val mediaType: MediaType,
    /** Display name shown in the UI. */
    val displayName: String,
    /** Total playback duration in milliseconds. Null for photos. */
    val durationMs: Long? = null,
    /** Width in pixels. */
    val widthPx: Long? = null,
    /** Height in pixels. */
    val heightPx: Long? = null,
    /** File size in bytes. */
    val fileSizeBytes: Long? = null,
    /** Capture or creation timestamp (Unix epoch seconds). */
    val takenAt: Long? = null,
    /** Initial sort order value. */
    val sortOrder: Long = 0L,
)
