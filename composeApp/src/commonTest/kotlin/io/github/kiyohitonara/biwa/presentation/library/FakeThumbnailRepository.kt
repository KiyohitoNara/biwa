package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.repository.ThumbnailRepository

/** In-memory [ThumbnailRepository] that records calls for use in tests. */
class FakeThumbnailRepository : ThumbnailRepository {
    /** Paths passed to [generateVideoThumbnail] in order. */
    val generatedPaths = mutableListOf<String>()

    /** Value returned by [generateVideoThumbnail]. Override to simulate failure. */
    var result: String? = "/cache/thumbnails/thumb.jpg"

    override suspend fun generateVideoThumbnail(videoPath: String): String? {
        generatedPaths.add(videoPath)
        return result
    }
}
