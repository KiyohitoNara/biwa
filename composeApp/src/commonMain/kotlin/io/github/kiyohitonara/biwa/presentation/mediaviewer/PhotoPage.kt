package io.github.kiyohitonara.biwa.presentation.mediaviewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.exp
import kotlin.math.min

private const val MAX_ZOOM = 8f
private const val DOUBLE_TAP_ZOOM = 2f

// Pixels of vertical drag that produce an e-fold (~2.72x) change in scale.
// Smaller value = more sensitive. 200px ≈ Google Maps' quick zoom feel.
private const val QUICK_ZOOM_SENSITIVITY_PX = 200f

/** Single zoomable photo page used inside [MediaViewerScreen]. */
@Composable
fun PhotoPage(
    filePath: String,
    rotationDegrees: Int = 0,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit = {},
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageIntrinsicSize by remember { mutableStateOf(IntSize.Zero) }

    // Lower bound for [scale]:
    //   1.0 when the natural image is larger than the viewport — fit-to-screen stays the floor.
    //   <1.0 when the natural image is smaller — let the user pinch down to 1:1 pixels
    //   instead of being locked at an upscaled, blurry fit-to-screen.
    val minScale by remember(imageIntrinsicSize, containerSize) {
        derivedStateOf {
            if (imageIntrinsicSize.width <= 0 ||
                imageIntrinsicSize.height <= 0 ||
                containerSize.width <= 0 ||
                containerSize.height <= 0
            ) {
                1f
            } else {
                val fitFactor =
                    min(
                        containerSize.width.toFloat() / imageIntrinsicSize.width,
                        containerSize.height.toFloat() / imageIntrinsicSize.height,
                    )
                min(1f, 1f / fitFactor)
            }
        }
    }

    fun setZoom(
        newScaleUnclamped: Float,
        anchor: Offset,
    ) {
        val newScale = newScaleUnclamped.coerceIn(minScale, MAX_ZOOM)
        if (newScale <= 1f) {
            scale = newScale
            offset = Offset.Zero
        } else {
            val ratio = newScale / scale.coerceAtLeast(0.0001f)
            val cx = containerSize.width / 2f
            val cy = containerSize.height / 2f
            offset =
                Offset(
                    (anchor.x - cx) * (1f - ratio) + offset.x * ratio,
                    (anchor.y - cy) * (1f - ratio) + offset.y * ratio,
                )
            scale = newScale
        }
        onZoomChanged(scale > 1f)
    }

    val transformableState =
        rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(minScale, MAX_ZOOM)
            scale = newScale
            offset = if (newScale > 1f) offset + panChange else Offset.Zero
            onZoomChanged(newScale > 1f)
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val firstUp =
                            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                waitForUpOrCancellation()
                            } ?: return@awaitEachGesture

                        val secondDown =
                            withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                                awaitFirstDown(requireUnconsumed = false)
                            }
                        if (secondDown == null) {
                            onTap()
                            return@awaitEachGesture
                        }

                        val anchor = secondDown.position
                        var dragStarted = false
                        var lastY = secondDown.position.y

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == secondDown.id } ?: break

                            if (!change.pressed) {
                                if (!dragStarted) {
                                    val target = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
                                    setZoom(target, anchor)
                                }
                                break
                            }

                            if (!dragStarted) {
                                val moved = (change.position - anchor).getDistance()
                                if (moved > viewConfiguration.touchSlop) {
                                    dragStarted = true
                                    lastY = change.position.y
                                    onZoomChanged(true)
                                    change.consume()
                                }
                            }

                            if (dragStarted) {
                                val dy = change.position.y - lastY
                                lastY = change.position.y
                                setZoom(scale * exp(dy / QUICK_ZOOM_SENSITIVITY_PX), anchor)
                                change.consume()
                            }
                        }
                    }
                }.transformable(state = transformableState, lockRotationOnZoomPan = true),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = filePath,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                val image = state.result.image
                imageIntrinsicSize = IntSize(image.width, image.height)
            },
            modifier =
                Modifier
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
}
