package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private lateinit var viewModel: LibraryViewModel
    private lateinit var collectionJob: Job

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LibraryViewModel(GetAllMediaUseCase(FakeMediaRepository(fakeItems)))
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
        // Create a ViewModel without subscribing to verify the initial cached value
        val freshViewModel = LibraryViewModel(GetAllMediaUseCase(FakeMediaRepository(fakeItems)))
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
