package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoveMediaFromAlbumUseCaseTest {
    private val repository = FakeAlbumRepository()
    private val useCase = RemoveMediaFromAlbumUseCase(repository)

    private suspend fun createAlbum(
        id: String,
        name: String,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute detaches album from media item`() =
        runTest {
            createAlbum("t1", "Nature")
            repository.addMediaToAlbum("media-1", "t1")

            useCase.execute("media-1", "t1")

            val albums = repository.getAlbumsForMedia("media-1").first()
            assertTrue(albums.isEmpty())
        }

    @Test
    fun `execute is no-op when album is not attached`() =
        runTest {
            createAlbum("t1", "Nature")

            useCase.execute("media-1", "t1")

            val albums = repository.getAlbumsForMedia("media-1").first()
            assertTrue(albums.isEmpty())
        }

    @Test
    fun `execute does not remove other album attachments`() =
        runTest {
            createAlbum("t1", "Nature")
            createAlbum("t2", "Travel")
            repository.addMediaToAlbum("media-1", "t1")
            repository.addMediaToAlbum("media-1", "t2")

            useCase.execute("media-1", "t1")

            val albums = repository.getAlbumsForMedia("media-1").first()
            assertEquals(1, albums.size)
            assertEquals("t2", albums.first().id)
        }
}
