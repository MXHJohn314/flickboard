package se.nullable.flickboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

val LocalDisplayLimits = staticCompositionLocalOf<DisplayLimits?> { null }

data class DisplayLimits(
    val portraitWidth: Dp,
    val largestDimensionPixels: Int,
    val isLandscape: Boolean
) {
    companion object {
        @Composable
        fun calculateCurrent(): DisplayLimits {
            val displayMetrics = LocalContext.current.resources.displayMetrics
            return DisplayLimits(
                portraitWidth = (min(
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels,
                ) / displayMetrics.density).dp,
                largestDimensionPixels = max(
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels,
                ),
                isLandscape = displayMetrics.widthPixels * 1.2F > displayMetrics.heightPixels,
            )
        }
    }
}