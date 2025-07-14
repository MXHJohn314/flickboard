package se.nullable.flickboard.model

import android.view.KeyEvent
import androidx.compose.runtime.State
import se.nullable.flickboard.util.times

data class ModifierState(
    val shift: ShiftState = ShiftState.Normal,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val zalgo: Boolean = false,
    val select: Boolean = false,
) {
    fun next(settings: ModifierSettings): ModifierState =
        ModifierState(
            shift = shift.next(),
            ctrl = ctrl && settings.stickyModifiers.value,
            alt = alt && settings.stickyModifiers.value,
        )

    val useRawKeyEvent = ctrl || alt

    val androidMetaState = KeyEvent.normalizeMetaState(
        when (shift) {
            ShiftState.Normal -> 0
            ShiftState.Shift -> KeyEvent.META_SHIFT_LEFT_ON
            ShiftState.CapsLock -> KeyEvent.META_CAPS_LOCK_ON
        }
                or (KeyEvent.META_CTRL_LEFT_ON * ctrl)
                or (KeyEvent.META_ALT_LEFT_ON * alt),
    )
}

enum class ShiftState {
    Normal,
    Shift,
    CapsLock;

    val isShift: Boolean
        get() = this == Shift
    val isCapsLock: Boolean
        get() = this == CapsLock

    fun next(): ShiftState = when (this) {
        Shift -> Normal
        else -> this
    }
}

data class ModifierSettings(val stickyModifiers: State<Boolean>)

data class KeyboardState(
    val modifiers: ModifierState?,
    val modifierSettings: ModifierSettings?,
    val hasEnterOverride: Boolean,
) {
    companion object {
        val Default = KeyboardState(
            modifiers = ModifierState(),
            modifierSettings = null,
            hasEnterOverride = false,
        )
        val ShowAll = KeyboardState(
            modifiers = null,
            modifierSettings = null,
            hasEnterOverride = false,
        )
    }
}