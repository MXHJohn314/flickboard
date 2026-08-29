package se.nullable.flickboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import se.nullable.flickboard.ui.theme.LocalKeyboardTheme
import kotlin.time.Duration.Companion.milliseconds

class PointerTrailState(val density: State<Density>) {
    /**
     * The current trail overlay
     * This is *mutable* and only replaced when the trail changes size.
     * To be notified whenever the trail is modified,
     * read (and discard the value of) [needRedraw].
     */
    // We store the rendered pointer trail instead of re-rendering from scratch on every frame.
    // This means that we end up with a slightly higher overhead cost per frame,
    // but this should make the cost-per-redraw ~constant rather than scaling with the trail length.
    // (Which does otherwise appear to add up for very-long trails.)
    // TODO: can we benchmark this loosely?
    // impl: after mutating, always set `needRedraw = Unit`
    var trailOverlayBitmap by mutableStateOf(ImageBitmap(1, 1))
    private val trailOverlayCanvas by derivedStateOf { Canvas(trailOverlayBitmap) }

    /**
     * Dummy-valued state that can be read to subscribe to redraw notifications.
     */
    var needRedraw by mutableStateOf(Unit, neverEqualPolicy())
        private set

    var gestureIsActive: Boolean by mutableStateOf(false)
        private set

    fun clearTrail() {
        trailOverlayCanvas.drawRect(
            0f,
            0f,
            trailOverlayBitmap.width.toFloat(),
            trailOverlayBitmap.height.toFloat(),
            ClearPaint,
        )
        needRedraw = Unit
    }

    fun forKey(keyPosition: Offset) = ForKey(this, keyPosition)
    data class ForKey(val state: PointerTrailState, val keyPosition: Offset) {
        fun onDown() {
            state.clearTrail()
            previousPoint = null
            state.gestureIsActive = true
        }

        fun onUp() {
            state.gestureIsActive = false
        }

        private var previousPoint: Offset? = null
        fun addPoint(offsetRelativeToKey: Offset) {
            val pos = offsetRelativeToKey + keyPosition
            val bitmap = state.trailOverlayBitmap
            val canvas = state.trailOverlayCanvas

            // Fade out previous points into the background
            canvas.drawRect(
                0f,
                0f,
                bitmap.width.toFloat(),
                bitmap.height.toFloat(),
                FadePaint,
            )

            state.density.value.apply {
                TrailPaint.strokeWidth = 10.dp.toPx()
            }
            canvas.drawLine(
                paint = TrailPaint,
                p1 = pos,
                p2 = previousPoint ?: pos,
            )

            state.needRedraw = Unit
            previousPoint = pos
        }
    }

    companion object {
        private val ClearPaint = Paint().also {
            it.blendMode = BlendMode.Clear
        }
        private val FadePaint = Paint().also {
            it.color = Color.Transparent.copy(alpha = 0.95f)
            it.blendMode = BlendMode.Modulate
        }
        private val TrailPaint = Paint().also {
            it.color = Color.Black
            it.strokeCap = StrokeCap.Round
        }
    }
}

@Composable
fun rememberPointerTrailState(): PointerTrailState = run {
    val density = rememberUpdatedState(LocalDensity.current)
    remember { PointerTrailState(density) }
}

@Composable
fun PointerTrail(modifier: Modifier, state: PointerTrailState) {
    val pointerTrailColour = LocalKeyboardTheme.current.pointerTrailColour
    val pointerTrailTint = ColorFilter.tint(pointerTrailColour)
    if (!state.gestureIsActive) {
        LaunchedEffect(Unit) {
            delay(2000.milliseconds)
            state.clearTrail()
        }
    }
    Canvas(
        modifier.onSizeChanged { state.trailOverlayBitmap = ImageBitmap(it.width, it.height) },
        "Pointer trail",
    ) {
        state.needRedraw
        drawImage(state.trailOverlayBitmap, colorFilter = pointerTrailTint)
    }
}