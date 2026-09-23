package se.nullable.flickboard.ui.util

import android.util.Log
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/**
 * A helper for [Painter] implementations that delegate to another inner [Painter].
 *
 * Subtypes should call [drawInner] in their [onDraw] implementations, rather than [draw].
 */
abstract class PainterWrapper : Painter() {
    // Let inner handle alpha/colorFilter however it wants to, rather than having the wrapping Painter
    // apply it as a Canvas layer.
    private var alpha: Float = DefaultAlpha
    override fun applyAlpha(alpha: Float): Boolean = true.also {
        this.alpha = alpha
    }

    private var colorFilter: ColorFilter? = null
    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean = true.also {
        this.colorFilter = colorFilter
    }

    protected fun DrawScope.drawInner(inner: Painter) {
        with(inner) {
            draw(size, alpha = alpha, colorFilter = colorFilter)
        }
    }
}

/**
 * A [Painter] wrapper that tries to render [inner], but falls back to [fallbackPainter] if it fails.
 *
 * This is a fail-safe to avoid crashing when rendering user-supplied images.
 * It'll be ugly, but at least it gives the user a chance to replace the image...
 */
data class FailsafePainter(
    private val what: String,
    private val inner: Painter,
    private val fallbackPainter: Painter? = null,
) : PainterWrapper() {
    override val intrinsicSize: Size
        get() = inner.intrinsicSize

    override fun DrawScope.onDraw() {
        try {
            drawInner(inner)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to render $what, ignoring...", e)
            if (fallbackPainter != null) {
                drawInner(fallbackPainter)
            }
        }
    }

    companion object {
        const val LOG_TAG = "FailsafePainter"
    }
}

/**
 * Wraps an inner [Painter] that depends on the target canvas.
 *
 * For example, this can be used to load images scaled to the target canvas,
 * rather than loading them at full size and then downscaling them to the target canvas when rendering.
 */
class SizeDependentPainter(private val makeInner: (Size) -> Painter) : PainterWrapper() {
    private var lastSize: Size? = null
    private var lastInner: Painter? = null

    override val intrinsicSize: Size = makeInner(Size(1F, 1F)).intrinsicSize

    override fun DrawScope.onDraw() {
        drawInner(
            lastInner.takeIf { lastSize == size }
                ?: makeInner(size).also {
                    lastInner = it
                    lastSize = size
                },
        )
    }
}
