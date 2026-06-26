package io.github.kiyohitonara.biwa.presentation.mediaviewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.exp

// Pixels of vertical drag that produce an e-fold (~2.72x) change in scale.
// Smaller value = more sensitive. 200px ≈ Google Maps' quick zoom feel.
private const val QUICK_ZOOM_SENSITIVITY_PX = 200f

/** A zoom state: the clamped [scale] and the recentered pan [offset]. */
data class ZoomTransform(
    val scale: Float,
    val offset: Offset,
)

/**
 * Computes the new [ZoomTransform] when zooming from [current] to [newScaleUnclamped] around
 * [anchor], keeping the anchored point stationary on screen. Shared by the photo and video pages.
 *
 * @param newScaleUnclamped Desired scale before clamping into [minScale]..[maxScale].
 */
fun zoomAround(
    current: ZoomTransform,
    newScaleUnclamped: Float,
    anchor: Offset,
    containerSize: IntSize,
    minScale: Float,
    maxScale: Float,
): ZoomTransform {
    val newScale = newScaleUnclamped.coerceIn(minScale, maxScale)
    if (newScale <= 1f) return ZoomTransform(newScale, Offset.Zero)

    val ratio = newScale / current.scale.coerceAtLeast(0.0001f)
    val cx = containerSize.width / 2f
    val cy = containerSize.height / 2f
    val newOffset =
        Offset(
            (anchor.x - cx) * (1f - ratio) + current.offset.x * ratio,
            (anchor.y - cy) * (1f - ratio) + current.offset.y * ratio,
        )
    return ZoomTransform(newScale, newOffset)
}

/**
 * Recognizes the media viewer's tap / double-tap / quick-zoom gesture protocol.
 *
 * After a quick first tap, a second tap within the double-tap timeout either fires
 * [onSecondTap] (when released without dragging) or, once the finger is dragged
 * vertically past touch slop, drives a continuous "quick zoom" via [onQuickZoom].
 * A lone first tap is reported through [onSingleTap]; a long first press is ignored.
 *
 * Shared by [PhotoPage] and the platform `VideoPage` so the gesture logic lives in one place.
 *
 * @param key Recomposition key forwarded to [pointerInput]; restarts detection when it changes.
 * @param onSingleTap Invoked for a single tap that is not followed by a second tap.
 * @param onSecondTap Invoked with the second-tap anchor when it is released without dragging.
 * @param onQuickZoomStart Invoked once when a quick-zoom drag begins.
 * @param onQuickZoom Invoked on each drag step with the gesture anchor and the incremental
 *        scale multiplier to apply to the current scale.
 */
fun Modifier.tapZoomSeekGestures(
    key: Any?,
    onSingleTap: () -> Unit,
    onSecondTap: (anchor: Offset) -> Unit,
    onQuickZoomStart: () -> Unit = {},
    onQuickZoom: (anchor: Offset, scaleMultiplier: Float) -> Unit,
): Modifier =
    pointerInput(key) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            // A held (long) first press is not part of this protocol — bail out.
            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                waitForUpOrCancellation()
            } ?: return@awaitEachGesture

            val secondDown =
                withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                    awaitFirstDown(requireUnconsumed = false)
                }
            if (secondDown == null) {
                onSingleTap()
                return@awaitEachGesture
            }

            val anchor = secondDown.position
            var dragStarted = false
            var lastY = secondDown.position.y

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == secondDown.id }
                if (change == null || !change.pressed) {
                    if (change != null && !dragStarted) onSecondTap(anchor)
                    break
                }

                if (!dragStarted) {
                    val moved = (change.position - anchor).getDistance()
                    if (moved > viewConfiguration.touchSlop) {
                        dragStarted = true
                        lastY = change.position.y
                        onQuickZoomStart()
                        change.consume()
                    }
                }

                if (dragStarted) {
                    val dy = change.position.y - lastY
                    lastY = change.position.y
                    onQuickZoom(anchor, exp(dy / QUICK_ZOOM_SENSITIVITY_PX))
                    change.consume()
                }
            }
        }
    }
