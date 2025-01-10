package se.nullable.flickboard.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.max

/**
 * A [androidx.compose.foundation.layout.Column] that lays out all items from the bottom-up.
 *
 * Similar to Column(verticalArrangement = Arrangement.Bottom), but prioritizes ensuring that lower
 * items get to maintain their preferred size.
 *
 * This can't be done with an Arrangement, since they don't seem able to forcibly resize items.
 */
@Composable
fun BottomUpColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val lastIndex = measurables.lastIndex
        var totalWidth = 0
        var totalHeight = 0
        val placeables = measurables.asReversed().mapIndexed { index, measurable ->
            val measured = measurable.measure(
                Constraints(
                    minHeight = when {
                        index == lastIndex -> (constraints.minHeight - totalHeight).coerceAtLeast(0)
                        else -> 0
                    },
                    maxHeight = (constraints.maxHeight - totalHeight).coerceAtLeast(0),
                    minWidth = constraints.minWidth,
                    maxWidth = constraints.maxWidth,
                ),
            )
            totalWidth = max(totalWidth, measured.width)
            totalHeight += measured.height
            measured
        }
        layout(totalWidth, totalHeight) {
            var y = 0
            placeables.asReversed().forEach {
                it.place(horizontalAlignment.align(it.width, totalWidth, layoutDirection), y)
                y += it.height
            }
        }
    }
}