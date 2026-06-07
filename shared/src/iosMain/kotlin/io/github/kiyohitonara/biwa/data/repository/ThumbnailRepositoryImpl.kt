package io.github.kiyohitonara.biwa.data.repository

import io.github.kiyohitonara.biwa.domain.repository.ThumbnailRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetImageGenerator
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

/** iOS implementation that extracts video thumbnails via AVFoundation. */
@OptIn(ExperimentalForeignApi::class)
class ThumbnailRepositoryImpl : ThumbnailRepository {
    override suspend fun generateVideoThumbnail(videoPath: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = NSURL.fileURLWithPath(videoPath)
                val asset = AVAsset.assetWithURL(url)
                val generator = AVAssetImageGenerator(asset = asset)
                generator.appliesPreferredTrackTransform = true

                val time = CMTimeMake(value = 1, timescale = 1)
                val cgImage = generator.copyCGImageAtTime(
                    requestedTime = time,
                    actualTime = null,
                    error = null,
                ) ?: return@withContext null

                val uiImage = UIImage.imageWithCGImage(cgImage)
                val jpegData = UIImageJPEGRepresentation(uiImage, 0.8)
                    ?: return@withContext null

                val cachesDir = NSSearchPathForDirectoriesInDomains(
                    NSCachesDirectory, NSUserDomainMask, true
                ).first() as String
                val thumbnailsDir = "$cachesDir/thumbnails"
                NSFileManager.defaultManager.createDirectoryAtPath(
                    path = thumbnailsDir,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )

                val filePath = "$thumbnailsDir/${videoPath.hashCode()}.jpg"
                NSFileManager.defaultManager.createFileAtPath(
                    path = filePath,
                    contents = jpegData,
                    attributes = null,
                )
                filePath
            } catch (_: Exception) {
                null
            }
        }
}
