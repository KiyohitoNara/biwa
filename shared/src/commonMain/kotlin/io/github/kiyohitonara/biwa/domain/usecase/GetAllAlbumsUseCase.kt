package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow

/** Returns a reactive stream of all albums, ordered alphabetically. */
class GetAllAlbumsUseCase(
    private val repository: AlbumRepository,
) {
    /** Executes the use case and returns the album stream. */
    fun execute(): Flow<List<Album>> = repository.getAllAlbums()
}
