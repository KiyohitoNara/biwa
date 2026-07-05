package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository

/** Detaches a album from a media item. */
class RemoveMediaFromAlbumUseCase(
    private val repository: AlbumRepository,
) {
    /** Detaches the album identified by [albumId] from the media item identified by [mediaId]. No-op if not attached. */
    suspend fun execute(
        mediaId: String,
        albumId: String,
    ) = repository.removeMediaFromAlbum(mediaId, albumId)
}
