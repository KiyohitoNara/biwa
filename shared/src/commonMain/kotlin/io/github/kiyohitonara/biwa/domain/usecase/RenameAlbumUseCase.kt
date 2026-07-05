package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository

/** Renames an existing album. */
class RenameAlbumUseCase(
    private val repository: AlbumRepository,
) {
    /**
     * Renames the album identified by [id] to [name].
     *
     * @throws IllegalArgumentException if [name] is blank.
     * @throws Exception if a album with the same name already exists.
     */
    suspend fun execute(
        id: String,
        name: String,
    ) {
        require(name.isNotBlank()) { "Album name must not be blank" }
        repository.renameAlbum(id, name.trim())
    }
}
