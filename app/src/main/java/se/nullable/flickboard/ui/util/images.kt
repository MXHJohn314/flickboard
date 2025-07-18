package se.nullable.flickboard.ui.util

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/**
 * If rendering the image, you probably want to wrap it in a [FailsafePainter].
 */
fun loadImageBitmap(contentResolver: ContentResolver, uri: Uri): ImageBitmap = when {
    Build.VERSION.SDK_INT >= 28 -> ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(contentResolver, uri),
    )

    else -> contentResolver.openInputStream(uri)
        .use { BitmapFactory.decodeStream(it) }
}.asImageBitmap()

/**
 * A [Painter] wrapper that tries to render [inner], but falls back to [fallbackPainter] if it fails.
 *
 * This is a fail-safe to avoid crashing when rendering user-supplied images.
 * It'll be ugly, but at least it gives the user a chance to replace the image...
 */
class FailsafePainter(
    private val what: String,
    private val inner: Painter,
    private val fallbackPainter: Painter? = null,
) : Painter() {
    override val intrinsicSize: Size
        get() = inner.intrinsicSize

    private var alpha: Float = DefaultAlpha
    override fun applyAlpha(alpha: Float): Boolean = true.also {
        this.alpha = alpha
    }

    private var colorFilter: ColorFilter? = null
    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean = true.also {
        this.colorFilter = colorFilter
    }

    override fun DrawScope.onDraw() {
        try {
            with(inner) {
                draw(size, alpha = alpha, colorFilter = colorFilter)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to render $what, ignoring...", e)
            if (fallbackPainter != null) {
                with(fallbackPainter) {
                    draw(size, alpha = alpha, colorFilter = colorFilter)
                }
            }
        }
    }

    companion object {
        const val LOG_TAG = "FailsafePainter"
    }
}
