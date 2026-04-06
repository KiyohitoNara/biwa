package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [MediaRepository] backed by a [MutableStateFlow] for use in tests. */
class FakeMediaRepository(
    private val items: MutableStateFlow<List<MediaItem>> = MutableStateFlow(emptyList()),
) : MediaRepository {
    /** Records each (id, timestamp) pair passed to [updateLastViewedAt]. */
    val lastViewedAtUpdates = mutableListOf<Pair<String, Long>>()

    override fun getAllMedia(): Flow<List<MediaItem>> = items
    override suspend fun getMediaById(id: String): MediaItem? = items.value.find { it.id == id }
    override suspend fun addMedia(item: MediaItem) { items.value = items.value + item }
    override suspend fun deleteMedia(id: String) { items.value = items.value.filter { it.id != id } }
    override suspend fun updateLastViewedAt(id: String, timestamp: Long) { lastViewedAtUpdates.add(id to timestamp) }
    override suspend fun updateThumbnailPath(id: String, path: String) {}

    /** Records each (id, sortOrder) pair passed to [updateSortOrder]. */
    val sortOrderUpdates = mutableListOf<Pair<String, Long>>()

    override suspend fun updateSortOrder(id: String, sortOrder: Long) {
        sortOrderUpdates.add(id to sortOrder)
    }
}
