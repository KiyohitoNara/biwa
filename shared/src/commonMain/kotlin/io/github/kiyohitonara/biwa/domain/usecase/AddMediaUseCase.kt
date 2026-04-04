package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.AddMediaRequest
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Copies a media file into internal storage and registers it in the library.
 *
 * @param repository Persistence layer for media metadata.
 * @param fileStorage File copy/delete operations.
 * @param clock Provides the current time as Unix epoch seconds.
 */
class AddMediaUseCase(
    private val repository: MediaRepository,
    private val fileStorage: FileStorage,
    private val clock: () -> Long,
) {
    /**
     * Executes the use case with the given [request].
     *
     * Copies the source file to internal storage, constructs a [MediaItem]
     * with a generated ID and the current timestamp, persists it, then
     * returns the newly created item.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun execute(request: AddMediaRequest): MediaItem {
        val id = Uuid.random().toString()
        val filePath = fileStorage.copyToInternalStorage(request.sourceUri, request.fileName)
        val item = MediaItem(
            id = id,
            filePath = filePath,
            mediaType = request.mediaType,
            displayName = request.displayName,
            durationMs = request.durationMs,
            widthPx = request.widthPx,
            heightPx = request.heightPx,
            fileSizeBytes = request.fileSizeBytes,
            thumbnailPath = null,
            takenAt = request.takenAt,
            sortOrder = request.sortOrder,
            lastViewedAt = null,
            addedAt = clock(),
        )
        repository.addMedia(item)
        return item
    }
}
