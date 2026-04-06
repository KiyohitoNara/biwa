package io.github.kiyohitonara.biwa.presentation.videoplayer

import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository

/** In-memory [PlaybackStateRepository] for use in tests. */
class FakePlaybackStateRepository : PlaybackStateRepository {
    private val stored = mutableMapOf<String, PlaybackState>()

    override suspend fun getPlaybackState(videoId: String): PlaybackState? = stored[videoId]
    override suspend fun savePlaybackState(state: PlaybackState) { stored[state.videoId] = state }
}
