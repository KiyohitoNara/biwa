package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoveTagFromMediaUseCaseTest {
    private val repository = FakeTagRepository()
    private val useCase = RemoveTagFromMediaUseCase(repository)

    private suspend fun createTag(id: String, name: String) =
        repository.createTag(Tag(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute detaches tag from media item`() = runTest {
        createTag("t1", "Nature")
        repository.addTagToMedia("media-1", "t1")

        useCase.execute("media-1", "t1")

        val tags = repository.getTagsForMedia("media-1").first()
        assertTrue(tags.isEmpty())
    }

    @Test
    fun `execute is no-op when tag is not attached`() = runTest {
        createTag("t1", "Nature")

        useCase.execute("media-1", "t1")

        val tags = repository.getTagsForMedia("media-1").first()
        assertTrue(tags.isEmpty())
    }

    @Test
    fun `execute does not remove other tag attachments`() = runTest {
        createTag("t1", "Nature")
        createTag("t2", "Travel")
        repository.addTagToMedia("media-1", "t1")
        repository.addTagToMedia("media-1", "t2")

        useCase.execute("media-1", "t1")

        val tags = repository.getTagsForMedia("media-1").first()
        assertEquals(1, tags.size)
        assertEquals("t2", tags.first().id)
    }
}
