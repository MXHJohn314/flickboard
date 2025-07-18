package se.nullable.flickboard.ui.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import se.nullable.flickboard.R
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * If rendering the image, you probably want to wrap it in a [FailsafePainter].
 *
 * [targetSizeHint] is a _hint_ for how the image should be scaled, there is no guarantee that it will
 * actually fit that size without further scaling.
 */
fun loadImageBitmap(contentResolver: ContentResolver, uri: Uri, targetSizeHint: Size): ImageBitmap =
    when {
        Build.VERSION.SDK_INT >= 28 -> ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(contentResolver, uri),
            { dec, _, _ ->
                dec.setTargetSize(
                    targetSizeHint.height.roundToInt(),
                    targetSizeHint.width.roundToInt(),
                )
            },
        )

        else -> {
            val opts = BitmapFactory.Options()
            fun decode(): Bitmap? =
                contentResolver.openInputStream(uri)!!
                    .use { BitmapFactory.decodeStream(it, null, opts) }

            // BitmapFactory doesn't support configuring the target size directly,
            // so instead we have to inspect the bounds ourselves, and adjust
            // the scaling factor.
            opts.inJustDecodeBounds = true
            decode()
            opts.inJustDecodeBounds = false
            // Sample-size only gives us an integer-power-of-2 approximation,
            // the user (like BitmapPainter) is still responsible for scaling to the exact size.
            opts.inSampleSize =
                min(
                    opts.outWidth / targetSizeHint.width,
                    opts.outHeight / targetSizeHint.height,
                ).toInt().coerceAtLeast(1)

            decode()!!
        }
    }.asImageBitmap()

/**
 * Loads a Painter for an image provided by the user.
 *
 * Automatically handles scaling it to the target canvas size,
 * and handling errors loading or rendering the image.
 */
@Composable
fun userImagePainter(uri: Uri): Painter = run {
    val contentResolver = LocalContext.current.contentResolver
    val brokenImage = painterResource(R.drawable.outline_broken_image_24)
    remember(uri, contentResolver, brokenImage) {
        FailsafePainter(
            "background image",
            SizeDependentPainter { size ->
                // No specific error handling required; makeInner is covered by FailsafePainter's
                // error handling logic.
                BitmapPainter(loadImageBitmap(contentResolver, uri, size))
            },
            fallbackPainter = brokenImage,
        )
    }
}