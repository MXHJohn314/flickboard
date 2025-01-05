package se.nullable.flickboard.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

data class DirectionNames(val start: String, val end: String)

@Composable
fun directionNames(): DirectionNames = run {
    val direction = LocalLayoutDirection.current
    remember(direction) {
        when (direction) {
            LayoutDirection.Ltr -> DirectionNames(start = "left", end = "right")
            LayoutDirection.Rtl -> DirectionNames(start = "right", end = "left")
        }
    }
}