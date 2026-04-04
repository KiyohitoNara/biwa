package io.github.kiyohitonara.biwa.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Android implementation that reads via [ContentResolver] and writes to [Context.filesDir]. */
actual class FileManager(private val context: Context) {
    actual suspend fun copyToInternalStorage(sourceUri: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val mediaDir = File(context.filesDir, MEDIA_DIR).also { it.mkdirs() }
            val destFile = uniqueFile(mediaDir, fileName)

            context.contentResolver
                .openInputStream(Uri.parse(sourceUri))
                ?.use { input -> destFile.outputStream().use { output -> input.copyTo(output) } }
                ?: error("Failed to open input stream for URI: $sourceUri")

            destFile.absolutePath
        }

    actual suspend fun deleteFromInternalStorage(filePath: String) =
        withContext(Dispatchers.IO) {
            File(filePath).takeIf { it.exists() }?.delete()
            Unit
        }

    private fun uniqueFile(dir: File, fileName: String): File {
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
