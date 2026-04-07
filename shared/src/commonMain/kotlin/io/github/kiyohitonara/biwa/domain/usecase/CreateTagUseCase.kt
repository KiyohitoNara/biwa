package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Tag
import io.github.kiyohitonara.biwa.domain.repository.TagRepository

/** Creates a new tag with the given name. */
class CreateTagUseCase(
    private val repository: TagRepository,
    private val idGenerator: () -> String,
    private val clock: () -> Long,
) {
    /**
     * Creates a tag with [name] and persists it.
     *
     * @throws IllegalArgumentException if [name] is blank.
     * @throws Exception if a tag with the same name already exists.
     */
    suspend fun execute(name: String): Tag {
        require(name.isNotBlank()) { "Tag name must not be blank" }
        val tag = Tag(id = idGenerator(), name = name.trim(), createdAt = clock())
        repository.createTag(tag)
        return tag
    }
}
