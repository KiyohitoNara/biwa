package io.github.kiyohitonara.biwa.presentation.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState

/** Android implementation backed by [PickMultipleVisualMedia] (system Photo Picker). */
@Composable
actual fun MediaPicker(
    active: Boolean,
    onPick: (List<String>) -> Unit,
    onCancel: () -> Unit,
) {
    val onPickState = rememberUpdatedState(onPick)
    val onCancelState = rememberUpdatedState(onCancel)
    val launcher =
        rememberLauncherForActivityResult(PickMultipleVisualMedia()) { uris ->
            if (uris.isEmpty()) {
                onCancelState.value()
            } else {
                onPickState.value(uris.map { it.toString() })
            }
        }
    LaunchedEffect(active) {
        if (active) {
            launcher.launch(PickVisualMediaRequest(ImageAndVideo))
        }
    }
}
