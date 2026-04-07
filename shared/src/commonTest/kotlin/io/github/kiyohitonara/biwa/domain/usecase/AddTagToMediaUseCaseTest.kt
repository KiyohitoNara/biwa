package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddTagToMediaUseCaseTest {
    private val repository = FakeTagRepository()
    private val useCase = AddTagToMediaUseCase(repository)

    private suspend fun createTag(id: String, name: String) =
        repository.createTag(Tag(id = id, name = name, createdAt = 0L))

    @Test
    fun `execute attaches tag to media item`() = runTest {
        createTag("t1", "Nature")

        useCase.execute("media-1", "t1")

        val tags = repository.getTagsForMedia("media-1").first()
        assertEquals(1, tags.size)
        assertEquals("t1", tags.first().id)
    }

    @Test
    fun `execute is idempotent for duplicate attachment`() = runTest {
        createTag("t1", "Nature")
        useCase.execute("media-1", "t1")

        useCase.execute("media-1", "t1")

        val tags = repository.getTagsForMedia("media-1").first()
        assertEquals(1, tags.size)
    }

    @Test
    fun `execute can attach multiple tags to the same media`() = runTest {
        createTag("t1", "Nature")
        createTag("t2", "Travel")

        useCase.execute("media-1", "t1")
        useCase.execute("media-1", "t2")

        val tags = repository.getTagsForMedia("media-1").first()
        assertEquals(2, tags.size)
    }

    @Test
    fun `execute does not affect other media items`() = runTest {
        createTag("t1", "Nature")
        useCase.execute("media-1", "t1")

        val tags = repository.getTagsForMedia("media-2").first()
        assertTrue(tags.isEmpty())
    }
}
