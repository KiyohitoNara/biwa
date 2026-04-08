package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

/** Returns the ordered list of media IDs for a single tag, respecting its tag-specific sort order. */
class GetOrderedMediaIdsForTagUseCase(private val repository: TagRepository) {
    /**
     * Executes the use case for the given [tagId].
     *
     * Returns a flow of media IDs in the order defined by [TagRepository.reorderTagMedia].
     */
    fun execute(tagId: String): Flow<List<String>> = repository.getOrderedMediaIdsForTag(tagId)
}
