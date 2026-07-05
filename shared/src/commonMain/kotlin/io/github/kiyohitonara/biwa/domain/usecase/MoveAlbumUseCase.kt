package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository

/** Moves an album to a new position in the album tree. */
class MoveAlbumUseCase(
    private val repository: AlbumRepository,
) {
    /**
     * Reparents the album identified by [id] under [parentId], or to the root when [parentId] is null.
     *
     * @throws IllegalArgumentException if [parentId] equals [id] or is one of its descendants,
     *   either of which would create a cycle in the album tree.
     */
    suspend fun execute(
        id: String,
        parentId: String?,
    ) {
        require(parentId != id) { "An album cannot be its own parent" }
        var ancestor = parentId
        while (ancestor != null) {
            require(ancestor != id) { "Cannot move an album into its own descendant" }
            ancestor = repository.getAlbumById(ancestor)?.parentId
        }
        repository.moveAlbum(id, parentId)
    }
}
