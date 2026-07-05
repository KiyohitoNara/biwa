package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow

/** Returns a reactive stream of albums attached to a specific media item. */
class GetAlbumsForMediaUseCase(
    private val repository: AlbumRepository,
) {
    /** Executes the use case for the media item identified by [mediaId]. */
    fun execute(mediaId: String): Flow<List<Album>> = repository.getAlbumsForMedia(mediaId)
}
