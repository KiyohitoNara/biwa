package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetOrderedMediaIdsForAlbumUseCaseTest {
    private val repository = FakeAlbumRepository()
    private val useCase = GetOrderedMediaIdsForAlbumUseCase(repository)

    private suspend fun createAlbum(
        id: String,
        name: String,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute emits empty list when album has no media`() =
        runTest {
            createAlbum("t1", "Nature")

            val ids = useCase.execute("t1").first()

            assertTrue(ids.isEmpty())
        }

    @Test
    fun `execute emits media ids in their attached order`() =
        runTest {
            createAlbum("t1", "Nature")
            repository.addMediaToAlbum("media-1", "t1")
            repository.addMediaToAlbum("media-2", "t1")

            val ids = useCase.execute("t1").first()

            assertEquals(listOf("media-1", "media-2"), ids)
        }

    @Test
    fun `execute reflects the persisted reorder`() =
        runTest {
            createAlbum("t1", "Nature")
            repository.addMediaToAlbum("media-1", "t1")
            repository.addMediaToAlbum("media-2", "t1")

            repository.reorderAlbumMedia("t1", listOf("media-2", "media-1"))

            val ids = useCase.execute("t1").first()
            assertEquals(listOf("media-2", "media-1"), ids)
        }
}
