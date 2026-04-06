package io.github.kiyohitonara.biwa.presentation.photoviewer

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.usecase.GetAllPhotosUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import io.github.kiyohitonara.biwa.presentation.library.FakeMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class PhotoViewerViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeMediaRepository(MutableStateFlow(emptyList()))

    private fun buildViewModel(mediaId: String = "p1") = PhotoViewerViewModel(
        mediaId = mediaId,
        getAllPhotosUseCase = GetAllPhotosUseCase(fakeRepository),
        updateLastViewedAtUseCase = UpdateLastViewedAtUseCase(fakeRepository, clock = { 0L }),
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    @Test
    fun `uiState becomes Error when initial photo is not found`() = runTest {
        fakeRepository.addMedia(photoItem("p1"))

        val viewModel = buildViewModel("nonexistent")

        assertIs<PhotoViewerUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `uiState becomes Ready when photo list contains initial mediaId`() = runTest {
        fakeRepository.addMedia(photoItem("p1"))

        val viewModel = buildViewModel("p1")

        assertIs<PhotoViewerUiState.Ready>(viewModel.uiState.value)
    }

    // ── Initial index ─────────────────────────────────────────────────────────

    @Test
    fun `Ready state sets currentIndex to position of initial mediaId`() = runTest {
        fakeRepository.addMedia(photoItem("p1"))
        fakeRepository.addMedia(photoItem("p2"))
        fakeRepository.addMedia(photoItem("p3"))

        val viewModel = buildViewModel("p2")

        val state = assertIs<PhotoViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(1, state.currentIndex)
    }

    @Test
    fun `Ready state sets currentIndex 0 for first photo`() = runTest {
        fakeRepository.addMedia(photoItem("p1"))
        fakeRepository.addMedia(photoItem("p2"))

        val viewModel = buildViewModel("p1")

        val state = assertIs<PhotoViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(0, state.currentIndex)
    }

    // ── Photos list ───────────────────────────────────────────────────────────

    @Test
    fun `Ready state includes only PHOTO items`() = runTest {
        fakeRepository.addMedia(videoItem("v1"))
        fakeRepository.addMedia(photoItem("p1"))
        fakeRepository.addMedia(photoItem("p2"))

        val viewModel = buildViewModel("p1")

        val state = assertIs<PhotoViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(2, state.photos.size)
        assertEquals(true, state.photos.all { it.mediaType == MediaType.PHOTO })
    }

    // ── Reactive updates ──────────────────────────────────────────────────────

    @Test
    fun `uiState updates photo list when a new photo is added`() = runTest {
        fakeRepository.addMedia(photoItem("p1"))
        val viewModel = buildViewModel("p1")

        fakeRepository.addMedia(photoItem("p2"))

        val state = assertIs<PhotoViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(2, state.photos.size)
    }

    @Test
    fun `currentIndex is clamped when photos are deleted`() = runTest {
        fakeRepository.addMedia(photoItem("p1"))
        fakeRepository.addMedia(photoItem("p2"))
        fakeRepository.addMedia(photoItem("p3"))
        val viewModel = buildViewModel("p3")
        viewModel.onPhotoChanged(2)

        // Delete the last photo; index 2 no longer exists
        fakeRepository.deleteMedia("p3")

        val state = assertIs<PhotoViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(1, state.currentIndex)
    }

    // ── onPhotoChanged ────────────────────────────────────────────────────────

    @Test
    fun `onPhotoChanged updates currentIndex in uiState`() = runTest {
        fakeRepository.addMedia(photoItem("p1"))
        fakeRepository.addMedia(photoItem("p2"))
        val viewModel = buildViewModel("p1")

        viewModel.onPhotoChanged(1)

        val state = assertIs<PhotoViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(1, state.currentIndex)
    }

    @Test
    fun `onPhotoChanged records lastViewedAt for the photo at given index`() = runTest {
        fakeRepository.addMedia(photoItem("p1"))
        fakeRepository.addMedia(photoItem("p2"))
        val viewModel = buildViewModel("p1")

        viewModel.onPhotoChanged(1)

        assertEquals(true, fakeRepository.lastViewedAtUpdates.any { it.first == "p2" })
    }

    // ── toggleToolbar ─────────────────────────────────────────────────────────

    @Test
    fun `toggleToolbar hides toolbar when visible`() = runTest {
        fakeRepository.addMedia(photoItem("p1"))
        val viewModel = buildViewModel("p1")

        viewModel.toggleToolbar()

        val state = assertIs<PhotoViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(false, state.isToolbarVisible)
    }

    @Test
    fun `toggleToolbar shows toolbar when hidden`() = runTest {
        fakeRepository.addMedia(photoItem("p1"))
        val viewModel = buildViewModel("p1")
        viewModel.toggleToolbar()

        viewModel.toggleToolbar()

        val state = assertIs<PhotoViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(true, state.isToolbarVisible)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun photoItem(id: String) = MediaItem(
        id = id,
        filePath = "/internal/photos/$id.jpg",
        mediaType = MediaType.PHOTO,
        displayName = "$id.jpg",
        durationMs = null,
        widthPx = 1920L,
        heightPx = 1080L,
        fileSizeBytes = 3_000_000L,
        thumbnailPath = null,
        takenAt = null,
        sortOrder = 0L,
        lastViewedAt = null,
        addedAt = 1_700_000_000L,
    )

    private fun videoItem(id: String) = MediaItem(
        id = id,
        filePath = "/internal/videos/$id.mp4",
        mediaType = MediaType.VIDEO,
        displayName = "$id.mp4",
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
