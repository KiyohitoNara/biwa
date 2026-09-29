package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository

/** Deletes an album and all its media associations. */
class DeleteAlbumUseCase(
    private val repository: AlbumRepository,
) {
    /** Deletes the album identified by [id]. Does nothing if the album does not exist. */
    suspend fun execute(id: String) = repository.deleteAlbum(id)
}
