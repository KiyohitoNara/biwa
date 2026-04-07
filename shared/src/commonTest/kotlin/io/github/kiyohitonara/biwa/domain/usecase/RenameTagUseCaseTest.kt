package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RenameTagUseCaseTest {
    private val repository = FakeTagRepository()
    private val useCase = RenameTagUseCase(repository)

    private suspend fun createTag(id: String, name: String) =
        repository.createTag(Tag(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute renames tag`() = runTest {
        createTag("t1", "Nature")

        useCase.execute("t1", "Travel")

        val tags = repository.getAllTags().first()
        assertEquals("Travel", tags.first().name)
    }

    @Test
    fun `execute trims whitespace from name`() = runTest {
        createTag("t1", "Nature")

        useCase.execute("t1", "  Travel  ")

        val tags = repository.getAllTags().first()
        assertEquals("Travel", tags.first().name)
    }

    @Test
    fun `execute throws for blank name`() = runTest {
        createTag("t1", "Nature")

        assertFailsWith<IllegalArgumentException> {
            useCase.execute("t1", "   ")
        }
    }

    @Test
    fun `execute throws for duplicate name`() = runTest {
        createTag("t1", "Nature")
        createTag("t2", "Travel")

        assertFailsWith<Exception> {
            useCase.execute("t1", "Travel")
        }
    }

    @Test
    fun `execute allows renaming to same name`() = runTest {
        createTag("t1", "Nature")

        useCase.execute("t1", "Nature")

        val tags = repository.getAllTags().first()
        assertEquals("Nature", tags.first().name)
    }
}
