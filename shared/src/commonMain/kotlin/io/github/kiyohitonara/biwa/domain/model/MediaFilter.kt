package io.github.kiyohitonara.biwa.domain.model

/** Media-type filter options for the library screen. */
enum class MediaFilter {
    /** Show all media items regardless of type. */
    ALL,

    /** Show only [MediaType.VIDEO] items. */
    VIDEO,

    /** Show only [MediaType.GIF] items. */
    GIF,

    /** Show only [MediaType.PHOTO] items. */
    PHOTO,
}
