package io.github.kiyohitonara.biwa.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase
import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackStateRepositoryImplTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: PlaybackStateRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BiwaDatabase.Schema.create(driver)
        repository = PlaybackStateRepositoryImpl(driver)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun `getPlaybackState returns null when no state exists`() = runTest {
        val result = repository.getPlaybackState("video-1")
        assertNull(result)
    }

    @Test
    fun `savePlaybackState persists state and getPlaybackState returns it`() = runTest {
        repository.savePlaybackState(playbackState())

        val result = repository.getPlaybackState("video-1")
        assertEquals("video-1", result?.videoId)
    }

    @Test
    fun `savePlaybackState preserves all fields`() = runTest {
        val state = playbackState()
        repository.savePlaybackState(state)

        val result = repository.getPlaybackState("video-1")!!
        assertEquals(state.videoId, result.videoId)
        assertEquals(state.positionMs, result.positionMs)
        assertEquals(state.abStartMs, result.abStartMs)
        assertEquals(state.abEndMs, result.abEndMs)
        assertEquals(state.playbackSpeed, result.playbackSpeed)
        assertEquals(state.updatedAt, result.updatedAt)
    }

    @Test
    fun `savePlaybackState preserves null ab points`() = runTest {
        repository.savePlaybackState(playbackState().copy(abStartMs = null, abEndMs = null))

        val result = repository.getPlaybackState("video-1")!!
        assertNull(result.abStartMs)
        assertNull(result.abEndMs)
    }

    @Test
    fun `savePlaybackState overwrites existing state for same videoId`() = runTest {
        repository.savePlaybackState(playbackState().copy(positionMs = 1_000L))
        repository.savePlaybackState(playbackState().copy(positionMs = 5_000L))

        val result = repository.getPlaybackState("video-1")
        assertEquals(5_000L, result?.positionMs)
    }

    @Test
    fun `savePlaybackState stores independent state per videoId`() = runTest {
        repository.savePlaybackState(playbackState().copy(videoId = "video-1", positionMs = 1_000L))
        repository.savePlaybackState(playbackState().copy(videoId = "video-2", positionMs = 2_000L))

        assertEquals(1_000L, repository.getPlaybackState("video-1")?.positionMs)
        assertEquals(2_000L, repository.getPlaybackState("video-2")?.positionMs)
    }

    @Test
    fun `savePlaybackState preserves fractional playback speed`() = runTest {
        repository.savePlaybackState(playbackState().copy(playbackSpeed = 0.5f))

        val result = repository.getPlaybackState("video-1")
        assertEquals(0.5f, result?.playbackSpeed)
    }

    private fun playbackState() = PlaybackState(
        videoId = "video-1",
        positionMs = 10_000L,
        abStartMs = 5_000L,
        abEndMs = 20_000L,
        playbackSpeed = 1.0f,
        updatedAt = 1_700_000_000L,
    )
}
