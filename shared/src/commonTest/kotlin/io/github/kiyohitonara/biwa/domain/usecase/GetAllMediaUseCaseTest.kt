package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAllMediaUseCaseTest {
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

    private val useCase = GetAllMediaUseCase(fakeRepository)

    @Test
    fun `execute emits empty list when no items exist`() = runTest {
        val result = useCase.execute().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute emits list after item is added`() = runTest {
        fakeRepository.addMedia(videoItem())

        val result = useCase.execute().first()
        assertEquals(1, result.size)
        assertEquals("id-1", result.first().id)
    }

    @Test
    fun `execute reflects items added after subscription`() = runTest {
        val flow = useCase.execute()

        fakeRepository.addMedia(videoItem())

        val result = flow.first()
        assertEquals(1, result.size)
    }

    @Test
    fun `execute reflects item removal`() = runTest {
        fakeRepository.addMedia(videoItem())
        fakeRepository.deleteMedia("id-1")

        val result = useCase.execute().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute returns items in repository order`() = runTest {
        fakeRepository.addMedia(videoItem().copy(id = "a", filePath = "/media/a.mp4"))
        fakeRepository.addMedia(videoItem().copy(id = "b", filePath = "/media/b.mp4"))
        fakeRepository.addMedia(videoItem().copy(id = "c", filePath = "/media/c.mp4"))

        val result = useCase.execute().first()
        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }

    private fun addMediaUseCase() = AddMediaUseCase(
        repository = fakeRepository,
        fileStorage = object : FileStorage {
            override suspend fun copyToInternalStorage(sourceUri: String, fileName: String) =
                "/internal/media/$fileName"
            override suspend fun deleteFromInternalStorage(filePath: String) {}
        },
        clock = { 1_700_000_000L },
    )

    private fun videoItem() = MediaItem(
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
