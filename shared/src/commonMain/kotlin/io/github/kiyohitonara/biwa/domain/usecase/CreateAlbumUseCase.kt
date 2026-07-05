package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository

/** Creates a new album with the given name. */
class CreateAlbumUseCase(
    private val repository: AlbumRepository,
    private val idGenerator: () -> String,
    private val clock: () -> Long,
) {
    /**
     * Creates an album with [name] under [parentId] (null for a root-level album) and persists it.
     *
     * @throws IllegalArgumentException if [name] is blank.
     * @throws Exception if an album with the same name already exists.
     */
    suspend fun execute(
        name: String,
        parentId: String? = null,
    ): Album {
        require(name.isNotBlank()) { "Album name must not be blank" }
        val album = Album(id = idGenerator(), name = name.trim(), createdAt = clock(), parentId = parentId)
        repository.createAlbum(album)
        return album
    }
}
