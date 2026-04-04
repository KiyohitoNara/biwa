package io.github.kiyohitonara.biwa.domain.repository

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/** Manages persistence and retrieval of media items in the library. */
interface MediaRepository {
    /** Returns a stream of all media items, ordered by [MediaItem.addedAt] descending. */
    fun getAllMedia(): Flow<List<MediaItem>>

    /** Returns the media item with the given [id], or null if not found. */
    suspend fun getMediaById(id: String): MediaItem?

    /** Inserts a new [item] into the library. */
    suspend fun addMedia(item: MediaItem)

    /** Removes the media item with the given [id] from the library. */
    suspend fun deleteMedia(id: String)

    /** Updates [lastViewedAt] for the item with the given [id]. */
    suspend fun updateLastViewedAt(id: String, timestamp: Long)

    /** Updates [thumbnailPath] for the item with the given [id]. */
    suspend fun updateThumbnailPath(id: String, path: String)

    /** Updates [sortOrder] for the item with the given [id]. */
    suspend fun updateSortOrder(id: String, sortOrder: Long)
}
