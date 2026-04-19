package io.github.kiyohitonara.biwa.presentation.addmedia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.compose.viewmodel.koinViewModel
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.darwin.NSObject

/** iOS implementation that uses PHPickerViewController to select media from the photo library. */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AddMediaScreen(onComplete: () -> Unit) {
    val viewModel: AddMediaViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState is AddMediaUiState.Idle) {
        val delegate = remember {
            PickerDelegate(
                onPicked = { path ->
                    if (path != null) viewModel.addMedia(path)
                    else onComplete()
                }
            )
        }
        UIKitViewController(
            factory = {
                val config = PHPickerConfiguration()
                config.selectionLimit = 1
                PHPickerViewController(configuration = config).also { it.delegate = delegate }
            },
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val state = uiState) {
                is AddMediaUiState.Idle -> {}
                is AddMediaUiState.Loading -> CircularProgressIndicator()
                is AddMediaUiState.Success -> {
                    Text("Added: ${state.item.displayName}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.resetState(); onComplete() }) {
                        Text("Done")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.resetState() }) {
                        Text("Add More")
                    }
                }
                is AddMediaUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.resetState() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PickerDelegate(
    private val onPicked: (String?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        val result = (didFinishPicking as? List<PHPickerResult>)?.firstOrNull()
        if (result == null) {
            onPicked(null)
            return
        }
        result.itemProvider.loadFileRepresentationForTypeIdentifier("public.item") { url, _ ->
            if (url != null) {
                val destPath = copyToTemp(url)
                onPicked(destPath)
            } else {
                onPicked(null)
            }
        }
    }

    /** Copies the temporary PHPicker file to our own temp location so it outlives the callback. */
    private fun copyToTemp(sourceUrl: NSURL): String? {
        val fileName = sourceUrl.lastPathComponent ?: "media"
        val uniqueName = "${NSUUID().UUIDString()}_$fileName"
        val destPath = "${NSTemporaryDirectory()}$uniqueName"
        val success = NSFileManager.defaultManager.copyItemAtPath(
            srcPath = sourceUrl.path ?: return null,
            toPath = destPath,
            error = null,
        )
        return if (success) destPath else null
    }
}
