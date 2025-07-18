package se.nullable.flickboard.ui.util

import android.content.ContentResolver
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
 */
fun loadImageBitmap(contentResolver: ContentResolver, uri: Uri, targetSize: Size): ImageBitmap =
    when {
        Build.VERSION.SDK_INT >= 28 -> ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(contentResolver, uri),
            { dec, _, _ ->
                dec.setTargetSize(
                    targetSize.height.roundToInt(),
                    targetSize.width.roundToInt(),
                )
            },
        )

        // TODO: handle resources?
        else -> contentResolver.openFileDescriptor(uri, "r")!!
            .use { file ->
                val opts = BitmapFactory.Options().also {
                    // BitmapFactory doesn't support configuring the target size directly,
                    // so instead we have to inspect the bounds ourselves, and adjust
                    // the scaling factor.
                    it.inJustDecodeBounds = true
                    BitmapFactory.decodeFileDescriptor(file.fileDescriptor, null, it)
                    it.inJustDecodeBounds = false
                    it.inSampleSize =
                        min(
                            it.outWidth / targetSize.width,
                            it.outHeight / targetSize.height,
                        ).toInt().coerceAtLeast(1)
                }
                BitmapFactory.decodeFileDescriptor(file.fileDescriptor, null, opts)!!
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