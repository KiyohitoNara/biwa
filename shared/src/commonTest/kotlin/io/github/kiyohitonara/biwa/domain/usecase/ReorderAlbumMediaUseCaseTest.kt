package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReorderAlbumMediaUseCaseTest {
    private val repository = FakeAlbumRepository()
    private val useCase = ReorderAlbumMediaUseCase(repository)

    private suspend fun createAlbum(
        id: String,
        name: String,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute persists the new order for the given album`() =
        runTest {
            createAlbum("t1", "Nature")
            repository.addMediaToAlbum("media-1", "t1")
            repository.addMediaToAlbum("media-2", "t1")

            useCase.execute("t1", listOf("media-2", "media-1"))

            val ids = repository.getOrderedMediaIdsForAlbum("t1").first()
            assertEquals(listOf("media-2", "media-1"), ids)
        }

    @Test
    fun `execute does not affect other albums' order`() =
        runTest {
            createAlbum("t1", "Nature")
            createAlbum("t2", "Travel")
            repository.addMediaToAlbum("media-1", "t1")
            repository.addMediaToAlbum("media-1", "t2")
            repository.addMediaToAlbum("media-2", "t2")

            useCase.execute("t2", listOf("media-2", "media-1"))

            val t1Ids = repository.getOrderedMediaIdsForAlbum("t1").first()
            assertEquals(listOf("media-1"), t1Ids)
        }
}
