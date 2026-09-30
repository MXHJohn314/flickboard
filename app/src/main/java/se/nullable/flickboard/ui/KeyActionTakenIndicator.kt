package se.nullable.flickboard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.ActionTint
import se.nullable.flickboard.model.KeyboardState
import se.nullable.flickboard.model.TextDirection
import se.nullable.flickboard.util.times
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun KeyActionTakenIndicator(
    state: KeyActionTakenState,
    shape: Shape,
    colour: Color,
    surfaceColour: Color,
    layoutTextDirection: TextDirection,
    modifier: Modifier = Modifier
) {
    var lastActionIsVisible by remember { mutableStateOf(false) }
    val lastActionAlpha = animateFloatAsState(1F * lastActionIsVisible, label = "lastActionAlpha") {
        if (!lastActionIsVisible) {
            state.lastActionTaken = null
        }
    }
    LaunchedEffect(state.lastActionTaken) {
        val isActive = state.lastActionTaken != null
        lastActionIsVisible = isActive
        if (isActive) {
            delay(300.milliseconds)
            lastActionIsVisible = false
        }
    }

    state.lastActionTaken?.let { takenAction ->
        KeyActionTakenIndicatorActive(
            takenAction,
            shape,
            colour,
            surfaceColour,
            layoutTextDirection,
            modifier = modifier.alpha(lastActionAlpha.value),
        )
    }
}

class KeyActionTakenState {
    var lastActionTaken: TakenAction? by mutableStateOf(null)

    // NOT a data class, so that we get a new identity every time
    class TakenAction(val action: Action, val keyboardState: KeyboardState)
}

@Composable
fun KeyActionTakenIndicatorActive(
    takenAction: KeyActionTakenState.TakenAction,
    shape: Shape,
    colour: Color,
    surfaceColour: Color,
    layoutTextDirection: TextDirection,
    modifier: Modifier = Modifier
) {
    Surface(
        color = surfaceColour,
        shape = shape,
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize()) {
            RenderActionVisual(
                action = takenAction.action.withHidden(false),
                keyboardState = takenAction.keyboardState,
                modifier = Modifier.align(Alignment.Center),
                colour = colour,
                activeColour = colour,
                tint = ActionTint.Normal,
                layoutTextDirection = layoutTextDirection,
            )
        }
    }
}

@Composable
@Preview
fun KeyActionTakenPreview() {
    FlickBoardParent {
        Column {
            Box(Modifier.size(100.dp)) {
                KeyActionTakenIndicatorActive(
                    takenAction = KeyActionTakenState.TakenAction(
                        action = Action.Text("A"),
                        keyboardState = KeyboardState.ShowAll,
                    ),
                    colour = MaterialTheme.colorScheme.onTertiary,
                    surfaceColour = MaterialTheme.colorScheme.tertiary,
                    shape = RectangleShape,
                    layoutTextDirection = TextDirection.LeftToRight,
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Box(Modifier.size(100.dp)) {
                KeyActionTakenIndicatorActive(
                    takenAction = KeyActionTakenState.TakenAction(
                        action = Action.Text("{"),
                        keyboardState = KeyboardState.ShowAll,
                    ),
                    colour = MaterialTheme.colorScheme.onTertiary,
                    surfaceColour = MaterialTheme.colorScheme.tertiary,
                    shape = RectangleShape,
                    layoutTextDirection = TextDirection.LeftToRight,
                )
            }
        }
    }
}
