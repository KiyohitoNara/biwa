package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReorderMediaUseCaseTest {
    private val sortOrderUpdates = mutableListOf<Pair<String, Long>>()

    private val fakeRepository = object : MediaRepository {
        override fun getAllMedia(): Flow<List<MediaItem>> = MutableStateFlow(emptyList())
        override suspend fun getMediaById(id: String): MediaItem? = null
        override suspend fun addMedia(item: MediaItem) {}
        override suspend fun deleteMedia(id: String) {}
        override suspend fun updateLastViewedAt(id: String, timestamp: Long) {}
        override suspend fun updateThumbnailPath(id: String, path: String) {}
        override suspend fun updateSortOrder(id: String, sortOrder: Long) {
            sortOrderUpdates.add(id to sortOrder)
        }
    }

    private val useCase = ReorderMediaUseCase(fakeRepository)

    @Test
    fun `execute assigns 0-based indices as sort_order`() = runTest {
        useCase.execute(listOf("a", "b", "c"))

        assertEquals(0L, sortOrderUpdates.first { it.first == "a" }.second)
        assertEquals(1L, sortOrderUpdates.first { it.first == "b" }.second)
        assertEquals(2L, sortOrderUpdates.first { it.first == "c" }.second)
    }

    @Test
    fun `execute calls updateSortOrder for every id`() = runTest {
        useCase.execute(listOf("x", "y"))

        assertEquals(2, sortOrderUpdates.size)
    }

    @Test
    fun `execute with single item sets sortOrder to 0`() = runTest {
        useCase.execute(listOf("only"))

        assertEquals(0L, sortOrderUpdates.single().second)
    }

    @Test
    fun `execute with empty list does nothing`() = runTest {
        useCase.execute(emptyList())

        assertTrue(sortOrderUpdates.isEmpty())
    }
}
