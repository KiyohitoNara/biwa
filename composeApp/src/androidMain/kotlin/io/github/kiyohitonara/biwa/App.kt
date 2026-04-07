package io.github.kiyohitonara.biwa

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kiyohitonara.biwa.presentation.addmedia.AddMediaScreen
import io.github.kiyohitonara.biwa.presentation.library.LibraryScreen
import io.github.kiyohitonara.biwa.presentation.photoviewer.PhotoViewerScreen
import io.github.kiyohitonara.biwa.presentation.tagmanagement.TagManagementScreen
import io.github.kiyohitonara.biwa.presentation.videoplayer.VideoPlayerScreen

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_ADD_MEDIA = "add_media"
private const val ROUTE_VIDEO_PLAYER = "video_player/{id}"
private const val ROUTE_PHOTO_VIEWER = "photo_viewer/{id}"
private const val ROUTE_TAG_MANAGEMENT = "tag_management"

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = ROUTE_LIBRARY) {
            composable(ROUTE_LIBRARY) {
                LibraryScreen(
                    onAddMedia = { navController.navigate(ROUTE_ADD_MEDIA) },
                    onOpenVideoPlayer = { id -> navController.navigate("video_player/$id") },
                    onOpenPhotoViewer = { id -> navController.navigate("photo_viewer/$id") },
                    onManageTags = { navController.navigate(ROUTE_TAG_MANAGEMENT) },
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
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    App()
}
