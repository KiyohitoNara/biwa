package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAlbumsForMediaUseCaseTest {
    private val repository = FakeAlbumRepository()
    private val useCase = GetAlbumsForMediaUseCase(repository)

    private suspend fun createAlbum(
        id: String,
        name: String,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute emits empty list when media has no albums`() =
        runTest {
            createAlbum("t1", "Nature")

            val albums = useCase.execute("media-1").first()

            assertTrue(albums.isEmpty())
        }

    @Test
    fun `execute emits albums attached to the media item`() =
        runTest {
            createAlbum("t1", "Nature")
            createAlbum("t2", "Travel")
            repository.addMediaToAlbum("media-1", "t1")

            val albums = useCase.execute("media-1").first()

            assertEquals(1, albums.size)
            assertEquals("t1", albums.first().id)
        }
}
