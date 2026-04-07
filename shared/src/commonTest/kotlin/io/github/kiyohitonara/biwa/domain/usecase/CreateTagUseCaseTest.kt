package io.github.kiyohitonara.biwa.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateTagUseCaseTest {
    private val repository = FakeTagRepository()
    private var nextId = "tag-1"
    private val useCase = CreateTagUseCase(
        repository = repository,
        idGenerator = { nextId },
        clock = { 1_000L },
    )

    @Test
    fun `execute creates tag with given name`() = runTest {
        useCase.execute("Nature")

        val tags = repository.getAllTags().first()
        assertEquals(1, tags.size)
        assertEquals("Nature", tags.first().name)
    }

    @Test
    fun `execute trims whitespace from name`() = runTest {
        useCase.execute("  Nature  ")

        val tags = repository.getAllTags().first()
        assertEquals("Nature", tags.first().name)
    }

    @Test
    fun `execute assigns id from generator`() = runTest {
        nextId = "custom-id"
        val tag = useCase.execute("Nature")

        assertEquals("custom-id", tag.id)
    }

    @Test
    fun `execute assigns createdAt from clock`() = runTest {
        val tag = useCase.execute("Nature")

        assertEquals(1_000L, tag.createdAt)
    }

    @Test
    fun `execute throws for blank name`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            useCase.execute("   ")
        }
    }

    @Test
    fun `execute throws for duplicate name`() = runTest {
        useCase.execute("Nature")

        assertFailsWith<Exception> {
            useCase.execute("Nature")
        }
    }
}
