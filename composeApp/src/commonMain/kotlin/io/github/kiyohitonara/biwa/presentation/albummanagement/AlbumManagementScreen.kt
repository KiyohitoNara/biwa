package io.github.kiyohitonara.biwa.presentation.albummanagement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

/** Screen for creating, renaming, moving, and deleting albums, with drill-down through nested albums. */
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
    var moveTarget by remember { mutableStateOf<Album?>(null) }
    var deleteTarget by remember { mutableStateOf<Album?>(null) }

    LaunchedEffect(viewModel.error) {
        viewModel.error.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val ready = uiState as? AlbumManagementUiState.Ready
    Scaffold(
        modifier = modifier,
        topBar = {
            AlbumTopBar(
                title = ready?.breadcrumb?.lastOrNull()?.name ?: "Albums",
                onBack = { if (viewModel.navigateUp()) Unit else onBack() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add album")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        AlbumList(
            ready = ready,
            onOpen = { viewModel.enterAlbum(it.id) },
            onRename = { renameTarget = it },
            onMove = { moveTarget = it },
            onDelete = { deleteTarget = it },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        )
    }

    AlbumManagementDialogs(
        showCreateDialog = showCreateDialog,
        renameTarget = renameTarget,
        moveTarget = moveTarget,
        deleteTarget = deleteTarget,
        viewModel = viewModel,
        onDismissCreate = { showCreateDialog = false },
        onDismissRename = { renameTarget = null },
        onDismissMove = { moveTarget = null },
        onDismissDelete = { deleteTarget = null },
    )
}

/** Album screen top app bar with a back navigation icon and the current album name as title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumTopBar(
    title: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
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

/** Renders the empty state or the scrollable list of albums at the current level. */
@Composable
private fun AlbumList(
    ready: AlbumManagementUiState.Ready?,
    onOpen: (Album) -> Unit,
    onRename: (Album) -> Unit,
    onMove: (Album) -> Unit,
    onDelete: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ready == null) return
    if (ready.currentAlbums.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            EmptyAlbums(isRoot = ready.currentParentId == null)
        }
    } else {
        val parentIds = remember(ready.allAlbums) { ready.allAlbums.mapNotNull { it.parentId }.toSet() }
        LazyColumn(modifier = modifier) {
            items(ready.currentAlbums, key = { it.id }) { album ->
                AlbumRow(
                    album = album,
                    hasChildren = album.id in parentIds,
                    onOpen = { onOpen(album) },
                    onRename = { onRename(album) },
                    onMove = { onMove(album) },
                    onDelete = { onDelete(album) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AlbumRow(
    album: Album,
    hasChildren: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (hasChildren) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AlbumRowMenu(onRename = onRename, onMove = onMove, onDelete = onDelete)
    }
}

/** The overflow menu for a single album row: rename, move, or delete. */
@Composable
private fun AlbumRowMenu(
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    expanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Move to…") },
                onClick = {
                    expanded = false
                    onMove()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun EmptyAlbums(isRoot: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text(
            text = if (isRoot) "No albums yet" else "No sub-albums yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Tap + to create ${if (isRoot) "your first album" else "a sub-album"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Hosts the create, rename, move, and delete dialogs, each shown when its trigger state is set. */
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
@Composable
private fun AlbumManagementDialogs(
    showCreateDialog: Boolean,
    renameTarget: Album?,
    moveTarget: Album?,
    deleteTarget: Album?,
    viewModel: AlbumManagementViewModel,
    onDismissCreate: () -> Unit,
    onDismissRename: () -> Unit,
    onDismissMove: () -> Unit,
    onDismissDelete: () -> Unit,
) {
    if (showCreateDialog) {
        AlbumNameDialog(
            title = "New album",
            initialName = "",
            onConfirm = { name ->
                viewModel.createAlbum(name)
                onDismissCreate()
            },
            onDismiss = onDismissCreate,
        )
    }

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

    moveTarget?.let { album ->
        MoveAlbumDialog(
            album = album,
            targets = viewModel.validMoveTargets(album.id),
            onMove = { targetId ->
                viewModel.moveAlbum(album.id, targetId)
                onDismissMove()
            },
            onDismiss = onDismissMove,
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** Lets the user reparent [album] under one of [targets], or move it to the top level. */
@Composable
private fun MoveAlbumDialog(
    album: Album,
    targets: List<Album>,
    onMove: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move \"${album.name}\"") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                item {
                    MoveTargetRow(name = "Top level", enabled = album.parentId != null) { onMove(null) }
                    HorizontalDivider()
                }
                items(targets, key = { it.id }) { target ->
                    MoveTargetRow(name = target.name, enabled = album.parentId != target.id) { onMove(target.id) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun MoveTargetRow(
    name: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 12.dp),
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
            Text("Delete \"$albumName\"? Its sub-albums are also deleted. Media items are not deleted.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
