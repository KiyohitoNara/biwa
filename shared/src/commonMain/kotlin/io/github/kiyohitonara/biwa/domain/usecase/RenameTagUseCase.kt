package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.TagRepository

/** Renames an existing tag. */
class RenameTagUseCase(private val repository: TagRepository) {
    /**
     * Renames the tag identified by [id] to [name].
     *
     * @throws IllegalArgumentException if [name] is blank.
     * @throws Exception if a tag with the same name already exists.
     */
    suspend fun execute(id: String, name: String) {
        require(name.isNotBlank()) { "Tag name must not be blank" }
        repository.renameTag(id, name.trim())
    }
}
