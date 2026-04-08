package io.github.kiyohitonara.biwa.domain.repository

import io.github.kiyohitonara.biwa.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/** Provides CRUD operations for tags and their media associations. */
interface TagRepository {
    /** Returns a flow of all tags, ordered alphabetically by name. */
    fun getAllTags(): Flow<List<Tag>>

    /** Returns the tag with the given [id], or null if not found. */
    suspend fun getTagById(id: String): Tag?

    /** Persists a new [tag]. Throws if the name is already taken. */
    suspend fun createTag(tag: Tag)

    /** Renames the tag identified by [id] to [name]. Throws if the name is already taken. */
    suspend fun renameTag(id: String, name: String)

    /** Deletes the tag and all its media associations. */
    suspend fun deleteTag(id: String)

    /** Returns a flow of tags attached to the media item identified by [mediaId]. */
    fun getTagsForMedia(mediaId: String): Flow<List<Tag>>

    /** Attaches the tag identified by [tagId] to the media item identified by [mediaId]. */
    suspend fun addTagToMedia(mediaId: String, tagId: String)

    /** Detaches the tag identified by [tagId] from the media item identified by [mediaId]. */
    suspend fun removeTagFromMedia(mediaId: String, tagId: String)

    /**
     * Returns a flow of media IDs that have ALL of the given [tagIds] attached.
     * Returns a flow of all media IDs when [tagIds] is empty.
     */
    fun getMediaIdsWithAllTags(tagIds: List<String>): Flow<Set<String>>

    /**
     * Returns a flow of media IDs associated with [tagId], ordered by their
     * tag-specific [sort_order][io.github.kiyohitonara.biwa.data.local.Media_tag.sort_order].
     */
    fun getOrderedMediaIdsForTag(tagId: String): Flow<List<String>>

    /**
     * Persists a tag-specific manual ordering by assigning sequential sort_order
     * values to the media items identified by [orderedIds].
     */
    suspend fun reorderTagMedia(tagId: String, orderedIds: List<String>)
}
