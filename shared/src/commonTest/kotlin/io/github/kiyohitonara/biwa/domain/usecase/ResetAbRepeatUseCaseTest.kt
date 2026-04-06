package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResetAbRepeatUseCaseTest {
    private val stored = mutableMapOf<String, PlaybackState>()

    private val fakeRepository = object : PlaybackStateRepository {
        override suspend fun getPlaybackState(videoId: String): PlaybackState? = stored[videoId]
        override suspend fun savePlaybackState(state: PlaybackState) { stored[state.videoId] = state }
    }

    private val useCase = ResetAbRepeatUseCase(fakeRepository, clock = { 1_700_000_000L })

    @Test
    fun `execute clears both ab points`() = runTest {
        stored["video-1"] = playbackState()

        useCase.execute("video-1")

        val result = fakeRepository.getPlaybackState("video-1")!!
        assertNull(result.abStartMs)
        assertNull(result.abEndMs)
    }

    @Test
    fun `execute preserves position and speed`() = runTest {
        stored["video-1"] = playbackState().copy(positionMs = 15_000L, playbackSpeed = 0.5f)

        useCase.execute("video-1")

        val result = fakeRepository.getPlaybackState("video-1")!!
        assertEquals(15_000L, result.positionMs)
        assertEquals(0.5f, result.playbackSpeed)
    }

    @Test
    fun `execute does nothing when no state exists`() = runTest {
        useCase.execute("nonexistent")

        assertNull(fakeRepository.getPlaybackState("nonexistent"))
    }

    @Test
    fun `execute updates timestamp`() = runTest {
        stored["video-1"] = playbackState().copy(updatedAt = 1_000L)

        useCase.execute("video-1")

        assertEquals(1_700_000_000L, fakeRepository.getPlaybackState("video-1")?.updatedAt)
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
