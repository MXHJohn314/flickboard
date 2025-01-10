package se.nullable.flickboard.ui.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.round

/**
 * Centers the item in the visible area of its bounds.
 */
fun Modifier.centerVerticallyInVisibleBounds(): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            Constraints(
                maxWidth = constraints.maxWidth,
                maxHeight = constraints.maxHeight,
            ),
        )
        layout(placeable.width, placeable.height) {
            coordinates?.let { coords ->
                val parentCoords = coords.parentCoordinates ?: coords
                val visibleBounds = parentCoords.boundsInWindow()
                val centerOfBounds = coords.windowToLocal(visibleBounds.center).round()
                val centerOfPlaceable = IntSize(placeable.width, placeable.height).center
                val position = centerOfBounds - centerOfPlaceable
                placeable.place(0, position.y)
            }
        }
    }