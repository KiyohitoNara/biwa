@file:Suppress("TooManyFunctions") // Screen file aggregates many small private composables.

package io.github.kiyohitonara.biwa.presentation.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.kiyohitonara.biwa.domain.model.Album
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.presentation.albummanagement.AlbumManagementUiState
import io.github.kiyohitonara.biwa.presentation.albummanagement.AlbumManagementViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val GRID_COLUMN_COUNT = 3

// Visual treatment applied while reordering items.
private const val DRAG_SCALE = 1.07f
private const val DRAG_SHADOW_ELEVATION = 16f
private const val DRAG_ALPHA = 0.85f
private const val DRAG_DIMMED_ALPHA = 0.55f

/** Screen that displays all media items in the library as a grid. */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
@Composable
fun LibraryScreen(
    onOpenMediaViewer: (String) -> Unit,
    onManageAlbums: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAdding by viewModel.isAdding.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var contextItem by remember { mutableStateOf<MediaItem?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var pickerActive by remember { mutableStateOf(false) }

    LibraryEffects(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onOpenMediaViewer = onOpenMediaViewer,
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            LibraryTopBar(
                sortEnabled = (uiState as? LibraryUiState.Success)?.let { it.activeAlbumIds.size <= 1 } ?: true,
                onManageAlbums = onManageAlbums,
                onSort = { showSortSheet = true },
                onOpenSettings = onOpenSettings,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickerActive = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add media")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LibraryContent(
            uiState = uiState,
            isAdding = isAdding,
            viewModel = viewModel,
            onLongPress = { contextItem = it },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        )
    }

    MediaPicker(
        active = pickerActive,
        onPick = { uris ->
            pickerActive = false
            viewModel.addMedia(uris)
        },
        onCancel = { pickerActive = false },
    )

    LibrarySheets(
        showSortSheet = showSortSheet,
        contextItem = contextItem,
        viewModel = viewModel,
        onDismissSort = { showSortSheet = false },
        onDismissContext = { contextItem = null },
    )
}

/** The sort-order and per-item context bottom sheets shown over the library. */
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
@Composable
private fun LibrarySheets(
    showSortSheet: Boolean,
    contextItem: MediaItem?,
    viewModel: LibraryViewModel,
    onDismissSort: () -> Unit,
    onDismissContext: () -> Unit,
) {
    if (showSortSheet) {
        SortSelectionSheet(
            onSortSelect = {
                viewModel.setSortOrder(it)
                onDismissSort()
            },
            onDismiss = onDismissSort,
        )
    }

    contextItem?.let { item ->
        MediaContextSheet(
            item = item,
            onDelete = {
                viewModel.deleteMedia(item.id)
                onDismissContext()
            },
            onDismiss = onDismissContext,
        )
    }
}

/** Collects the library's one-shot snackbar and navigation effects. */
@Composable
private fun LibraryEffects(
    viewModel: LibraryViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenMediaViewer: (String) -> Unit,
) {
    LaunchedEffect(viewModel.deleteError) {
        viewModel.deleteError.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    LaunchedEffect(viewModel.addMediaError) {
        viewModel.addMediaError.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    val currentOnOpenMediaViewer by rememberUpdatedState(onOpenMediaViewer)
    LaunchedEffect(viewModel.navEffect) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                is LibraryNavEffect.OpenMediaViewer -> currentOnOpenMediaViewer(effect.id)
            }
        }
    }
}

/** Library top app bar with manage-albums, sort, and overflow (settings) actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    sortEnabled: Boolean,
    onManageAlbums: () -> Unit,
    onSort: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("Library") },
        actions = {
            IconButton(onClick = onManageAlbums) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = "Manage albums",
                )
            }
            IconButton(onClick = onSort, enabled = sortEnabled) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = "Sort",
                )
            }
            IconButton(onClick = { showOverflowMenu = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                )
            }
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        showOverflowMenu = false
                        onOpenSettings()
                    },
                )
            }
        },
    )
}

/** Scaffold body: optional progress bar, album filter row, and the media grid / empty state. */
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    isAdding: Boolean,
    viewModel: LibraryViewModel,
    onLongPress: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (isAdding) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        val successState = uiState as? LibraryUiState.Success
        AlbumFilterChipsRow(
            availableAlbums = successState?.availableAlbums ?: emptyList(),
            activeAlbumIds = successState?.activeAlbumIds ?: emptySet(),
            onAlbumToggle = viewModel::toggleAlbum,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is LibraryUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is LibraryUiState.Success ->
                    if (uiState.items.isEmpty()) {
                        EmptyLibrary(modifier = Modifier.align(Alignment.Center))
                    } else {
                        MediaGrid(
                            items = uiState.items,
                            draggable = uiState.activeAlbumIds.size <= 1,
                            onTap = { viewModel.openMedia(it.id) },
                            onLongPress = onLongPress,
                            onReorder = viewModel::reorderMedia,
                        )
                    }
            }
        }
    }
}

@Composable
private fun AlbumFilterChipsRow(
    availableAlbums: List<Album>,
    activeAlbumIds: Set<String>,
    onAlbumToggle: (String) -> Unit,
) {
    if (availableAlbums.isEmpty()) return
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        availableAlbums.forEach { album ->
            FilterChip(
                selected = album.id in activeAlbumIds,
                onClick = { onAlbumToggle(album.id) },
                label = { Text(album.name) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSelectionSheet(
    onSortSelect: (SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = "Sort by",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        SortOrder.entries.forEach { order ->
            Text(
                text = sortLabel(order),
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSortSelect(order) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MediaContextSheet(
    item: MediaItem,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val albumVm: AlbumManagementViewModel = koinViewModel(key = item.id) { parametersOf(item.id) }
    val albumState by albumVm.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            val ready = albumState as? AlbumManagementUiState.Ready
            if (ready != null && ready.allAlbums.isNotEmpty()) {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                FlowRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ready.allAlbums.forEach { album ->
                        FilterChip(
                            selected = ready.mediaAlbums.any { it.id == album.id },
                            onClick = { albumVm.toggleMediaInAlbum(album.id) },
                            label = { Text(album.name) },
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            TextButton(
                onClick = onDelete,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.PermMedia,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "No media yet",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Tap + to add your first file",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MediaGrid(
    items: List<MediaItem>,
    draggable: Boolean,
    onTap: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    if (draggable) {
        DraggableMediaGrid(
            items = items,
            onTap = onTap,
            onLongPress = onLongPress,
            onReorder = onReorder,
        )
    } else {
        StaticMediaGrid(items = items, onTap = onTap, onLongPress = onLongPress)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StaticMediaGrid(
    items: List<MediaItem>,
    onTap: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMN_COUNT),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
            Box(
                modifier =
                    Modifier
                        .aspectRatio(1f)
                        .combinedClickable(
                            onClick = { onTap(item) },
                            onLongClick = { onLongPress(item) },
                        ),
            ) {
                ThumbnailImage(item)
                MediaTypeBadge(item)
            }
        }
    }
}

@Composable
private fun DraggableMediaGrid(
    items: List<MediaItem>,
    onTap: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    val reorderState = remember { ReorderState() }
    val actions = remember(onTap, onLongPress, onReorder) { GridItemActions(onTap, onLongPress, onReorder) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                DraggableGridItem(
                    item = item,
                    items = items,
                    state = reorderState,
                    actions = actions,
                    modifier = Modifier.aspectRatio(1f),
                )
            }
        }
    }
}

/** Tap / long-press / reorder callbacks for a draggable grid item. */
private class GridItemActions(
    val onTap: (MediaItem) -> Unit,
    val onLongPress: (MediaItem) -> Unit,
    val onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
)

/**
 * Drag-and-drop reorder state shared across the grid's items.
 *
 * [draggedKey] / [dropTargetKey] are snapshot state because items read them while composing;
 * [itemBounds] and the drag deltas are plain fields since they are only touched inside gestures.
 */
@Stable
private class ReorderState {
    val itemBounds = HashMap<String, Rect>()

    var draggedKey by mutableStateOf<String?>(null)
        private set
    var dropTargetKey by mutableStateOf<String?>(null)
        private set

    private var dragOffset = Offset.Zero
    private var dragStartBounds = Rect.Zero

    val isDragging: Boolean get() = draggedKey != null

    fun isDragged(id: String): Boolean = id == draggedKey

    fun isDropTarget(id: String): Boolean = id == dropTargetKey && id != draggedKey

    fun start(id: String) {
        draggedKey = id
        dropTargetKey = id
        dragOffset = Offset.Zero
        dragStartBounds = itemBounds[id] ?: Rect.Zero
    }

    fun drag(amount: Offset) {
        dragOffset += amount
        val pointer = Offset(dragStartBounds.center.x + dragOffset.x, dragStartBounds.center.y + dragOffset.y)
        dropTargetKey = itemBounds.entries.minByOrNull { (_, bounds) -> (pointer - bounds.center).getDistance() }?.key
    }

    fun reset() {
        draggedKey = null
        dropTargetKey = null
        dragOffset = Offset.Zero
    }

    fun dragDistance(): Float = dragOffset.getDistance()
}

/** A single reorderable grid cell with drag-to-move, long-press, and tap handling. */
@Composable
private fun DraggableGridItem(
    item: MediaItem,
    items: List<MediaItem>,
    state: ReorderState,
    actions: GridItemActions,
    modifier: Modifier = Modifier,
) {
    val isDragged = state.isDragged(item.id)
    val isDropTarget = state.isDropTarget(item.id)
    val isDragActive = state.isDragging

    Box(
        modifier =
            modifier
                .onGloballyPositioned { coords -> state.itemBounds[item.id] = coords.boundsInRoot() }
                .graphicsLayer {
                    when {
                        isDragged -> {
                            scaleX = DRAG_SCALE
                            scaleY = DRAG_SCALE
                            shadowElevation = DRAG_SHADOW_ELEVATION
                            alpha = DRAG_ALPHA
                        }
                        isDragActive && !isDropTarget -> alpha = DRAG_DIMMED_ALPHA
                    }
                }.pointerInput(item.id) {
                    val slop = viewConfiguration.touchSlop
                    detectDragGesturesAfterLongPress(
                        onDragStart = { state.start(item.id) },
                        onDrag = { _, amount -> state.drag(amount) },
                        onDragEnd = {
                            val draggedItemId = state.draggedKey
                            val fromIdx = items.indexOfFirst { it.id == draggedItemId }
                            val toIdx = items.indexOfFirst { it.id == state.dropTargetKey }
                            val moved = state.dragDistance() > slop
                            if (!moved && draggedItemId != null) {
                                items.firstOrNull { it.id == draggedItemId }?.let(actions.onLongPress)
                            } else if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                                actions.onReorder(fromIdx, toIdx)
                            }
                            state.reset()
                        },
                        onDragCancel = { state.reset() },
                    )
                }.clickable { if (!state.isDragging) actions.onTap(item) },
    ) {
        ThumbnailImage(item)
        MediaTypeBadge(item)
    }
}

@Composable
private fun ThumbnailImage(item: MediaItem) {
    AsyncImage(
        model = item.thumbnailPath ?: item.filePath,
        contentDescription = item.displayName,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun MediaTypeBadge(
    item: MediaItem,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (item.mediaType) {
            MediaType.VIDEO ->
                VideoBadge(
                    durationMs = item.durationMs,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            MediaType.GIF ->
                GifBadge(
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            MediaType.PHOTO -> Unit
        }
    }
}

@Composable
private fun VideoBadge(
    durationMs: Long?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        if (durationMs != null) {
            Text(
                text = formatDuration(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun GifBadge(modifier: Modifier = Modifier) {
    Text(
        text = "GIF",
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

private fun sortLabel(order: SortOrder) =
    when (order) {
        SortOrder.ADDED_AT_DESC -> "Added (newest first)"
        SortOrder.ADDED_AT_ASC -> "Added (oldest first)"
        SortOrder.FILE_NAME -> "File name"
        SortOrder.LAST_VIEWED_AT -> "Last viewed"
        SortOrder.FILE_SIZE -> "File size"
    }

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
