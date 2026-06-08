package io.github.kiyohitonara.biwa

import androidx.compose.ui.window.ComposeUIViewController
import io.github.kiyohitonara.biwa.presentation.library.LibraryScreen
import io.github.kiyohitonara.biwa.presentation.mediaviewer.MediaViewerScreen
import org.koin.compose.KoinContext

/** Returns a UIViewController hosting [LibraryScreen] connected to the shared Koin context. */
fun makeLibraryViewController(
    onOpenMediaViewer: (String) -> Unit,
    onManageTags: () -> Unit,
    onOpenSettings: () -> Unit,
) = ComposeUIViewController {
    KoinContext {
        LibraryScreen(
            onOpenMediaViewer = onOpenMediaViewer,
            onManageTags = onManageTags,
            onOpenSettings = onOpenSettings,
        )
    }
}

/** Returns a UIViewController hosting [MediaViewerScreen] connected to the shared Koin context. */
fun makeMediaViewerViewController(
    mediaId: String,
    onBack: () -> Unit,
) = ComposeUIViewController {
    KoinContext {
        MediaViewerScreen(
            mediaId = mediaId,
            onBack = onBack,
        )
    }
}
