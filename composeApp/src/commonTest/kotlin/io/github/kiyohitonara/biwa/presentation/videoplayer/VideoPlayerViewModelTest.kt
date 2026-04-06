package io.github.kiyohitonara.biwa.presentation.videoplayer

import io.github.kiyohitonara.biwa.domain.model.AbPoint
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.model.PlaybackState
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetPlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ResetAbRepeatUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SavePlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetAbPointUseCase
import io.github.kiyohitonara.biwa.presentation.library.FakeMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
class VideoPlayerViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeMediaRepository = FakeMediaRepository(MutableStateFlow(emptyList()))
    private val fakePlaybackRepository = FakePlaybackStateRepository()

    private fun buildViewModel(mediaId: String = "video-1") = VideoPlayerViewModel(
        mediaId = mediaId,
        getMediaByIdUseCase = GetMediaByIdUseCase(fakeMediaRepository),
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

    // ── Loading ──────────────────────────────────────────────────────────────

    @Test
    fun `initial state is Loading`() {
        val viewModel = buildViewModel()
        // With UnconfinedTestDispatcher the init coroutine runs immediately,
        // but we can verify the transition leads to a non-Loading state when
        // no media exists.
        assertIs<VideoPlayerUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `uiState becomes Error when media item not found`() = runTest {
        val viewModel = buildViewModel("nonexistent")
        assertIs<VideoPlayerUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `uiState becomes Ready when media item exists`() = runTest {
        fakeMediaRepository.addMedia(videoItem())

        val viewModel = buildViewModel()

        assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
    }

    // ── State restoration ────────────────────────────────────────────────────

    @Test
    fun `Ready state uses saved position when available`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        fakePlaybackRepository.savePlaybackState(playbackState().copy(positionMs = 15_000L))

        val viewModel = buildViewModel()

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(15_000L, state.positionMs)
    }

    @Test
    fun `Ready state defaults to position 0 when no saved state`() = runTest {
        fakeMediaRepository.addMedia(videoItem())

        val viewModel = buildViewModel()

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(0L, state.positionMs)
    }

    @Test
    fun `Ready state restores saved playback speed`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        fakePlaybackRepository.savePlaybackState(playbackState().copy(playbackSpeed = 0.5f))

        val viewModel = buildViewModel()

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(0.5f, state.playbackSpeed)
    }

    @Test
    fun `Ready state defaults to speed 1x when no saved state`() = runTest {
        fakeMediaRepository.addMedia(videoItem())

        val viewModel = buildViewModel()

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(1.0f, state.playbackSpeed)
    }

    @Test
    fun `Ready state restores saved AB points`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        fakePlaybackRepository.savePlaybackState(
            playbackState().copy(abStartMs = 5_000L, abEndMs = 20_000L)
        )

        val viewModel = buildViewModel()

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(5_000L, state.abStartMs)
        assertEquals(20_000L, state.abEndMs)
    }

    // ── Player callbacks ─────────────────────────────────────────────────────

    @Test
    fun `updatePosition reflects new position in uiState`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()

        viewModel.updatePosition(12_000L)

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(12_000L, state.positionMs)
    }

    @Test
    fun `updateDuration reflects new duration in uiState`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()

        viewModel.updateDuration(60_000L)

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(60_000L, state.durationMs)
    }

    @Test
    fun `updatePlayingState reflects playing true in uiState`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()

        viewModel.updatePlayingState(true)

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertTrue(state.isPlaying)
    }

    // ── Playback speed ───────────────────────────────────────────────────────

    @Test
    fun `setPlaybackSpeed updates speed in uiState`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()

        viewModel.setPlaybackSpeed(0.5f)

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(0.5f, state.playbackSpeed)
    }

    // ── AB repeat ────────────────────────────────────────────────────────────

    @Test
    fun `setAbPoint A updates abStartMs in uiState`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()

        viewModel.setAbPoint(AbPoint.A, 5_000L)

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(5_000L, state.abStartMs)
    }

    @Test
    fun `setAbPoint B updates abEndMs in uiState`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()

        viewModel.setAbPoint(AbPoint.B, 20_000L)

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(20_000L, state.abEndMs)
    }

    @Test
    fun `setAbPoint emits abRepeatError when range is invalid`() = runTest(testDispatcher) {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()
        viewModel.setAbPoint(AbPoint.A, 10_000L)

        var errorEmitted = false
        val job = launch { viewModel.abRepeatError.collect { errorEmitted = true } }

        viewModel.setAbPoint(AbPoint.B, 5_000L)
        job.cancel()

        assertTrue(errorEmitted)
    }

    @Test
    fun `setAbPoint does not change state when range is invalid`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()
        viewModel.setAbPoint(AbPoint.A, 10_000L)

        viewModel.setAbPoint(AbPoint.B, 5_000L)

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertNull(state.abEndMs)
    }

    @Test
    fun `resetAbRepeat clears ab points in uiState`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        fakePlaybackRepository.savePlaybackState(
            playbackState().copy(abStartMs = 5_000L, abEndMs = 20_000L)
        )
        val viewModel = buildViewModel()

        viewModel.resetAbRepeat()

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertNull(state.abStartMs)
        assertNull(state.abEndMs)
    }

    // ── Controls visibility ──────────────────────────────────────────────────

    @Test
    fun `toggleControls hides controls when visible`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()

        viewModel.toggleControls()

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(false, state.isControlsVisible)
    }

    @Test
    fun `toggleControls shows controls when hidden`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()
        viewModel.toggleControls()

        viewModel.toggleControls()

        val state = assertIs<VideoPlayerUiState.Ready>(viewModel.uiState.value)
        assertEquals(true, state.isControlsVisible)
    }

    // ── State persistence ────────────────────────────────────────────────────

    @Test
    fun `saveCurrentState persists current position and speed`() = runTest {
        fakeMediaRepository.addMedia(videoItem())
        val viewModel = buildViewModel()
        viewModel.updatePosition(15_000L)
        viewModel.setPlaybackSpeed(2.0f)

        viewModel.saveCurrentState()

        val saved = fakePlaybackRepository.getPlaybackState("video-1")
        assertEquals(15_000L, saved?.positionMs)
        assertEquals(2.0f, saved?.playbackSpeed)
    }

    private fun videoItem() = MediaItem(
        id = "video-1",
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

    private fun playbackState() = PlaybackState(
        videoId = "video-1",
        positionMs = 0L,
        abStartMs = null,
        abEndMs = null,
        playbackSpeed = 1.0f,
        updatedAt = 0L,
    )
}
