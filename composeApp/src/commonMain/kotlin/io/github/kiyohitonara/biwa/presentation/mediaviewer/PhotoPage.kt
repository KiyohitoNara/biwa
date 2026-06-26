package io.github.kiyohitonara.biwa.presentation.mediaviewer

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import kotlin.math.min

private const val MAX_ZOOM = 8f
private const val DOUBLE_TAP_ZOOM = 2f

/** Single zoomable photo page used inside [MediaViewerScreen]. */
@Composable
fun PhotoPage(
    filePath: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    rotationDegrees: Int = 0,
    onZoomChange: (Boolean) -> Unit = {},
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageIntrinsicSize by remember { mutableStateOf(IntSize.Zero) }

    val minScale = rememberMinScale(imageIntrinsicSize, containerSize)

    fun setZoom(
        newScaleUnclamped: Float,
        anchor: Offset,
    ) {
        val result =
            zoomAround(
                current = ZoomTransform(scale, offset),
                newScaleUnclamped = newScaleUnclamped,
                anchor = anchor,
                containerSize = containerSize,
                minScale = minScale,
                maxScale = MAX_ZOOM,
            )
        scale = result.scale
        offset = result.offset
        onZoomChange(scale > 1f)
    }

    val transformableState =
        rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(minScale, MAX_ZOOM)
            scale = newScale
            offset = if (newScale > 1f) offset + panChange else Offset.Zero
            onZoomChange(newScale > 1f)
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .tapZoomSeekGestures(
                    key = Unit,
                    onSingleTap = onTap,
                    onSecondTap = { anchor ->
                        val target = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
                        setZoom(target, anchor)
                    },
                    onQuickZoomStart = { onZoomChange(true) },
                    onQuickZoom = { anchor, scaleMultiplier -> setZoom(scale * scaleMultiplier, anchor) },
                ).transformable(state = transformableState, lockRotationOnZoomPan = true),
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

/**
 * Lower bound for the photo's zoom scale.
 *
 * Returns 1.0 when the natural image is larger than the viewport (fit-to-screen stays the floor),
 * or a value < 1.0 when the image is smaller, letting the user pinch down to 1:1 pixels instead of
 * being locked at an upscaled, blurry fit-to-screen.
 */
@Composable
private fun rememberMinScale(
    imageIntrinsicSize: IntSize,
    containerSize: IntSize,
): Float =
    remember(imageIntrinsicSize, containerSize) {
        val hasIntrinsicSize = imageIntrinsicSize.width > 0 && imageIntrinsicSize.height > 0
        val hasContainerSize = containerSize.width > 0 && containerSize.height > 0
        if (hasIntrinsicSize && hasContainerSize) {
            val fitFactor =
                min(
                    containerSize.width.toFloat() / imageIntrinsicSize.width,
                    containerSize.height.toFloat() / imageIntrinsicSize.height,
                )
            min(1f, 1f / fitFactor)
        } else {
            1f
        }
    }
