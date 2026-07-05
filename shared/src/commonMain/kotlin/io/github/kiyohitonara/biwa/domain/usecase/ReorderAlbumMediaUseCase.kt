package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository

/**
 * Persists a new album-specific manual ordering for the media items of a given album.
 *
 * The items are assigned sequential sort_order values (0, 1, 2, …) in the
 * order provided by [orderedIds].
 */
class ReorderAlbumMediaUseCase(
    private val repository: AlbumRepository,
) {
    /** Executes the reorder for [albumId] with [orderedIds] defining the new order. */
    suspend fun execute(
        albumId: String,
        orderedIds: List<String>,
    ) = repository.reorderAlbumMedia(albumId, orderedIds)
}
