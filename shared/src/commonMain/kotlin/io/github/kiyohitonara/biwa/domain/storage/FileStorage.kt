package io.github.kiyohitonara.biwa.domain.storage

/**
 * Port for file copy and deletion operations in app-internal storage.
 *
 * Implementations are responsible for resolving platform-specific URIs and
 * persisting files in an area accessible only to the app.
 */
interface FileStorage {
    /**
     * Copies the file identified by [sourceUri] into app-internal storage.
     *
     * @param sourceUri Platform-specific URI or path of the source file.
     * @param fileName Desired destination file name.
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
