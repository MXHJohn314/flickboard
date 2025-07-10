package se.nullable.flickboard.ui.util

import android.os.Build

val isSamsungDevice = Build.MANUFACTURER == "samsung"

val isEmulator = Build.PRODUCT.startsWith("sdk_")