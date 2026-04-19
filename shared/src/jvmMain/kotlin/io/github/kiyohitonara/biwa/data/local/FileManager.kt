package io.github.kiyohitonara.biwa.data.local

import io.github.kiyohitonara.biwa.domain.storage.FileStorage

/** JVM stub — not used at runtime; real implementation is injected per platform. */
actual class FileManager : FileStorage {
    actual override suspend fun copyToInternalStorage(sourceUri: String, fileName: String): String =
        error("FileManager is not supported on JVM")

    actual override suspend fun deleteFromInternalStorage(filePath: String): Unit =
        error("FileManager is not supported on JVM")
}
