package io.github.kiyohitonara.biwa.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.data.local.Album
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase
import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import io.github.kiyohitonara.biwa.domain.model.Album as DomainAlbum

/** SQLDelight-backed implementation of [AlbumRepository]. */
@Suppress("TooManyFunctions") // Mirrors the AlbumRepository CRUD interface surface.
class AlbumRepositoryImpl(
    driver: SqlDriver,
    logger: Logger,
) : AlbumRepository {
    private val log = logger.withTag("AlbumRepository")
    private val db = BiwaDatabase(driver)
    private val albumQueries = db.albumQueries
    private val mediaAlbumQueries = db.mediaAlbumQueries

    override fun getAllAlbums(): Flow<List<DomainAlbum>> =
        albumQueries
            .selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAlbumById(id: String): DomainAlbum? =
        withContext(Dispatchers.IO) {
            albumQueries.selectById(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun createAlbum(album: DomainAlbum) =
        withContext(Dispatchers.IO) {
            log.d { "insert album id=${album.id} name=${album.name} parentId=${album.parentId}" }
            albumQueries.insert(
                id = album.id,
                name = album.name,
                parent_id = album.parentId,
                created_at = album.createdAt,
            )
        }

    override suspend fun renameAlbum(
        id: String,
        name: String,
    ) = withContext(Dispatchers.IO) {
        log.d { "updateName album id=$id name=$name" }
        albumQueries.updateName(name = name, id = id)
    }

    override suspend fun moveAlbum(
        id: String,
        parentId: String?,
    ) = withContext(Dispatchers.IO) {
        log.d { "updateParent album id=$id parentId=$parentId" }
        albumQueries.updateParent(parent_id = parentId, id = id)
    }

    override suspend fun deleteAlbum(id: String) =
        withContext(Dispatchers.IO) {
            log.d { "deleteById album id=$id" }
            albumQueries.deleteById(id)
        }

    override fun getAlbumsForMedia(mediaId: String): Flow<List<DomainAlbum>> =
        mediaAlbumQueries
            .selectAlbumsByMediaId(mediaId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun addMediaToAlbum(
        mediaId: String,
        albumId: String,
    ) = withContext(Dispatchers.IO) {
        log.d { "addMediaToAlbum mediaId=$mediaId albumId=$albumId" }
        mediaAlbumQueries.insert(mediaId = mediaId, albumId = albumId)
    }

    override suspend fun removeMediaFromAlbum(
        mediaId: String,
        albumId: String,
    ) = withContext(Dispatchers.IO) {
        log.d { "removeMediaFromAlbum mediaId=$mediaId albumId=$albumId" }
        mediaAlbumQueries.deleteByMediaIdAndAlbumId(mediaId, albumId)
    }

    override fun getMediaIdsInAllAlbums(albumIds: List<String>): Flow<Set<String>> =
        mediaAlbumQueries
            .selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                if (albumIds.isEmpty()) return@map emptySet()
                rows
                    .groupBy { it.media_id }
                    .filterValues { associations ->
                        albumIds.all { albumId -> associations.any { it.album_id == albumId } }
                    }.keys
                    .toSet()
            }

    override fun getOrderedMediaIdsForAlbum(albumId: String): Flow<List<String>> =
        mediaAlbumQueries
            .selectMediaIdsByAlbumOrdered(albumId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override suspend fun reorderAlbumMedia(
        albumId: String,
        orderedIds: List<String>,
    ) = withContext(Dispatchers.IO) {
        log.d { "reorderAlbumMedia albumId=$albumId count=${orderedIds.size}" }
        db.transaction {
            orderedIds.forEachIndexed { index, mediaId ->
                mediaAlbumQueries.updateSortOrder(
                    sort_order = index.toLong(),
                    album_id = albumId,
                    media_id = mediaId,
                )
            }
        }
    }

    private fun Album.toDomain() = DomainAlbum(id = id, name = name, createdAt = created_at, parentId = parent_id)
}
