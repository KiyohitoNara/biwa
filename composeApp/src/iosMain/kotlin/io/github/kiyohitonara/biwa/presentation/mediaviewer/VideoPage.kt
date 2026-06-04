package io.github.kiyohitonara.biwa.presentation.mediaviewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

/** iOS implementation using AVPlayerViewController. Auto-plays only when the page is active. */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPage(
    item: MediaItem,
    isActive: Boolean,
    state: MediaViewerUiState.Ready,
    viewModel: MediaViewerViewModel,
) {
    val controller = remember(item.id) {
        val url = NSURL.fileURLWithPath(item.filePath)
        val avPlayer = AVPlayer(uRL = url)
        AVPlayerViewController().also { it.player = avPlayer }
    }

    LaunchedEffect(isActive) {
        if (isActive) controller.player?.play() else controller.player?.pause()
    }

    UIKitViewController(
        factory = { controller },
        modifier = Modifier.fillMaxSize(),
    )
}
