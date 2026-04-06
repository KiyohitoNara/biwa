package io.github.kiyohitonara.biwa.presentation.videoplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.github.kiyohitonara.biwa.domain.model.AbPoint
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val PLAYBACK_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private const val FRAME_STEP_MS = 33L
private const val CONTROLS_HIDE_DELAY_MS = 5_000L
private val BrandOrange = Color(0xFFF4A44A)

/** Full-screen video player screen backed by ExoPlayer. */
@Composable
fun VideoPlayerScreen(
    mediaId: String,
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = koinViewModel(parameters = { parametersOf(mediaId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.abRepeatError) {
        viewModel.abRepeatError.collect {
            snackbarHostState.showSnackbar("B point must be after A point")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (val state = uiState) {
            is VideoPlayerUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }
            is VideoPlayerUiState.Error -> {
                Text(
                    text = state.message,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is VideoPlayerUiState.Ready -> {
                VideoPlayerContent(
                    state = state,
                    viewModel = viewModel,
                    onBack = onBack,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlayerContent(
    state: VideoPlayerUiState.Ready,
    viewModel: VideoPlayerViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showSpeedSheet by remember { mutableStateOf(false) }

    val player = remember { ExoPlayer.Builder(context).build() }

    // Prepare player with media and restore saved state
    LaunchedEffect(state.mediaItem.filePath) {
        player.setMediaItem(Media3MediaItem.fromUri(state.mediaItem.filePath))
        player.prepare()
        player.seekTo(state.positionMs)
        player.playbackParameters = PlaybackParameters(state.playbackSpeed)
    }

    // Mirror player events into ViewModel
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.updatePlayingState(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    viewModel.updateDuration(player.duration.coerceAtLeast(0L))
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            viewModel.saveCurrentState()
            player.release()
        }
    }

    // Position polling + AB repeat enforcement during playback
    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            val currentPos = player.currentPosition
            viewModel.updatePosition(currentPos)

            val currentState = viewModel.uiState.value as? VideoPlayerUiState.Ready
            val abStart = currentState?.abStartMs
            val abEnd = currentState?.abEndMs
            if (abStart != null && abEnd != null && currentPos >= abEnd) {
                player.seekTo(abStart)
            }

            delay(100)
        }
    }

    // Auto-hide controls 5s after they become visible during playback
    LaunchedEffect(state.isControlsVisible) {
        if (state.isControlsVisible) {
            delay(CONTROLS_HIDE_DELAY_MS)
            val current = viewModel.uiState.value as? VideoPlayerUiState.Ready
            if (current != null && current.isPlaying && current.isControlsVisible) {
                viewModel.toggleControls()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { viewModel.toggleControls() },
            ),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = state.isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            PlayerControls(
                state = state,
                onBack = onBack,
                onTogglePlay = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                onSeekTo = { positionMs ->
                    player.seekTo(positionMs)
                    viewModel.updatePosition(positionMs)
                },
                onStepFrame = { forward ->
                    val step = if (forward) FRAME_STEP_MS else -FRAME_STEP_MS
                    val newPos = (player.currentPosition + step).coerceIn(0L, player.duration.coerceAtLeast(0L))
                    player.pause()
                    player.seekTo(newPos)
                    viewModel.updatePosition(newPos)
                },
                onShowSpeedSheet = { showSpeedSheet = true },
                onSetAbPoint = { point, positionMs -> viewModel.setAbPoint(point, positionMs) },
                onResetAbRepeat = { viewModel.resetAbRepeat() },
            )
        }
    }

    if (showSpeedSheet) {
        SpeedSelectionSheet(
            currentSpeed = state.playbackSpeed,
            onSpeedSelected = { speed ->
                player.playbackParameters = PlaybackParameters(speed)
                viewModel.setPlaybackSpeed(speed)
                showSpeedSheet = false
            },
            onDismiss = { showSpeedSheet = false },
        )
    }
}

@Composable
private fun PlayerControls(
    state: VideoPlayerUiState.Ready,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onStepFrame: (forward: Boolean) -> Unit,
    onShowSpeedSheet: () -> Unit,
    onSetAbPoint: (AbPoint, Long) -> Unit,
    onResetAbRepeat: () -> Unit,
) {
    val scrim = Color.Black.copy(alpha = 0.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scrim)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Text(
                    text = "\u2190",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(
                text = state.mediaItem.displayName,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(scrim)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.BottomCenter),
        ) {
            SeekBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                abStartMs = state.abStartMs,
                abEndMs = state.abEndMs,
                onSeek = onSeekTo,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Play controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Speed button
                IconButton(onClick = onShowSpeedSheet) {
                    Text(
                        text = "${state.playbackSpeed.let { if (it == it.toLong().toFloat()) it.toLong() else it }}x",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Frame step + play/pause
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = { onStepFrame(false) }) {
                        Text("\u23EE", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Text(
                            text = if (state.isPlaying) "\u23F8" else "\u25B6",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    IconButton(onClick = { onStepFrame(true) }) {
                        Text("\u23ED", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                }

                // AB repeat controls
                AbRepeatControls(
                    positionMs = state.positionMs,
                    abStartMs = state.abStartMs,
                    abEndMs = state.abEndMs,
                    onSetAbPoint = onSetAbPoint,
                    onResetAbRepeat = onResetAbRepeat,
                )
            }
        }
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    abStartMs: Long?,
    abEndMs: Long?,
    onSeek: (Long) -> Unit,
) {
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val abStartFraction = if (abStartMs != null && durationMs > 0) abStartMs.toFloat() / durationMs else null
    val abEndFraction = if (abEndMs != null && durationMs > 0) abEndMs.toFloat() / durationMs else null

    Column {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // AB highlight drawn behind the slider track
            if (abStartFraction != null && abEndFraction != null) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(horizontal = 10.dp)
                        .drawBehind {
                            val startX = abStartFraction * size.width
                            val endX = abEndFraction * size.width
                            drawRoundRect(
                                color = BrandOrange,
                                topLeft = Offset(startX.coerceAtLeast(0f), 0f),
                                size = Size((endX - startX).coerceAtLeast(0f), size.height),
                                cornerRadius = CornerRadius(2.dp.toPx()),
                            )
                        },
                )
            }

            Slider(
                value = fraction,
                onValueChange = { f -> onSeek((f * durationMs).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(positionMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = formatDuration(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun AbRepeatControls(
    positionMs: Long,
    abStartMs: Long?,
    abEndMs: Long?,
    onSetAbPoint: (AbPoint, Long) -> Unit,
    onResetAbRepeat: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onSetAbPoint(AbPoint.A, positionMs) }) {
            Text(
                text = "A",
                color = if (abStartMs != null) BrandOrange else Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        IconButton(onClick = { onSetAbPoint(AbPoint.B, positionMs) }) {
            Text(
                text = "B",
                color = if (abEndMs != null) BrandOrange else Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        if (abStartMs != null || abEndMs != null) {
            IconButton(onClick = onResetAbRepeat) {
                Text(
                    text = "\u2715",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSelectionSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Text(
            text = "Playback speed",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PLAYBACK_SPEEDS.forEach { speed ->
                FilterChip(
                    selected = speed == currentSpeed,
                    onClick = { onSpeedSelected(speed) },
                    label = {
                        val label = if (speed == speed.toLong().toFloat()) "${speed.toLong()}x" else "${speed}x"
                        Text(label)
                    },
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
