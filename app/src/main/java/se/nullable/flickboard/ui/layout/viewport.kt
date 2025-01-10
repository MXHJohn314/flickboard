package se.nullable.flickboard.ui.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.round

/**
 * Centers the item in the visible area of its bounds.
 *
 * Assumes that it "owns" all maxSize.
 */
fun Modifier.centerInVisibleBounds(): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            Constraints(
                maxWidth = constraints.maxWidth,
                maxHeight = constraints.maxHeight,
            ),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            coordinates?.let { coords ->
                val visibleBounds = coords.boundsInRoot()
                val centerOfBounds = coords.windowToLocal(visibleBounds.center).round()
                val centerOfPlaceable = IntSize(placeable.width, placeable.height).center
                val position = centerOfBounds - centerOfPlaceable
                placeable.place(
                    position.x.coerceIn(0, constraints.maxWidth - placeable.width),
                    position.y.coerceIn(0, constraints.maxHeight - placeable.height),
                )
            }
        }
    }