package io.github.kiyohitonara.biwa.presentation.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** iOS implementation backed by [PHPickerViewController] with unlimited selection. */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MediaPicker(
    active: Boolean,
    onPick: (List<String>) -> Unit,
    onCancel: () -> Unit,
) {
    if (!active) return

    val onPickState = rememberUpdatedState(onPick)
    val onCancelState = rememberUpdatedState(onCancel)
    val delegate =
        remember {
            MultiPickerDelegate { paths ->
                if (paths.isEmpty()) onCancelState.value() else onPickState.value(paths)
            }
        }
    UIKitViewController(
        factory = {
            val config = PHPickerConfiguration()
            config.selectionLimit = 0
            PHPickerViewController(configuration = config).also { it.delegate = delegate }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalForeignApi::class)
private class MultiPickerDelegate(
    private val onComplete: (List<String>) -> Unit,
) : NSObject(),
    PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        val results = (didFinishPicking as? List<PHPickerResult>).orEmpty()
        if (results.isEmpty()) {
            onComplete(emptyList())
            return
        }

        val paths = mutableListOf<String>()
        var remaining = results.size
        for (result in results) {
            result.itemProvider.loadFileRepresentationForTypeIdentifier("public.item") { url, _ ->
                val destPath = url?.let { copyToTemp(it) }
                dispatch_async(dispatch_get_main_queue()) {
                    if (destPath != null) paths.add(destPath)
                    remaining--
                    if (remaining == 0) onComplete(paths.toList())
                }
            }
        }
    }

    private fun copyToTemp(sourceUrl: NSURL): String? {
        val fileName = sourceUrl.lastPathComponent ?: "media"
        val uniqueName = "${NSUUID().UUIDString()}_$fileName"
        val destPath = "${NSTemporaryDirectory()}$uniqueName"
        val success =
            NSFileManager.defaultManager.copyItemAtPath(
                srcPath = sourceUrl.path ?: return null,
                toPath = destPath,
                error = null,
            )
        return if (success) destPath else null
    }
}
