package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val fakeRepository = FakeMediaRepository(fakeItems)
    private lateinit var viewModel: LibraryViewModel
    private lateinit var collectionJob: Job

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LibraryViewModel(
            getAllMediaUseCase = GetAllMediaUseCase(fakeRepository),
            deleteMediaUseCase = DeleteMediaUseCase(fakeRepository, fakeFileStorage()),
            getMediaByIdUseCase = GetMediaByIdUseCase(fakeRepository),
            updateLastViewedAtUseCase = UpdateLastViewedAtUseCase(fakeRepository, clock = { 0L }),
        )
        // Subscribe to activate WhileSubscribed sharing
        collectionJob = CoroutineScope(testDispatcher).launch { viewModel.uiState.collect() }
    }

    @AfterTest
    fun teardown() {
        collectionJob.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading before first subscriber`() {
        val freshViewModel = LibraryViewModel(
            getAllMediaUseCase = GetAllMediaUseCase(fakeRepository),
            deleteMediaUseCase = DeleteMediaUseCase(fakeRepository, fakeFileStorage()),
            getMediaByIdUseCase = GetMediaByIdUseCase(fakeRepository),
            updateLastViewedAtUseCase = UpdateLastViewedAtUseCase(fakeRepository, clock = { 0L }),
        )
        assertIs<LibraryUiState.Loading>(freshViewModel.uiState.value)
    }

    @Test
    fun `uiState becomes Success with empty list when repository emits empty`() = runTest {
        fakeItems.value = emptyList()

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(emptyList(), state.items)
    }

    @Test
    fun `uiState Success contains items emitted by repository`() = runTest {
        fakeItems.value = listOf(videoItem())

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(1, state.items.size)
        assertEquals("id-1", state.items.first().id)
    }

    @Test
    fun `uiState updates when repository emits new list`() = runTest {
        fakeItems.value = listOf(videoItem())
        fakeItems.update { it + videoItem().copy(id = "id-2", filePath = "/media/b.mp4") }

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(2, state.items.size)
    }

    @Test
    fun `uiState reflects item removal`() = runTest {
        fakeItems.value = listOf(videoItem())
        fakeItems.value = emptyList()

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(emptyList(), state.items)
    }

    @Test
    fun `deleteMedia removes item from uiState`() = runTest {
        fakeItems.value = listOf(videoItem())

        viewModel.deleteMedia("id-1")

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `deleteMedia emits deleteError when use case throws`() = runTest(testDispatcher) {
        val throwingViewModel = LibraryViewModel(
            getAllMediaUseCase = GetAllMediaUseCase(fakeRepository),
            deleteMediaUseCase = DeleteMediaUseCase(fakeRepository, throwingFileStorage("delete failed")),
            getMediaByIdUseCase = GetMediaByIdUseCase(fakeRepository),
            updateLastViewedAtUseCase = UpdateLastViewedAtUseCase(fakeRepository, clock = { 0L }),
        )
        fakeItems.value = listOf(videoItem())

        var receivedError: String? = null
        val errorJob = launch { throwingViewModel.deleteError.collect { receivedError = it } }

        throwingViewModel.deleteMedia("id-1")
        errorJob.cancel()

        assertEquals("delete failed", receivedError)
    }

    @Test
    fun `deleteMedia does not affect other items`() = runTest {
        fakeItems.value = listOf(
            videoItem().copy(id = "a", filePath = "/media/a.mp4"),
            videoItem().copy(id = "b", filePath = "/media/b.mp4"),
        )

        viewModel.deleteMedia("a")

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(1, state.items.size)
        assertEquals("b", state.items.first().id)
    }

    @Test
    fun `openMedia emits OpenVideoPlayer for VIDEO item`() = runTest(testDispatcher) {
        fakeItems.value = listOf(videoItem())

        var received: LibraryNavEffect? = null
        val job = launch { viewModel.navEffect.collect { received = it } }

        viewModel.openMedia("id-1")
        job.cancel()

        assertEquals(LibraryNavEffect.OpenVideoPlayer("id-1"), received)
    }

    @Test
    fun `openMedia emits OpenVideoPlayer for GIF item`() = runTest(testDispatcher) {
        fakeItems.value = listOf(videoItem().copy(mediaType = MediaType.GIF))

        var received: LibraryNavEffect? = null
        val job = launch { viewModel.navEffect.collect { received = it } }

        viewModel.openMedia("id-1")
        job.cancel()

        assertEquals(LibraryNavEffect.OpenVideoPlayer("id-1"), received)
    }

    @Test
    fun `openMedia emits OpenPhotoViewer for PHOTO item`() = runTest(testDispatcher) {
        fakeItems.value = listOf(videoItem().copy(mediaType = MediaType.PHOTO))

        var received: LibraryNavEffect? = null
        val job = launch { viewModel.navEffect.collect { received = it } }

        viewModel.openMedia("id-1")
        job.cancel()

        assertEquals(LibraryNavEffect.OpenPhotoViewer("id-1"), received)
    }

    @Test
    fun `openMedia does nothing when item not found`() = runTest(testDispatcher) {
        var received: LibraryNavEffect? = null
        val job = launch { viewModel.navEffect.collect { received = it } }

        viewModel.openMedia("nonexistent")
        job.cancel()

        assertEquals(null, received)
    }

    @Test
    fun `openMedia records lastViewedAt timestamp`() = runTest(testDispatcher) {
        fakeItems.value = listOf(videoItem())

        viewModel.openMedia("id-1")

        assertTrue(fakeRepository.lastViewedAtUpdates.any { it.first == "id-1" })
    }

    private fun fakeFileStorage() = object : FileStorage {
        override suspend fun copyToInternalStorage(sourceUri: String, fileName: String) =
            "/internal/media/$fileName"
        override suspend fun deleteFromInternalStorage(filePath: String) {}
    }

    private fun throwingFileStorage(message: String) = object : FileStorage {
        override suspend fun copyToInternalStorage(sourceUri: String, fileName: String) =
            "/internal/media/$fileName"
        override suspend fun deleteFromInternalStorage(filePath: String) {
            error(message)
        }
    }

    private fun videoItem() = MediaItem(
        id = "id-1",
        filePath = "/internal/media/sample.mp4",
        mediaType = MediaType.VIDEO,
        displayName = "sample.mp4",
        durationMs = 30_000L,
        widthPx = 1920L,
        heightPx = 1080L,
        fileSizeBytes = 10_000_000L,
        thumbnailPath = null,
        takenAt = null,
        sortOrder = 0L,
        lastViewedAt = null,
        addedAt = 1_700_000_000L,
    )
}
