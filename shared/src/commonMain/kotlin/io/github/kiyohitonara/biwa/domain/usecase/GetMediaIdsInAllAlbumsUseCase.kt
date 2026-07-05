package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow

/** Returns the set of media IDs that have all of the specified albums attached (AND logic). */
class GetMediaIdsInAllAlbumsUseCase(
    private val repository: AlbumRepository,
) {
    /**
     * Executes the use case for the given [albumIds].
     *
     * Returns a flow of media ID sets where each item in the set has ALL [albumIds] attached.
     * Returns a flow of empty set when [albumIds] is empty (no active album filter).
     */
    fun execute(albumIds: List<String>): Flow<Set<String>> = repository.getMediaIdsInAllAlbums(albumIds)
}
