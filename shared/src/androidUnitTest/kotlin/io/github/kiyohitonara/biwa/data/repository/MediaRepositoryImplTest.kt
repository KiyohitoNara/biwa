package io.github.kiyohitonara.biwa.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaRepositoryImplTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: MediaRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BiwaDatabase.Schema.create(driver)
        repository = MediaRepositoryImpl(driver)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun `getAllMedia emits empty list when no items exist`() = runTest {
        val result = repository.getAllMedia().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `addMedia inserts item and getAllMedia emits it`() = runTest {
        repository.addMedia(videoItem())

        val result = repository.getAllMedia().first()
        assertEquals(1, result.size)
        assertEquals("id-1", result.first().id)
    }

    @Test
    fun `addMedia preserves all fields`() = runTest {
        val item = videoItem()
        repository.addMedia(item)

        val stored = repository.getAllMedia().first().first()
        assertEquals(item.id, stored.id)
        assertEquals(item.filePath, stored.filePath)
        assertEquals(item.mediaType, stored.mediaType)
        assertEquals(item.displayName, stored.displayName)
        assertEquals(item.durationMs, stored.durationMs)
        assertEquals(item.widthPx, stored.widthPx)
        assertEquals(item.heightPx, stored.heightPx)
        assertEquals(item.fileSizeBytes, stored.fileSizeBytes)
        assertEquals(item.thumbnailPath, stored.thumbnailPath)
        assertEquals(item.takenAt, stored.takenAt)
        assertEquals(item.sortOrder, stored.sortOrder)
        assertEquals(item.lastViewedAt, stored.lastViewedAt)
        assertEquals(item.addedAt, stored.addedAt)
    }

    @Test
    fun `addMedia supports VIDEO, GIF, and PHOTO types`() = runTest {
        repository.addMedia(videoItem().copy(id = "v", filePath = "/internal/media/video.mp4", mediaType = MediaType.VIDEO))
        repository.addMedia(videoItem().copy(id = "g", filePath = "/internal/media/gif.gif", mediaType = MediaType.GIF))
        repository.addMedia(videoItem().copy(id = "p", filePath = "/internal/media/photo.jpg", mediaType = MediaType.PHOTO))

        val result = repository.getAllMedia().first()
        val types = result.map { it.mediaType }.toSet()
        assertEquals(setOf(MediaType.VIDEO, MediaType.GIF, MediaType.PHOTO), types)
    }

    @Test
    fun `getMediaById returns item when it exists`() = runTest {
        repository.addMedia(videoItem())

        val result = repository.getMediaById("id-1")
        assertEquals("id-1", result?.id)
    }

    @Test
    fun `getMediaById returns null when item does not exist`() = runTest {
        val result = repository.getMediaById("nonexistent")
        assertNull(result)
    }

    @Test
    fun `deleteMedia removes item from library`() = runTest {
        repository.addMedia(videoItem())
        repository.deleteMedia("id-1")

        val result = repository.getAllMedia().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `deleteMedia on nonexistent id does not throw`() = runTest {
        repository.deleteMedia("nonexistent")
        assertTrue(repository.getAllMedia().first().isEmpty())
    }

    @Test
    fun `updateLastViewedAt persists timestamp`() = runTest {
        repository.addMedia(videoItem())
        repository.updateLastViewedAt("id-1", timestamp = 1_000L)

        val result = repository.getMediaById("id-1")
        assertEquals(1_000L, result?.lastViewedAt)
    }

    @Test
    fun `updateThumbnailPath persists path`() = runTest {
        repository.addMedia(videoItem())
        repository.updateThumbnailPath("id-1", path = "/internal/thumb/id-1.jpg")

        val result = repository.getMediaById("id-1")
        assertEquals("/internal/thumb/id-1.jpg", result?.thumbnailPath)
    }

    @Test
    fun `updateSortOrder persists new order`() = runTest {
        repository.addMedia(videoItem())
        repository.updateSortOrder("id-1", sortOrder = 5L)

        val result = repository.getMediaById("id-1")
        assertEquals(5L, result?.sortOrder)
    }

    @Test
    fun `getAllMedia orders items by addedAt descending`() = runTest {
        repository.addMedia(videoItem().copy(id = "old", filePath = "/internal/media/old.mp4", addedAt = 100L))
        repository.addMedia(videoItem().copy(id = "new", filePath = "/internal/media/new.mp4", addedAt = 200L))

        val result = repository.getAllMedia().first()
        assertEquals("new", result[0].id)
        assertEquals("old", result[1].id)
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
