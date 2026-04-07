package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.TagRepository

/** Deletes a tag and all its media associations. */
class DeleteTagUseCase(private val repository: TagRepository) {
    /** Deletes the tag identified by [id]. Does nothing if the tag does not exist. */
    suspend fun execute(id: String) = repository.deleteTag(id)
}
