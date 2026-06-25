package io.github.kiyohitonara.biwa.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.domain.repository.ThumbnailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val JPEG_QUALITY = 80

/** Android implementation that extracts frames via [MediaMetadataRetriever]. */
class ThumbnailRepositoryImpl(
    private val context: Context,
    logger: Logger,
) : ThumbnailRepository {
    private val log = logger.withTag("ThumbnailRepository")

    // Frame extraction can fail in many ways (codec, IO, retriever state); degrade to null on any failure.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun generateVideoThumbnail(videoPath: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val bitmap =
                    MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(videoPath)
                        retriever.getFrameAtTime(
                            1_000_000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        )
                    } ?: return@withContext null

                val cacheDir = File(context.cacheDir, "thumbnails").also { it.mkdirs() }
                val file = File(cacheDir, "${videoPath.hashCode()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
                bitmap.recycle()
                log.d { "Generated thumbnail for videoPath=$videoPath at ${file.absolutePath}" }
                file.absolutePath
            } catch (e: Exception) {
                log.w(e) { "Failed to generate thumbnail for videoPath=$videoPath" }
                null
            }
        }
}
