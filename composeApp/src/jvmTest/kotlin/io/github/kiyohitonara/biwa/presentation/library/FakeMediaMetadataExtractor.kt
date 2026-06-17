package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.extractor.MediaMetadataExtractor
import io.github.kiyohitonara.biwa.domain.model.MediaFileMetadata
import io.github.kiyohitonara.biwa.domain.model.MediaType

/** Minimal [MediaMetadataExtractor] for presentation-layer tests. */
class FakeMediaMetadataExtractor : MediaMetadataExtractor {
    override suspend fun extract(sourceUri: String): MediaFileMetadata =
        MediaFileMetadata(
            fileName = sourceUri.substringAfterLast("/"),
            mediaType = MediaType.PHOTO,
        )
}
