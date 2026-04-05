package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [MediaRepository] backed by a [MutableStateFlow] for use in tests. */
class FakeMediaRepository(
    private val items: MutableStateFlow<List<MediaItem>> = MutableStateFlow(emptyList()),
) : MediaRepository {
    override fun getAllMedia(): Flow<List<MediaItem>> = items
    override suspend fun getMediaById(id: String): MediaItem? = items.value.find { it.id == id }
    override suspend fun addMedia(item: MediaItem) { items.value = items.value + item }
    override suspend fun deleteMedia(id: String) { items.value = items.value.filter { it.id != id } }
    override suspend fun updateLastViewedAt(id: String, timestamp: Long) {}
    override suspend fun updateThumbnailPath(id: String, path: String) {}
    override suspend fun updateSortOrder(id: String, sortOrder: Long) {}
}
