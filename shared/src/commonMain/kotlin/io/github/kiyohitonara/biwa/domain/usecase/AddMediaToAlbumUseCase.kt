package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository

/** Attaches a album to a media item. */
class AddMediaToAlbumUseCase(
    private val repository: AlbumRepository,
) {
    /** Attaches the album identified by [albumId] to the media item identified by [mediaId]. No-op if already attached. */
    suspend fun execute(
        mediaId: String,
        albumId: String,
    ) = repository.addMediaToAlbum(mediaId, albumId)
}
