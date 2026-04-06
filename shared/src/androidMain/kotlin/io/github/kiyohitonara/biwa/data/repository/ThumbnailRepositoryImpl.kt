package io.github.kiyohitonara.biwa.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import io.github.kiyohitonara.biwa.domain.repository.ThumbnailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Android implementation that extracts frames via [MediaMetadataRetriever]. */
class ThumbnailRepositoryImpl(private val context: Context) : ThumbnailRepository {
    override suspend fun generateVideoThumbnail(videoPath: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(videoPath)
                    retriever.getFrameAtTime(
                        1_000_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    )
                } ?: return@withContext null

                val cacheDir = File(context.cacheDir, "thumbnails").also { it.mkdirs() }
                val file = File(cacheDir, "${videoPath.hashCode()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                bitmap.recycle()
                file.absolutePath
            } catch (e: Exception) {
                null
            }
        }
}
