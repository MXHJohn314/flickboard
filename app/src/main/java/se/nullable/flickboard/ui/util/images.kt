package se.nullable.flickboard.ui.util

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

fun loadImageBitmap(contentResolver: ContentResolver, uri: Uri): ImageBitmap = when {
    Build.VERSION.SDK_INT >= 28 -> ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(contentResolver, uri),
    )

    else -> contentResolver.openInputStream(uri)
        .use { BitmapFactory.decodeStream(it) }
}.asImageBitmap()
