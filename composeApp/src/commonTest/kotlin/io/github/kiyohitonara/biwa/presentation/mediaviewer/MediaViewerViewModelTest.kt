package io.github.kiyohitonara.biwa.presentation.mediaviewer

import io.github.kiyohitonara.biwa.domain.model.AbPoint
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetPlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ResetAbRepeatUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SavePlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetAbPointUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import io.github.kiyohitonara.biwa.presentation.library.FakeMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewerViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeMediaRepository = FakeMediaRepository(MutableStateFlow(emptyList()))
    private val fakePlaybackRepository = FakePlaybackStateRepository()

    private fun buildViewModel(mediaId: String = "p1") = MediaViewerViewModel(
        mediaId = mediaId,
        getAllMediaUseCase = GetAllMediaUseCase(fakeMediaRepository),
        updateLastViewedAtUseCase = UpdateLastViewedAtUseCase(fakeMediaRepository, clock = { 0L }),
        deleteMediaUseCase = DeleteMediaUseCase(fakeMediaRepository, fakeFileStorage()),
        getPlaybackStateUseCase = GetPlaybackStateUseCase(fakePlaybackRepository),
        savePlaybackStateUseCase = SavePlaybackStateUseCase(fakePlaybackRepository, clock = { 0L }),
        setAbPointUseCase = SetAbPointUseCase(fakePlaybackRepository, clock = { 0L }),
        resetAbRepeatUseCase = ResetAbRepeatUseCase(fakePlaybackRepository, clock = { 0L }),
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState becomes Error when initial media is not found`() = runTest {
        fakeMediaRepository.addMedia(photoItem("p1"))

        val viewModel = buildViewModel("nonexistent")

        assertIs<MediaViewerUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `Ready state lists all media types together`() = runTest {
        fakeMediaRepository.addMedia(photoItem("p1"))
        fakeMediaRepository.addMedia(videoItem("v1"))
        fakeMediaRepository.addMedia(photoItem("p2"))

        val viewModel = buildViewModel("p1")

        val state = assertIs<MediaViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(3, state.items.size)
    }

    @Test
    fun `Ready state sets currentIndex to position of initial mediaId`() = runTest {
        fakeMediaRepository.addMedia(photoItem("p1"))
        fakeMediaRepository.addMedia(videoItem("v1"))
        fakeMediaRepository.addMedia(photoItem("p2"))

        val viewModel = buildViewModel("v1")

        val state = assertIs<MediaViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(1, state.currentIndex)
    }

    @Test
    fun `onMediaChanged updates currentIndex`() = runTest {
        fakeMediaRepository.addMedia(photoItem("p1"))
        fakeMediaRepository.addMedia(videoItem("v1"))
        val viewModel = buildViewModel("p1")

        viewModel.onMediaChanged(1)

        val state = assertIs<MediaViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(1, state.currentIndex)
    }

    @Test
    fun `onMediaChanged loads saved playback state when target is a video`() = runTest {
        fakeMediaRepository.addMedia(photoItem("p1"))
        fakeMediaRepository.addMedia(videoItem("v1"))
        fakePlaybackRepository.savePlaybackState(playbackState("v1").copy(positionMs = 12_000L))
        val viewModel = buildViewModel("p1")

        viewModel.onMediaChanged(1)

        val state = assertIs<MediaViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(12_000L, state.positionMs)
    }

    @Test
    fun `toggleToolbar hides toolbar when visible`() = runTest {
        fakeMediaRepository.addMedia(photoItem("p1"))
        val viewModel = buildViewModel("p1")

        viewModel.toggleToolbar()

        val state = assertIs<MediaViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(false, state.isToolbarVisible)
    }

    @Test
    fun `setAbPoint A updates abStartMs when current is a video`() = runTest {
        fakeMediaRepository.addMedia(videoItem("v1"))
        val viewModel = buildViewModel("v1")

        viewModel.setAbPoint(AbPoint.A, 5_000L)

        val state = assertIs<MediaViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(5_000L, state.abStartMs)
    }

    @Test
    fun `setAbPoint is no-op when current is a photo`() = runTest {
        fakeMediaRepository.addMedia(photoItem("p1"))
        val viewModel = buildViewModel("p1")

        viewModel.setAbPoint(AbPoint.A, 5_000L)

        val state = assertIs<MediaViewerUiState.Ready>(viewModel.uiState.value)
        assertNull(state.abStartMs)
    }

    @Test
    fun `deleteCurrentMedia removes the item from the library`() = runTest {
        fakeMediaRepository.addMedia(photoItem("p1"))
        fakeMediaRepository.addMedia(photoItem("p2"))
        val viewModel = buildViewModel("p1")

        viewModel.deleteCurrentMedia()

        val state = assertIs<MediaViewerUiState.Ready>(viewModel.uiState.value)
        assertEquals(1, state.items.size)
        assertEquals("p2", state.items.first().id)
    }

    @Test
    fun `deleteCurrentMedia emits navigateBack when last item is deleted`() = runTest(testDispatcher) {
        fakeMediaRepository.addMedia(photoItem("p1"))
        val viewModel = buildViewModel("p1")
        var backFired = false
        val job = launch { viewModel.navigateBack.collect { backFired = true } }

        viewModel.deleteCurrentMedia()
        job.cancel()

        assertTrue(backFired)
    }

    @Test
    fun `saveCurrentState persists position for current video`() = runTest {
        fakeMediaRepository.addMedia(videoItem("v1"))
        val viewModel = buildViewModel("v1")
        viewModel.updatePosition(20_000L)

        viewModel.saveCurrentState()

        val saved = fakePlaybackRepository.getPlaybackState("v1")
        assertEquals(20_000L, saved?.positionMs)
    }

    @Test
    fun `saveCurrentState does not persist for photo items`() = runTest {
        fakeMediaRepository.addMedia(photoItem("p1"))
        val viewModel = buildViewModel("p1")

        viewModel.saveCurrentState()

        assertNull(fakePlaybackRepository.getPlaybackState("p1"))
    }

    private fun fakeFileStorage() = object : FileStorage {
        override suspend fun copyToInternalStorage(sourceUri: String, fileName: String) =
            "/internal/media/$fileName"
        override suspend fun deleteFromInternalStorage(filePath: String) {}
    }

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

    private fun playbackState(videoId: String) = io.github.kiyohitonara.biwa.domain.model.PlaybackState(
        videoId = videoId,
        positionMs = 0L,
        abStartMs = null,
        abEndMs = null,
        playbackSpeed = 1.0f,
        updatedAt = 0L,
    )
}
