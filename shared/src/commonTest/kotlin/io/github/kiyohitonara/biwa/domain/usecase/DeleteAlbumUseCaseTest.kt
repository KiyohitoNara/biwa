package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteAlbumUseCaseTest {
    private val repository = FakeAlbumRepository()
    private val useCase = DeleteAlbumUseCase(repository)

    private suspend fun createAlbum(
        id: String,
        name: String,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute removes album from repository`() =
        runTest {
            createAlbum("t1", "Nature")

            useCase.execute("t1")

            val albums = repository.getAllAlbums().first()
            assertTrue(albums.isEmpty())
        }

    @Test
    fun `execute does not affect other albums`() =
        runTest {
            createAlbum("t1", "Nature")
            createAlbum("t2", "Travel")

            useCase.execute("t1")

            val albums = repository.getAllAlbums().first()
            assertEquals(1, albums.size)
            assertEquals("t2", albums.first().id)
        }

    @Test
    fun `execute removes media associations for deleted album`() =
        runTest {
            createAlbum("t1", "Nature")
            repository.addMediaToAlbum("media-1", "t1")

            useCase.execute("t1")

            val albumsForMedia = repository.getAlbumsForMedia("media-1").first()
            assertTrue(albumsForMedia.isEmpty())
        }

    @Test
    fun `execute is no-op when album does not exist`() =
        runTest {
            useCase.execute("nonexistent")

            val albums = repository.getAllAlbums().first()
            assertTrue(albums.isEmpty())
        }
}
