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
import kotlin.test.assertNull

class GetMediaByIdUseCaseTest {
    private val fakeItems = MutableStateFlow<List<MediaItem>>(emptyList())

    private val fakeRepository = object : MediaRepository {
        override fun getAllMedia(): Flow<List<MediaItem>> = fakeItems
        override suspend fun getMediaById(id: String): MediaItem? = fakeItems.value.find { it.id == id }
        override suspend fun addMedia(item: MediaItem) { fakeItems.update { it + item } }
        override suspend fun deleteMedia(id: String) { fakeItems.update { list -> list.filter { it.id != id } } }
        override suspend fun updateLastViewedAt(id: String, timestamp: Long) {}
        override suspend fun updateThumbnailPath(id: String, path: String) {}
        override suspend fun updateSortOrder(id: String, sortOrder: Long) {}
    }

    private val useCase = GetMediaByIdUseCase(fakeRepository)

    @Test
    fun `execute returns item when id exists`() = runTest {
        val item = mediaItem()
        fakeRepository.addMedia(item)

        val result = useCase.execute("id-1")

        assertEquals(item, result)
    }

    @Test
    fun `execute returns null when id does not exist`() = runTest {
        val result = useCase.execute("nonexistent")

        assertNull(result)
    }

    @Test
    fun `execute returns correct item among multiple`() = runTest {
        val item1 = mediaItem().copy(id = "id-1", displayName = "first.mp4")
        val item2 = mediaItem().copy(id = "id-2", displayName = "second.mp4")
        fakeRepository.addMedia(item1)
        fakeRepository.addMedia(item2)

        val result = useCase.execute("id-2")

        assertEquals(item2, result)
    }

    private fun mediaItem() = MediaItem(
        id = "id-1",
        filePath = "/internal/media/sample.mp4",
        mediaType = MediaType.VIDEO,
        displayName = "sample.mp4",
        durationMs = 30_000L,
        widthPx = 1920L,
        heightPx = 1080L,
        fileSizeBytes = 10_000_000L,
        thumbnailPath = null,
        takenAt = null,
        sortOrder = 0L,
        lastViewedAt = null,
        addedAt = 1_700_000_000L,
    )
}
