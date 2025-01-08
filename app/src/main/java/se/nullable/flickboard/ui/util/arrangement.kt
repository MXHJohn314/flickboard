package se.nullable.flickboard.ui.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

fun Arrangement.Horizontal.reverse(): Arrangement.Horizontal = let { wrapped ->
    object : Arrangement.Horizontal {
        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        ) {
            with(wrapped) {
                arrange(
                    totalSize, sizes,
                    when (layoutDirection) {
                        LayoutDirection.Ltr -> LayoutDirection.Rtl
                        LayoutDirection.Rtl -> LayoutDirection.Ltr
                    },
                    outPositions,
                )
            }
        }
    }
}