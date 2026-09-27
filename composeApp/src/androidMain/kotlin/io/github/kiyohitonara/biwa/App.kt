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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.presentation.about.AboutScreen
import io.github.kiyohitonara.biwa.presentation.albummanagement.AlbumManagementScreen
import io.github.kiyohitonara.biwa.presentation.library.LibraryScreen
import io.github.kiyohitonara.biwa.presentation.mediaviewer.MediaViewerScreen
import io.github.kiyohitonara.biwa.presentation.settings.SettingsScreen
import io.github.kiyohitonara.biwa.presentation.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

private const val ROUTE_MEDIA = "media"
private const val ROUTE_ALBUMS = "albums"
private const val ROUTE_MEDIA_VIEWER = "media_viewer/{id}"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ABOUT = "about"

/** A top-level destination reachable from the bottom navigation bar. */
private data class TopLevelTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val topLevelTabs =
    listOf(
        TopLevelTab(ROUTE_MEDIA, "Media", Icons.Outlined.PermMedia),
        TopLevelTab(ROUTE_ALBUMS, "Albums", Icons.Filled.Folder),
    )

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
@Suppress("ktlint:compose:modifier-missing-check", "ModifierMissing") // Platform entry point; signature fixed by expect fun.
@Composable
actual fun App() {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val darkTheme =
        when (settingsState.theme) {
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
            AppTheme.SYSTEM -> isSystemInDarkTheme()
        }

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        val navController = rememberNavController()
        val currentRoute by navController.currentBackStackEntryAsState()
        val showBottomBar = currentRoute?.destination?.route in topLevelTabs.map { it.route }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomBar) {
                    AppBottomBar(
                        currentRoute = currentRoute?.destination?.route,
                        onSelectTab = { navController.navigateToTab(it) },
                    )
                }
            },
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_MEDIA,
        enterTransition = forwardEnter,
        exitTransition = forwardExit,
        popEnterTransition = backEnter,
        popExitTransition = backExit,
        modifier = modifier,
    ) {
        composable(ROUTE_MEDIA) {
            LibraryScreen(
                onOpenMediaViewer = { id -> navController.navigate("media_viewer/$id") },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_ALBUMS) {
            AlbumManagementScreen()
        }
        composable(ROUTE_MEDIA_VIEWER) { backStackEntry ->
            MediaViewerScreen(
                mediaId = backStackEntry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() },
            )
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

/** Bottom navigation bar switching between the top-level [topLevelTabs]. */
@Composable
private fun AppBottomBar(
    currentRoute: String?,
    onSelectTab: (String) -> Unit,
) {
    NavigationBar {
        topLevelTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelectTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

/**
 * Switches to the top-level tab [route], saving and restoring each tab's own back stack
 * so that returning to a tab preserves its scroll position and drill-down state.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
