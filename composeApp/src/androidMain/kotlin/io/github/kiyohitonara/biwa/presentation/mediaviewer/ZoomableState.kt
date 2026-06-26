package io.github.kiyohitonara.biwa.presentation.mediaviewer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

private const val ZOOM_PIVOT = 1f

/**
 * Holds the pan/zoom state of a media page and applies anchored zoom and pinch updates.
 *
 * Keeping this in a small state holder lets the page composable stay focused on layout.
 */
@Stable
class ZoomableState(
    private val maxScale: Float,
) {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** Whether the content is currently magnified beyond its resting size. */
    val isZoomed: Boolean get() = scale > ZOOM_PIVOT

    /** Sets the scale to [newScaleUnclamped] around [anchor], keeping that point stationary. */
    fun zoomTo(
        newScaleUnclamped: Float,
        anchor: Offset,
        containerSize: IntSize,
    ) {
        val result =
            zoomAround(
                current = ZoomTransform(scale, offset),
                newScaleUnclamped = newScaleUnclamped,
                anchor = anchor,
                containerSize = containerSize,
                minScale = ZOOM_PIVOT,
                maxScale = maxScale,
            )
        scale = result.scale
        offset = result.offset
    }

    /** Applies a pinch gesture step: multiplies the scale and pans, clamping to bounds. */
    fun pinch(
        zoomChange: Float,
        panChange: Offset,
    ) {
        val newScale = (scale * zoomChange).coerceIn(ZOOM_PIVOT, maxScale)
        scale = newScale
        offset = if (newScale > ZOOM_PIVOT) offset + panChange else Offset.Zero
    }
}
