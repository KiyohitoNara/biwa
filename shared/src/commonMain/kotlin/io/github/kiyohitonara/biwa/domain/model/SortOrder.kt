package io.github.kiyohitonara.biwa.domain.model

/** Ordering options for the media library. */
enum class SortOrder {
    /** Added date, newest first (default). */
    ADDED_AT_DESC,

    /** Added date, oldest first. */
    ADDED_AT_ASC,

    /** Display name, alphabetically ascending. */
    FILE_NAME,

    /** Last viewed date, most recent first. Items never viewed sort last. */
    LAST_VIEWED_AT,

    /** File size, largest first. */
    FILE_SIZE,

    /** User-defined manual order stored in [MediaItem.sortOrder]. */
    MANUAL,
}
