package io.github.kiyohitonara.biwa.presentation.addmedia

import io.github.kiyohitonara.biwa.domain.extractor.MediaMetadataExtractor
import io.github.kiyohitonara.biwa.domain.model.AddMediaRequest
import io.github.kiyohitonara.biwa.domain.model.MediaFileMetadata
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AddMediaViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: AddMediaViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AddMediaViewModel(useCase(), extractor())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertIs<AddMediaUiState.Idle>(viewModel.uiState.value)
    }

    @Test
    fun `addMedia transitions to Success on completion`() = runTest {
        viewModel.addMedia("content://media/sample.mp4")

        assertIs<AddMediaUiState.Success>(viewModel.uiState.value)
    }

    @Test
    fun `addMedia Success contains the returned MediaItem`() = runTest {
        viewModel.addMedia("content://media/sample.mp4")

        val state = assertIs<AddMediaUiState.Success>(viewModel.uiState.value)
        assertEquals("sample.mp4", state.item.displayName)
    }

    @Test
    fun `addMedia transitions to Error when extractor throws`() = runTest {
        val failingViewModel = AddMediaViewModel(useCase(), extractor(throwOnExtract = true))
        failingViewModel.addMedia("content://media/sample.mp4")

        assertIs<AddMediaUiState.Error>(failingViewModel.uiState.value)
    }

    @Test
    fun `addMedia Error contains the exception message`() = runTest {
        val failingViewModel = AddMediaViewModel(
            useCase(),
            extractor(throwOnExtract = true, errorMessage = "permission denied"),
        )
        failingViewModel.addMedia("content://media/sample.mp4")

        val state = assertIs<AddMediaUiState.Error>(failingViewModel.uiState.value)
        assertEquals("permission denied", state.message)
    }

    @Test
    fun `addMedia transitions to Error when use case throws`() = runTest {
        val failingViewModel = AddMediaViewModel(useCase(throwOnCopy = true), extractor())
        failingViewModel.addMedia("content://media/sample.mp4")

        assertIs<AddMediaUiState.Error>(failingViewModel.uiState.value)
    }

    @Test
    fun `resetState returns to Idle after Success`() = runTest {
        viewModel.addMedia("content://media/sample.mp4")
        viewModel.resetState()

        assertIs<AddMediaUiState.Idle>(viewModel.uiState.value)
    }

    @Test
    fun `resetState returns to Idle after Error`() = runTest {
        val failingViewModel = AddMediaViewModel(useCase(), extractor(throwOnExtract = true))
        failingViewModel.addMedia("content://media/sample.mp4")
        failingViewModel.resetState()

        assertIs<AddMediaUiState.Idle>(failingViewModel.uiState.value)
    }

    private fun extractor(throwOnExtract: Boolean = false, errorMessage: String = "error") =
        object : MediaMetadataExtractor {
            override suspend fun extract(sourceUri: String): MediaFileMetadata {
                if (throwOnExtract) error(errorMessage)
                val fileName = sourceUri.substringAfterLast("/")
                return MediaFileMetadata(
                    fileName = fileName,
                    mediaType = MediaType.VIDEO,
                    durationMs = 30_000L,
                    widthPx = 1920L,
                    heightPx = 1080L,
                    fileSizeBytes = 10_000_000L,
                )
            }
        }

    private fun useCase(throwOnCopy: Boolean = false) =
        AddMediaUseCase(
            repository = object : MediaRepository {
                val items = mutableListOf<MediaItem>()
                override fun getAllMedia(): Flow<List<MediaItem>> = flowOf(items.toList())
                override suspend fun getMediaById(id: String): MediaItem? = items.find { it.id == id }
                override suspend fun addMedia(item: MediaItem) { items.add(item) }
                override suspend fun deleteMedia(id: String) { items.removeAll { it.id == id } }
                override suspend fun updateLastViewedAt(id: String, timestamp: Long) {}
                override suspend fun updateThumbnailPath(id: String, path: String) {}
                override suspend fun updateSortOrder(id: String, sortOrder: Long) {}
            },
            fileStorage = object : FileStorage {
                override suspend fun copyToInternalStorage(sourceUri: String, fileName: String): String {
                    if (throwOnCopy) error("copy failed")
                    return "/internal/media/$fileName"
                }
                override suspend fun deleteFromInternalStorage(filePath: String) {}
            },
            clock = { 1_700_000_000L },
        )
}
