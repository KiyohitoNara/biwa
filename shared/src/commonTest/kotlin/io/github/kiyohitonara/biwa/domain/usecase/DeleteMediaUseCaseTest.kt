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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteMediaUseCaseTest {
    private val fakeItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val deletedPaths = mutableListOf<String>()

    private val fakeRepository = object : MediaRepository {
        override fun getAllMedia(): Flow<List<MediaItem>> = fakeItems
        override suspend fun getMediaById(id: String): MediaItem? = fakeItems.value.find { it.id == id }
        override suspend fun addMedia(item: MediaItem) { fakeItems.update { it + item } }
        override suspend fun deleteMedia(id: String) { fakeItems.update { list -> list.filter { it.id != id } } }
        override suspend fun updateLastViewedAt(id: String, timestamp: Long) {}
        override suspend fun updateThumbnailPath(id: String, path: String) {}
        override suspend fun updateSortOrder(id: String, sortOrder: Long) {}
    }

    private val fakeFileStorage = object : FileStorage {
        override suspend fun copyToInternalStorage(sourceUri: String, fileName: String) =
            "/internal/media/$fileName"
        override suspend fun deleteFromInternalStorage(filePath: String) {
            deletedPaths.add(filePath)
        }
    }

    private val useCase = DeleteMediaUseCase(fakeRepository, fakeFileStorage)

    @Test
    fun `execute removes item from repository`() = runTest {
        fakeRepository.addMedia(videoItem())

        useCase.execute("id-1")

        val result = fakeRepository.getAllMedia().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute deletes file from storage`() = runTest {
        fakeRepository.addMedia(videoItem())

        useCase.execute("id-1")

        assertEquals(listOf("/internal/media/sample.mp4"), deletedPaths)
    }

    @Test
    fun `execute does nothing when id does not exist`() = runTest {
        useCase.execute("nonexistent")

        assertTrue(deletedPaths.isEmpty())
    }

    @Test
    fun `execute does not affect other items`() = runTest {
        fakeRepository.addMedia(videoItem().copy(id = "a", filePath = "/internal/media/a.mp4"))
        fakeRepository.addMedia(videoItem().copy(id = "b", filePath = "/internal/media/b.mp4"))

        useCase.execute("a")

        val result = fakeRepository.getAllMedia().first()
        assertEquals(1, result.size)
        assertEquals("b", result.first().id)
    }

    @Test
    fun `execute looks up item before deleting to get filePath`() = runTest {
        fakeRepository.addMedia(videoItem().copy(filePath = "/internal/media/custom.mp4"))

        useCase.execute("id-1")

        assertEquals("/internal/media/custom.mp4", deletedPaths.first())
    }

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
