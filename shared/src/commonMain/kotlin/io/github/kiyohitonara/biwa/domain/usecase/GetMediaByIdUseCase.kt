package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository

/**
 * Retrieves a single media item from the library by its ID.
 *
 * @param repository Persistence layer for media metadata.
 */
class GetMediaByIdUseCase(
    private val repository: MediaRepository,
) {
    /**
     * Returns the [MediaItem] with the given [id], or null if not found.
     */
    suspend fun execute(id: String): MediaItem? {
        return repository.getMediaById(id)
    }
}
