package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Returns a reactive stream of all PHOTO items in the library.
 *
 * Items are ordered by [MediaItem.addedAt] descending, inherited from
 * [MediaRepository.getAllMedia]. This list is used for swipe navigation
 * in the photo viewer.
 *
 * @param repository Persistence layer for media metadata.
 */
class GetAllPhotosUseCase(
    private val repository: MediaRepository,
) {
    /** Emits the current list of photos and updates on any library change. */
    fun execute(): Flow<List<MediaItem>> =
        repository.getAllMedia().map { items ->
            items.filter { it.mediaType == MediaType.PHOTO }
        }
}
