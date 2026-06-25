package io.github.kiyohitonara.biwa.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase
import io.github.kiyohitonara.biwa.data.local.Tag
import io.github.kiyohitonara.biwa.domain.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import io.github.kiyohitonara.biwa.domain.model.Tag as DomainTag

/** SQLDelight-backed implementation of [TagRepository]. */
@Suppress("TooManyFunctions") // Mirrors the TagRepository CRUD interface surface.
class TagRepositoryImpl(
    driver: SqlDriver,
    logger: Logger,
) : TagRepository {
    private val log = logger.withTag("TagRepository")
    private val db = BiwaDatabase(driver)
    private val tagQueries = db.tagQueries
    private val mediaTagQueries = db.mediaTagQueries

    override fun getAllTags(): Flow<List<DomainTag>> =
        tagQueries
            .selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTagById(id: String): DomainTag? =
        withContext(Dispatchers.IO) {
            tagQueries.selectById(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun createTag(tag: DomainTag) =
        withContext(Dispatchers.IO) {
            log.d { "insert tag id=${tag.id} name=${tag.name}" }
            tagQueries.insert(id = tag.id, name = tag.name, created_at = tag.createdAt)
        }

    override suspend fun renameTag(
        id: String,
        name: String,
    ) = withContext(Dispatchers.IO) {
        log.d { "updateName tag id=$id name=$name" }
        tagQueries.updateName(name = name, id = id)
    }

    override suspend fun deleteTag(id: String) =
        withContext(Dispatchers.IO) {
            log.d { "deleteById tag id=$id" }
            tagQueries.deleteById(id)
        }

    override fun getTagsForMedia(mediaId: String): Flow<List<DomainTag>> =
        mediaTagQueries
            .selectTagsByMediaId(mediaId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun addTagToMedia(
        mediaId: String,
        tagId: String,
    ) = withContext(Dispatchers.IO) {
        log.d { "addTagToMedia mediaId=$mediaId tagId=$tagId" }
        mediaTagQueries.insert(mediaId = mediaId, tagId = tagId)
    }

    override suspend fun removeTagFromMedia(
        mediaId: String,
        tagId: String,
    ) = withContext(Dispatchers.IO) {
        log.d { "removeTagFromMedia mediaId=$mediaId tagId=$tagId" }
        mediaTagQueries.deleteByMediaIdAndTagId(mediaId, tagId)
    }

    override fun getMediaIdsWithAllTags(tagIds: List<String>): Flow<Set<String>> =
        mediaTagQueries
            .selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                if (tagIds.isEmpty()) return@map emptySet()
                rows
                    .groupBy { it.media_id }
                    .filterValues { associations ->
                        tagIds.all { tagId -> associations.any { it.tag_id == tagId } }
                    }.keys
                    .toSet()
            }

    override fun getOrderedMediaIdsForTag(tagId: String): Flow<List<String>> =
        mediaTagQueries
            .selectMediaIdsByTagOrdered(tagId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override suspend fun reorderTagMedia(
        tagId: String,
        orderedIds: List<String>,
    ) = withContext(Dispatchers.IO) {
        log.d { "reorderTagMedia tagId=$tagId count=${orderedIds.size}" }
        db.transaction {
            orderedIds.forEachIndexed { index, mediaId ->
                mediaTagQueries.updateSortOrder(
                    sort_order = index.toLong(),
                    tag_id = tagId,
                    media_id = mediaId,
                )
            }
        }
    }

    private fun Tag.toDomain() = DomainTag(id = id, name = name, createdAt = created_at)
}
