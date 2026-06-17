package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.storage.FileStorage

/** No-op [FileStorage] for presentation-layer tests. */
class FakeFileStorage : FileStorage {
    override suspend fun copyToInternalStorage(
        sourceUri: String,
        fileName: String,
    ): String = ""

    override suspend fun deleteFromInternalStorage(filePath: String) {}
}
