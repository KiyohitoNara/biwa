package io.github.kiyohitonara.biwa.data.extractor

import io.github.kiyohitonara.biwa.domain.extractor.MediaMetadataExtractor
import io.github.kiyohitonara.biwa.domain.model.MediaFileMetadata
import io.github.kiyohitonara.biwa.domain.model.MediaType

/** iOS implementation that infers metadata from the file name and path. */
class MediaMetadataExtractorImpl : MediaMetadataExtractor {
    override suspend fun extract(sourceUri: String): MediaFileMetadata {
        val fileName = sourceUri.substringAfterLast("/").ifBlank { "unknown" }
        val ext = fileName.substringAfterLast(".", "").lowercase()
        val mediaType = when {
            ext == "gif" -> MediaType.GIF
            ext in setOf("mp4", "mov", "m4v", "avi", "mkv") -> MediaType.VIDEO
            else -> MediaType.PHOTO
        }
        return MediaFileMetadata(
            fileName = fileName,
            mediaType = mediaType,
        )
    }
}
