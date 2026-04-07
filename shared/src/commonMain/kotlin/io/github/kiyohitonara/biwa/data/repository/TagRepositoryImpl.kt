package io.github.kiyohitonara.biwa.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase
import io.github.kiyohitonara.biwa.data.local.Tag
import io.github.kiyohitonara.biwa.domain.model.Tag as DomainTag
import io.github.kiyohitonara.biwa.domain.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** SQLDelight-backed implementation of [TagRepository]. */
class TagRepositoryImpl(driver: SqlDriver) : TagRepository {
    private val db = BiwaDatabase(driver)
    private val tagQueries = db.tagQueries
    private val mediaTagQueries = db.mediaTagQueries

    override fun getAllTags(): Flow<List<DomainTag>> =
        tagQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTagById(id: String): DomainTag? =
        withContext(Dispatchers.IO) {
            tagQueries.selectById(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun createTag(tag: DomainTag) =
        withContext(Dispatchers.IO) {
            tagQueries.insert(id = tag.id, name = tag.name, created_at = tag.createdAt)
        }

    override suspend fun renameTag(id: String, name: String) =
        withContext(Dispatchers.IO) {
            tagQueries.updateName(name = name, id = id)
        }

    override suspend fun deleteTag(id: String) =
        withContext(Dispatchers.IO) {
            tagQueries.deleteById(id)
        }

    override fun getTagsForMedia(mediaId: String): Flow<List<DomainTag>> =
        mediaTagQueries.selectTagsByMediaId(mediaId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun addTagToMedia(mediaId: String, tagId: String) =
        withContext(Dispatchers.IO) {
            mediaTagQueries.insert(media_id = mediaId, tag_id = tagId)
        }

    override suspend fun removeTagFromMedia(mediaId: String, tagId: String) =
        withContext(Dispatchers.IO) {
            mediaTagQueries.deleteByMediaIdAndTagId(media_id = mediaId, tag_id = tagId)
        }

    override fun getMediaIdsWithAllTags(tagIds: List<String>): Flow<Set<String>> =
        mediaTagQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                if (tagIds.isEmpty()) return@map emptySet()
                rows.groupBy { it.media_id }
                    .filterValues { associations ->
                        tagIds.all { tagId -> associations.any { it.tag_id == tagId } }
                    }
                    .keys
                    .toSet()
            }

    private fun Tag.toDomain() = DomainTag(id = id, name = name, createdAt = created_at)
}
