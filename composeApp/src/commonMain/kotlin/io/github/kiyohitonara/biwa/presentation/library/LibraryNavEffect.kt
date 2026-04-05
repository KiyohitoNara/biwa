package io.github.kiyohitonara.biwa.presentation.library

/** One-shot navigation events emitted by [LibraryViewModel]. */
sealed interface LibraryNavEffect {
    /** Navigate to the video player for the given media [id]. Used for VIDEO and GIF types. */
    data class OpenVideoPlayer(val id: String) : LibraryNavEffect

    /** Navigate to the photo viewer for the given media [id]. Used for PHOTO type. */
    data class OpenPhotoViewer(val id: String) : LibraryNavEffect
}
