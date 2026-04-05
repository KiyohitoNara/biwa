package io.github.kiyohitonara.biwa

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kiyohitonara.biwa.presentation.addmedia.AddMediaScreen
import io.github.kiyohitonara.biwa.presentation.library.LibraryScreen

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_ADD_MEDIA = "add_media"

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = ROUTE_LIBRARY) {
            composable(ROUTE_LIBRARY) {
                LibraryScreen(onAddMedia = { navController.navigate(ROUTE_ADD_MEDIA) })
            }
            composable(ROUTE_ADD_MEDIA) {
                AddMediaScreen(onComplete = { navController.popBackStack() })
            }
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    App()
}
