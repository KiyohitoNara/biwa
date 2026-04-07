package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteTagUseCaseTest {
    private val repository = FakeTagRepository()
    private val useCase = DeleteTagUseCase(repository)

    private suspend fun createTag(id: String, name: String) =
        repository.createTag(Tag(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute removes tag from repository`() = runTest {
        createTag("t1", "Nature")

        useCase.execute("t1")

        val tags = repository.getAllTags().first()
        assertTrue(tags.isEmpty())
    }

    @Test
    fun `execute does not affect other tags`() = runTest {
        createTag("t1", "Nature")
        createTag("t2", "Travel")

        useCase.execute("t1")

        val tags = repository.getAllTags().first()
        assertEquals(1, tags.size)
        assertEquals("t2", tags.first().id)
    }

    @Test
    fun `execute removes media associations for deleted tag`() = runTest {
        createTag("t1", "Nature")
        repository.addTagToMedia("media-1", "t1")

        useCase.execute("t1")

        val tagsForMedia = repository.getTagsForMedia("media-1").first()
        assertTrue(tagsForMedia.isEmpty())
    }

    @Test
    fun `execute is no-op when tag does not exist`() = runTest {
        useCase.execute("nonexistent")

        val tags = repository.getAllTags().first()
        assertTrue(tags.isEmpty())
    }
}
