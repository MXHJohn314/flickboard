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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import se.nullable.flickboard.R
import se.nullable.flickboard.ui.settings.AppSettings
import se.nullable.flickboard.ui.settings.PointerTrailRenderer
import se.nullable.flickboard.ui.settings.localAppSetting
import se.nullable.flickboard.ui.theme.LocalKeyboardTheme
import se.nullable.flickboard.util.RingBuffer
import se.nullable.flickboard.util.ringBuffer
import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

sealed class PointerTrailState {
    abstract var gestureIsActive: Boolean
        protected set

    abstract fun clearTrail()

    abstract fun forKey(keyPosition: Offset): ForKey
    sealed class ForKey {
        protected abstract val state: PointerTrailState

        open fun onDown() {
            state.clearTrail()
            state.gestureIsActive = true
        }

        fun onUp() {
            state.gestureIsActive = false
        }

        abstract fun addPoint(offsetRelativeToKey: Offset)
    }

    companion object {
        const val FADE_PER_POINT: Float = 0.95F
    }
}

/**
 * [PointerTrailState] that eagerly renders each line to a persistent [ImageBitmap].
 */
private class BitmapPointerTrailState(val density: State<Density>) : PointerTrailState() {
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

    override var gestureIsActive: Boolean by mutableStateOf(false)

    override fun clearTrail() {
        trailOverlayCanvas.drawRect(
            0f,
            0f,
            trailOverlayBitmap.width.toFloat(),
            trailOverlayBitmap.height.toFloat(),
            ClearPaint,
        )
        needRedraw = Unit
    }

    override fun forKey(keyPosition: Offset) = ForKey(this, keyPosition)
    data class ForKey(override val state: BitmapPointerTrailState, val keyPosition: Offset) :
        PointerTrailState.ForKey() {
        override fun onDown() {
            super.onDown()
            previousPoint = null
        }

        private var previousPoint: Offset? = null
        override fun addPoint(offsetRelativeToKey: Offset) {
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
            it.color = Color.Transparent.copy(alpha = FADE_PER_POINT)
            it.blendMode = BlendMode.Modulate
        }
        private val TrailPaint = Paint().also {
            it.color = Color.Black
            it.strokeCap = StrokeCap.Round
        }
    }
}

/**
 * [PointerTrailState] that buffers all points into a list,
 * and is rendered from scratch every time.
 */
private class DeferredPointerTrailState : PointerTrailState() {
    private val _points = ringBuffer<Offset>(POINTS_WORTH_KEEPING)

    /**
     * This is *mutable* and only replaced when the trail changes size.
     * To be notified whenever the trail is modified,
     * read (and discard the value of) [needRedraw].
     */
    val points: RingBuffer<Offset> = _points

    /**
     * Dummy-valued state that can be read to subscribe to redraw notifications.
     */
    var needRedraw by mutableStateOf(Unit, neverEqualPolicy())
        private set

    override var gestureIsActive: Boolean by mutableStateOf(false)

    override fun clearTrail() {
        _points.clear()
        needRedraw = Unit
    }

    override fun forKey(keyPosition: Offset) = ForKey(this, keyPosition)
    data class ForKey(override val state: DeferredPointerTrailState, val keyPosition: Offset) :
        PointerTrailState.ForKey() {
        override fun addPoint(offsetRelativeToKey: Offset) {
            val pos = offsetRelativeToKey + keyPosition
            state._points.push(pos)
            state.needRedraw = Unit
        }
    }

    companion object {
        // Rough estimate of how many points are still visible before they've faded out completely
        private val POINTS_WORTH_KEEPING = ceil(
            log(
                // Conservative estimate, assuming 8-bit alpha this means that the
                // colour has faded completely into the background
                1F / 256,
                FADE_PER_POINT,
            ),
        ).toInt()
    }
}

@Composable
fun rememberPointerTrailState(): PointerTrailState =
    when (localAppSetting(AppSettings.pointerTrailRenderer).value) {
        PointerTrailRenderer.Deferred -> remember { DeferredPointerTrailState() }
        PointerTrailRenderer.Eager -> {
            val density = rememberUpdatedState(LocalDensity.current)
            remember { BitmapPointerTrailState(density) }
        }
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
    @Suppress("UnusedVariable", "unused", "LocalVariableName")
    val _enforceExhaustive: Unit = when (state) {
        is BitmapPointerTrailState ->
            Canvas(
                modifier.onSizeChanged {
                    state.trailOverlayBitmap = ImageBitmap(it.width, it.height)
                },
                stringResource(R.string.pointerTrail),
            ) {
                state.needRedraw
                drawImage(state.trailOverlayBitmap, colorFilter = pointerTrailTint)
            }

        is DeferredPointerTrailState -> {
            val trailPaint = remember {
                Paint().also {
                    it.color = Color.Black
                    it.strokeCap = StrokeCap.Round
                    it.blendMode = BlendMode.Src
                }
            }
            Canvas(
                modifier.graphicsLayer(colorFilter = pointerTrailTint),
                stringResource(R.string.pointerTrail),
            ) {
                trailPaint.strokeWidth = 10.dp.toPx()
                state.needRedraw
                drawIntoCanvas { canvas ->
                    var previousPoint: Offset? = null
                    var i = state.points.size
                    state.points.forEach { pos ->
                        i -= 1
                        trailPaint.alpha = PointerTrailState.FADE_PER_POINT.pow(i)
                        canvas.drawLine(
                            paint = trailPaint,
                            p1 = pos,
                            p2 = previousPoint ?: pos,
                        )
                        previousPoint = pos
                    }
                }
            }
        }
    }
}