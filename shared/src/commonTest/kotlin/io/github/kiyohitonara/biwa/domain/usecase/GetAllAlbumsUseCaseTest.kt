package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAllAlbumsUseCaseTest {
    private val repository = FakeAlbumRepository()
    private val useCase = GetAllAlbumsUseCase(repository)

    private suspend fun createAlbum(
        id: String,
        name: String,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute emits empty list when no albums exist`() =
        runTest {
            val albums = useCase.execute().first()

            assertTrue(albums.isEmpty())
        }

    @Test
    fun `execute emits all albums from the repository`() =
        runTest {
            createAlbum("t1", "Nature")
            createAlbum("t2", "Travel")

            val albums = useCase.execute().first()

            assertEquals(setOf("t1", "t2"), albums.map { it.id }.toSet())
        }
}
