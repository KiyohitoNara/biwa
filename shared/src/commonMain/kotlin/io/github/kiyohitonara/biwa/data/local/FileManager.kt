package io.github.kiyohitonara.biwa.data.local

/**
 * Handles file copy and deletion operations in app-internal storage.
 *
 * Android stores files under `filesDir/media/`.
 * iOS stores files under `Application Support/media/`.
 */
expect class FileManager {
    /**
     * Copies the file identified by [sourceUri] into app-internal storage.
     *
     * @param sourceUri Platform-specific URI or path of the source file.
     * @param fileName Destination file name (must be unique within internal storage).
     * @return Absolute path of the copied file in internal storage.
     */
    suspend fun copyToInternalStorage(sourceUri: String, fileName: String): String

    /**
     * Deletes the file at [filePath] from app-internal storage.
     *
     * Silently succeeds if the file does not exist.
     */
    suspend fun deleteFromInternalStorage(filePath: String)
}
