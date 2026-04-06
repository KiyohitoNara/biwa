package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetPlaybackStateUseCaseTest {
    private val stored = mutableMapOf<String, PlaybackState>()

    private val fakeRepository = object : PlaybackStateRepository {
        override suspend fun getPlaybackState(videoId: String): PlaybackState? = stored[videoId]
        override suspend fun savePlaybackState(state: PlaybackState) { stored[state.videoId] = state }
    }

    private val useCase = GetPlaybackStateUseCase(fakeRepository)

    @Test
    fun `execute returns null when no state exists`() = runTest {
        val result = useCase.execute("video-1")
        assertNull(result)
    }

    @Test
    fun `execute returns saved state`() = runTest {
        fakeRepository.savePlaybackState(playbackState())

        val result = useCase.execute("video-1")
        assertEquals(playbackState(), result)
    }

    @Test
    fun `execute returns correct state among multiple`() = runTest {
        fakeRepository.savePlaybackState(playbackState().copy(videoId = "video-1", positionMs = 1_000L))
        fakeRepository.savePlaybackState(playbackState().copy(videoId = "video-2", positionMs = 2_000L))

        val result = useCase.execute("video-2")
        assertEquals(2_000L, result?.positionMs)
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
