package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.model.Tag
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GenerateThumbnailUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllTagsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaIdsWithAllTagsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetOrderedMediaIdsForTagUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetUserPreferencesUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderTagMediaUseCase
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
    private val fakeTagRepository = FakeTagRepository()
    private val fakePreferencesRepository = FakeUserPreferencesRepository()
    private lateinit var viewModel: LibraryViewModel
    private lateinit var collectionJob: Job

    private fun buildViewModel(
        repository: FakeMediaRepository = fakeRepository,
        thumbnailRepository: FakeThumbnailRepository = fakeThumbnailRepository,
        tagRepository: FakeTagRepository = fakeTagRepository,
        preferencesRepository: FakeUserPreferencesRepository = fakePreferencesRepository,
    ) = LibraryViewModel(
        getAllMediaUseCase = GetAllMediaUseCase(repository),
        deleteMediaUseCase = DeleteMediaUseCase(repository, fakeFileStorage()),
        getMediaByIdUseCase = GetMediaByIdUseCase(repository),
        updateLastViewedAtUseCase = UpdateLastViewedAtUseCase(repository, clock = { 0L }),
        generateThumbnailUseCase = GenerateThumbnailUseCase(thumbnailRepository, repository),
        reorderMediaUseCase = ReorderMediaUseCase(repository),
        getAllTagsUseCase = GetAllTagsUseCase(tagRepository),
        getMediaIdsWithAllTagsUseCase = GetMediaIdsWithAllTagsUseCase(tagRepository),
        getUserPreferencesUseCase = GetUserPreferencesUseCase(preferencesRepository),
        getOrderedMediaIdsForTagUseCase = GetOrderedMediaIdsForTagUseCase(tagRepository),
        reorderTagMediaUseCase = ReorderTagMediaUseCase(tagRepository),
    )

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
            generateThumbnailUseCase = GenerateThumbnailUseCase(fakeThumbnailRepository, fakeRepository),
            reorderMediaUseCase = ReorderMediaUseCase(fakeRepository),
            getAllTagsUseCase = GetAllTagsUseCase(fakeTagRepository),
            getMediaIdsWithAllTagsUseCase = GetMediaIdsWithAllTagsUseCase(fakeTagRepository),
            getUserPreferencesUseCase = GetUserPreferencesUseCase(fakePreferencesRepository),
            getOrderedMediaIdsForTagUseCase = GetOrderedMediaIdsForTagUseCase(fakeTagRepository),
            reorderTagMediaUseCase = ReorderTagMediaUseCase(fakeTagRepository),
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

    // ── Sort order ───────────────────────────────────────────────────────────

    @Test
    fun `setSortOrder ADDED_AT_DESC sorts newest first`() = runTest {
        fakeItems.value = listOf(
            videoItem().copy(id = "old", addedAt = 1_000L),
            videoItem().copy(id = "new", addedAt = 2_000L),
        )

        viewModel.setSortOrder(SortOrder.ADDED_AT_DESC)

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals("new", state.items.first().id)
    }

    @Test
    fun `setSortOrder ADDED_AT_ASC sorts oldest first`() = runTest {
        fakeItems.value = listOf(
            videoItem().copy(id = "old", addedAt = 1_000L),
            videoItem().copy(id = "new", addedAt = 2_000L),
        )

        viewModel.setSortOrder(SortOrder.ADDED_AT_ASC)

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals("old", state.items.first().id)
    }

    @Test
    fun `setSortOrder FILE_NAME sorts alphabetically`() = runTest {
        fakeItems.value = listOf(
            videoItem().copy(id = "b", displayName = "banana.mp4"),
            videoItem().copy(id = "a", displayName = "apple.mp4"),
        )

        viewModel.setSortOrder(SortOrder.FILE_NAME)

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals("a", state.items.first().id)
    }

    @Test
    fun `setSortOrder FILE_SIZE sorts largest first`() = runTest {
        fakeItems.value = listOf(
            videoItem().copy(id = "small", fileSizeBytes = 1_000L),
            videoItem().copy(id = "large", fileSizeBytes = 9_000L),
        )

        viewModel.setSortOrder(SortOrder.FILE_SIZE)

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals("large", state.items.first().id)
    }

    @Test
    fun `setSortOrder MANUAL sorts by sortOrder ascending`() = runTest {
        fakeItems.value = listOf(
            videoItem().copy(id = "second", sortOrder = 1L),
            videoItem().copy(id = "first", sortOrder = 0L),
        )

        viewModel.setSortOrder(SortOrder.MANUAL)

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals("first", state.items.first().id)
    }

    @Test
    fun `setSortOrder updates sortOrder in uiState`() = runTest {
        viewModel.setSortOrder(SortOrder.FILE_NAME)

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(SortOrder.FILE_NAME, state.sortOrder)
    }

    // ── Manual reorder ────────────────────────────────────────────────────────

    @Test
    fun `reorderMedia persists new order via use case`() = runTest {
        fakeItems.value = listOf(
            videoItem().copy(id = "a", sortOrder = 0L),
            videoItem().copy(id = "b", sortOrder = 1L),
            videoItem().copy(id = "c", sortOrder = 2L),
        )
        viewModel.setSortOrder(SortOrder.MANUAL)

        viewModel.reorderMedia(fromIndex = 0, toIndex = 2)

        // After moving "a" to index 2, order is b, c, a → sort_orders 0, 1, 2 assigned
        val ids = fakeRepository.sortOrderUpdates
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.last() }
        assertEquals(0L, ids["b"])
        assertEquals(1L, ids["c"])
        assertEquals(2L, ids["a"])
    }

    @Test
    fun `reorderMedia with single active tag persists tag-specific order`() = runTest {
        fakeTagRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        fakeItems.value = listOf(
            videoItem().copy(id = "a", filePath = "/media/a.mp4"),
            videoItem().copy(id = "b", filePath = "/media/b.mp4"),
            videoItem().copy(id = "c", filePath = "/media/c.mp4"),
        )
        fakeTagRepository.addTagToMedia("a", "t1")
        fakeTagRepository.addTagToMedia("b", "t1")
        fakeTagRepository.addTagToMedia("c", "t1")
        viewModel.setSortOrder(SortOrder.MANUAL)
        viewModel.toggleTag("t1")

        // Move "a" (index 0) to index 2 → expected order: b, c, a
        viewModel.reorderMedia(fromIndex = 0, toIndex = 2)

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(listOf("b", "c", "a"), state.items.map { it.id })
    }

    @Test
    fun `reorderMedia with single tag does not affect global sort order`() = runTest {
        fakeTagRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        fakeItems.value = listOf(
            videoItem().copy(id = "a", sortOrder = 0L, filePath = "/media/a.mp4"),
            videoItem().copy(id = "b", sortOrder = 1L, filePath = "/media/b.mp4"),
        )
        fakeTagRepository.addTagToMedia("a", "t1")
        fakeTagRepository.addTagToMedia("b", "t1")
        viewModel.setSortOrder(SortOrder.MANUAL)
        viewModel.toggleTag("t1")

        viewModel.reorderMedia(fromIndex = 0, toIndex = 1)

        // Global sort order (sortOrder field) should be unchanged
        val globalOrder = fakeRepository.sortOrderUpdates
        assertTrue(globalOrder.isEmpty())
    }

    @Test
    fun `single tag MANUAL order is preserved when toggling off and on again`() = runTest {
        fakeTagRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        fakeItems.value = listOf(
            videoItem().copy(id = "a", filePath = "/media/a.mp4"),
            videoItem().copy(id = "b", filePath = "/media/b.mp4"),
        )
        fakeTagRepository.addTagToMedia("a", "t1")
        fakeTagRepository.addTagToMedia("b", "t1")
        viewModel.setSortOrder(SortOrder.MANUAL)
        viewModel.toggleTag("t1")
        viewModel.reorderMedia(fromIndex = 0, toIndex = 1) // b, a

        // Toggle off then on again
        viewModel.toggleTag("t1")
        viewModel.toggleTag("t1")

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(listOf("b", "a"), state.items.map { it.id })
    }

    // ── Thumbnail generation ──────────────────────────────────────────────────

    @Test
    fun `thumbnail generation is triggered for VIDEO without thumbnail`() = runTest {
        fakeItems.value = listOf(videoItem())
        val thumbnailRepository = FakeThumbnailRepository()
        val vm = buildViewModel(thumbnailRepository = thumbnailRepository)
        // Activate uiState subscription to start the generator coroutine
        CoroutineScope(testDispatcher).launch { vm.uiState.collect() }.cancel()

        assertTrue(thumbnailRepository.generatedPaths.contains(videoItem().filePath))
    }

    @Test
    fun `thumbnail generation is not triggered for VIDEO with existing thumbnail`() = runTest {
        fakeItems.value = listOf(videoItem().copy(thumbnailPath = "/cache/existing.jpg"))
        val thumbnailRepository = FakeThumbnailRepository()
        buildViewModel(thumbnailRepository = thumbnailRepository)

        assertTrue(thumbnailRepository.generatedPaths.isEmpty())
    }

    @Test
    fun `thumbnail generation is not triggered for PHOTO items`() = runTest {
        fakeItems.value = listOf(videoItem().copy(mediaType = MediaType.PHOTO))
        val thumbnailRepository = FakeThumbnailRepository()
        buildViewModel(thumbnailRepository = thumbnailRepository)

        assertTrue(thumbnailRepository.generatedPaths.isEmpty())
    }

    @Test
    fun `thumbnail generation is not repeated for same VIDEO across emissions`() = runTest {
        fakeItems.value = listOf(videoItem())
        val thumbnailRepository = FakeThumbnailRepository()
        buildViewModel(thumbnailRepository = thumbnailRepository)

        // Second emission of the same item
        fakeItems.value = listOf(videoItem())

        assertEquals(1, thumbnailRepository.generatedPaths.size)
    }

    // ── Default sort order from preferences ───────────────────────────────────

    @Test
    fun `uiState applies persisted default sort order on init`() = runTest {
        val vm = buildViewModel(
            preferencesRepository = FakeUserPreferencesRepository(
                initial = io.github.kiyohitonara.biwa.domain.model.UserPreferences(
                    defaultSortOrder = SortOrder.FILE_NAME,
                ),
            ),
        )
        CoroutineScope(testDispatcher).launch { vm.uiState.collect() }.cancel()

        val state = assertIs<LibraryUiState.Success>(vm.uiState.value)
        assertEquals(SortOrder.FILE_NAME, state.sortOrder)
    }

    // ── Tag filter ────────────────────────────────────────────────────────────

    @Test
    fun `uiState exposes availableTags from repository`() = runTest {
        fakeTagRepository.tags.value = listOf(Tag("t1", "Nature", 0L))

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(1, state.availableTags.size)
        assertEquals("Nature", state.availableTags.first().name)
    }

    @Test
    fun `toggleTag adds tag to activeTagIds`() = runTest {
        viewModel.toggleTag("t1")

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertTrue(state.activeTagIds.contains("t1"))
    }

    @Test
    fun `toggleTag removes tag when already active`() = runTest {
        viewModel.toggleTag("t1")
        viewModel.toggleTag("t1")

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertTrue(state.activeTagIds.isEmpty())
    }

    @Test
    fun `toggleTag filters items by active tag`() = runTest {
        fakeTagRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        fakeItems.value = listOf(
            videoItem().copy(id = "a", filePath = "/media/a.mp4"),
            videoItem().copy(id = "b", filePath = "/media/b.mp4"),
        )
        fakeTagRepository.addTagToMedia("a", "t1")

        viewModel.toggleTag("t1")

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(1, state.items.size)
        assertEquals("a", state.items.first().id)
    }

    @Test
    fun `toggleTag with multiple tags applies AND logic`() = runTest {
        fakeTagRepository.tags.value = listOf(
            Tag("t1", "Nature", 0L),
            Tag("t2", "Travel", 0L),
        )
        fakeItems.value = listOf(
            videoItem().copy(id = "both", filePath = "/media/both.mp4"),
            videoItem().copy(id = "one", filePath = "/media/one.mp4"),
        )
        fakeTagRepository.addTagToMedia("both", "t1")
        fakeTagRepository.addTagToMedia("both", "t2")
        fakeTagRepository.addTagToMedia("one", "t1")

        viewModel.toggleTag("t1")
        viewModel.toggleTag("t2")

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(1, state.items.size)
        assertEquals("both", state.items.first().id)
    }

    @Test
    fun `clearing all active tags shows all items`() = runTest {
        fakeItems.value = listOf(
            videoItem().copy(id = "a", filePath = "/media/a.mp4"),
            videoItem().copy(id = "b", filePath = "/media/b.mp4"),
        )
        fakeTagRepository.addTagToMedia("a", "t1")

        viewModel.toggleTag("t1")
        viewModel.toggleTag("t1")

        val state = assertIs<LibraryUiState.Success>(viewModel.uiState.value)
        assertEquals(2, state.items.size)
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
