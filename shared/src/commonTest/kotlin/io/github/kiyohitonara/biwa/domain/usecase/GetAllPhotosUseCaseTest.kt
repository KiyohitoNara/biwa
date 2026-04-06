package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAllPhotosUseCaseTest {
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

    private val useCase = GetAllPhotosUseCase(fakeRepository)

    @Test
    fun `execute emits empty list when library is empty`() = runTest {
        val result = useCase.execute().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute filters out VIDEO items`() = runTest {
        fakeRepository.addMedia(mediaItem("v", MediaType.VIDEO))
        fakeRepository.addMedia(mediaItem("p", MediaType.PHOTO))

        val result = useCase.execute().first()
        assertEquals(1, result.size)
        assertEquals("p", result.first().id)
    }

    @Test
    fun `execute filters out GIF items`() = runTest {
        fakeRepository.addMedia(mediaItem("g", MediaType.GIF))
        fakeRepository.addMedia(mediaItem("p", MediaType.PHOTO))

        val result = useCase.execute().first()
        assertEquals(1, result.size)
        assertEquals("p", result.first().id)
    }

    @Test
    fun `execute returns only PHOTO items`() = runTest {
        fakeRepository.addMedia(mediaItem("v", MediaType.VIDEO))
        fakeRepository.addMedia(mediaItem("g", MediaType.GIF))
        fakeRepository.addMedia(mediaItem("p1", MediaType.PHOTO))
        fakeRepository.addMedia(mediaItem("p2", MediaType.PHOTO))

        val result = useCase.execute().first()
        assertEquals(2, result.size)
        assertTrue(result.all { it.mediaType == MediaType.PHOTO })
    }

    @Test
    fun `execute preserves repository ordering`() = runTest {
        fakeItems.value = listOf(
            mediaItem("newer", MediaType.PHOTO),
            mediaItem("older", MediaType.PHOTO),
        )

        val result = useCase.execute().first()
        assertEquals("newer", result[0].id)
        assertEquals("older", result[1].id)
    }

    @Test
    fun `execute reacts to new photos added to library`() = runTest {
        fakeRepository.addMedia(mediaItem("p1", MediaType.PHOTO))
        val before = useCase.execute().first()
        assertEquals(1, before.size)

        fakeRepository.addMedia(mediaItem("p2", MediaType.PHOTO))
        val after = useCase.execute().first()
        assertEquals(2, after.size)
    }

    private fun mediaItem(id: String, mediaType: MediaType) = MediaItem(
        id = id,
        filePath = "/internal/media/$id",
        mediaType = mediaType,
        displayName = "$id.jpg",
        durationMs = null,
        widthPx = 1920L,
        heightPx = 1080L,
        fileSizeBytes = 5_000_000L,
        thumbnailPath = null,
        takenAt = null,
        sortOrder = 0L,
        lastViewedAt = null,
        addedAt = 1_700_000_000L,
    )
}
