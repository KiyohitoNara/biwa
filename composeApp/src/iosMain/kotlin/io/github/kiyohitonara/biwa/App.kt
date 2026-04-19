package io.github.kiyohitonara.biwa

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.presentation.about.AboutScreen
import io.github.kiyohitonara.biwa.presentation.addmedia.AddMediaScreen
import io.github.kiyohitonara.biwa.presentation.library.LibraryScreen
import io.github.kiyohitonara.biwa.presentation.photoviewer.PhotoViewerScreen
import io.github.kiyohitonara.biwa.presentation.settings.SettingsScreen
import io.github.kiyohitonara.biwa.presentation.settings.SettingsViewModel
import io.github.kiyohitonara.biwa.presentation.tagmanagement.TagManagementScreen
import io.github.kiyohitonara.biwa.presentation.videoplayer.VideoPlayerScreen
import org.koin.compose.viewmodel.koinViewModel

private sealed interface Screen {
    data object Library : Screen
    data object AddMedia : Screen
    data class VideoPlayer(val id: String) : Screen
    data class PhotoViewer(val id: String) : Screen
    data object TagManagement : Screen
    data object Settings : Screen
    data object About : Screen
}

/** iOS implementation that uses a simple remembered back-stack for navigation. */
@Composable
actual fun App() {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val darkTheme = when (settingsState.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        val backStack = remember { mutableStateListOf<Screen>(Screen.Library) }
        val current = backStack.lastOrNull() ?: Screen.Library
        val goBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }

        when (val screen = current) {
            is Screen.Library -> LibraryScreen(
                onAddMedia = { backStack.add(Screen.AddMedia) },
                onOpenVideoPlayer = { id -> backStack.add(Screen.VideoPlayer(id)) },
                onOpenPhotoViewer = { id -> backStack.add(Screen.PhotoViewer(id)) },
                onManageTags = { backStack.add(Screen.TagManagement) },
                onOpenSettings = { backStack.add(Screen.Settings) },
            )
            is Screen.AddMedia -> AddMediaScreen(onComplete = goBack)
            is Screen.VideoPlayer -> VideoPlayerScreen(mediaId = screen.id, onBack = goBack)
            is Screen.PhotoViewer -> PhotoViewerScreen(mediaId = screen.id, onBack = goBack)
            is Screen.TagManagement -> TagManagementScreen(onBack = goBack)
            is Screen.Settings -> SettingsScreen(
                onBack = goBack,
                onAbout = { backStack.add(Screen.About) },
            )
            is Screen.About -> AboutScreen(onBack = goBack)
        }
    }
}
