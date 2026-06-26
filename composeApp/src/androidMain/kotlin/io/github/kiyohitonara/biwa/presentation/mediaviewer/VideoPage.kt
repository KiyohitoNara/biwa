@file:Suppress("TooManyFunctions") // Video page is composed of many small private helpers.

package io.github.kiyohitonara.biwa.presentation.mediaviewer

import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.github.kiyohitonara.biwa.R
import io.github.kiyohitonara.biwa.domain.model.AbPoint
import io.github.kiyohitonara.biwa.domain.model.MediaItem
import kotlinx.coroutines.delay
import androidx.media3.common.MediaItem as Media3MediaItem

private val PLAYBACK_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private const val FRAME_STEP_MS = 33L
private const val SEEK_STEP_MS = 10_000L
private const val CONTROLS_HIDE_DELAY_MS = 5_000L
private val BrandOrange = Color(0xFFF4A44A)

private const val MAX_ZOOM = 8f
private const val DOUBLE_TAP_ZOOM = 2f

private const val POSITION_POLL_INTERVAL_MS = 100L

// Single-tap zones: left third seeks back, right third seeks forward, center toggles zoom.
private const val SEEK_TAP_LEFT_FRACTION = 1f / 3f
private const val SEEK_TAP_RIGHT_FRACTION = 2f / 3f

/**
 * Android implementation of a single video / GIF page backed by ExoPlayer.
 *
 * The player is auto-played only when [isActive] is true. When the page is
 * scrolled off-screen the page composable is disposed and the player released.
 */
@OptIn(ExperimentalMaterial3Api::class)
// modifier-missing / ModifierMissing: the signature is fixed by the expect/actual declaration.
@Suppress("ktlint:compose:modifier-missing-check", "ModifierMissing")
@Composable
actual fun VideoPage(
    item: MediaItem,
    isActive: Boolean,
    state: MediaViewerUiState.Ready,
    viewModel: MediaViewerViewModel,
    rotationDegrees: Int,
    onZoomChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val player = remember(item.id) { ExoPlayer.Builder(context).build() }
    var showSpeedSheet by remember { mutableStateOf(false) }

    val zoom = remember(item.id) { ZoomableState(maxScale = MAX_ZOOM) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // The state-derived fields only apply to this video while it is the current item.
    val isCurrent = state.items.getOrNull(state.currentIndex)?.id == item.id

    PlayerStateEffects(
        player = player,
        item = item,
        isActive = isActive,
        isCurrent = isCurrent,
        state = state,
        viewModel = viewModel,
    )
    PlaybackPollingEffect(player = player, state = state, isCurrent = isCurrent, viewModel = viewModel)
    ControlsAutoHideEffect(state = state, isCurrent = isCurrent, viewModel = viewModel)

    ReportZoomChange(isZoomed = zoom.isZoomed, onZoomChange = onZoomChange)

    fun seekBy(deltaMs: Long) {
        val newPos = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(newPos)
        viewModel.updatePosition(newPos)
    }

    val transformableState = rememberTransformableState { z, p, _ -> zoom.pinch(z, p) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .videoViewerGestures(
                    key = item.id,
                    zoom = zoom,
                    containerSize = { containerSize },
                    onSingleTap = {
                        viewModel.toggleToolbar()
                        viewModel.toggleControls()
                    },
                    onSeek = { delta -> seekBy(delta) },
                ).transformable(state = transformableState, lockRotationOnZoomPan = true),
    ) {
        VideoPlayerSurface(
            player = player,
            scale = zoom.scale,
            offset = zoom.offset,
            rotationDegrees = rotationDegrees,
        )

        if (isCurrent) {
            VideoControlsOverlay(
                player = player,
                state = state,
                isZoomed = zoom.isZoomed,
                viewModel = viewModel,
                onShowSpeedSheet = { showSpeedSheet = true },
            )
        }
    }

    VideoSpeedSheet(
        visible = showSpeedSheet,
        player = player,
        state = state,
        viewModel = viewModel,
        onDismiss = { showSpeedSheet = false },
    )
}

/** Wires the shared tap/zoom/seek gesture protocol to this page's zoom state and seeking. */
private fun Modifier.videoViewerGestures(
    key: Any?,
    zoom: ZoomableState,
    containerSize: () -> IntSize,
    onSingleTap: () -> Unit,
    onSeek: (Long) -> Unit,
): Modifier =
    tapZoomSeekGestures(
        key = key,
        onSingleTap = onSingleTap,
        onSecondTap = { anchor ->
            handleSecondTap(
                anchor = anchor,
                containerWidth = containerSize().width,
                onSeekBack = { onSeek(-SEEK_STEP_MS) },
                onSeekForward = { onSeek(SEEK_STEP_MS) },
                onToggleZoom = {
                    val target = if (zoom.isZoomed) 1f else DOUBLE_TAP_ZOOM
                    zoom.zoomTo(target, anchor, containerSize())
                },
            )
        },
        onQuickZoom = { anchor, scaleMultiplier ->
            zoom.zoomTo(zoom.scale * scaleMultiplier, anchor, containerSize())
        },
    )

/** Resolves a confirmed second tap into a seek (screen edges) or a zoom toggle (center). */
private fun handleSecondTap(
    anchor: Offset,
    containerWidth: Int,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onToggleZoom: () -> Unit,
) {
    when {
        containerWidth > 0 && anchor.x < containerWidth * SEEK_TAP_LEFT_FRACTION -> onSeekBack()
        containerWidth > 0 && anchor.x > containerWidth * SEEK_TAP_RIGHT_FRACTION -> onSeekForward()
        else -> onToggleZoom()
    }
}

/** Mirrors the page's zoom state to the host via [onZoomChange]. */
@Composable
private fun ReportZoomChange(
    isZoomed: Boolean,
    onZoomChange: (Boolean) -> Unit,
) {
    val current by rememberUpdatedState(onZoomChange)
    LaunchedEffect(isZoomed) { current(isZoomed) }
}

/** Drives the ExoPlayer lifecycle: media loading, saved position/speed, pause, and player events. */
@Composable
private fun PlayerStateEffects(
    player: ExoPlayer,
    item: MediaItem,
    isActive: Boolean,
    isCurrent: Boolean,
    state: MediaViewerUiState.Ready,
    viewModel: MediaViewerViewModel,
) {
    LaunchedEffect(item.filePath) {
        player.setMediaItem(Media3MediaItem.fromUri(item.filePath))
        player.prepare()
    }

    // Apply saved position / speed once the page becomes the current one.
    LaunchedEffect(isCurrent) {
        if (isCurrent) {
            player.seekTo(state.positionMs)
            player.playbackParameters = PlaybackParameters(state.playbackSpeed)
        }
    }

    // Pause when the page leaves the viewport. Don't auto-play on activation —
    // the user starts playback via the play button.
    LaunchedEffect(isActive) {
        if (!isActive) player.pause()
    }

    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isCurrent) viewModel.updatePlayingState(isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (isCurrent && playbackState == Player.STATE_READY) {
                        viewModel.updateDuration(player.duration.coerceAtLeast(0L))
                    }
                }
            }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
}

/** Polls the player position while playing and enforces the AB-repeat loop (current page only). */
@Composable
private fun PlaybackPollingEffect(
    player: ExoPlayer,
    state: MediaViewerUiState.Ready,
    isCurrent: Boolean,
    viewModel: MediaViewerViewModel,
) {
    LaunchedEffect(state.isPlaying, isCurrent) {
        if (!isCurrent) return@LaunchedEffect
        while (state.isPlaying) {
            val currentPos = player.currentPosition
            viewModel.updatePosition(currentPos)
            val abStart = state.abStartMs
            val abEnd = state.abEndMs
            if (abStart != null && abEnd != null && currentPos >= abEnd) {
                player.seekTo(abStart)
            }
            delay(POSITION_POLL_INTERVAL_MS)
        }
    }
}

/** Auto-hides the playback controls a few seconds after they appear during playback. */
@Composable
private fun ControlsAutoHideEffect(
    state: MediaViewerUiState.Ready,
    isCurrent: Boolean,
    viewModel: MediaViewerViewModel,
) {
    LaunchedEffect(state.isControlsVisible, isCurrent) {
        if (!isCurrent) return@LaunchedEffect
        if (state.isControlsVisible) {
            delay(CONTROLS_HIDE_DELAY_MS)
            if (state.isPlaying && state.isControlsVisible) {
                viewModel.toggleControls()
            }
        }
    }
}

/** The ExoPlayer-backed video surface with pan/zoom/rotation applied via a graphics layer. */
@Composable
private fun VideoPlayerSurface(
    player: ExoPlayer,
    scale: Float,
    offset: Offset,
    rotationDegrees: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.zoomable_player_view, null) as PlayerView).apply {
                this.player = player
            }
        },
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    rotationZ = rotationDegrees.toFloat(),
                ),
    )
}

/** The fade-in playback controls overlay shown over the current video when not zoomed. */
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
@Composable
private fun VideoControlsOverlay(
    player: ExoPlayer,
    state: MediaViewerUiState.Ready,
    isZoomed: Boolean,
    viewModel: MediaViewerViewModel,
    onShowSpeedSheet: () -> Unit,
) {
    AnimatedVisibility(
        visible = state.isControlsVisible && !isZoomed,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        BottomControls(
            state = state,
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
            onShowSpeedSheet = onShowSpeedSheet,
            onSetAbPoint = viewModel::setAbPoint,
            onResetAbRepeat = viewModel::resetAbRepeat,
        )
    }
}

@Composable
private fun BottomControls(
    state: MediaViewerUiState.Ready,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onStepFrame: (forward: Boolean) -> Unit,
    onShowSpeedSheet: () -> Unit,
    onSetAbPoint: (AbPoint, Long) -> Unit,
    onResetAbRepeat: () -> Unit,
) {
    val scrim = Color.Black.copy(alpha = 0.5f)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onShowSpeedSheet) {
                    Text(
                        text = "${state.playbackSpeed.let { if (it == it.toLong().toFloat()) it.toLong() else it }}x",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                TransportButtons(
                    isPlaying = state.isPlaying,
                    onTogglePlay = onTogglePlay,
                    onStepFrame = onStepFrame,
                )

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

/** Step-backward / play-pause / step-forward transport buttons. */
@Composable
private fun TransportButtons(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onStepFrame: (forward: Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = { onStepFrame(false) }) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Step backward",
                tint = Color.White,
            )
        }
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = { onStepFrame(true) }) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Step forward",
                tint = Color.White,
            )
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
            if (abStartFraction != null && abEndFraction != null) {
                Spacer(
                    modifier =
                        Modifier
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
                colors =
                    SliderDefaults.colors(
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
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Reset AB repeat",
                    tint = Color.White,
                )
            }
        }
    }
}

/** Bottom sheet for choosing playback speed; renders nothing when [visible] is false. */
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
@Composable
private fun VideoSpeedSheet(
    visible: Boolean,
    player: ExoPlayer,
    state: MediaViewerUiState.Ready,
    viewModel: MediaViewerViewModel,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    SpeedSelectionSheet(
        currentSpeed = state.playbackSpeed,
        onSpeedSelect = { speed ->
            player.playbackParameters = PlaybackParameters(speed)
            viewModel.setPlaybackSpeed(speed)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSelectionSheet(
    currentSpeed: Float,
    onSpeedSelect: (Float) -> Unit,
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PLAYBACK_SPEEDS.forEach { speed ->
                FilterChip(
                    selected = speed == currentSpeed,
                    onClick = { onSpeedSelect(speed) },
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
