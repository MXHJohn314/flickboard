package se.nullable.flickboard.ui.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrain

// HACK: Android tries to remeasure the dialog twice,
// without and then with taking system bar insets into account.
// This causes both sizes to be propagated down the composable stack, causing [LazyVerticalGrid]
// to lose its place when the "top item" is temporarily moved into the previous row.
// For some reason, Views' sizes aren't affected by this issue, so we constrain the size to the
// view from the previous frame.
// Obviously, this must be applied at the top of the composable hierarchy, *before* any padding etc
// is applied.
// See https://codeberg.org/natkr/flickboard/issues/316
fun Modifier.constrainSizeToView(view: AbstractComposeView): Modifier =
    this.layout { measurable, constraints ->
        val viewConstraints = when {
            // view.{width,height} are 0 before the layout is done, so leave unconstrained then
            !view.isLaidOut -> Constraints()
            else -> Constraints(maxWidth = view.width, maxHeight = view.height)
        }
        val placeable =
            measurable.measure(viewConstraints.constrain(constraints))
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }