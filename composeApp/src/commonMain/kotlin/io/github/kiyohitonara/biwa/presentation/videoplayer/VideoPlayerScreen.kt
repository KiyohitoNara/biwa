package io.github.kiyohitonara.biwa.presentation.videoplayer

import androidx.compose.runtime.Composable

/** Platform-specific full-screen video player screen. */
@Composable
expect fun VideoPlayerScreen(mediaId: String, onBack: () -> Unit)
