package io.github.kiyohitonara.biwa.data.repository

import app.cash.sqldelight.db.SqlDriver
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase
import io.github.kiyohitonara.biwa.data.local.Playback_state
import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** SQLDelight-backed implementation of [PlaybackStateRepository]. */
class PlaybackStateRepositoryImpl(driver: SqlDriver) : PlaybackStateRepository {
    private val queries = BiwaDatabase(driver).playbackStateQueries

    override suspend fun getPlaybackState(videoId: String): PlaybackState? =
        withContext(Dispatchers.IO) {
            queries.selectByVideoId(videoId).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun savePlaybackState(state: PlaybackState) =
        withContext(Dispatchers.IO) {
            queries.upsert(
                video_id = state.videoId,
                position_ms = state.positionMs,
                ab_start_ms = state.abStartMs,
                ab_end_ms = state.abEndMs,
                playback_speed = state.playbackSpeed.toDouble(),
                updated_at = state.updatedAt,
            )
        }

    private fun Playback_state.toDomain() = PlaybackState(
        videoId = video_id,
        positionMs = position_ms,
        abStartMs = ab_start_ms,
        abEndMs = ab_end_ms,
        playbackSpeed = playback_speed.toFloat(),
        updatedAt = updated_at,
    )
}
