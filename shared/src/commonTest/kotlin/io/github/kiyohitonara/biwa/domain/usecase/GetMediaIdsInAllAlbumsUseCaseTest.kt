package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetMediaIdsInAllAlbumsUseCaseTest {
    private val repository = FakeAlbumRepository()
    private val useCase = GetMediaIdsInAllAlbumsUseCase(repository)

    private suspend fun createAlbum(
        id: String,
        name: String,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute returns empty set when no albums specified`() =
        runTest {
            repository.addMediaToAlbum("media-1", "t1")

            val result = useCase.execute(emptyList()).first()

            assertTrue(result.isEmpty())
        }

    @Test
    fun `execute returns media with single matching album`() =
        runTest {
            createAlbum("t1", "Nature")
            repository.addMediaToAlbum("media-1", "t1")
            repository.addMediaToAlbum("media-2", "t1")

            val result = useCase.execute(listOf("t1")).first()

            assertEquals(setOf("media-1", "media-2"), result)
        }

    @Test
    fun `execute requires ALL albums for AND filtering`() =
        runTest {
            createAlbum("t1", "Nature")
            createAlbum("t2", "Travel")
            repository.addMediaToAlbum("media-1", "t1")
            repository.addMediaToAlbum("media-1", "t2")
            repository.addMediaToAlbum("media-2", "t1")

            val result = useCase.execute(listOf("t1", "t2")).first()

            assertEquals(setOf("media-1"), result)
        }

    @Test
    fun `execute returns empty set when no media has all albums`() =
        runTest {
            createAlbum("t1", "Nature")
            createAlbum("t2", "Travel")
            repository.addMediaToAlbum("media-1", "t1")
            repository.addMediaToAlbum("media-2", "t2")

            val result = useCase.execute(listOf("t1", "t2")).first()

            assertTrue(result.isEmpty())
        }
}
