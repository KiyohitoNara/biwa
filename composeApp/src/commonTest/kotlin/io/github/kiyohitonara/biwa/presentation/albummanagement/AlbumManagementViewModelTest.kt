package io.github.kiyohitonara.biwa.presentation.albummanagement

import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import io.github.kiyohitonara.biwa.domain.model.Album
import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaToAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.CreateAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAlbumsForMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllAlbumsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RemoveMediaFromAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RenameAlbumUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
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
class AlbumManagementViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeAlbumRepository()
    private lateinit var collectionJob: Job

    private fun buildViewModel(mediaId: String? = null) =
        AlbumManagementViewModel(
            mediaId = mediaId,
            getAllAlbumsUseCase = GetAllAlbumsUseCase(fakeRepository),
            createAlbumUseCase =
                CreateAlbumUseCase(
                    repository = fakeRepository,
                    idGenerator = { "generated-id" },
                    clock = { 0L },
                ),
            renameAlbumUseCase = RenameAlbumUseCase(fakeRepository),
            deleteAlbumUseCase = DeleteAlbumUseCase(fakeRepository),
            getAlbumsForMediaUseCase = GetAlbumsForMediaUseCase(fakeRepository),
            addMediaToAlbumUseCase = AddMediaToAlbumUseCase(fakeRepository),
            removeMediaFromAlbumUseCase = RemoveMediaFromAlbumUseCase(fakeRepository),
            logger = Logger(loggerConfigInit()),
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        if (::collectionJob.isInitialized) collectionJob.cancel()
        Dispatchers.resetMain()
    }

    private fun AlbumManagementViewModel.activate(): AlbumManagementViewModel {
        collectionJob = CoroutineScope(testDispatcher).launch { uiState.collect() }
        return this
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Loading before first subscriber`() {
        val vm = buildViewModel()
        assertIs<AlbumManagementUiState.Loading>(vm.uiState.value)
    }

    @Test
    fun `uiState becomes Ready with empty list when no albums exist`() =
        runTest {
            val vm = buildViewModel().activate()

            val state = assertIs<AlbumManagementUiState.Ready>(vm.uiState.value)
            assertTrue(state.allAlbums.isEmpty())
        }

    @Test
    fun `uiState Ready contains albums from repository`() =
        runTest {
            fakeRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            val vm = buildViewModel().activate()

            val state = assertIs<AlbumManagementUiState.Ready>(vm.uiState.value)
            assertEquals(1, state.allAlbums.size)
            assertEquals("Nature", state.allAlbums.first().name)
        }

    // ── Global mode (mediaId = null) ──────────────────────────────────────────

    @Test
    fun `mediaAlbums is empty when mediaId is null`() =
        runTest {
            fakeRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            fakeRepository.addMediaToAlbum("some-media", "t1")
            val vm = buildViewModel(mediaId = null).activate()

            val state = assertIs<AlbumManagementUiState.Ready>(vm.uiState.value)
            assertTrue(state.mediaAlbums.isEmpty())
        }

    // ── Media-specific mode ───────────────────────────────────────────────────

    @Test
    fun `mediaAlbums reflects albums attached to mediaId`() =
        runTest {
            fakeRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            fakeRepository.addMediaToAlbum("media-1", "t1")
            val vm = buildViewModel(mediaId = "media-1").activate()

            val state = assertIs<AlbumManagementUiState.Ready>(vm.uiState.value)
            assertEquals(1, state.mediaAlbums.size)
            assertEquals("t1", state.mediaAlbums.first().id)
        }

    // ── createAlbum ─────────────────────────────────────────────────────────────

    @Test
    fun `createAlbum adds album to repository`() =
        runTest {
            val vm = buildViewModel().activate()

            vm.createAlbum("Nature")

            val state = assertIs<AlbumManagementUiState.Ready>(vm.uiState.value)
            assertEquals(1, state.allAlbums.size)
            assertEquals("Nature", state.allAlbums.first().name)
        }

    @Test
    fun `createAlbum emits error for blank name`() =
        runTest(testDispatcher) {
            val vm = buildViewModel().activate()
            var receivedError: String? = null
            val job = launch { vm.error.collect { receivedError = it } }

            vm.createAlbum("  ")
            job.cancel()

            assertTrue(receivedError != null)
        }

    // ── renameAlbum ─────────────────────────────────────────────────────────────

    @Test
    fun `renameAlbum updates album name`() =
        runTest {
            fakeRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            val vm = buildViewModel().activate()

            vm.renameAlbum("t1", "Travel")

            val state = assertIs<AlbumManagementUiState.Ready>(vm.uiState.value)
            assertEquals("Travel", state.allAlbums.first().name)
        }

    @Test
    fun `renameAlbum emits error for blank name`() =
        runTest(testDispatcher) {
            fakeRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            val vm = buildViewModel().activate()
            var receivedError: String? = null
            val job = launch { vm.error.collect { receivedError = it } }

            vm.renameAlbum("t1", "")
            job.cancel()

            assertTrue(receivedError != null)
        }

    // ── deleteAlbum ─────────────────────────────────────────────────────────────

    @Test
    fun `deleteAlbum removes album from repository`() =
        runTest {
            fakeRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            val vm = buildViewModel().activate()

            vm.deleteAlbum("t1")

            val state = assertIs<AlbumManagementUiState.Ready>(vm.uiState.value)
            assertTrue(state.allAlbums.isEmpty())
        }

    // ── toggleMediaInAlbum ─────────────────────────────────────────────────────

    @Test
    fun `toggleMediaInAlbum attaches album when not assigned`() =
        runTest {
            fakeRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            val vm = buildViewModel(mediaId = "media-1").activate()

            vm.toggleMediaInAlbum("t1")

            val state = assertIs<AlbumManagementUiState.Ready>(vm.uiState.value)
            assertTrue(state.mediaAlbums.any { it.id == "t1" })
        }

    @Test
    fun `toggleMediaInAlbum detaches album when already assigned`() =
        runTest {
            fakeRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            fakeRepository.addMediaToAlbum("media-1", "t1")
            val vm = buildViewModel(mediaId = "media-1").activate()

            vm.toggleMediaInAlbum("t1")

            val state = assertIs<AlbumManagementUiState.Ready>(vm.uiState.value)
            assertTrue(state.mediaAlbums.isEmpty())
        }

    @Test
    fun `toggleMediaInAlbum is no-op when mediaId is null`() =
        runTest {
            fakeRepository.albums.value = listOf(Album("t1", "Nature", 0L))
            val vm = buildViewModel(mediaId = null).activate()

            vm.toggleMediaInAlbum("t1")

            val state = assertIs<AlbumManagementUiState.Ready>(vm.uiState.value)
            assertTrue(state.mediaAlbums.isEmpty())
        }
}

/** Minimal in-memory [AlbumRepository] for AlbumManagementViewModelTest. */
private class FakeAlbumRepository : AlbumRepository {
    val albums = MutableStateFlow<List<Album>>(emptyList())
    private val associations = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    override fun getAllAlbums(): Flow<List<Album>> = albums

    override suspend fun getAlbumById(id: String): Album? = albums.value.find { it.id == id }

    override suspend fun createAlbum(album: Album) {
        require(album.name.isNotBlank())
        albums.value = albums.value + album
    }

    override suspend fun renameAlbum(
        id: String,
        name: String,
    ) {
        require(name.isNotBlank())
        albums.value = albums.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun deleteAlbum(id: String) {
        albums.value = albums.value.filter { it.id != id }
        associations.value = associations.value.filter { it.second != id }
    }

    override fun getAlbumsForMedia(mediaId: String): Flow<List<Album>> =
        associations.map { pairs ->
            val albumIds = pairs.filter { it.first == mediaId }.map { it.second }
            albums.value.filter { it.id in albumIds }
        }

    override suspend fun addMediaToAlbum(
        mediaId: String,
        albumId: String,
    ) {
        if (associations.value.none { it.first == mediaId && it.second == albumId }) {
            associations.value = associations.value + (mediaId to albumId)
        }
    }

    override suspend fun removeMediaFromAlbum(
        mediaId: String,
        albumId: String,
    ) {
        associations.value = associations.value.filter { !(it.first == mediaId && it.second == albumId) }
    }

    override fun getMediaIdsInAllAlbums(albumIds: List<String>): Flow<Set<String>> =
        associations.map { pairs ->
            if (albumIds.isEmpty()) return@map emptySet()
            pairs
                .groupBy { it.first }
                .filterValues { group -> albumIds.all { t -> group.any { it.second == t } } }
                .keys
                .toSet()
        }

    override fun getOrderedMediaIdsForAlbum(albumId: String): Flow<List<String>> =
        associations.map { pairs -> pairs.filter { it.second == albumId }.map { it.first } }

    override suspend fun reorderAlbumMedia(
        albumId: String,
        orderedIds: List<String>,
    ) {}
}
