package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.TagRepository

/**
 * Persists a new tag-specific manual ordering for the media items of a given tag.
 *
 * The items are assigned sequential sort_order values (0, 1, 2, …) in the
 * order provided by [orderedIds].
 */
class ReorderTagMediaUseCase(private val repository: TagRepository) {
    /** Executes the reorder for [tagId] with [orderedIds] defining the new order. */
    suspend fun execute(tagId: String, orderedIds: List<String>) =
        repository.reorderTagMedia(tagId, orderedIds)
}
