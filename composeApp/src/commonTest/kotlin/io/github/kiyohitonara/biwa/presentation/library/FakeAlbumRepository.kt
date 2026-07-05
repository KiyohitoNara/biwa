package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.Album
import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [AlbumRepository] backed by [MutableStateFlow] for use in tests. */
class FakeAlbumRepository : AlbumRepository {
    val albums = MutableStateFlow<List<Album>>(emptyList())

    // Triple: (mediaId, albumId, sortOrder)
    private val associations = MutableStateFlow<List<Triple<String, String, Int>>>(emptyList())

    override fun getAllAlbums(): Flow<List<Album>> = albums

    override suspend fun getAlbumById(id: String): Album? = albums.value.find { it.id == id }

    override suspend fun createAlbum(album: Album) {
        albums.value = albums.value + album
    }

    override suspend fun renameAlbum(
        id: String,
        name: String,
    ) {
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
        associations.value = associations.value.filterNot { it.second in subtree }
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
        associations.map { triples ->
            val albumIds = triples.filter { it.first == mediaId }.map { it.second }
            albums.value.filter { it.id in albumIds }
        }

    override suspend fun addMediaToAlbum(
        mediaId: String,
        albumId: String,
    ) {
        if (associations.value.none { it.first == mediaId && it.second == albumId }) {
            val nextOrder = associations.value.filter { it.second == albumId }.size
            associations.value = associations.value + Triple(mediaId, albumId, nextOrder)
        }
    }

    override suspend fun removeMediaFromAlbum(
        mediaId: String,
        albumId: String,
    ) {
        associations.value = associations.value.filter { !(it.first == mediaId && it.second == albumId) }
    }

    override fun getMediaIdsInAllAlbums(albumIds: List<String>): Flow<Set<String>> =
        associations.map { triples ->
            if (albumIds.isEmpty()) return@map emptySet()
            triples
                .groupBy { it.first }
                .filterValues { group -> albumIds.all { albumId -> group.any { it.second == albumId } } }
                .keys
                .toSet()
        }

    override fun getOrderedMediaIdsForAlbum(albumId: String): Flow<List<String>> =
        associations.map { triples ->
            triples
                .filter { it.second == albumId }
                .sortedBy { it.third }
                .map { it.first }
        }

    override suspend fun reorderAlbumMedia(
        albumId: String,
        orderedIds: List<String>,
    ) {
        associations.value =
            associations.value.map { triple ->
                if (triple.second == albumId) {
                    val newOrder = orderedIds.indexOf(triple.first)
                    triple.copy(third = if (newOrder != -1) newOrder else triple.third)
                } else {
                    triple
                }
            }
    }
}
