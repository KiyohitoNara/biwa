package io.github.kiyohitonara.biwa.data.local

import android.content.Context
import android.net.Uri
import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Android implementation that reads via [ContentResolver] and writes to [Context.filesDir]. */
actual class FileManager(
    private val context: Context,
    logger: Logger,
) : FileStorage {
    private val log = logger.withTag("FileManager")

    actual override suspend fun copyToInternalStorage(
        sourceUri: String,
        fileName: String,
    ): String =
        withContext(Dispatchers.IO) {
            val mediaDir = File(context.filesDir, MEDIA_DIR).also { it.mkdirs() }
            val destFile = uniqueFile(mediaDir, fileName)

            context.contentResolver
                .openInputStream(Uri.parse(sourceUri))
                ?.use { input -> destFile.outputStream().use { output -> input.copyTo(output) } }
                ?: error("Failed to open input stream for URI: $sourceUri")

            log.d { "Copied $sourceUri to ${destFile.absolutePath}" }
            destFile.absolutePath
        }

    actual override suspend fun deleteFromInternalStorage(filePath: String) =
        withContext(Dispatchers.IO) {
            val deleted = File(filePath).takeIf { it.exists() }?.delete()
            log.d { "Delete filePath=$filePath deleted=${deleted == true}" }
            Unit
        }

    private fun uniqueFile(
        dir: File,
        fileName: String,
    ): File {
        val base = fileName.substringBeforeLast(".")
        val ext = fileName.substringAfterLast(".", "")
        val extSuffix = if (ext.isNotEmpty()) ".$ext" else ""
        var candidate = File(dir, fileName)
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base($index)$extSuffix")
            index++
        }
        return candidate
    }

    private companion object {
        const val MEDIA_DIR = "media"
    }
}
