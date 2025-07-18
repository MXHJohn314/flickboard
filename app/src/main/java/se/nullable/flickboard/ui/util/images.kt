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
 * [targetSize] is a _hint_ for how the image should be scaled, there is no guarantee that it will
 * actually fit that size without further scaling.
 */
fun loadBitmap(
    contentResolver: ContentResolver,
    uri: Uri,
    targetSize: Size,
    scalingMode: ScalingMode,
): Bitmap = run {
    // Sample-size only gives us an integer-power-of-2 approximation,
    // the user (like BitmapPainter) is still responsible for scaling to the exact size.
    fun targetSampleSize(sourceWidth: Int, sourceHeight: Int): Int =
        min(
            sourceWidth / targetSize.width,
            sourceHeight / targetSize.height,
        ).toInt().coerceAtLeast(1)

    when {
        Build.VERSION.SDK_INT >= 28 -> ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(contentResolver, uri),
            { dec, info, _ ->
                when (scalingMode) {
                    ScalingMode.TryPrecise ->
                        dec.setTargetSize(
                            targetSize.height.roundToInt(),
                            targetSize.width.roundToInt(),
                        )

                    ScalingMode.Conservative ->
                        dec.setTargetSampleSize(targetSampleSize(info.size.width, info.size.height))
                }
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
            when (scalingMode) {
                ScalingMode.Conservative,
                    // TryPrecise isn't available before API28, so fall back to Approximate
                ScalingMode.TryPrecise ->
                    opts.inSampleSize = targetSampleSize(opts.outWidth, opts.outHeight)
            }

            decode()!!
        }
    }
}

enum class ScalingMode {
    /**
     * Try to preserve characteristics of the original image, such as the aspect ratio.
     * Will only loosely approximate the requested target size.
     */
    Conservative,

    /** Try to scale to the precise requested size. May still fall back to [Conservative]. */
    TryPrecise,
}

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
                BitmapPainter(
                    loadBitmap(
                        contentResolver, uri,
                        targetSize = size,
                        scalingMode = ScalingMode.TryPrecise,
                    ).asImageBitmap(),
                )
            },
            fallbackPainter = brokenImage,
        )
    }
}