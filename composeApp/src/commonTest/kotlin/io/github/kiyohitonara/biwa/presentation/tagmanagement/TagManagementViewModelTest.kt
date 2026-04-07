package io.github.kiyohitonara.biwa.presentation.tagmanagement

import io.github.kiyohitonara.biwa.domain.model.Tag
import io.github.kiyohitonara.biwa.domain.repository.TagRepository
import io.github.kiyohitonara.biwa.domain.usecase.AddTagToMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.CreateTagUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteTagUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllTagsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetTagsForMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RemoveTagFromMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RenameTagUseCase
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
class TagManagementViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeTagRepository()
    private lateinit var collectionJob: Job

    private fun buildViewModel(mediaId: String? = null) = TagManagementViewModel(
        mediaId = mediaId,
        getAllTagsUseCase = GetAllTagsUseCase(fakeRepository),
        createTagUseCase = CreateTagUseCase(
            repository = fakeRepository,
            idGenerator = { "generated-id" },
            clock = { 0L },
        ),
        renameTagUseCase = RenameTagUseCase(fakeRepository),
        deleteTagUseCase = DeleteTagUseCase(fakeRepository),
        getTagsForMediaUseCase = GetTagsForMediaUseCase(fakeRepository),
        addTagToMediaUseCase = AddTagToMediaUseCase(fakeRepository),
        removeTagFromMediaUseCase = RemoveTagFromMediaUseCase(fakeRepository),
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

    private fun TagManagementViewModel.activate(): TagManagementViewModel {
        collectionJob = CoroutineScope(testDispatcher).launch { uiState.collect() }
        return this
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Loading before first subscriber`() {
        val vm = buildViewModel()
        assertIs<TagManagementUiState.Loading>(vm.uiState.value)
    }

    @Test
    fun `uiState becomes Ready with empty list when no tags exist`() = runTest {
        val vm = buildViewModel().activate()

        val state = assertIs<TagManagementUiState.Ready>(vm.uiState.value)
        assertTrue(state.allTags.isEmpty())
    }

    @Test
    fun `uiState Ready contains tags from repository`() = runTest {
        fakeRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        val vm = buildViewModel().activate()

        val state = assertIs<TagManagementUiState.Ready>(vm.uiState.value)
        assertEquals(1, state.allTags.size)
        assertEquals("Nature", state.allTags.first().name)
    }

    // ── Global mode (mediaId = null) ──────────────────────────────────────────

    @Test
    fun `mediaTags is empty when mediaId is null`() = runTest {
        fakeRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        fakeRepository.addTagToMedia("some-media", "t1")
        val vm = buildViewModel(mediaId = null).activate()

        val state = assertIs<TagManagementUiState.Ready>(vm.uiState.value)
        assertTrue(state.mediaTags.isEmpty())
    }

    // ── Media-specific mode ───────────────────────────────────────────────────

    @Test
    fun `mediaTags reflects tags attached to mediaId`() = runTest {
        fakeRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        fakeRepository.addTagToMedia("media-1", "t1")
        val vm = buildViewModel(mediaId = "media-1").activate()

        val state = assertIs<TagManagementUiState.Ready>(vm.uiState.value)
        assertEquals(1, state.mediaTags.size)
        assertEquals("t1", state.mediaTags.first().id)
    }

    // ── createTag ─────────────────────────────────────────────────────────────

    @Test
    fun `createTag adds tag to repository`() = runTest {
        val vm = buildViewModel().activate()

        vm.createTag("Nature")

        val state = assertIs<TagManagementUiState.Ready>(vm.uiState.value)
        assertEquals(1, state.allTags.size)
        assertEquals("Nature", state.allTags.first().name)
    }

    @Test
    fun `createTag emits error for blank name`() = runTest(testDispatcher) {
        val vm = buildViewModel().activate()
        var receivedError: String? = null
        val job = launch { vm.error.collect { receivedError = it } }

        vm.createTag("  ")
        job.cancel()

        assertTrue(receivedError != null)
    }

    // ── renameTag ─────────────────────────────────────────────────────────────

    @Test
    fun `renameTag updates tag name`() = runTest {
        fakeRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        val vm = buildViewModel().activate()

        vm.renameTag("t1", "Travel")

        val state = assertIs<TagManagementUiState.Ready>(vm.uiState.value)
        assertEquals("Travel", state.allTags.first().name)
    }

    @Test
    fun `renameTag emits error for blank name`() = runTest(testDispatcher) {
        fakeRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        val vm = buildViewModel().activate()
        var receivedError: String? = null
        val job = launch { vm.error.collect { receivedError = it } }

        vm.renameTag("t1", "")
        job.cancel()

        assertTrue(receivedError != null)
    }

    // ── deleteTag ─────────────────────────────────────────────────────────────

    @Test
    fun `deleteTag removes tag from repository`() = runTest {
        fakeRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        val vm = buildViewModel().activate()

        vm.deleteTag("t1")

        val state = assertIs<TagManagementUiState.Ready>(vm.uiState.value)
        assertTrue(state.allTags.isEmpty())
    }

    // ── toggleTagForMedia ─────────────────────────────────────────────────────

    @Test
    fun `toggleTagForMedia attaches tag when not assigned`() = runTest {
        fakeRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        val vm = buildViewModel(mediaId = "media-1").activate()

        vm.toggleTagForMedia("t1")

        val state = assertIs<TagManagementUiState.Ready>(vm.uiState.value)
        assertTrue(state.mediaTags.any { it.id == "t1" })
    }

    @Test
    fun `toggleTagForMedia detaches tag when already assigned`() = runTest {
        fakeRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        fakeRepository.addTagToMedia("media-1", "t1")
        val vm = buildViewModel(mediaId = "media-1").activate()

        vm.toggleTagForMedia("t1")

        val state = assertIs<TagManagementUiState.Ready>(vm.uiState.value)
        assertTrue(state.mediaTags.isEmpty())
    }

    @Test
    fun `toggleTagForMedia is no-op when mediaId is null`() = runTest {
        fakeRepository.tags.value = listOf(Tag("t1", "Nature", 0L))
        val vm = buildViewModel(mediaId = null).activate()

        vm.toggleTagForMedia("t1")

        val state = assertIs<TagManagementUiState.Ready>(vm.uiState.value)
        assertTrue(state.mediaTags.isEmpty())
    }
}

/** Minimal in-memory [TagRepository] for TagManagementViewModelTest. */
private class FakeTagRepository : TagRepository {
    val tags = MutableStateFlow<List<Tag>>(emptyList())
    private val associations = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    override fun getAllTags(): Flow<List<Tag>> = tags

    override suspend fun getTagById(id: String): Tag? = tags.value.find { it.id == id }

    override suspend fun createTag(tag: Tag) {
        require(tag.name.isNotBlank())
        tags.value = tags.value + tag
    }

    override suspend fun renameTag(id: String, name: String) {
        require(name.isNotBlank())
        tags.value = tags.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun deleteTag(id: String) {
        tags.value = tags.value.filter { it.id != id }
        associations.value = associations.value.filter { it.second != id }
    }

    override fun getTagsForMedia(mediaId: String): Flow<List<Tag>> =
        associations.map { pairs ->
            val tagIds = pairs.filter { it.first == mediaId }.map { it.second }
            tags.value.filter { it.id in tagIds }
        }

    override suspend fun addTagToMedia(mediaId: String, tagId: String) {
        if (associations.value.none { it.first == mediaId && it.second == tagId }) {
            associations.value = associations.value + (mediaId to tagId)
        }
    }

    override suspend fun removeTagFromMedia(mediaId: String, tagId: String) {
        associations.value = associations.value.filter { !(it.first == mediaId && it.second == tagId) }
    }

    override fun getMediaIdsWithAllTags(tagIds: List<String>): Flow<Set<String>> =
        associations.map { pairs ->
            if (tagIds.isEmpty()) return@map emptySet()
            pairs.groupBy { it.first }
                .filterValues { group -> tagIds.all { t -> group.any { it.second == t } } }
                .keys.toSet()
        }
}
