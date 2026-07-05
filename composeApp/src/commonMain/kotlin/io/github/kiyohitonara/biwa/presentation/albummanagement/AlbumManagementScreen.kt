package io.github.kiyohitonara.biwa.presentation.albummanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kiyohitonara.biwa.domain.model.Album
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Screen for creating, renaming, and deleting albums globally. */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
@Composable
fun AlbumManagementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumManagementViewModel = koinViewModel { parametersOf(null) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Album?>(null) }
    var deleteTarget by remember { mutableStateOf<Album?>(null) }

    LaunchedEffect(viewModel.error) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { AlbumTopBar(onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add album")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        AlbumList(
            uiState = uiState,
            onRename = { renameTarget = it },
            onDelete = { deleteTarget = it },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        )
    }

    if (showCreateDialog) {
        AlbumNameDialog(
            title = "New album",
            initialName = "",
            onConfirm = { name ->
                viewModel.createAlbum(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    AlbumEditDialogs(
        renameTarget = renameTarget,
        deleteTarget = deleteTarget,
        viewModel = viewModel,
        onDismissRename = { renameTarget = null },
        onDismissDelete = { deleteTarget = null },
    )
}

/** Album screen top app bar with a back navigation icon. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Albums") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
    )
}

/** Renders the empty state or the scrollable list of albums. */
@Composable
private fun AlbumList(
    uiState: AlbumManagementUiState,
    onRename: (Album) -> Unit,
    onDelete: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = uiState as? AlbumManagementUiState.Ready ?: return
    if (ready.allAlbums.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            EmptyAlbums()
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(ready.allAlbums, key = { it.id }) { album ->
                AlbumRow(
                    album = album,
                    onRename = { onRename(album) },
                    onDelete = { onDelete(album) },
                )
                HorizontalDivider()
            }
        }
    }
}

/** The rename and delete confirmation dialogs, shown when their target album is set. */
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
@Composable
private fun AlbumEditDialogs(
    renameTarget: Album?,
    deleteTarget: Album?,
    viewModel: AlbumManagementViewModel,
    onDismissRename: () -> Unit,
    onDismissDelete: () -> Unit,
) {
    renameTarget?.let { album ->
        AlbumNameDialog(
            title = "Rename album",
            initialName = album.name,
            onConfirm = { name ->
                viewModel.renameAlbum(album.id, name)
                onDismissRename()
            },
            onDismiss = onDismissRename,
        )
    }

    deleteTarget?.let { album ->
        DeleteAlbumDialog(
            albumName = album.name,
            onConfirm = {
                viewModel.deleteAlbum(album.id)
                onDismissDelete()
            },
            onDismiss = onDismissDelete,
        )
    }
}

@Composable
private fun AlbumRow(
    album: Album,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Row {
            IconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Rename",
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EmptyAlbums() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text(
            text = "No albums yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Tap + to create your first album",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlbumNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(onDone = {
                        if (name.isNotBlank()) onConfirm(name)
                    }),
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DeleteAlbumDialog(
    albumName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete album") },
        text = {
            Text("Delete \"$albumName\"? Media items in this album will not be deleted.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
