package io.github.kiyohitonara.biwa.data.extractor

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import io.github.kiyohitonara.biwa.domain.extractor.MediaMetadataExtractor
import io.github.kiyohitonara.biwa.domain.model.MediaFileMetadata
import io.github.kiyohitonara.biwa.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android implementation that reads metadata via [ContentResolver] and [MediaMetadataRetriever]. */
class MediaMetadataExtractorImpl(private val context: Context) : MediaMetadataExtractor {
    override suspend fun extract(sourceUri: String): MediaFileMetadata = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val mediaType = mimeTypeToMediaType(mimeType)

        var fileName = "unknown"
        var fileSizeBytes: Long? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) fileName = cursor.getString(nameIndex)
                if (sizeIndex >= 0) fileSizeBytes = cursor.getLong(sizeIndex).takeIf { it > 0 }
            }
        }

        var durationMs: Long? = null
        var widthPx: Long? = null
        var heightPx: Long? = null

        when (mediaType) {
            MediaType.VIDEO -> {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri)
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    widthPx = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toLongOrNull()
                    heightPx = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toLongOrNull()
                }
            }
            MediaType.GIF, MediaType.PHOTO -> {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                    widthPx = options.outWidth.toLong().takeIf { it > 0 }
                    heightPx = options.outHeight.toLong().takeIf { it > 0 }
                }
            }
        }

        MediaFileMetadata(
            fileName = fileName,
            mediaType = mediaType,
            durationMs = durationMs,
            widthPx = widthPx,
            heightPx = heightPx,
            fileSizeBytes = fileSizeBytes,
        )
    }

    private fun mimeTypeToMediaType(mimeType: String): MediaType = when {
        mimeType == "image/gif" -> MediaType.GIF
        mimeType.startsWith("video/") -> MediaType.VIDEO
        else -> MediaType.PHOTO
    }
}
