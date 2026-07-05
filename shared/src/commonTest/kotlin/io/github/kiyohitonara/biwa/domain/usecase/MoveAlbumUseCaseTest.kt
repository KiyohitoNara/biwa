package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MoveAlbumUseCaseTest {
    private val repository = FakeAlbumRepository()
    private val useCase = MoveAlbumUseCase(repository)

    private suspend fun createAlbum(
        id: String,
        name: String,
        parentId: String? = null,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L, parentId = parentId))

    private suspend fun albumId(id: String) =
        repository
            .getAllAlbums()
            .first()
            .first { it.id == id }
            .parentId

    @Test
    fun `execute reparents album under a new parent`() =
        runTest {
            createAlbum("parent", "Parent")
            createAlbum("child", "Child")

            useCase.execute(id = "child", parentId = "parent")

            assertEquals("parent", albumId("child"))
        }

    @Test
    fun `execute moves album to root when parentId is null`() =
        runTest {
            createAlbum("parent", "Parent")
            createAlbum("child", "Child", parentId = "parent")

            useCase.execute(id = "child", parentId = null)

            assertNull(albumId("child"))
        }

    @Test
    fun `execute throws when moving album under itself`() =
        runTest {
            createAlbum("a", "A")

            assertFailsWith<IllegalArgumentException> {
                useCase.execute(id = "a", parentId = "a")
            }
        }

    @Test
    fun `execute throws when moving album into its own descendant`() =
        runTest {
            createAlbum("a", "A")
            createAlbum("b", "B", parentId = "a")
            createAlbum("c", "C", parentId = "b")

            assertFailsWith<IllegalArgumentException> {
                useCase.execute(id = "a", parentId = "c")
            }
        }
}
