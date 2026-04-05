package io.github.kiyohitonara.biwa.domain.extractor

import io.github.kiyohitonara.biwa.domain.model.MediaFileMetadata

/** Extracts metadata from a media file identified by a platform-specific URI. */
interface MediaMetadataExtractor {
    /**
     * Reads metadata from the file at [sourceUri].
     *
     * @param sourceUri Platform-specific URI or path of the source file.
     * @return Extracted [MediaFileMetadata].
     */
    suspend fun extract(sourceUri: String): MediaFileMetadata
}
