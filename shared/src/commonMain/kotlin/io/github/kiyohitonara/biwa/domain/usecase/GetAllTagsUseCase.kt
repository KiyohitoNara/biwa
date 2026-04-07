package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Tag
import io.github.kiyohitonara.biwa.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

/** Returns a reactive stream of all tags, ordered alphabetically. */
class GetAllTagsUseCase(private val repository: TagRepository) {
    /** Executes the use case and returns the tag stream. */
    fun execute(): Flow<List<Tag>> = repository.getAllTags()
}
