package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.repository.ThumbnailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GenerateThumbnailUseCaseTest {
    private val fakeItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private var thumbnailPathToReturn: String? = "/cache/thumbnails/thumb.jpg"
    private val updatedThumbnailPaths = mutableMapOf<String, String>()

    private val fakeMediaRepository = object : MediaRepository {
        override fun getAllMedia(): Flow<List<MediaItem>> = fakeItems
        override suspend fun getMediaById(id: String): MediaItem? = fakeItems.value.find { it.id == id }
        override suspend fun addMedia(item: MediaItem) { fakeItems.update { it + item } }
        override suspend fun deleteMedia(id: String) { fakeItems.update { list -> list.filter { it.id != id } } }
        override suspend fun updateLastViewedAt(id: String, timestamp: Long) {}
        override suspend fun updateThumbnailPath(id: String, path: String) { updatedThumbnailPaths[id] = path }
        override suspend fun updateSortOrder(id: String, sortOrder: Long) {}
    }

    private val fakeThumbnailRepository = object : ThumbnailRepository {
        override suspend fun generateVideoThumbnail(videoPath: String): String? = thumbnailPathToReturn
    }

    private val useCase = GenerateThumbnailUseCase(fakeThumbnailRepository, fakeMediaRepository)

    @Test
    fun `execute returns null for PHOTO items`() = runTest {
        val result = useCase.execute(mediaItem("p", MediaType.PHOTO))
        assertNull(result)
    }

    @Test
    fun `execute returns null for GIF items`() = runTest {
        val result = useCase.execute(mediaItem("g", MediaType.GIF))
        assertNull(result)
    }

    @Test
    fun `execute returns existing thumbnailPath for VIDEO with cached thumbnail`() = runTest {
        val item = mediaItem("v", MediaType.VIDEO).copy(thumbnailPath = "/cache/existing.jpg")
        val result = useCase.execute(item)
        assertEquals("/cache/existing.jpg", result)
    }

    @Test
    fun `execute does not call repository when VIDEO already has thumbnailPath`() = runTest {
        val item = mediaItem("v", MediaType.VIDEO).copy(thumbnailPath = "/cache/existing.jpg")
        useCase.execute(item)
        assertEquals(0, updatedThumbnailPaths.size)
    }

    @Test
    fun `execute generates thumbnail and updates repository for VIDEO without cache`() = runTest {
        val item = mediaItem("v", MediaType.VIDEO)
        val result = useCase.execute(item)
        assertEquals("/cache/thumbnails/thumb.jpg", result)
        assertEquals("/cache/thumbnails/thumb.jpg", updatedThumbnailPaths["v"])
    }

    @Test
    fun `execute returns null when thumbnail generation fails`() = runTest {
        thumbnailPathToReturn = null
        val result = useCase.execute(mediaItem("v", MediaType.VIDEO))
        assertNull(result)
    }

    @Test
    fun `execute does not update repository when thumbnail generation fails`() = runTest {
        thumbnailPathToReturn = null
        useCase.execute(mediaItem("v", MediaType.VIDEO))
        assertEquals(0, updatedThumbnailPaths.size)
    }

    private fun mediaItem(id: String, mediaType: MediaType) = MediaItem(
        id = id,
        filePath = "/internal/media/$id",
        mediaType = mediaType,
        displayName = "$id.file",
        durationMs = if (mediaType == MediaType.VIDEO) 30_000L else null,
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
