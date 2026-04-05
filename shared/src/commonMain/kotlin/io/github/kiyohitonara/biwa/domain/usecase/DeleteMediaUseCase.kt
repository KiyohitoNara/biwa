package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.storage.FileStorage

/**
 * Removes a media item from the library and deletes its file from internal storage.
 *
 * @param repository Persistence layer for media metadata.
 * @param fileStorage File deletion operations.
 */
class DeleteMediaUseCase(
    private val repository: MediaRepository,
    private val fileStorage: FileStorage,
) {
    /**
     * Executes the use case for the item identified by [id].
     *
     * Deletes the metadata record first, then removes the file from internal
     * storage. If the file does not exist, the deletion silently succeeds.
     */
    suspend fun execute(id: String) {
        val item = repository.getMediaById(id) ?: return
        repository.deleteMedia(id)
        fileStorage.deleteFromInternalStorage(item.filePath)
    }
}
