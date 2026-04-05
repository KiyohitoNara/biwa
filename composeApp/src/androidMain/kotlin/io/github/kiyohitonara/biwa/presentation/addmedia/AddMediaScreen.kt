package io.github.kiyohitonara.biwa.presentation.addmedia

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/** Screen that allows the user to pick a media file and add it to the library. */
@Composable
fun AddMediaScreen(
    viewModel: AddMediaViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.toString()?.let { viewModel.addMedia(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Button(onClick = { launcher.launch(PickVisualMediaRequest(ImageAndVideo)) }) {
            Text("Choose File")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is AddMediaUiState.Idle -> {}
            is AddMediaUiState.Loading -> CircularProgressIndicator()
            is AddMediaUiState.Success -> {
                Text("Added: ${state.item.displayName}")
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
