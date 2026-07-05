package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddMediaToAlbumUseCaseTest {
    private val repository = FakeAlbumRepository()
    private val useCase = AddMediaToAlbumUseCase(repository)

    private suspend fun createAlbum(
        id: String,
        name: String,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute attaches album to media item`() =
        runTest {
            createAlbum("t1", "Nature")

            useCase.execute("media-1", "t1")

            val albums = repository.getAlbumsForMedia("media-1").first()
            assertEquals(1, albums.size)
            assertEquals("t1", albums.first().id)
        }

    @Test
    fun `execute is idempotent for duplicate attachment`() =
        runTest {
            createAlbum("t1", "Nature")
            useCase.execute("media-1", "t1")

            useCase.execute("media-1", "t1")

            val albums = repository.getAlbumsForMedia("media-1").first()
            assertEquals(1, albums.size)
        }

    @Test
    fun `execute can attach multiple albums to the same media`() =
        runTest {
            createAlbum("t1", "Nature")
            createAlbum("t2", "Travel")

            useCase.execute("media-1", "t1")
            useCase.execute("media-1", "t2")

            val albums = repository.getAlbumsForMedia("media-1").first()
            assertEquals(2, albums.size)
        }

    @Test
    fun `execute does not affect other media items`() =
        runTest {
            createAlbum("t1", "Nature")
            useCase.execute("media-1", "t1")

            val albums = repository.getAlbumsForMedia("media-2").first()
            assertTrue(albums.isEmpty())
        }
}
