package io.github.kiyohitonara.biwa.presentation.library

import androidx.compose.runtime.Composable

/**
 * Platform-specific media picker that allows multi-selection of photos and videos.
 *
 * When [active] becomes true, the platform picker UI is shown. The caller is
 * responsible for resetting [active] to false in the [onPick] / [onCancel]
 * callbacks. The maximum number of selectable items is determined by the
 * platform (device-dependent on Android, unlimited on iOS).
 *
 * @param active Whether the picker should be visible.
 * @param onPick Invoked with the list of selected URIs / file paths.
 * @param onCancel Invoked if the user dismisses the picker without selecting anything.
 */
@Composable
expect fun MediaPicker(
    active: Boolean,
    onPick: (List<String>) -> Unit,
    onCancel: () -> Unit,
)
