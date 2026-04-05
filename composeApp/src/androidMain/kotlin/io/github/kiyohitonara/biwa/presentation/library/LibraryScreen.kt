package io.github.kiyohitonara.biwa.presentation.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import org.koin.compose.viewmodel.koinViewModel

/** Screen that displays all media items in the library as a grid. */
@Composable
fun LibraryScreen(
    onAddMedia: () -> Unit,
    onOpenVideoPlayer: (String) -> Unit,
    onOpenPhotoViewer: (String) -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteItem by remember { mutableStateOf<MediaItem?>(null) }

    LaunchedEffect(viewModel.deleteError) {
        viewModel.deleteError.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(viewModel.navEffect) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is LibraryNavEffect.OpenVideoPlayer -> onOpenVideoPlayer(effect.id)
                is LibraryNavEffect.OpenPhotoViewer -> onOpenPhotoViewer(effect.id)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMedia) {
                Text("+")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LibraryUiState.Success -> {
                    if (state.items.isEmpty()) {
                        EmptyLibrary(modifier = Modifier.align(Alignment.Center))
                    } else {
                        MediaGrid(
                            items = state.items,
                            onTap = { viewModel.openMedia(it.id) },
                            onLongPress = { pendingDeleteItem = it },
                        )
                    }
                }
            }
        }
    }

    pendingDeleteItem?.let { item ->
        DeleteConfirmationDialog(
            itemName = item.displayName,
            onConfirm = {
                viewModel.deleteMedia(item.id)
                pendingDeleteItem = null
            },
            onDismiss = { pendingDeleteItem = null },
        )
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "No media yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Tap + to add your first file",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MediaGrid(
    items: List<MediaItem>,
    onTap: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { item ->
            MediaThumbnail(
                item = item,
                onTap = { onTap(item) },
                onLongPress = { onLongPress(item) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaThumbnail(item: MediaItem, onTap: () -> Unit, onLongPress: () -> Unit) {
    val imageModel = item.thumbnailPath ?: item.filePath
    AsyncImage(
        model = imageModel,
        contentDescription = item.displayName,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            ),
    )
}

@Composable
private fun DeleteConfirmationDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete media") },
        text = { Text("Delete \"$itemName\"? This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
