package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.TagRepository

/** Detaches a tag from a media item. */
class RemoveTagFromMediaUseCase(private val repository: TagRepository) {
    /** Detaches the tag identified by [tagId] from the media item identified by [mediaId]. No-op if not attached. */
    suspend fun execute(mediaId: String, tagId: String) = repository.removeTagFromMedia(mediaId, tagId)
}
