package io.github.kiyohitonara.biwa.domain.repository

import io.github.kiyohitonara.biwa.domain.model.PlaybackState

/** Manages persistence and retrieval of per-video playback state. */
interface PlaybackStateRepository {
    /** Returns the saved [PlaybackState] for [videoId], or null if none exists. */
    suspend fun getPlaybackState(videoId: String): PlaybackState?

    /** Inserts or replaces the playback state for [state.videoId]. */
    suspend fun savePlaybackState(state: PlaybackState)
}
