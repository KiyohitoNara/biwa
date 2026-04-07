package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.Tag
import io.github.kiyohitonara.biwa.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [TagRepository] for use in tests. */
class FakeTagRepository : TagRepository {
    private val tags = MutableStateFlow<List<Tag>>(emptyList())
    private val mediaTagAssociations = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    override fun getAllTags(): Flow<List<Tag>> = tags

    override suspend fun getTagById(id: String): Tag? = tags.value.find { it.id == id }

    override suspend fun createTag(tag: Tag) {
        if (tags.value.any { it.name == tag.name }) error("Tag name '${tag.name}' already exists")
        tags.value = tags.value + tag
    }

    override suspend fun renameTag(id: String, name: String) {
        if (tags.value.any { it.name == name && it.id != id }) error("Tag name '$name' already exists")
        tags.value = tags.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun deleteTag(id: String) {
        tags.value = tags.value.filter { it.id != id }
        mediaTagAssociations.value = mediaTagAssociations.value.filter { it.second != id }
    }

    override fun getTagsForMedia(mediaId: String): Flow<List<Tag>> =
        mediaTagAssociations.map { associations ->
            val tagIds = associations.filter { it.first == mediaId }.map { it.second }
            tags.value.filter { it.id in tagIds }.sortedBy { it.name }
        }

    override suspend fun addTagToMedia(mediaId: String, tagId: String) {
        if (mediaTagAssociations.value.none { it.first == mediaId && it.second == tagId }) {
            mediaTagAssociations.value = mediaTagAssociations.value + (mediaId to tagId)
        }
    }

    override suspend fun removeTagFromMedia(mediaId: String, tagId: String) {
        mediaTagAssociations.value = mediaTagAssociations.value.filter {
            !(it.first == mediaId && it.second == tagId)
        }
    }

    override fun getMediaIdsWithAllTags(tagIds: List<String>): Flow<Set<String>> =
        mediaTagAssociations.map { associations ->
            if (tagIds.isEmpty()) return@map emptySet()
            associations.groupBy { it.first }
                .filterValues { pairs -> tagIds.all { tagId -> pairs.any { it.second == tagId } } }
                .keys
                .toSet()
        }
}
