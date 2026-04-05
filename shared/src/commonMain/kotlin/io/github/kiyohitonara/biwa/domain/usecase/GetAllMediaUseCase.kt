package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

/**
 * Returns a stream of all media items in the library, ordered by [MediaItem.addedAt] descending.
 *
 * @param repository Source of media item data.
 */
class GetAllMediaUseCase(
    private val repository: MediaRepository,
) {
    /** Executes the use case and returns a [Flow] that emits the current list on each change. */
    fun execute(): Flow<List<MediaItem>> = repository.getAllMedia()
}
