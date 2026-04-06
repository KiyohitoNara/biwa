package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.repository.ThumbnailRepository

/**
 * Generates and caches a thumbnail for a VIDEO [MediaItem] that does not yet
 * have one, then persists the resulting path via [MediaRepository].
 *
 * PHOTO and GIF items are skipped because Coil loads them directly from their
 * [MediaItem.filePath] without a separate thumbnail file.
 *
 * @param thumbnailRepository Platform-specific frame extractor.
 * @param mediaRepository Persistence layer for updating [MediaItem.thumbnailPath].
 */
class GenerateThumbnailUseCase(
    private val thumbnailRepository: ThumbnailRepository,
    private val mediaRepository: MediaRepository,
) {
    /**
     * Generates a thumbnail for [item] if it is a VIDEO without a cached path.
     *
     * @return The thumbnail file path on success, or null if the item is not a
     *         VIDEO, already has a thumbnail, or frame extraction failed.
     */
    suspend fun execute(item: MediaItem): String? {
        if (item.mediaType != MediaType.VIDEO) return null
        if (item.thumbnailPath != null) return item.thumbnailPath
        val path = thumbnailRepository.generateVideoThumbnail(item.filePath) ?: return null
        mediaRepository.updateThumbnailPath(item.id, path)
        return path
    }
}
