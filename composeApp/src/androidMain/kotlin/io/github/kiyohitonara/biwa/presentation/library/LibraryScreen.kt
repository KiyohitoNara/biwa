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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.github.kiyohitonara.biwa.domain.model.MediaFilter
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.domain.model.Tag
import io.github.kiyohitonara.biwa.presentation.tagmanagement.TagManagementUiState
import io.github.kiyohitonara.biwa.presentation.tagmanagement.TagManagementViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Screen that displays all media items in the library as a grid. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAddMedia: () -> Unit,
    onOpenVideoPlayer: (String) -> Unit,
    onOpenPhotoViewer: (String) -> Unit,
    onManageTags: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var contextItem by remember { mutableStateOf<MediaItem?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }

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
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(onClick = onManageTags) {
                        Text(
                            text = "\uD83C\uDFF7",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    IconButton(onClick = { showSortSheet = true }) {
                        Text(
                            text = "\u21C5",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Text(
                            text = "\u2699",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMedia) {
                Text("+")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val successState = uiState as? LibraryUiState.Success
            FilterChipsRow(
                currentFilter = successState?.mediaFilter ?: MediaFilter.ALL,
                onFilterChanged = viewModel::setMediaFilter,
                availableTags = successState?.availableTags ?: emptyList(),
                activeTagIds = successState?.activeTagIds ?: emptySet(),
                onTagToggled = viewModel::toggleTag,
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is LibraryUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is LibraryUiState.Success -> {
                        if (state.items.isEmpty()) {
                            EmptyLibrary(modifier = Modifier.align(Alignment.Center))
                        } else {
                            val filtersActive = state.mediaFilter != MediaFilter.ALL ||
                                state.activeTagIds.size > 1
                            MediaGrid(
                                items = state.items,
                                sortOrder = state.sortOrder,
                                filtersActive = filtersActive,
                                onTap = { viewModel.openMedia(it.id) },
                                onLongPress = { contextItem = it },
                                onReorder = viewModel::reorderMedia,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        SortSelectionSheet(
            currentSort = (uiState as? LibraryUiState.Success)?.sortOrder ?: SortOrder.ADDED_AT_DESC,
            onSortSelected = { viewModel.setSortOrder(it); showSortSheet = false },
            onDismiss = { showSortSheet = false },
        )
    }

    contextItem?.let { item ->
        MediaContextSheet(
            item = item,
            onDelete = {
                viewModel.deleteMedia(item.id)
                contextItem = null
            },
            onDismiss = { contextItem = null },
        )
    }
}

@Composable
private fun FilterChipsRow(
    currentFilter: MediaFilter,
    onFilterChanged: (MediaFilter) -> Unit,
    availableTags: List<Tag>,
    activeTagIds: Set<String>,
    onTagToggled: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == currentFilter,
                onClick = { onFilterChanged(filter) },
                label = { Text(filterLabel(filter)) },
            )
        }

        if (availableTags.isNotEmpty()) {
            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
            )
            availableTags.forEach { tag ->
                FilterChip(
                    selected = tag.id in activeTagIds,
                    onClick = { onTagToggled(tag.id) },
                    label = { Text(tag.name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSelectionSheet(
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSortSelected(order) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(
                    selected = order == currentSort,
                    onClick = { onSortSelected(order) },
                )
                Text(sortLabel(order), style = MaterialTheme.typography.bodyLarge)
            }
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
    val tagVm: TagManagementViewModel = koinViewModel(key = item.id) { parametersOf(item.id) }
    val tagState by tagVm.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            val ready = tagState as? TagManagementUiState.Ready
            if (ready != null && ready.allTags.isNotEmpty()) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ready.allTags.forEach { tag ->
                        FilterChip(
                            selected = ready.mediaTags.any { it.id == tag.id },
                            onClick = { tagVm.toggleTagForMedia(tag.id) },
                            label = { Text(tag.name) },
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            TextButton(
                onClick = onDelete,
                modifier = Modifier
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
    sortOrder: SortOrder,
    filtersActive: Boolean,
    onTap: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    if (sortOrder == SortOrder.MANUAL && !filtersActive) {
        DraggableMediaGrid(items = items, onTap = onTap, onReorder = onReorder)
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
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
            Box(
                modifier = Modifier
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
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    // Mutable map — not snapshot state to avoid recomposition during onGloballyPositioned
    val itemBounds = remember { HashMap<String, Rect>() }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartBounds by remember { mutableStateOf(Rect.Zero) }
    var dropTargetKey by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val isDragged = item.id == draggedKey
                val isDropTarget = item.id == dropTargetKey && !isDragged
                val isDragActive = draggedKey != null

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .onGloballyPositioned { coords ->
                            itemBounds[item.id] = coords.boundsInRoot()
                        }
                        .graphicsLayer {
                            when {
                                isDragged -> {
                                    scaleX = 1.07f
                                    scaleY = 1.07f
                                    shadowElevation = 16f
                                    alpha = 0.85f
                                }
                                isDragActive && !isDropTarget -> alpha = 0.55f
                            }
                        }
                        .pointerInput(item.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ ->
                                    draggedKey = item.id
                                    dropTargetKey = item.id
                                    dragOffset = Offset.Zero
                                    dragStartBounds = itemBounds[item.id] ?: Rect.Zero
                                },
                                onDrag = { _, amount ->
                                    dragOffset += amount
                                    val pointerPos = Offset(
                                        dragStartBounds.center.x + dragOffset.x,
                                        dragStartBounds.center.y + dragOffset.y,
                                    )
                                    dropTargetKey = itemBounds.entries
                                        .minByOrNull { (_, bounds) ->
                                            (pointerPos - bounds.center).getDistance()
                                        }?.key
                                },
                                onDragEnd = {
                                    val fromIdx = items.indexOfFirst { it.id == draggedKey }
                                    val toIdx = items.indexOfFirst { it.id == dropTargetKey }
                                    if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                                        onReorder(fromIdx, toIdx)
                                    }
                                    draggedKey = null
                                    dropTargetKey = null
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    draggedKey = null
                                    dropTargetKey = null
                                    dragOffset = Offset.Zero
                                },
                            )
                        }
                        .clickable { if (draggedKey == null) onTap(item) },
                ) {
                    ThumbnailImage(item)
                    MediaTypeBadge(item)
                }
            }
        }
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
private fun MediaTypeBadge(item: MediaItem, modifier: Modifier = Modifier) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (item.mediaType) {
            MediaType.VIDEO -> VideoBadge(
                durationMs = item.durationMs,
                modifier = Modifier.align(Alignment.BottomStart),
            )
            MediaType.GIF -> GifBadge(
                modifier = Modifier.align(Alignment.BottomStart),
            )
            MediaType.PHOTO -> Unit
        }
    }
}

@Composable
private fun VideoBadge(durationMs: Long?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "\u25B6",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
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
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

private fun filterLabel(filter: MediaFilter) = when (filter) {
    MediaFilter.ALL -> "All"
    MediaFilter.VIDEO -> "Videos"
    MediaFilter.GIF -> "GIFs"
    MediaFilter.PHOTO -> "Photos"
}

private fun sortLabel(order: SortOrder) = when (order) {
    SortOrder.ADDED_AT_DESC -> "Added (newest first)"
    SortOrder.ADDED_AT_ASC -> "Added (oldest first)"
    SortOrder.FILE_NAME -> "File name"
    SortOrder.LAST_VIEWED_AT -> "Last viewed"
    SortOrder.FILE_SIZE -> "File size"
    SortOrder.MANUAL -> "Manual"
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
