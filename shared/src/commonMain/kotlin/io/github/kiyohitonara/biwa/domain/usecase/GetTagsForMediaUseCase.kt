package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Tag
import io.github.kiyohitonara.biwa.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

/** Returns a reactive stream of tags attached to a specific media item. */
class GetTagsForMediaUseCase(private val repository: TagRepository) {
    /** Executes the use case for the media item identified by [mediaId]. */
    fun execute(mediaId: String): Flow<List<Tag>> = repository.getTagsForMedia(mediaId)
}
