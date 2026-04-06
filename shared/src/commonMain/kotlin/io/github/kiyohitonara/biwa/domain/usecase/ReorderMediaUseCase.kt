package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.MediaRepository

/**
 * Persists a user-defined manual ordering for the media library.
 *
 * Each item's [MediaItem.sortOrder] is set to its index in [orderedIds],
 * so smaller values appear first in [SortOrder.MANUAL] mode.
 *
 * @param repository Persistence layer for updating sort order values.
 */
class ReorderMediaUseCase(private val repository: MediaRepository) {
    /**
     * Writes each ID's list position as its new [MediaItem.sortOrder].
     *
     * @param orderedIds IDs in the desired display order, first to last.
     */
    suspend fun execute(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            repository.updateSortOrder(id, index.toLong())
        }
    }
}
