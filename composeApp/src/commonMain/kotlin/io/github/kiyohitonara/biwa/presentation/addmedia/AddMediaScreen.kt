package io.github.kiyohitonara.biwa.presentation.addmedia

import androidx.compose.runtime.Composable

/** Platform-specific screen for picking and adding a media file to the library. */
@Composable
expect fun AddMediaScreen(onComplete: () -> Unit)
