package io.github.kiyohitonara.biwa.domain.model

/**
 * Reorder actions the user can apply to the persisted manual order.
 *
 * The library always displays items in [MediaItem.sortOrder] sequence; selecting
 * one of these actions computes a new ordering by the chosen field and persists
 * it via the reorder use cases.
 */
enum class SortOrder {
    /** Added date, newest first. */
    ADDED_AT_DESC,

    /** Added date, oldest first. */
    ADDED_AT_ASC,

    /** Display name, alphabetically ascending. */
    FILE_NAME,

    /** Last viewed date, most recent first. Items never viewed sort last. */
    LAST_VIEWED_AT,

    /** File size, largest first. */
    FILE_SIZE,
}
