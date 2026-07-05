package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Album
import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [AlbumRepository] for use in tests. */
class FakeAlbumRepository : AlbumRepository {
    private val albums = MutableStateFlow<List<Album>>(emptyList())

    // Triple: (mediaId, albumId, sortOrder)
    private val mediaAlbumAssociations = MutableStateFlow<List<Triple<String, String, Int>>>(emptyList())

    override fun getAllAlbums(): Flow<List<Album>> = albums

    override suspend fun getAlbumById(id: String): Album? = albums.value.find { it.id == id }

    override suspend fun createAlbum(album: Album) {
        if (albums.value.any { it.name == album.name }) error("Album name '${album.name}' already exists")
        albums.value = albums.value + album
    }

    override suspend fun renameAlbum(
        id: String,
        name: String,
    ) {
        if (albums.value.any { it.name == name && it.id != id }) error("Album name '$name' already exists")
        albums.value = albums.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun moveAlbum(
        id: String,
        parentId: String?,
    ) {
        albums.value = albums.value.map { if (it.id == id) it.copy(parentId = parentId) else it }
    }

    override suspend fun deleteAlbum(id: String) {
        val subtree = collectSubtree(id)
        albums.value = albums.value.filterNot { it.id in subtree }
        mediaAlbumAssociations.value = mediaAlbumAssociations.value.filterNot { it.second in subtree }
    }

    // Mirrors the ON DELETE CASCADE on album.parent_id used by the real database.
    private fun collectSubtree(rootId: String): Set<String> {
        val subtree = mutableSetOf(rootId)
        var frontier = listOf(rootId)
        while (frontier.isNotEmpty()) {
            val children = albums.value.filter { it.parentId in frontier && it.id !in subtree }.map { it.id }
            subtree += children
            frontier = children
        }
        return subtree
    }

    override fun getAlbumsForMedia(mediaId: String): Flow<List<Album>> =
        mediaAlbumAssociations.map { associations ->
            val albumIds = associations.filter { it.first == mediaId }.map { it.second }
            albums.value.filter { it.id in albumIds }.sortedBy { it.name }
        }

    override suspend fun addMediaToAlbum(
        mediaId: String,
        albumId: String,
    ) {
        if (mediaAlbumAssociations.value.none { it.first == mediaId && it.second == albumId }) {
            val nextOrder = mediaAlbumAssociations.value.filter { it.second == albumId }.size
            mediaAlbumAssociations.value = mediaAlbumAssociations.value + Triple(mediaId, albumId, nextOrder)
        }
    }

    override suspend fun removeMediaFromAlbum(
        mediaId: String,
        albumId: String,
    ) {
        mediaAlbumAssociations.value =
            mediaAlbumAssociations.value.filter {
                !(it.first == mediaId && it.second == albumId)
            }
    }

    override fun getMediaIdsInAllAlbums(albumIds: List<String>): Flow<Set<String>> =
        mediaAlbumAssociations.map { associations ->
            if (albumIds.isEmpty()) return@map emptySet()
            associations
                .groupBy { it.first }
                .filterValues { pairs -> albumIds.all { albumId -> pairs.any { it.second == albumId } } }
                .keys
                .toSet()
        }

    override fun getOrderedMediaIdsForAlbum(albumId: String): Flow<List<String>> =
        mediaAlbumAssociations.map { associations ->
            associations
                .filter { it.second == albumId }
                .sortedBy { it.third }
                .map { it.first }
        }

    override suspend fun reorderAlbumMedia(
        albumId: String,
        orderedIds: List<String>,
    ) {
        val updated =
            mediaAlbumAssociations.value.map { triple ->
                if (triple.second == albumId) {
                    val newOrder = orderedIds.indexOf(triple.first)
                    triple.copy(third = if (newOrder != -1) newOrder else triple.third)
                } else {
                    triple
                }
            }
        mediaAlbumAssociations.value = updated
    }
}
