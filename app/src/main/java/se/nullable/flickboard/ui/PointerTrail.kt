package se.nullable.flickboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import se.nullable.flickboard.ui.theme.LocalKeyboardTheme
import kotlin.time.Duration.Companion.milliseconds

class PointerTrailState {
    var isActive: Boolean by mutableStateOf(false)
        private set

    var activeKeyPosition: Offset by mutableStateOf(Offset.Zero)
        private set

    private val mutablePointsRelativeToKey = mutableStateListOf<Offset>()
    val pointsRelativeToKey: List<Offset>
        get() = mutablePointsRelativeToKey

    fun clearTrail() {
        mutablePointsRelativeToKey.clear()
    }

    fun forKey(keyPosition: Offset) = ForKey(this, keyPosition)
    data class ForKey(val state: PointerTrailState, val keyPosition: Offset) {
        fun onDown() {
            state.activeKeyPosition = keyPosition
            state.mutablePointsRelativeToKey.clear()
            state.isActive = true
        }

        fun onUp() {
            state.isActive = false
        }

        fun addPoint(offsetRelativeToKey: Offset) {
            state.mutablePointsRelativeToKey.add(offsetRelativeToKey)
        }
    }
}

@Composable
fun rememberPointerTrailState(): PointerTrailState =
    remember { PointerTrailState() }

@Composable
fun PointerTrail(modifier: Modifier, state: PointerTrailState) {
    val pointerTrailColor = LocalKeyboardTheme.current.pointerTrailColour
    if (!state.isActive) {
        LaunchedEffect(Unit) {
            delay(2000.milliseconds)
            state.clearTrail()
        }
    }
    Canvas(modifier, "Pointer trail") {
        clipRect {
            val keyPosition = state.activeKeyPosition
            state.pointsRelativeToKey.forEach {
                this.drawCircle(
                    pointerTrailColor,
                    center = it + keyPosition,
                    radius = 10.dp.toPx(),
                    alpha = 0.4f,
                )
            }
        }
    }
}