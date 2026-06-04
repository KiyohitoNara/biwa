package io.github.kiyohitonara.biwa.presentation.library

/** One-shot navigation events emitted by [LibraryViewModel]. */
sealed interface LibraryNavEffect {
    /** Navigate to the unified media viewer positioned at the given media [id]. */
    data class OpenMediaViewer(val id: String) : LibraryNavEffect
}
