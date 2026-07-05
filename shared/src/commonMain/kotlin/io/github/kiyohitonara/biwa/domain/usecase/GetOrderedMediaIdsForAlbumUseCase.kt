package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow

/** Returns the ordered list of media IDs for a single album, respecting its album-specific sort order. */
class GetOrderedMediaIdsForAlbumUseCase(
    private val repository: AlbumRepository,
) {
    /**
     * Executes the use case for the given [albumId].
     *
     * Returns a flow of media IDs in the order defined by [AlbumRepository.reorderAlbumMedia].
     */
    fun execute(albumId: String): Flow<List<String>> = repository.getOrderedMediaIdsForAlbum(albumId)
}
