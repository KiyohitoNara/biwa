package io.github.kiyohitonara.biwa

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.presentation.about.AboutScreen
import io.github.kiyohitonara.biwa.presentation.library.LibraryScreen
import io.github.kiyohitonara.biwa.presentation.mediaviewer.MediaViewerScreen
import io.github.kiyohitonara.biwa.presentation.settings.SettingsScreen
import io.github.kiyohitonara.biwa.presentation.settings.SettingsViewModel
import io.github.kiyohitonara.biwa.presentation.tagmanagement.TagManagementScreen
import org.koin.compose.viewmodel.koinViewModel

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_MEDIA_VIEWER = "media_viewer/{id}"
private const val ROUTE_TAG_MANAGEMENT = "tag_management"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ABOUT = "about"

// Material 3 "Shared axis X" motion: 300ms with the standard easing curve, used for peer-to-peer navigation.
private const val SHARED_AXIS_DURATION_MS = 300

private val forwardEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(SlideDirection.Start, tween(SHARED_AXIS_DURATION_MS, easing = FastOutSlowInEasing)) +
        fadeIn(tween(SHARED_AXIS_DURATION_MS, easing = FastOutSlowInEasing))
}
private val forwardExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(SlideDirection.Start, tween(SHARED_AXIS_DURATION_MS, easing = FastOutSlowInEasing)) +
        fadeOut(tween(SHARED_AXIS_DURATION_MS, easing = FastOutSlowInEasing))
}
private val backEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(SlideDirection.End, tween(SHARED_AXIS_DURATION_MS, easing = FastOutSlowInEasing)) +
        fadeIn(tween(SHARED_AXIS_DURATION_MS, easing = FastOutSlowInEasing))
}
private val backExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(SlideDirection.End, tween(SHARED_AXIS_DURATION_MS, easing = FastOutSlowInEasing)) +
        fadeOut(tween(SHARED_AXIS_DURATION_MS, easing = FastOutSlowInEasing))
}

/** Android implementation that uses AndroidX Navigation Compose for the navigation graph. */
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
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = ROUTE_LIBRARY,
            enterTransition = forwardEnter,
            exitTransition = forwardExit,
            popEnterTransition = backEnter,
            popExitTransition = backExit,
        ) {
            composable(ROUTE_LIBRARY) {
                LibraryScreen(
                    onOpenMediaViewer = { id -> navController.navigate("media_viewer/$id") },
                    onManageTags = { navController.navigate(ROUTE_TAG_MANAGEMENT) },
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                )
            }
            composable(ROUTE_MEDIA_VIEWER) { backStackEntry ->
                MediaViewerScreen(
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
