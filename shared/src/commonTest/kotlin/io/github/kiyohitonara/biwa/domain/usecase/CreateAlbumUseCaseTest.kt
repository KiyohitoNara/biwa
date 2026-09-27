package io.github.kiyohitonara.biwa.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateAlbumUseCaseTest {
    private val repository = FakeAlbumRepository()
    private var nextId = "album-1"
    private val useCase =
        CreateAlbumUseCase(
            repository = repository,
            idGenerator = { nextId },
            clock = { 1_000L },
        )

    @Test
    fun `execute creates album with given name`() =
        runTest {
            useCase.execute("Nature")

            val albums = repository.getAllAlbums().first()
            assertEquals(1, albums.size)
            assertEquals("Nature", albums.first().name)
        }

    @Test
    fun `execute trims whitespace from name`() =
        runTest {
            useCase.execute("  Nature  ")

            val albums = repository.getAllAlbums().first()
            assertEquals("Nature", albums.first().name)
        }

    @Test
    fun `execute assigns id from generator`() =
        runTest {
            nextId = "custom-id"
            val album = useCase.execute("Nature")

            assertEquals("custom-id", album.id)
        }

    @Test
    fun `execute assigns createdAt from clock`() =
        runTest {
            val album = useCase.execute("Nature")

            assertEquals(1_000L, album.createdAt)
        }

    @Test
    fun `execute throws for blank name`() =
        runTest {
            assertFailsWith<IllegalArgumentException> {
                useCase.execute("   ")
            }
        }

    @Test
    fun `execute throws for duplicate name under the same parent`() =
        runTest {
            useCase.execute("Nature")

            assertFailsWith<Exception> {
                useCase.execute("Nature")
            }
        }

    @Test
    fun `execute allows duplicate name under different parents`() =
        runTest {
            nextId = "parent-a"
            val parentA = useCase.execute("Folder A")
            nextId = "parent-b"
            val parentB = useCase.execute("Folder B")

            nextId = "child-a"
            useCase.execute("Nature", parentId = parentA.id)
            nextId = "child-b"
            useCase.execute("Nature", parentId = parentB.id)

            val albums = repository.getAllAlbums().first()
            assertEquals(2, albums.count { it.name == "Nature" })
        }
}
