package se.nullable.flickboard.ui.util

import android.os.Build
import java.util.Locale

fun locale(language: String, country: String): Locale = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> Locale.of(language, country)
    else -> {
        @Suppress("DEPRECATION")
        Locale(language, country)
    }
}