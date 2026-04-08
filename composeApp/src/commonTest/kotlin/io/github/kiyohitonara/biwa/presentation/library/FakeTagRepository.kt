package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.Tag
import io.github.kiyohitonara.biwa.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [TagRepository] backed by [MutableStateFlow] for use in tests. */
class FakeTagRepository : TagRepository {
    val tags = MutableStateFlow<List<Tag>>(emptyList())
    // Triple: (mediaId, tagId, sortOrder)
    private val associations = MutableStateFlow<List<Triple<String, String, Int>>>(emptyList())

    override fun getAllTags(): Flow<List<Tag>> = tags

    override suspend fun getTagById(id: String): Tag? = tags.value.find { it.id == id }

    override suspend fun createTag(tag: Tag) {
        tags.value = tags.value + tag
    }

    override suspend fun renameTag(id: String, name: String) {
        tags.value = tags.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun deleteTag(id: String) {
        tags.value = tags.value.filter { it.id != id }
        associations.value = associations.value.filter { it.second != id }
    }

    override fun getTagsForMedia(mediaId: String): Flow<List<Tag>> =
        associations.map { triples ->
            val tagIds = triples.filter { it.first == mediaId }.map { it.second }
            tags.value.filter { it.id in tagIds }
        }

    override suspend fun addTagToMedia(mediaId: String, tagId: String) {
        if (associations.value.none { it.first == mediaId && it.second == tagId }) {
            val nextOrder = associations.value.filter { it.second == tagId }.size
            associations.value = associations.value + Triple(mediaId, tagId, nextOrder)
        }
    }

    override suspend fun removeTagFromMedia(mediaId: String, tagId: String) {
        associations.value = associations.value.filter { !(it.first == mediaId && it.second == tagId) }
    }

    override fun getMediaIdsWithAllTags(tagIds: List<String>): Flow<Set<String>> =
        associations.map { triples ->
            if (tagIds.isEmpty()) return@map emptySet()
            triples.groupBy { it.first }
                .filterValues { group -> tagIds.all { tagId -> group.any { it.second == tagId } } }
                .keys
                .toSet()
        }

    override fun getOrderedMediaIdsForTag(tagId: String): Flow<List<String>> =
        associations.map { triples ->
            triples.filter { it.second == tagId }
                .sortedBy { it.third }
                .map { it.first }
        }

    override suspend fun reorderTagMedia(tagId: String, orderedIds: List<String>) {
        associations.value = associations.value.map { triple ->
            if (triple.second == tagId) {
                val newOrder = orderedIds.indexOf(triple.first)
                triple.copy(third = if (newOrder != -1) newOrder else triple.third)
            } else {
                triple
            }
        }
    }
}
