package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetMediaIdsWithAllTagsUseCaseTest {
    private val repository = FakeTagRepository()
    private val useCase = GetMediaIdsWithAllTagsUseCase(repository)

    private suspend fun createTag(id: String, name: String) =
        repository.createTag(Tag(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute returns empty set when no tags specified`() = runTest {
        repository.addTagToMedia("media-1", "t1")

        val result = useCase.execute(emptyList()).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute returns media with single matching tag`() = runTest {
        createTag("t1", "Nature")
        repository.addTagToMedia("media-1", "t1")
        repository.addTagToMedia("media-2", "t1")

        val result = useCase.execute(listOf("t1")).first()

        assertEquals(setOf("media-1", "media-2"), result)
    }

    @Test
    fun `execute requires ALL tags for AND filtering`() = runTest {
        createTag("t1", "Nature")
        createTag("t2", "Travel")
        repository.addTagToMedia("media-1", "t1")
        repository.addTagToMedia("media-1", "t2")
        repository.addTagToMedia("media-2", "t1")

        val result = useCase.execute(listOf("t1", "t2")).first()

        assertEquals(setOf("media-1"), result)
    }

    @Test
    fun `execute returns empty set when no media has all tags`() = runTest {
        createTag("t1", "Nature")
        createTag("t2", "Travel")
        repository.addTagToMedia("media-1", "t1")
        repository.addTagToMedia("media-2", "t2")

        val result = useCase.execute(listOf("t1", "t2")).first()

        assertTrue(result.isEmpty())
    }
}
