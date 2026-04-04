package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.AddMediaRequest
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddMediaUseCaseTest {
    private val fixedTime = 1_700_000_000L

    private val fakeFileStorage = object : FileStorage {
        val copiedFiles = mutableListOf<Pair<String, String>>()

        override suspend fun copyToInternalStorage(sourceUri: String, fileName: String): String {
            copiedFiles.add(sourceUri to fileName)
            return "/internal/media/$fileName"
        }

        override suspend fun deleteFromInternalStorage(filePath: String) {}
    }

    private val fakeRepository = object : MediaRepository {
        val items = mutableListOf<MediaItem>()

        override fun getAllMedia(): Flow<List<MediaItem>> = flowOf(items.toList())

        override suspend fun getMediaById(id: String): MediaItem? = items.find { it.id == id }

        override suspend fun addMedia(item: MediaItem) {
            items.add(item)
        }

        override suspend fun deleteMedia(id: String) {
            items.removeAll { it.id == id }
        }

        override suspend fun updateLastViewedAt(id: String, timestamp: Long) {}

        override suspend fun updateThumbnailPath(id: String, path: String) {}

        override suspend fun updateSortOrder(id: String, sortOrder: Long) {}
    }

    private val useCase = AddMediaUseCase(
        repository = fakeRepository,
        fileStorage = fakeFileStorage,
        clock = { fixedTime },
    )

    @Test
    fun `execute copies file to internal storage`() = runTest {
        val request = videoRequest()
        useCase.execute(request)

        assertEquals(1, fakeFileStorage.copiedFiles.size)
        assertEquals("content://media/sample.mp4" to "sample.mp4", fakeFileStorage.copiedFiles.first())
    }

    @Test
    fun `execute saves item to repository`() = runTest {
        useCase.execute(videoRequest())

        assertEquals(1, fakeRepository.items.size)
    }

    @Test
    fun `execute returns MediaItem with resolved filePath`() = runTest {
        val result = useCase.execute(videoRequest())

        assertEquals("/internal/media/sample.mp4", result.filePath)
    }

    @Test
    fun `execute returns MediaItem with addedAt from clock`() = runTest {
        val result = useCase.execute(videoRequest())

        assertEquals(fixedTime, result.addedAt)
    }

    @Test
    fun `execute returns MediaItem with null thumbnailPath and lastViewedAt`() = runTest {
        val result = useCase.execute(videoRequest())

        assertNull(result.thumbnailPath)
        assertNull(result.lastViewedAt)
    }

    @Test
    fun `execute returns MediaItem preserving request fields`() = runTest {
        val request = videoRequest()
        val result = useCase.execute(request)

        assertEquals(request.mediaType, result.mediaType)
        assertEquals(request.displayName, result.displayName)
        assertEquals(request.durationMs, result.durationMs)
        assertEquals(request.widthPx, result.widthPx)
        assertEquals(request.heightPx, result.heightPx)
        assertEquals(request.fileSizeBytes, result.fileSizeBytes)
        assertEquals(request.takenAt, result.takenAt)
        assertEquals(request.sortOrder, result.sortOrder)
    }

    @Test
    fun `execute generates non-blank id`() = runTest {
        val result = useCase.execute(videoRequest())

        assertNotNull(result.id)
        assertTrue(result.id.isNotBlank())
    }

    @Test
    fun `execute generates unique ids across calls`() = runTest {
        val first = useCase.execute(videoRequest().copy(fileName = "a.mp4"))
        val second = useCase.execute(videoRequest().copy(fileName = "b.mp4"))

        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `execute supports VIDEO media type`() = runTest {
        val result = useCase.execute(videoRequest().copy(mediaType = MediaType.VIDEO))

        assertEquals(MediaType.VIDEO, result.mediaType)
    }

    @Test
    fun `execute supports GIF media type`() = runTest {
        val result = useCase.execute(videoRequest().copy(mediaType = MediaType.GIF, fileName = "anim.gif"))

        assertEquals(MediaType.GIF, result.mediaType)
    }

    @Test
    fun `execute supports PHOTO media type`() = runTest {
        val result = useCase.execute(videoRequest().copy(mediaType = MediaType.PHOTO, durationMs = null, fileName = "photo.jpg"))

        assertEquals(MediaType.PHOTO, result.mediaType)
    }

    @Test
    fun `execute persists returned item to repository with matching id`() = runTest {
        val result = useCase.execute(videoRequest())
        val stored = fakeRepository.items.find { it.id == result.id }

        assertNotNull(stored)
        assertEquals(result, stored)
    }

    private fun videoRequest() = AddMediaRequest(
        sourceUri = "content://media/sample.mp4",
        fileName = "sample.mp4",
        mediaType = MediaType.VIDEO,
        displayName = "sample.mp4",
        durationMs = 30_000L,
        widthPx = 1920L,
        heightPx = 1080L,
        fileSizeBytes = 10_000_000L,
        takenAt = null,
        sortOrder = 0L,
    )
}
