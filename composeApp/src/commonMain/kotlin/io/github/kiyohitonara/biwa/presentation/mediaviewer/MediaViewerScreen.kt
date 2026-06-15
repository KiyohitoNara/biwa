package io.github.kiyohitonara.biwa.presentation.mediaviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import io.github.kiyohitonara.biwa.domain.model.MediaType
import io.github.kiyohitonara.biwa.presentation.tagmanagement.TagManagementUiState
import io.github.kiyohitonara.biwa.presentation.tagmanagement.TagManagementViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Unified full-screen media viewer that swipes across photos, videos and GIFs.
 *
 * Photos are shown via [PhotoPage] (pinch-to-zoom). Videos / GIFs are shown
 * via the platform-specific [VideoPage], which owns its own player. The top
 * toolbar is shared and exposes a Delete action.
 */
@Composable
fun MediaViewerScreen(
    mediaId: String,
    onBack: () -> Unit,
    viewModel: MediaViewerViewModel = koinViewModel(parameters = { parametersOf(mediaId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.navigateBack) {
        viewModel.navigateBack.collect { onBack() }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.saveCurrentState() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (val state = uiState) {
            is MediaViewerUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }
            is MediaViewerUiState.Error -> {
                Text(
                    text = state.message,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is MediaViewerUiState.Ready -> {
                MediaViewerContent(
                    state = state,
                    viewModel = viewModel,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun MediaViewerContent(
    state: MediaViewerUiState.Ready,
    viewModel: MediaViewerViewModel,
    onBack: () -> Unit,
) {
    var isZoomed by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = state.currentIndex,
        pageCount = { state.items.size },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            isZoomed = false
            viewModel.onMediaChanged(page)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            key = { index -> state.items[index].id },
            userScrollEnabled = !isZoomed,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = state.items[page]
            val isActive = page == pagerState.currentPage
            when (item.mediaType) {
                MediaType.PHOTO -> PhotoPage(
                    filePath = item.filePath,
                    onTap = viewModel::toggleToolbar,
                    onZoomChanged = { zoomed -> isZoomed = zoomed },
                )
                MediaType.VIDEO, MediaType.GIF -> VideoPage(
                    item = item,
                    isActive = isActive,
                    state = state,
                    viewModel = viewModel,
                )
            }
        }

        TopToolbar(
            state = state,
            onBack = onBack,
            onEditTags = { showTagSheet = true },
            onDelete = viewModel::deleteCurrentMedia,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }

    val currentItem = state.items.getOrNull(state.currentIndex)
    if (showTagSheet && currentItem != null) {
        TagAssignmentSheet(
            mediaId = currentItem.id,
            onDismiss = { showTagSheet = false },
        )
    }
}

@Composable
private fun TopToolbar(
    state: MediaViewerUiState.Ready,
    onBack: () -> Unit,
    onEditTags: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = state.isToolbarVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            val currentItem = state.items.getOrNull(state.currentIndex)
            if (currentItem != null) {
                Text(
                    text = currentItem.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
            IconButton(onClick = onEditTags) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = "Edit tags",
                    tint = Color.White,
                )
            }
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White,
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showOverflowMenu = false; onDelete() },
                    )
                }
            }
        }
    }
}

/**
 * Bottom sheet that lets the user toggle each available tag on the current
 * media item. Mirrors the per-item tag editor used by the library's context
 * sheet so users get the same workflow without leaving the viewer.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagAssignmentSheet(
    mediaId: String,
    onDismiss: () -> Unit,
) {
    val tagVm: TagManagementViewModel = koinViewModel(key = mediaId) { parametersOf(mediaId) }
    val tagState by tagVm.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            val ready = tagState as? TagManagementUiState.Ready
            if (ready == null || ready.allTags.isEmpty()) {
                Text(
                    text = "No tags yet. Create one from the library's Tags screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            } else {
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
        }
    }
}

/** Platform-specific page that renders a single video or GIF item. */
@Composable
expect fun VideoPage(
    item: MediaItem,
    isActive: Boolean,
    state: MediaViewerUiState.Ready,
    viewModel: MediaViewerViewModel,
)
