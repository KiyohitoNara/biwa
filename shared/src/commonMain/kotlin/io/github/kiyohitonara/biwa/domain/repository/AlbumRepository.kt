package io.github.kiyohitonara.biwa.domain.repository

import io.github.kiyohitonara.biwa.domain.model.Album
import kotlinx.coroutines.flow.Flow

/** Provides CRUD operations for albums, their nesting, and their media associations. */
@Suppress("TooManyFunctions") // Cohesive album persistence surface (CRUD + nesting + media associations).
interface AlbumRepository {
    /** Returns a flow of all albums, ordered alphabetically by name. */
    fun getAllAlbums(): Flow<List<Album>>

    /** Returns the album with the given [id], or null if not found. */
    suspend fun getAlbumById(id: String): Album?

    /** Persists a new [album], including its [parentId][Album.parentId] placement. Throws if the name is already taken. */
    suspend fun createAlbum(album: Album)

    /** Renames the album identified by [id] to [name]. Throws if the name is already taken. */
    suspend fun renameAlbum(
        id: String,
        name: String,
    )

    /** Reparents the album identified by [id] under [parentId], or to the root when [parentId] is null. */
    suspend fun moveAlbum(
        id: String,
        parentId: String?,
    )

    /** Deletes the album, its descendant albums, and all their media associations. */
    suspend fun deleteAlbum(id: String)

    /** Returns a flow of albums attached to the media item identified by [mediaId]. */
    fun getAlbumsForMedia(mediaId: String): Flow<List<Album>>

    /** Attaches the album identified by [albumId] to the media item identified by [mediaId]. */
    suspend fun addMediaToAlbum(
        mediaId: String,
        albumId: String,
    )

    /** Detaches the album identified by [albumId] from the media item identified by [mediaId]. */
    suspend fun removeMediaFromAlbum(
        mediaId: String,
        albumId: String,
    )

    /**
     * Returns a flow of media IDs that have ALL of the given [albumIds] attached.
     * Returns a flow of an empty set when [albumIds] is empty.
     */
    fun getMediaIdsInAllAlbums(albumIds: List<String>): Flow<Set<String>>

    /**
     * Returns a flow of media IDs associated with [albumId], ordered by their
     * album-specific [sort_order][io.github.kiyohitonara.biwa.data.local.Media_album.sort_order].
     */
    fun getOrderedMediaIdsForAlbum(albumId: String): Flow<List<String>>

    /**
     * Persists an album-specific manual ordering by assigning sequential sort_order
     * values to the media items identified by [orderedIds].
     */
    suspend fun reorderAlbumMedia(
        albumId: String,
        orderedIds: List<String>,
    )
}
