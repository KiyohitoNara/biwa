package io.github.kiyohitonara.biwa.domain.model

/** Represents the type of a media file managed in the library. */
enum class MediaType {
    /** A video file with audio and/or motion. */
    VIDEO,

    /** An animated GIF image. */
    GIF,

    /** A still photo. */
    PHOTO,
}
