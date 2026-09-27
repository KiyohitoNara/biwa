package io.github.kiyohitonara.biwa.presentation.library

import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import io.github.kiyohitonara.biwa.domain.extractor.MediaMetadataExtractor
import io.github.kiyohitonara.biwa.domain.model.Album
import io.github.kiyohitonara.biwa.domain.model.MediaFileMetadata
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GenerateThumbnailUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllAlbumsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaIdsInAllAlbumsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetOrderedMediaIdsForAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderAlbumMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderMediaUseCase
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
    private val fakeThumbnailRepository = FakeThumbnailRepository()
    private val fakeAlbumRepository = FakeAlbumRepository()
    private lateinit var viewModel: LibraryViewModel
    private lateinit var collectionJob: Job

    private val libraryDisplayState = LibraryDisplayState()

    private fun buildViewModel(
        repository: FakeMediaRepository = fakeRepository,
        thumbnailRepository: FakeThumbnailRepository = fakeThumbnailRepository,
        albumRepository: FakeAlbumRepository = fakeAlbumRepository,
        displayState: LibraryDisplayState = libraryDisplayState,
    ) = LibraryViewModel(
        getAllMediaUseCase = GetAllMediaUseCase(repository),
        deleteMediaUseCase = DeleteMediaUseCase(repository, fakeFileStorage()),
        getMediaByIdUseCase = GetMediaByIdUseCase(repository),
        updateLastViewedAtUseCase = UpdateLastViewedAtUseCase(repository, clock = { 0L }),
        generateThumbnailUseCase = GenerateThumbnailUseCase(thumbnailRepository, repository),
        reorderMediaUseCase = ReorderMediaUseCase(repository),
        getAllAlbumsUseCase = GetAllAlbumsUseCase(albumRepository),
        getMediaIdsInAllAlbumsUseCase = GetMediaIdsInAllAlbumsUseCase(albumRepository),
        getOrderedMediaIdsForAlbumUseCase = GetOrderedMediaIdsForAlbumUseCase(albumRepository),
        reorderAlbumMediaUseCase = ReorderAlbumMediaUseCase(albumRepository),
        addMediaUseCase = AddMediaUseCase(repository, fakeFileStorage(), clock = { 0L }),
        metadataExtractor = fakeMetadataExtractor(),
        libraryDisplayState = displayState,
        logger = Logger(loggerConfigInit()),
    )

    private fun fakeMetadataExtractor() =
        object : MediaMetadataExtractor {
            override suspend fun extract(sourceUri: String) =
                MediaFileMetadata(
                    fileName = sourceUri.substringAfterLast("/"),
                    mediaType = MediaType.PHOTO,
                )
        }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = buildViewModel()
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
        val freshViewModel = buildViewModel()
        assertIs<LibraryUiState.Loading>(freshViewModel.uiState.value)
    }

    @Test
    fun `uiState becomes Success with empty list when repository emits empty`() =
        runTest {
            fakeItems.value = emptyList()

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(emptyList(), state.items)
        }

    @Test
    fun `uiState Success contains items emitted by repository`() =
        runTest {
            fakeItems.value = listOf(videoItem())

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(1, state.items.size)
            assertEquals("id-1", state.items.first().id)
        }

    @Test
    fun `uiState items are ordered by sortOrder ascending`() =
        runTest {
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "second", sortOrder = 1L, filePath = "/media/b.mp4"),
                    videoItem().copy(id = "first", sortOrder = 0L, filePath = "/media/a.mp4"),
                )

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(listOf("first", "second"), state.items.map { it.id })
        }

    @Test
    fun `displayState mirrors emitted items in order`() =
        runTest {
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "a", sortOrder = 0L, filePath = "/media/a.mp4"),
                    videoItem().copy(id = "b", sortOrder = 1L, filePath = "/media/b.mp4"),
                )

            // Ensure uiState has emitted before reading the display state.
            assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(listOf("a", "b"), libraryDisplayState.orderedIds.value)
        }

    @Test
    fun `uiState updates when repository emits new list`() =
        runTest {
            fakeItems.value = listOf(videoItem())
            fakeItems.update { it + videoItem().copy(id = "id-2", filePath = "/media/b.mp4") }

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(2, state.items.size)
        }

    @Test
    fun `uiState reflects item removal`() =
        runTest {
            fakeItems.value = listOf(videoItem())
            fakeItems.value = emptyList()

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(emptyList(), state.items)
        }

    @Test
    fun `deleteMedia removes item from uiState`() =
        runTest {
            fakeItems.value = listOf(videoItem())

            viewModel.deleteMedia("id-1")

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertTrue(state.items.isEmpty())
        }

    @Test
    fun `deleteMedia emits deleteError when use case throws`() =
        runTest(testDispatcher) {
            val throwingViewModel =
                LibraryViewModel(
                    getAllMediaUseCase = GetAllMediaUseCase(fakeRepository),
                    deleteMediaUseCase = DeleteMediaUseCase(fakeRepository, throwingFileStorage("delete failed")),
                    getMediaByIdUseCase = GetMediaByIdUseCase(fakeRepository),
                    updateLastViewedAtUseCase = UpdateLastViewedAtUseCase(fakeRepository, clock = { 0L }),
                    generateThumbnailUseCase = GenerateThumbnailUseCase(fakeThumbnailRepository, fakeRepository),
                    reorderMediaUseCase = ReorderMediaUseCase(fakeRepository),
                    getAllAlbumsUseCase = GetAllAlbumsUseCase(fakeAlbumRepository),
                    getMediaIdsInAllAlbumsUseCase = GetMediaIdsInAllAlbumsUseCase(fakeAlbumRepository),
                    getOrderedMediaIdsForAlbumUseCase = GetOrderedMediaIdsForAlbumUseCase(fakeAlbumRepository),
                    reorderAlbumMediaUseCase = ReorderAlbumMediaUseCase(fakeAlbumRepository),
                    addMediaUseCase = AddMediaUseCase(fakeRepository, fakeFileStorage(), clock = { 0L }),
                    metadataExtractor = fakeMetadataExtractor(),
                    libraryDisplayState = LibraryDisplayState(),
                    logger = Logger(loggerConfigInit()),
                )
            fakeItems.value = listOf(videoItem())

            var receivedError: String? = null
            val errorJob = launch { throwingViewModel.deleteError.collect { receivedError = it } }

            throwingViewModel.deleteMedia("id-1")
            errorJob.cancel()

            assertEquals("delete failed", receivedError)
        }

    @Test
    fun `deleteMedia does not affect other items`() =
        runTest {
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "a", filePath = "/media/a.mp4"),
                    videoItem().copy(id = "b", filePath = "/media/b.mp4"),
                )

            viewModel.deleteMedia("a")

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(1, state.items.size)
            assertEquals("b", state.items.first().id)
        }

    @Test
    fun `openMedia emits OpenMediaViewer for VIDEO item`() =
        runTest(testDispatcher) {
            fakeItems.value = listOf(videoItem())

            var received: LibraryNavEffect? = null
            val job = launch { viewModel.navEffect.collect { received = it } }

            viewModel.openMedia("id-1")
            job.cancel()

            assertEquals(LibraryNavEffect.OpenMediaViewer("id-1"), received)
        }

    @Test
    fun `openMedia emits OpenMediaViewer for GIF item`() =
        runTest(testDispatcher) {
            fakeItems.value = listOf(videoItem().copy(mediaType = MediaType.GIF))

            var received: LibraryNavEffect? = null
            val job = launch { viewModel.navEffect.collect { received = it } }

            viewModel.openMedia("id-1")
            job.cancel()

            assertEquals(LibraryNavEffect.OpenMediaViewer("id-1"), received)
        }

    @Test
    fun `openMedia emits OpenMediaViewer for PHOTO item`() =
        runTest(testDispatcher) {
            fakeItems.value = listOf(videoItem().copy(mediaType = MediaType.PHOTO))

            var received: LibraryNavEffect? = null
            val job = launch { viewModel.navEffect.collect { received = it } }

            viewModel.openMedia("id-1")
            job.cancel()

            assertEquals(LibraryNavEffect.OpenMediaViewer("id-1"), received)
        }

    @Test
    fun `openMedia does nothing when item not found`() =
        runTest(testDispatcher) {
            var received: LibraryNavEffect? = null
            val job = launch { viewModel.navEffect.collect { received = it } }

            viewModel.openMedia("nonexistent")
            job.cancel()

            assertEquals(null, received)
        }

    @Test
    fun `openMedia records lastViewedAt timestamp`() =
        runTest(testDispatcher) {
            fakeItems.value = listOf(videoItem())

            viewModel.openMedia("id-1")

            assertTrue(fakeRepository.lastViewedAtUpdates.any { it.first == "id-1" })
        }

    // ── Apply-once sort actions ──────────────────────────────────────────────

    @Test
    fun `setSortOrder ADDED_AT_DESC persists new order with newest first`() =
        runTest {
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "old", addedAt = 1_000L, sortOrder = 0L),
                    videoItem().copy(id = "new", addedAt = 2_000L, sortOrder = 1L),
                )

            viewModel.setSortOrder(SortOrder.ADDED_AT_DESC)

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(listOf("new", "old"), state.items.map { it.id })
        }

    @Test
    fun `setSortOrder ADDED_AT_ASC persists new order with oldest first`() =
        runTest {
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "new", addedAt = 2_000L, sortOrder = 0L),
                    videoItem().copy(id = "old", addedAt = 1_000L, sortOrder = 1L),
                )

            viewModel.setSortOrder(SortOrder.ADDED_AT_ASC)

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(listOf("old", "new"), state.items.map { it.id })
        }

    @Test
    fun `setSortOrder FILE_NAME persists alphabetical order`() =
        runTest {
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "b", displayName = "banana.mp4", sortOrder = 0L),
                    videoItem().copy(id = "a", displayName = "apple.mp4", sortOrder = 1L),
                )

            viewModel.setSortOrder(SortOrder.FILE_NAME)

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(listOf("a", "b"), state.items.map { it.id })
        }

    @Test
    fun `setSortOrder FILE_SIZE persists order with largest first`() =
        runTest {
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "small", fileSizeBytes = 1_000L, sortOrder = 0L),
                    videoItem().copy(id = "large", fileSizeBytes = 9_000L, sortOrder = 1L),
                )

            viewModel.setSortOrder(SortOrder.FILE_SIZE)

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(listOf("large", "small"), state.items.map { it.id })
        }

    @Test
    fun `setSortOrder writes ordered indices via the reorder use case`() =
        runTest {
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "b", displayName = "b.mp4", sortOrder = 0L),
                    videoItem().copy(id = "a", displayName = "a.mp4", sortOrder = 1L),
                )

            viewModel.setSortOrder(SortOrder.FILE_NAME)

            val finalOrders =
                fakeRepository.sortOrderUpdates
                    .groupBy({ it.first }, { it.second })
                    .mapValues { it.value.last() }
            assertEquals(0L, finalOrders["a"])
            assertEquals(1L, finalOrders["b"])
        }

    @Test
    fun `setSortOrder is a no-op when multiple album filters are active`() =
        runTest {
            fakeAlbumRepository.albums.value =
                listOf(
                    Album("t1", "Nature", 0L),
                    Album("t2", "Travel", 0L),
                )
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "a", filePath = "/media/a.mp4"),
                    videoItem().copy(id = "b", filePath = "/media/b.mp4"),
                )
            fakeAlbumRepository.addMediaToAlbum("a", "t1")
            fakeAlbumRepository.addMediaToAlbum("a", "t2")
            viewModel.toggleAlbum("t1")
            viewModel.toggleAlbum("t2")

            viewModel.setSortOrder(SortOrder.FILE_NAME)

            assertTrue(fakeRepository.sortOrderUpdates.isEmpty())
        }

    @Test
    fun `setSortOrder with single active album persists album-specific order`() =
        runTest {
            fakeAlbumRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "b", displayName = "b.mp4", filePath = "/media/b.mp4"),
                    videoItem().copy(id = "a", displayName = "a.mp4", filePath = "/media/a.mp4"),
                )
            fakeAlbumRepository.addMediaToAlbum("a", "t1")
            fakeAlbumRepository.addMediaToAlbum("b", "t1")
            viewModel.toggleAlbum("t1")

            viewModel.setSortOrder(SortOrder.FILE_NAME)

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(listOf("a", "b"), state.items.map { it.id })
            // Global sort order should not be touched when reordering a tagged subset.
            assertTrue(fakeRepository.sortOrderUpdates.isEmpty())
        }

    // ── Manual reorder ────────────────────────────────────────────────────────

    @Test
    fun `reorderMedia persists new order via use case`() =
        runTest {
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "a", sortOrder = 0L),
                    videoItem().copy(id = "b", sortOrder = 1L),
                    videoItem().copy(id = "c", sortOrder = 2L),
                )

            viewModel.reorderMedia(fromIndex = 0, toIndex = 2)

            // After moving "a" to index 2, order is b, c, a → sort_orders 0, 1, 2 assigned
            val ids =
                fakeRepository.sortOrderUpdates
                    .groupBy({ it.first }, { it.second })
                    .mapValues { it.value.last() }
            assertEquals(0L, ids["b"])
            assertEquals(1L, ids["c"])
            assertEquals(2L, ids["a"])
        }

    @Test
    fun `reorderMedia with single active album persists album-specific order`() =
        runTest {
            fakeAlbumRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "a", filePath = "/media/a.mp4"),
                    videoItem().copy(id = "b", filePath = "/media/b.mp4"),
                    videoItem().copy(id = "c", filePath = "/media/c.mp4"),
                )
            fakeAlbumRepository.addMediaToAlbum("a", "t1")
            fakeAlbumRepository.addMediaToAlbum("b", "t1")
            fakeAlbumRepository.addMediaToAlbum("c", "t1")
            viewModel.toggleAlbum("t1")

            // Move "a" (index 0) to index 2 → expected order: b, c, a
            viewModel.reorderMedia(fromIndex = 0, toIndex = 2)

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(listOf("b", "c", "a"), state.items.map { it.id })
        }

    @Test
    fun `reorderMedia with single album does not affect global sort order`() =
        runTest {
            fakeAlbumRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "a", sortOrder = 0L, filePath = "/media/a.mp4"),
                    videoItem().copy(id = "b", sortOrder = 1L, filePath = "/media/b.mp4"),
                )
            fakeAlbumRepository.addMediaToAlbum("a", "t1")
            fakeAlbumRepository.addMediaToAlbum("b", "t1")
            viewModel.toggleAlbum("t1")

            viewModel.reorderMedia(fromIndex = 0, toIndex = 1)

            // Global sort order (sortOrder field) should be unchanged
            val globalOrder = fakeRepository.sortOrderUpdates
            assertTrue(globalOrder.isEmpty())
        }

    @Test
    fun `single album manual order is preserved when toggling off and on again`() =
        runTest {
            fakeAlbumRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "a", filePath = "/media/a.mp4"),
                    videoItem().copy(id = "b", filePath = "/media/b.mp4"),
                )
            fakeAlbumRepository.addMediaToAlbum("a", "t1")
            fakeAlbumRepository.addMediaToAlbum("b", "t1")
            viewModel.toggleAlbum("t1")
            viewModel.reorderMedia(fromIndex = 0, toIndex = 1) // b, a

            // Toggle off then on again
            viewModel.toggleAlbum("t1")
            viewModel.toggleAlbum("t1")

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(listOf("b", "a"), state.items.map { it.id })
        }

    // ── Thumbnail generation ──────────────────────────────────────────────────

    @Test
    fun `thumbnail generation is triggered for VIDEO without thumbnail`() =
        runTest {
            fakeItems.value = listOf(videoItem())
            val thumbnailRepository = FakeThumbnailRepository()
            val vm = buildViewModel(thumbnailRepository = thumbnailRepository)
            // Activate uiState subscription to start the generator coroutine
            CoroutineScope(testDispatcher).launch { vm.uiState.collect() }.cancel()

            assertTrue(thumbnailRepository.generatedPaths.contains(videoItem().filePath))
        }

    @Test
    fun `thumbnail generation is not triggered for VIDEO with existing thumbnail`() =
        runTest {
            fakeItems.value = listOf(videoItem().copy(thumbnailPath = "/cache/existing.jpg"))
            val thumbnailRepository = FakeThumbnailRepository()
            buildViewModel(thumbnailRepository = thumbnailRepository)

            assertTrue(thumbnailRepository.generatedPaths.isEmpty())
        }

    @Test
    fun `thumbnail generation is not triggered for PHOTO items`() =
        runTest {
            fakeItems.value = listOf(videoItem().copy(mediaType = MediaType.PHOTO))
            val thumbnailRepository = FakeThumbnailRepository()
            buildViewModel(thumbnailRepository = thumbnailRepository)

            assertTrue(thumbnailRepository.generatedPaths.isEmpty())
        }

    @Test
    fun `thumbnail generation is not repeated for same VIDEO across emissions`() =
        runTest {
            fakeItems.value = listOf(videoItem())
            val thumbnailRepository = FakeThumbnailRepository()
            buildViewModel(thumbnailRepository = thumbnailRepository)

            // Second emission of the same item
            fakeItems.value = listOf(videoItem())

            assertEquals(1, thumbnailRepository.generatedPaths.size)
        }

    // ── Album filter ────────────────────────────────────────────────────────────

    @Test
    fun `uiState exposes availableAlbums from repository`() =
        runTest {
            fakeAlbumRepository.albums.value = listOf(Album("t1", "Nature", 0L))

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(1, state.availableAlbums.size)
            assertEquals("Nature", state.availableAlbums.first().name)
        }

    @Test
    fun `toggleAlbum adds album to activeAlbumIds`() =
        runTest {
            fakeAlbumRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            viewModel.toggleAlbum("t1")

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertTrue(state.activeAlbumIds.contains("t1"))
        }

    @Test
    fun `toggleAlbum removes album when already active`() =
        runTest {
            fakeAlbumRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            viewModel.toggleAlbum("t1")

            val activeState = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertTrue(activeState.activeAlbumIds.contains("t1"))

            viewModel.toggleAlbum("t1")

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertTrue(state.activeAlbumIds.isEmpty())
        }

    @Test
    fun `toggleAlbum filters items by active album`() =
        runTest {
            fakeAlbumRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "a", filePath = "/media/a.mp4"),
                    videoItem().copy(id = "b", filePath = "/media/b.mp4"),
                )
            fakeAlbumRepository.addMediaToAlbum("a", "t1")

            viewModel.toggleAlbum("t1")

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(1, state.items.size)
            assertEquals("a", state.items.first().id)
        }

    @Test
    fun `deleting the active album clears it from activeAlbumIds`() =
        runTest {
            fakeAlbumRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            viewModel.toggleAlbum("t1")

            fakeAlbumRepository.albums.value = emptyList()

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertTrue(state.activeAlbumIds.isEmpty())
        }

    @Test
    fun `toggleAlbum with multiple albums applies AND logic`() =
        runTest {
            fakeAlbumRepository.albums.value =
                listOf(
                    Album("t1", "Nature", 0L),
                    Album("t2", "Travel", 0L),
                )
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "both", filePath = "/media/both.mp4"),
                    videoItem().copy(id = "one", filePath = "/media/one.mp4"),
                )
            fakeAlbumRepository.addMediaToAlbum("both", "t1")
            fakeAlbumRepository.addMediaToAlbum("both", "t2")
            fakeAlbumRepository.addMediaToAlbum("one", "t1")

            viewModel.toggleAlbum("t1")
            viewModel.toggleAlbum("t2")

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(1, state.items.size)
            assertEquals("both", state.items.first().id)
        }

    @Test
    fun `clearing all active albums shows all items`() =
        runTest {
            fakeAlbumRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            fakeItems.value =
                listOf(
                    videoItem().copy(id = "a", filePath = "/media/a.mp4"),
                    videoItem().copy(id = "b", filePath = "/media/b.mp4"),
                )
            fakeAlbumRepository.addMediaToAlbum("a", "t1")

            viewModel.toggleAlbum("t1")

            val filteredState = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(1, filteredState.items.size)

            viewModel.toggleAlbum("t1")

            val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
            assertEquals(2, state.items.size)
        }

    @Test
    fun `addMedia is no-op on empty list`() =
        runTest {
            viewModel.addMedia(emptyList())

            assertEquals(false, viewModel.isAdding.value)
        }

    @Test
    fun `addMedia resets isAdding to false after completion`() =
        runTest(testDispatcher) {
            viewModel.addMedia(listOf("content://media/a.jpg"))

            assertEquals(false, viewModel.isAdding.value)
        }

    @Test
    fun `addMedia inserts items into repository on success`() =
        runTest(testDispatcher) {
            viewModel.addMedia(listOf("content://media/a.jpg", "content://media/b.jpg"))

            assertEquals(2, fakeRepository.getAllMedia().first().size)
        }

    @Test
    fun `addMedia emits addMediaError summary on partial failure`() =
        runTest(testDispatcher) {
            val partialExtractor =
                object : MediaMetadataExtractor {
                    override suspend fun extract(sourceUri: String): MediaFileMetadata {
                        if (sourceUri.endsWith("bad.jpg")) error("bad file")
                        return MediaFileMetadata(
                            fileName = sourceUri.substringAfterLast("/"),
                            mediaType = MediaType.PHOTO,
                        )
                    }
                }
            val partialViewModel =
                LibraryViewModel(
                    getAllMediaUseCase = GetAllMediaUseCase(fakeRepository),
                    deleteMediaUseCase = DeleteMediaUseCase(fakeRepository, fakeFileStorage()),
                    getMediaByIdUseCase = GetMediaByIdUseCase(fakeRepository),
                    updateLastViewedAtUseCase = UpdateLastViewedAtUseCase(fakeRepository, clock = { 0L }),
                    generateThumbnailUseCase = GenerateThumbnailUseCase(fakeThumbnailRepository, fakeRepository),
                    reorderMediaUseCase = ReorderMediaUseCase(fakeRepository),
                    getAllAlbumsUseCase = GetAllAlbumsUseCase(fakeAlbumRepository),
                    getMediaIdsInAllAlbumsUseCase = GetMediaIdsInAllAlbumsUseCase(fakeAlbumRepository),
                    getOrderedMediaIdsForAlbumUseCase = GetOrderedMediaIdsForAlbumUseCase(fakeAlbumRepository),
                    reorderAlbumMediaUseCase = ReorderAlbumMediaUseCase(fakeAlbumRepository),
                    addMediaUseCase = AddMediaUseCase(fakeRepository, fakeFileStorage(), clock = { 0L }),
                    metadataExtractor = partialExtractor,
                    libraryDisplayState = LibraryDisplayState(),
                    logger = Logger(loggerConfigInit()),
                )
            var received: String? = null
            val job = launch { partialViewModel.addMediaError.collect { received = it } }

            partialViewModel.addMedia(listOf("content://media/good.jpg", "content://media/bad.jpg"))
            job.cancel()

            assertEquals("Added 1, failed 1", received)
        }

    @Test
    fun `addMedia does not emit addMediaError when all succeed`() =
        runTest(testDispatcher) {
            var received: String? = null
            val job = launch { viewModel.addMediaError.collect { received = it } }

            viewModel.addMedia(listOf("content://media/a.jpg"))
            job.cancel()

            assertEquals(null, received)
        }

    private fun fakeFileStorage() =
        object : FileStorage {
            override suspend fun copyToInternalStorage(
                sourceUri: String,
                fileName: String,
            ) = "/internal/media/$fileName"

            override suspend fun deleteFromInternalStorage(filePath: String) {}
        }

    private fun throwingFileStorage(message: String) =
        object : FileStorage {
            override suspend fun copyToInternalStorage(
                sourceUri: String,
                fileName: String,
            ) = "/internal/media/$fileName"

            override suspend fun deleteFromInternalStorage(filePath: String) {
                error(message)
            }
        }

    private fun videoItem() =
        MediaItem(
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
