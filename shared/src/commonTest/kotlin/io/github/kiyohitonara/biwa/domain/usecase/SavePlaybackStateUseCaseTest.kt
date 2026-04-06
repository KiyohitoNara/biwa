package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavePlaybackStateUseCaseTest {
    private val stored = mutableMapOf<String, PlaybackState>()

    private val fakeRepository = object : PlaybackStateRepository {
        override suspend fun getPlaybackState(videoId: String): PlaybackState? = stored[videoId]
        override suspend fun savePlaybackState(state: PlaybackState) { stored[state.videoId] = state }
    }

    private val fixedTime = 1_700_000_000L
    private val useCase = SavePlaybackStateUseCase(fakeRepository, clock = { fixedTime })

    @Test
    fun `execute persists state with clock timestamp`() = runTest {
        useCase.execute(
            videoId = "video-1",
            positionMs = 10_000L,
            abStartMs = null,
            abEndMs = null,
            playbackSpeed = 1.0f,
        )

        val result = fakeRepository.getPlaybackState("video-1")
        assertEquals(fixedTime, result?.updatedAt)
    }

    @Test
    fun `execute preserves all fields`() = runTest {
        useCase.execute(
            videoId = "video-1",
            positionMs = 10_000L,
            abStartMs = 5_000L,
            abEndMs = 20_000L,
            playbackSpeed = 0.5f,
        )

        val result = fakeRepository.getPlaybackState("video-1")!!
        assertEquals("video-1", result.videoId)
        assertEquals(10_000L, result.positionMs)
        assertEquals(5_000L, result.abStartMs)
        assertEquals(20_000L, result.abEndMs)
        assertEquals(0.5f, result.playbackSpeed)
    }

    @Test
    fun `execute preserves null ab points`() = runTest {
        useCase.execute(
            videoId = "video-1",
            positionMs = 0L,
            abStartMs = null,
            abEndMs = null,
            playbackSpeed = 1.0f,
        )

        val result = fakeRepository.getPlaybackState("video-1")!!
        assertNull(result.abStartMs)
        assertNull(result.abEndMs)
    }

    @Test
    fun `execute overwrites existing state`() = runTest {
        useCase.execute("video-1", positionMs = 1_000L, abStartMs = null, abEndMs = null, playbackSpeed = 1.0f)
        useCase.execute("video-1", positionMs = 5_000L, abStartMs = null, abEndMs = null, playbackSpeed = 1.0f)

        val result = fakeRepository.getPlaybackState("video-1")
        assertEquals(5_000L, result?.positionMs)
    }
}
