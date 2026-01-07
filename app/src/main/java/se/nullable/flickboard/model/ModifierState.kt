package se.nullable.flickboard.model

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.State
import se.nullable.flickboard.R
import se.nullable.flickboard.ui.settings.EnabledLayers
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
    val enterOverride: EnterActionOverride?,
    val enabledLayers: EnabledLayers,
) {
    companion object {
        val Default = KeyboardState(
            modifiers = ModifierState(),
            modifierSettings = null,
            enterOverride = null,
            enabledLayers = EnabledLayers.Letters,
        )
        val ShowAll = KeyboardState(
            modifiers = null,
            modifierSettings = null,
            enterOverride = EnterActionOverride(
                EnterActionOverride.Mode.Secondary,
                visual = ActionVisual.Icon(R.drawable.baseline_done_24),
            ),
            enabledLayers = EnabledLayers.Letters,
        )
    }
}

data class EnterActionOverride(val mode: Mode, val visual: ActionVisual) {
    enum class Mode {
        /**
         * Override replaces the regular enter button.
         */
        Replace,

        /**
         * Override appears as a secondary enter button.
         */
        Secondary;

        companion object {
            fun fromEditorInfo(editorInfo: EditorInfo): Mode? = when {
                editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION == EditorInfo.IME_ACTION_NONE -> null
                editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0 -> Secondary
                else -> Replace
            }
        }
    }
}