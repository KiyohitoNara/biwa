package io.github.kiyohitonara.biwa

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_ADD_MEDIA = "add_media"
private const val ROUTE_VIDEO_PLAYER = "video_player/{id}"
private const val ROUTE_PHOTO_VIEWER = "photo_viewer/{id}"
private const val ROUTE_TAG_MANAGEMENT = "tag_management"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ABOUT = "about"

@Composable
fun App() {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val darkTheme = when (settingsState.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = ROUTE_LIBRARY) {
            composable(ROUTE_LIBRARY) {
                LibraryScreen(
                    onAddMedia = { navController.navigate(ROUTE_ADD_MEDIA) },
                    onOpenVideoPlayer = { id -> navController.navigate("video_player/$id") },
                    onOpenPhotoViewer = { id -> navController.navigate("photo_viewer/$id") },
                    onManageTags = { navController.navigate(ROUTE_TAG_MANAGEMENT) },
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                )
            }
            composable(ROUTE_ADD_MEDIA) {
                AddMediaScreen(onComplete = { navController.popBackStack() })
            }
            composable(ROUTE_VIDEO_PLAYER) { backStackEntry ->
                VideoPlayerScreen(
                    mediaId = backStackEntry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(ROUTE_PHOTO_VIEWER) { backStackEntry ->
                PhotoViewerScreen(
                    mediaId = backStackEntry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(ROUTE_TAG_MANAGEMENT) {
                TagManagementScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onAbout = { navController.navigate(ROUTE_ABOUT) },
                )
            }
            composable(ROUTE_ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    App()
}
