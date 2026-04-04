package io.github.kiyohitonara.biwa.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase
import io.github.kiyohitonara.biwa.data.local.Media_metadata
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** SQLDelight-backed implementation of [MediaRepository]. */
class MediaRepositoryImpl(driver: SqlDriver) : MediaRepository {
    private val queries = BiwaDatabase(driver).mediaMetadataQueries

    override fun getAllMedia(): Flow<List<MediaItem>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getMediaById(id: String): MediaItem? =
        withContext(Dispatchers.IO) {
            queries.selectById(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun addMedia(item: MediaItem) =
        withContext(Dispatchers.IO) {
            queries.insert(
                id = item.id,
                file_path = item.filePath,
                media_type = item.mediaType.name,
                display_name = item.displayName,
                duration_ms = item.durationMs,
                width_px = item.widthPx,
                height_px = item.heightPx,
                file_size_bytes = item.fileSizeBytes,
                thumbnail_path = item.thumbnailPath,
                taken_at = item.takenAt,
                sort_order = item.sortOrder,
                added_at = item.addedAt,
            )
        }

    override suspend fun deleteMedia(id: String) =
        withContext(Dispatchers.IO) {
            queries.deleteById(id)
        }

    override suspend fun updateLastViewedAt(id: String, timestamp: Long) =
        withContext(Dispatchers.IO) {
            queries.updateLastViewedAt(last_viewed_at = timestamp, id = id)
        }

    override suspend fun updateThumbnailPath(id: String, path: String) =
        withContext(Dispatchers.IO) {
            queries.updateThumbnailPath(thumbnail_path = path, id = id)
        }

    override suspend fun updateSortOrder(id: String, sortOrder: Long) =
        withContext(Dispatchers.IO) {
            queries.updateSortOrder(sort_order = sortOrder, id = id)
        }

    private fun Media_metadata.toDomain() = MediaItem(
        id = id,
        filePath = file_path,
        mediaType = MediaType.valueOf(media_type),
        displayName = display_name,
        durationMs = duration_ms,
        widthPx = width_px,
        heightPx = height_px,
        fileSizeBytes = file_size_bytes,
        thumbnailPath = thumbnail_path,
        takenAt = taken_at,
        sortOrder = sort_order,
        lastViewedAt = last_viewed_at,
        addedAt = added_at,
    )
}
