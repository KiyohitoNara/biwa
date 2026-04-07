package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.TagRepository

/** Attaches a tag to a media item. */
class AddTagToMediaUseCase(private val repository: TagRepository) {
    /** Attaches the tag identified by [tagId] to the media item identified by [mediaId]. No-op if already attached. */
    suspend fun execute(mediaId: String, tagId: String) = repository.addTagToMedia(mediaId, tagId)
}
