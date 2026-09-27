package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RenameAlbumUseCaseTest {
    private val repository = FakeAlbumRepository()
    private val useCase = RenameAlbumUseCase(repository)

    private suspend fun createAlbum(
        id: String,
        name: String,
        parentId: String? = null,
    ) = repository.createAlbum(Album(id = id, name = name, createdAt = 0L, parentId = parentId))

    @Test
    fun `execute renames album`() =
        runTest {
            createAlbum("t1", "Nature")

            useCase.execute("t1", "Travel")

            val albums = repository.getAllAlbums().first()
            assertEquals("Travel", albums.first().name)
        }

    @Test
    fun `execute trims whitespace from name`() =
        runTest {
            createAlbum("t1", "Nature")

            useCase.execute("t1", "  Travel  ")

            val albums = repository.getAllAlbums().first()
            assertEquals("Travel", albums.first().name)
        }

    @Test
    fun `execute throws for blank name`() =
        runTest {
            createAlbum("t1", "Nature")

            assertFailsWith<IllegalArgumentException> {
                useCase.execute("t1", "   ")
            }
        }

    @Test
    fun `execute throws for duplicate name under the same parent`() =
        runTest {
            createAlbum("t1", "Nature")
            createAlbum("t2", "Travel")

            assertFailsWith<Exception> {
                useCase.execute("t1", "Travel")
            }
        }

    @Test
    fun `execute allows duplicate name under different parents`() =
        runTest {
            createAlbum("p1", "Folder A")
            createAlbum("p2", "Folder B")
            createAlbum("t1", "Nature", parentId = "p1")
            createAlbum("t2", "Travel", parentId = "p2")

            useCase.execute("t2", "Nature")

            val albums = repository.getAllAlbums().first()
            assertEquals(2, albums.count { it.name == "Nature" })
        }

    @Test
    fun `execute allows renaming to same name`() =
        runTest {
            createAlbum("t1", "Nature")

            useCase.execute("t1", "Nature")

            val albums = repository.getAllAlbums().first()
            assertEquals("Nature", albums.first().name)
        }
}
