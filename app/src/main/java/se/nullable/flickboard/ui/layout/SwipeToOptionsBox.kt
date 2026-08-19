package se.nullable.flickboard.ui.layout

import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Something that can be swiped aside to reveal a set of options.
 *
 * Like a [androidx.compose.material3.SwipeToDismissBox], but for use-cases where the "front object"
 * will be persisted, rather than dismissed.
 *
 * Heavily based on [androidx.compose.material3.SwipeToDismissBox].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeToOptionsBox(
    state: SwipeToOptionsBoxState,
    backgroundContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    swipeMarginOffset: Dp = 0.dp,
    allowSwipeGesture: Boolean = true,
    content: @Composable () -> Unit,
) {
    val swipeMarginOffsetPx = with(LocalDensity.current) {
        swipeMarginOffset.toPx()
    }
    val draggableModifier = when {
        allowSwipeGesture -> Modifier.anchoredDraggable(
            state.draggableState,
            Orientation.Horizontal,
            flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                state.draggableState,
                positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold,
                animationSpec = SpringSpec(),
            ),
        )

        else -> Modifier
    }
    Box(modifier.then(draggableModifier)) {
        Box(Modifier.matchParentSize()) { backgroundContent() }
        Box(
            Modifier
                .onSizeChanged { size -> state.updateAnchors(size.width.toFloat() - swipeMarginOffsetPx) }
                .offset { IntOffset(x = state.draggableState.offset.roundToInt(), y = 0) },
        ) { content() }
    }
}

@Composable
fun rememberSwipeToOptionsBoxState(): SwipeToOptionsBoxState = run {
    remember {
        SwipeToOptionsBoxState()
    }
}

@OptIn(ExperimentalFoundationApi::class)
class SwipeToOptionsBoxState {
    internal val draggableState = AnchoredDraggableState(SwipeToDismissBoxValue.Settled)

    fun updateAnchors(widthPx: Float) {
        draggableState.updateAnchors(
            DraggableAnchors {
                SwipeToDismissBoxValue.EndToStart at -widthPx
                SwipeToDismissBoxValue.Settled at 0F
                SwipeToDismissBoxValue.StartToEnd at widthPx
            },
        )
        epsilonPx.floatValue = widthPx * 0.1F
    }

    val currentValue: SwipeToDismissBoxValue
        get() = draggableState.currentValue

    val swipeDirection: SwipeToDismissBoxValue
        get() = when (draggableState.offset) {
            in Float.NEGATIVE_INFINITY..<0F -> SwipeToDismissBoxValue.EndToStart
            in Float.MIN_VALUE..Float.POSITIVE_INFINITY -> SwipeToDismissBoxValue.StartToEnd
            else -> SwipeToDismissBoxValue.Settled
        }

    fun swipeDirectionPadding(padding: Dp): PaddingValues = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd -> PaddingValues(end = padding)
        SwipeToDismissBoxValue.EndToStart -> PaddingValues(start = padding)
        SwipeToDismissBoxValue.Settled -> PaddingValues()
    }

    /**
     * The smallest nontrivial offset
     */
    private val epsilonPx = mutableFloatStateOf(Float.NaN)

    /**
     * A float between 0..1 that represents how visible the background should be.
     */
    val backgroundProgress: Float
        get() = (draggableState.offset.absoluteValue / epsilonPx.floatValue - 1F)
            .coerceIn(0F..1F)

    suspend fun animateReset() {
        draggableState.animateTo(SwipeToDismissBoxValue.Settled)
    }
}