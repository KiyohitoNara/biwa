package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.MediaRepository

/**
 * Records the current time as the last-viewed timestamp for a media item.
 *
 * @param repository Persistence layer for media metadata.
 * @param clock Provides the current time as Unix epoch seconds.
 */
class UpdateLastViewedAtUseCase(
    private val repository: MediaRepository,
    private val clock: () -> Long,
) {
    /**
     * Stamps the item identified by [id] with the current time.
     */
    suspend fun execute(id: String) {
        repository.updateLastViewedAt(id, clock())
    }
}
