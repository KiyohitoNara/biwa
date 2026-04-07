package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

/** Returns the set of media IDs that have all of the specified tags attached (AND logic). */
class GetMediaIdsWithAllTagsUseCase(private val repository: TagRepository) {
    /**
     * Executes the use case for the given [tagIds].
     *
     * Returns a flow of media ID sets where each item in the set has ALL [tagIds] attached.
     * Returns a flow of empty set when [tagIds] is empty (no active tag filter).
     */
    fun execute(tagIds: List<String>): Flow<Set<String>> = repository.getMediaIdsWithAllTags(tagIds)
}
