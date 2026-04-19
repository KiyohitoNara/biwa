package io.github.kiyohitonara.biwa.presentation.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LibraryScreenTest {

    @Test
    fun `clicking More options opens overflow menu with Settings item`() = runComposeUiTest {
        setContent {
            LibraryScreen(
                onAddMedia = {},
                onOpenVideoPlayer = {},
                onOpenPhotoViewer = {},
                onManageTags = {},
                onOpenSettings = {},
                viewModel = buildTestViewModel(),
            )
        }
        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun `clicking Settings in overflow menu invokes onOpenSettings callback`() = runComposeUiTest {
        var settingsOpened = false
        setContent {
            LibraryScreen(
                onAddMedia = {},
                onOpenVideoPlayer = {},
                onOpenPhotoViewer = {},
                onManageTags = {},
                onOpenSettings = { settingsOpened = true },
                viewModel = buildTestViewModel(),
            )
        }
        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Settings").performClick()
        assertTrue(settingsOpened)
    }

    @Test
    fun `overflow menu is dismissed after clicking Settings`() = runComposeUiTest {
        setContent {
            LibraryScreen(
                onAddMedia = {},
                onOpenVideoPlayer = {},
                onOpenPhotoViewer = {},
                onManageTags = {},
                onOpenSettings = {},
                viewModel = buildTestViewModel(),
            )
        }
        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Settings").performClick()
        assertTrue(onAllNodesWithText("Settings").fetchSemanticsNodes().isEmpty())
    }

    private fun buildTestViewModel(): LibraryViewModel {
        val fakeItems = MutableStateFlow<List<MediaItem>>(emptyList())
        val fakeMediaRepository = FakeMediaRepository(fakeItems)
        val fakeThumbnailRepository = FakeThumbnailRepository()
        val fakeTagRepository = FakeTagRepository()
        val fakePreferencesRepository = FakeUserPreferencesRepository()
        val fakeFileStorage = object : FileStorage {
            override suspend fun copyToInternalStorage(sourceUri: String, fileName: String) = ""
            override suspend fun deleteFromInternalStorage(filePath: String) {}
        }
        return LibraryViewModel(
            getAllMediaUseCase = GetAllMediaUseCase(fakeMediaRepository),
            deleteMediaUseCase = DeleteMediaUseCase(fakeMediaRepository, fakeFileStorage),
            getMediaByIdUseCase = GetMediaByIdUseCase(fakeMediaRepository),
            updateLastViewedAtUseCase = UpdateLastViewedAtUseCase(fakeMediaRepository, clock = { 0L }),
            generateThumbnailUseCase = GenerateThumbnailUseCase(fakeThumbnailRepository, fakeMediaRepository),
            reorderMediaUseCase = ReorderMediaUseCase(fakeMediaRepository),
            getAllTagsUseCase = GetAllTagsUseCase(fakeTagRepository),
            getMediaIdsWithAllTagsUseCase = GetMediaIdsWithAllTagsUseCase(fakeTagRepository),
            getUserPreferencesUseCase = GetUserPreferencesUseCase(fakePreferencesRepository),
            getOrderedMediaIdsForTagUseCase = GetOrderedMediaIdsForTagUseCase(fakeTagRepository),
            reorderTagMediaUseCase = ReorderTagMediaUseCase(fakeTagRepository),
        )
    }
}
