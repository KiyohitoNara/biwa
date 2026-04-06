package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.AbPoint
import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import io.github.kiyohitonara.biwa.domain.model.SetAbPointResult
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SetAbPointUseCaseTest {
    private val stored = mutableMapOf<String, PlaybackState>()

    private val fakeRepository = object : PlaybackStateRepository {
        override suspend fun getPlaybackState(videoId: String): PlaybackState? = stored[videoId]
        override suspend fun savePlaybackState(state: PlaybackState) { stored[state.videoId] = state }
    }

    private val useCase = SetAbPointUseCase(fakeRepository, clock = { 1_700_000_000L })

    @Test
    fun `execute sets A point when no existing state`() = runTest {
        useCase.execute("video-1", AbPoint.A, 5_000L)

        assertEquals(5_000L, fakeRepository.getPlaybackState("video-1")?.abStartMs)
    }

    @Test
    fun `execute sets B point when no existing state`() = runTest {
        useCase.execute("video-1", AbPoint.B, 20_000L)

        assertEquals(20_000L, fakeRepository.getPlaybackState("video-1")?.abEndMs)
    }

    @Test
    fun `execute updates A point preserving existing B`() = runTest {
        stored["video-1"] = playbackState().copy(abStartMs = 1_000L, abEndMs = 20_000L)

        useCase.execute("video-1", AbPoint.A, 5_000L)

        val result = fakeRepository.getPlaybackState("video-1")
        assertEquals(5_000L, result?.abStartMs)
        assertEquals(20_000L, result?.abEndMs)
    }

    @Test
    fun `execute updates B point preserving existing A`() = runTest {
        stored["video-1"] = playbackState().copy(abStartMs = 5_000L, abEndMs = 10_000L)

        useCase.execute("video-1", AbPoint.B, 20_000L)

        val result = fakeRepository.getPlaybackState("video-1")
        assertEquals(5_000L, result?.abStartMs)
        assertEquals(20_000L, result?.abEndMs)
    }

    @Test
    fun `execute returns Success when A point only set`() = runTest {
        val result = useCase.execute("video-1", AbPoint.A, 5_000L)
        assertIs<SetAbPointResult.Success>(result)
    }

    @Test
    fun `execute returns Success when B point only set`() = runTest {
        val result = useCase.execute("video-1", AbPoint.B, 20_000L)
        assertIs<SetAbPointResult.Success>(result)
    }

    @Test
    fun `execute returns Success when B is greater than A`() = runTest {
        stored["video-1"] = playbackState().copy(abStartMs = 5_000L, abEndMs = null)

        val result = useCase.execute("video-1", AbPoint.B, 20_000L)
        assertIs<SetAbPointResult.Success>(result)
    }

    @Test
    fun `execute returns InvalidRange when B equals A`() = runTest {
        stored["video-1"] = playbackState().copy(abStartMs = 5_000L, abEndMs = null)

        val result = useCase.execute("video-1", AbPoint.B, 5_000L)
        assertIs<SetAbPointResult.InvalidRange>(result)
    }

    @Test
    fun `execute returns InvalidRange when B is less than A`() = runTest {
        stored["video-1"] = playbackState().copy(abStartMs = 10_000L, abEndMs = null)

        val result = useCase.execute("video-1", AbPoint.B, 5_000L)
        assertIs<SetAbPointResult.InvalidRange>(result)
    }

    @Test
    fun `execute does not save state when range is invalid`() = runTest {
        stored["video-1"] = playbackState().copy(abStartMs = 10_000L, abEndMs = 20_000L)

        useCase.execute("video-1", AbPoint.B, 5_000L)

        assertEquals(20_000L, fakeRepository.getPlaybackState("video-1")?.abEndMs)
    }

    @Test
    fun `execute uses defaults for position and speed when no prior state`() = runTest {
        useCase.execute("video-1", AbPoint.A, 5_000L)

        val result = fakeRepository.getPlaybackState("video-1")!!
        assertEquals(0L, result.positionMs)
        assertEquals(1.0f, result.playbackSpeed)
    }

    @Test
    fun `execute preserves existing position and speed`() = runTest {
        stored["video-1"] = playbackState().copy(positionMs = 15_000L, playbackSpeed = 0.5f, abStartMs = null, abEndMs = null)

        useCase.execute("video-1", AbPoint.A, 5_000L)

        val result = fakeRepository.getPlaybackState("video-1")!!
        assertEquals(15_000L, result.positionMs)
        assertEquals(0.5f, result.playbackSpeed)
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
