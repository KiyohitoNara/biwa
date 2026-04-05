package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateLastViewedAtUseCaseTest {
    private val fakeItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val recordedUpdates = mutableListOf<Pair<String, Long>>()

    private val fakeRepository = object : MediaRepository {
        override fun getAllMedia(): Flow<List<MediaItem>> = fakeItems
        override suspend fun getMediaById(id: String): MediaItem? = fakeItems.value.find { it.id == id }
        override suspend fun addMedia(item: MediaItem) { fakeItems.update { it + item } }
        override suspend fun deleteMedia(id: String) { fakeItems.update { list -> list.filter { it.id != id } } }
        override suspend fun updateLastViewedAt(id: String, timestamp: Long) {
            recordedUpdates.add(id to timestamp)
        }
        override suspend fun updateThumbnailPath(id: String, path: String) {}
        override suspend fun updateSortOrder(id: String, sortOrder: Long) {}
    }

    private val fixedTime = 1_700_000_000L
    private val useCase = UpdateLastViewedAtUseCase(fakeRepository, clock = { fixedTime })

    @Test
    fun `execute passes clock timestamp to repository`() = runTest {
        useCase.execute("id-1")

        assertEquals(listOf("id-1" to fixedTime), recordedUpdates)
    }

    @Test
    fun `execute uses current clock value each call`() = runTest {
        var time = 1_000L
        val useCase = UpdateLastViewedAtUseCase(fakeRepository, clock = { time })

        useCase.execute("id-1")
        time = 2_000L
        useCase.execute("id-1")

        assertEquals(listOf("id-1" to 1_000L, "id-1" to 2_000L), recordedUpdates)
    }

    @Test
    fun `execute does not throw for non-existent id`() = runTest {
        useCase.execute("nonexistent")

        assertTrue(recordedUpdates.isNotEmpty())
    }

    @Test
    fun `execute passes correct id to repository`() = runTest {
        useCase.execute("target-id")

        assertEquals("target-id", recordedUpdates.first().first)
    }
}
