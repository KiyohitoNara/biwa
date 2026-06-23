package io.github.kiyohitonara.biwa.data.local

import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/** iOS implementation that copies files into the app sandbox's Application Support directory. */
@OptIn(ExperimentalForeignApi::class)
actual class FileManager(
    logger: Logger,
) : FileStorage {
    private val log = logger.withTag("FileManager")

    actual override suspend fun copyToInternalStorage(
        sourceUri: String,
        fileName: String,
    ): String =
        withContext(Dispatchers.IO) {
            val mediaDir = mediaDirectory()
            NSFileManager.defaultManager.createDirectoryAtPath(
                path = mediaDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )

            val destPath = uniquePath(mediaDir, fileName)
            val success =
                NSFileManager.defaultManager.copyItemAtPath(
                    srcPath = sourceUri,
                    toPath = destPath,
                    error = null,
                )
            check(success) { "Failed to copy file from $sourceUri to $destPath" }

            log.d { "Copied $sourceUri to $destPath" }
            destPath
        }

    actual override suspend fun deleteFromInternalStorage(filePath: String) =
        withContext(Dispatchers.IO) {
            val existed = NSFileManager.defaultManager.fileExistsAtPath(filePath)
            if (existed) {
                NSFileManager.defaultManager.removeItemAtPath(filePath, error = null)
            }
            log.d { "Delete filePath=$filePath existed=$existed" }
            Unit
        }

    private fun mediaDirectory(): String {
        val appSupport =
            NSSearchPathForDirectoriesInDomains(
                NSApplicationSupportDirectory,
                NSUserDomainMask,
                true,
            ).first() as String
        return "$appSupport/$MEDIA_DIR"
    }

    private fun uniquePath(
        dir: String,
        fileName: String,
    ): String {
        val base = fileName.substringBeforeLast(".")
        val ext = fileName.substringAfterLast(".", "")
        val extSuffix = if (ext.isNotEmpty()) ".$ext" else ""
        var candidate = "$dir/$fileName"
        var index = 1
        while (NSFileManager.defaultManager.fileExistsAtPath(candidate)) {
            candidate = "$dir/$base($index)$extSuffix"
            index++
        }
        return candidate
    }

    private companion object {
        const val MEDIA_DIR = "media"
    }
}
