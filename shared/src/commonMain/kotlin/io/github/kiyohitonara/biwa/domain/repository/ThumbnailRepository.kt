package io.github.kiyohitonara.biwa.domain.repository

/**
 * Generates and caches video thumbnails on the local filesystem.
 *
 * Implementations are platform-specific and use the platform's media APIs
 * to extract a representative frame from a video file.
 */
interface ThumbnailRepository {
    /**
     * Extracts a frame from the video at [videoPath] and writes it to the
     * platform's cache directory as a JPEG.
     *
     * @param videoPath Absolute file-system path to the video file.
     * @return Absolute path of the written JPEG, or null if extraction failed.
     */
    suspend fun generateVideoThumbnail(videoPath: String): String?
}
