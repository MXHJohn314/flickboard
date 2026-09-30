package se.nullable.flickboard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.ActionTint
import se.nullable.flickboard.model.KeyboardState
import se.nullable.flickboard.model.TextDirection

@Composable
fun KeyActionTakenIndicator(
    action: Action,
    keyboardState: KeyboardState,
    shape: Shape,
    colour: Color,
    surfaceColour: Color,
    layoutTextDirection: TextDirection,
    modifier: Modifier = Modifier
) {
    Surface(color = surfaceColour, shape = shape, modifier = modifier) {
        Box(Modifier.fillMaxSize()) {
            RenderActionVisual(
                action = action.withHidden(false),
                keyboardState = keyboardState,
                modifier = Modifier.align(Alignment.Center),
                colour = colour,
                activeColour = colour,
                tint = ActionTint.Normal,
                layoutTextDirection = layoutTextDirection,
            )
        }
    }
}
