package se.nullable.flickboard.model

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import se.nullable.flickboard.R
import se.nullable.flickboard.model.layouts.MESSAGEASE_SYMBOLS_LAYER
import se.nullable.flickboard.model.layouts.MINI_NUMBERS_SYMBOLS_LAYER
import se.nullable.flickboard.ui.settings.ModifierBehaviourOption
import se.nullable.flickboard.util.HardLineBreakIterator
import se.nullable.flickboard.util.flipIfBracket
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.max

data class Layout(
    val mainLayer: Layer,
    val locale: Locale = Locale.ENGLISH,
    val shiftLayer: Layer = mainLayer.autoShift(locale),
    val controlLayer: Layer? = null,
    val symbolLayer: Layer = MESSAGEASE_SYMBOLS_LAYER,
    val miniSymbolLayer: Layer = MINI_NUMBERS_SYMBOLS_LAYER,
    val digits: String = "0123456789",
    val textDirection: TextDirection = TextDirection.LeftToRight,
)

data class Layer(val keyRows: List<List<KeyM>>) {
    private inline fun zipKeys(other: Layer, f: (KeyM, KeyM) -> KeyM): Layer =
        copy(
            keyRows = keyRows.zip(other.keyRows) { thisRow, otherRow ->
                thisRow.zip(otherRow) { thisKey, otherKey ->
                    f(thisKey, otherKey)
                }
            },
        )


    fun mergeFallback(fallback: Layer?, holdForFallback: Boolean = false): Layer =
        if (fallback != null) {
            zipKeys(fallback) { thisKey, fallbackKey ->
                thisKey.mergeFallback(
                    fallbackKey,
                    holdForFallback = holdForFallback,
                )
            }
        } else {
            this
        }

    /**
     * Inverse of [mergeFallback], where [overlay] takes priority over self.
     */
    fun mergeOverlay(overlay: Layer?): Layer =
        overlay?.mergeFallback(this) ?: this

    fun setShift(shift: Layer): Layer =
        zipKeys(shift) { thisKey, shiftKey -> thisKey.copy(shift = shiftKey) }

    fun chain(other: Layer): Layer =
        copy(keyRows = keyRows.zip(other.keyRows) { thisRow, otherRow -> thisRow + otherRow })

    private inline fun mapKeys(f: (KeyM) -> KeyM): Layer =
        copy(keyRows = keyRows.map { row -> row.map { key -> f(key) } })

    fun flipBrackets(): Layer = mapKeys { it.flipBrackets() }

    fun autoShift(locale: Locale): Layer =
        mapKeys { it.autoShift(locale) }

    fun filterActions(shownActionClasses: Set<ActionClass>, enableHiddenActions: Boolean) =
        mapKeys {
            it.filterActions(
                shownActionClasses = shownActionClasses,
                enableHiddenActions = enableHiddenActions,
            )
        }

    companion object {
        val empty = Layer(
            // The number of rows must match any other layers being merged with
            keyRows = listOf(listOf(), listOf(), listOf(), listOf()),
        )
    }
}

data class KeyM(
    val actions: Map<Direction, Action>,
    // Fast actions are performed immediately when detected, rather than when the finger is released.
    val fastActions: Map<Direction, Action> = emptyMap(),
    val longActions: Map<Direction, Action> = emptyMap(),
    val holdAction: Action? = null,
    val colspan: Int = 1,
    val fixedWidth: Dp? = null,
    val rendered: Boolean = true,
    val shift: KeyM? = null,
    val transientShift: KeyM? = null
) {
    fun mergeFallback(fallback: KeyM, holdForFallback: Boolean): KeyM = copy(
        actions = fallback.actions + actions,
        fastActions = fallback.fastActions + fastActions,
        longActions = fallback.longActions + longActions,
        holdAction = this.holdAction ?: fallback.holdAction ?: when {
            holdForFallback -> fallback.actions[Direction.CENTER]
            else -> null
        },
        colspan = max(colspan, fallback.colspan),
        fixedWidth = fixedWidth ?: fallback.fixedWidth,
        rendered = rendered && fallback.rendered,
        shift = when {
            shift == null -> fallback.shift
            fallback.shift == null -> shift
            else -> shift.mergeFallback(fallback.shift, holdForFallback = holdForFallback)
        },
        transientShift = this.transientShift ?: fallback.transientShift,
    )

    fun autoShift(locale: Locale): KeyM = (shift ?: this).copy(
        actions = actions.mapValues { it.value.shift(locale) } + (shift?.actions ?: emptyMap()),
        fastActions = fastActions.mapValues { it.value.shift(locale) } + (shift?.fastActions
            ?: emptyMap()),
    )

    fun filterActions(shownActionClasses: Set<ActionClass>, enableHiddenActions: Boolean) =
        copy(
            actions = actions.mapNotNull { (direction, action) ->
                val isShown = shownActionClasses.contains(action.actionClass)
                when {
                    isShown -> direction to action
                    enableHiddenActions -> direction to action.withHidden(true)
                    else -> null
                }
            }.toMap(),
        )

    fun flipBrackets(): KeyM = copy(
        actions = actions.mapValues {
            when (val action = it.value) {
                is Action.Text -> action.copy(character = action.character.flipIfBracket())
                else -> action
            }
        },
        shift = shift?.flipBrackets(),
        transientShift = transientShift?.flipBrackets(),
    )
}

sealed class Action {
    abstract val title: Translated
    abstract val description: Translated
    open val showAsRelatedInHelp: Boolean = true

    abstract fun visual(keyboardState: KeyboardState): ActionVisual
    open val actionClass = ActionClass.Other
    open fun tint(modifier: ModifierState?): ActionTint = when (actionClass) {
        ActionClass.Symbol -> ActionTint.Dim
        else -> ActionTint.Normal
    }

    open val fastActionType: FastActionType? = null
    open fun shift(locale: Locale): Action = this
    open fun withHidden(hidden: Boolean): Action = this

    open val isModifier = false
    open val isHiddenAction: Boolean
        get() = isModifier

    data class Text(
        val character: String,
        val forceRawKeyEvent: Boolean = false,
        val visualOverride: ActionVisual? = null,
        val hidden: (KeyboardState) -> Boolean = { false },
        val actionClassOverride: ActionClass? = null,
    ) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual = when {
            hidden(keyboardState) -> ActionVisual.None
            else -> visualOverride ?: ActionVisual.Label(character)
        }

        override fun withHidden(hidden: Boolean): Action = copy(hidden = { hidden })

        private val characterName = when (character) {
            " " -> "space"
            "\t" -> "tab"
            "\n" -> "newline"
            else -> character
        }
        override val title: Translated = translated(R.string.action_text, characterName)
        override val description: Translated = translated(R.string.action_text_desc, characterName)

        override val actionClass: ActionClass = actionClassOverride ?: when {
            character.isBlank() -> ActionClass.Other
            character.all { it.isDigit() } -> ActionClass.Number
            character.all { it.isLetter() } -> ActionClass.Letter
            else -> ActionClass.Symbol
        }

        override fun shift(locale: Locale): Action =
            copy(character = character.uppercase(locale = locale))
    }

    data class Delete(
        val direction: SearchDirection = SearchDirection.Backwards,
        val boundary: TextBoundary = TextBoundary.Character,
        val hidden: Boolean = false,
    ) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual = when {
            hidden -> ActionVisual.None
            else -> ActionVisual.Icon(R.drawable.baseline_backspace_24)
        }

        override fun withHidden(hidden: Boolean): Action = copy(hidden = hidden)

        override val title: Translated =
            translated(
                when (direction) {
                    SearchDirection.Backwards -> when (boundary) {
                        TextBoundary.Character -> R.string.action_delete_backwardsCharacter
                        TextBoundary.Word -> R.string.action_delete_backwardsWord
                        TextBoundary.Line -> R.string.action_delete_backwardsLine
                    }

                    SearchDirection.Forwards -> when (boundary) {
                        TextBoundary.Character -> R.string.action_delete_forwardsCharacter
                        TextBoundary.Word -> R.string.action_delete_forwardsWord
                        TextBoundary.Line -> R.string.action_delete_forwardsLine
                    }
                },
            )
        override val description: Translated = run {
            val mainDesc = translated(
                when (direction) {
                    SearchDirection.Backwards -> when (boundary) {
                        TextBoundary.Character -> R.string.action_delete_desc_backwardsCharacter
                        TextBoundary.Word -> R.string.action_delete_desc_backwardsWord
                        TextBoundary.Line -> R.string.action_delete_desc_backwardsLine
                    }

                    SearchDirection.Forwards -> when (boundary) {
                        TextBoundary.Character -> R.string.action_delete_desc_forwardsCharacter
                        TextBoundary.Word -> R.string.action_delete_desc_forwardsWord
                        TextBoundary.Line -> R.string.action_delete_desc_forwardsLine
                    }
                },
            )
            val fastActionsCaveat = translated(
                when (boundary) {
                    TextBoundary.Character -> R.string.action_delete_desc_fastActions_character
                    TextBoundary.Word -> R.string.action_delete_desc_fastActions_word
                    TextBoundary.Line -> R.string.action_delete_desc_fastActions_line
                },
            )
            Translated.Concat(
                listOf(mainDesc, fastActionsCaveat),
                separator = "\n",
            )
        }

        override fun shift(locale: Locale): Action = copy(boundary = TextBoundary.Word)
    }

    data class FastDelete(
        val direction: SearchDirection,
        val boundary: TextBoundary = TextBoundary.Character
    ) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_backspace_24)

        override val title: Translated = translated(R.string.action_fastDelete)
        override val description: Translated = translated(R.string.action_fastDelete_desc)

        override val fastActionType: FastActionType = FastActionType.Delete

        override fun shift(locale: Locale): Action = copy(boundary = TextBoundary.Word)
    }

    data object BeginFastAction : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual = ActionVisual.None
        override val isHiddenAction: Boolean = true

        override val title: Translated = translated(R.string.action_beginFastAction)
        override val description: Translated = translated(R.string.action_beginFastAction_desc)
    }

    data class FastActionDone(val type: FastActionType) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual = ActionVisual.None
        override val isHiddenAction: Boolean = true

        override val title: Translated = translated(R.string.action_fastActionDone)
        override val description: Translated = translated(R.string.action_fastActionDone_desc)
    }

    data class Tab(val shift: Boolean = false) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual = ActionVisual.Icon(
            R.drawable.baseline_keyboard_tab_24,
            when {
                shift -> Modifier.rotate(degrees = 180F)
                else -> Modifier
            },
        )

        override val title: Translated = translated(
            when {
                shift -> R.string.action_tab_shift
                else -> R.string.action_tab
            },
        )
        override val description: Translated = translated(
            when {
                shift -> R.string.action_tab_desc_shift
                else -> R.string.action_tab_desc
            },
        )

        override fun shift(locale: Locale): Action = copy(shift = true)
    }

    data class Enter(
        val shift: Boolean = false,
        val secondary: Boolean = false
    ) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual = when {
            secondary -> when (keyboardState.enterOverride?.mode) {
                EnterActionOverride.Mode.Secondary -> keyboardState.enterOverride.visual
                else -> ActionVisual.None
            }

            keyboardState.enterOverride?.mode == EnterActionOverride.Mode.Replace ->
                keyboardState.enterOverride.visual

            else -> ActionVisual.Icon(R.drawable.baseline_keyboard_return_24)
        }

        override val title: Translated = translated(
            when {
                secondary -> R.string.action_enter_secondary
                else -> R.string.action_enter
            },
        )
        override val description: Translated = translated(
            when {
                secondary -> R.string.action_enter_desc_secondary
                else -> R.string.action_enter_desc
            },
        )

        override fun shift(locale: Locale): Action = copy(shift = true)
    }

    data object Escape : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual {
            return ActionVisual.Label("esc")
        }

        override val title: Translated = translated(R.string.action_escape)
        override val description: Translated = translated(R.string.action_escape_desc)
    }

    data class Jump(
        val direction: SearchDirection,
        val boundary: TextBoundary = TextBoundary.Character,
    ) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual = when (direction) {
            SearchDirection.Backwards -> ActionVisual.Icon(R.drawable.baseline_keyboard_arrow_left_24)
            SearchDirection.Forwards -> ActionVisual.Icon(R.drawable.baseline_keyboard_arrow_right_24)
        }

        override val title: Translated = translated(
            when (direction) {
                SearchDirection.Backwards -> when (boundary) {
                    TextBoundary.Character -> R.string.action_jump_backwardsCharacter
                    TextBoundary.Word -> R.string.action_jump_backwardsWord
                    TextBoundary.Line -> R.string.action_jump_backwardsLine
                }

                SearchDirection.Forwards -> when (boundary) {
                    TextBoundary.Character -> R.string.action_jump_forwardsCharacter
                    TextBoundary.Word -> R.string.action_jump_forwardsWord
                    TextBoundary.Line -> R.string.action_jump_forwardsLine
                }
            },
        )
        override val description: Translated = run {
            val mainDesc = translated(
                when (direction) {
                    SearchDirection.Backwards -> when (boundary) {
                        TextBoundary.Character -> R.string.action_jump_desc_backwardsCharacter
                        TextBoundary.Word -> R.string.action_jump_desc_backwardsWord
                        TextBoundary.Line -> R.string.action_jump_desc_backwardsLine
                    }

                    SearchDirection.Forwards -> when (boundary) {
                        TextBoundary.Character -> R.string.action_jump_desc_forwardsCharacter
                        TextBoundary.Word -> R.string.action_jump_desc_forwardsWord
                        TextBoundary.Line -> R.string.action_jump_desc_forwardsLine
                    }
                },
            )
            val fastActionCaveat = translated(
                when (boundary) {
                    TextBoundary.Character -> R.string.action_jump_desc_fastActions_character
                    TextBoundary.Word -> R.string.action_jump_desc_fastActions_word
                    TextBoundary.Line -> R.string.action_jump_desc_fastActions_line
                },
            )
            val selectCaveat = translated(
                when (boundary) {
                    TextBoundary.Character -> R.string.action_jump_desc_select_character
                    TextBoundary.Word -> R.string.action_jump_desc_select_word
                    TextBoundary.Line -> R.string.action_jump_desc_select_line
                },
            )
            Translated.Concat(
                listOf(mainDesc, fastActionCaveat, selectCaveat),
                separator = "\n",
            )
        }

        override fun shift(locale: Locale): Action = copy(boundary = TextBoundary.Word)
    }

    data class JumpLineKeepPos(val direction: SearchDirection, val rawEvent: Boolean = false) :
        Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual = when (direction) {
            SearchDirection.Backwards -> ActionVisual.Icon(R.drawable.baseline_keyboard_arrow_up_24)
            SearchDirection.Forwards -> ActionVisual.Icon(R.drawable.baseline_keyboard_arrow_down_24)
        }

        // TODO: should this have distinct help texts?
        private val equivalentJump = Jump(direction, TextBoundary.Line)
        override val title: Translated = equivalentJump.title
        override val description: Translated = equivalentJump.description
        override val showAsRelatedInHelp: Boolean = !rawEvent

        override fun shift(locale: Locale): Action = copy(rawEvent = true)
    }

    data class ToggleShift(val state: ShiftState) : Action() {
        override val isModifier: Boolean = true

        override fun visual(keyboardState: KeyboardState): ActionVisual = when (state) {
            ShiftState.Normal -> ActionVisual.Icon(R.drawable.baseline_arrow_drop_down_24)
            ShiftState.Shift -> ActionVisual.Icon(R.drawable.baseline_arrow_drop_up_24)
            ShiftState.CapsLock -> ActionVisual.Icon(R.drawable.baseline_keyboard_capslock_24)
        }

        override val title: Translated = translated(
            when (state) {
                ShiftState.Normal -> R.string.action_toggleShift_normal
                ShiftState.Shift -> R.string.action_toggleShift_shift
                ShiftState.CapsLock -> R.string.action_toggleShift_capsLock
            },
        )
        override val description: Translated = translated(
            when (state) {
                ShiftState.Normal -> R.string.action_toggleShift_desc_normal
                ShiftState.Shift -> R.string.action_toggleShift_desc_shift
                ShiftState.CapsLock -> R.string.action_toggleShift_desc_capsLock
            },
        )

        override fun tint(modifier: ModifierState?): ActionTint = when (state) {
            modifier?.shift -> ActionTint.Active
            else -> ActionTint.Normal
        }

        override fun shift(locale: Locale): Action = when (state) {
            ShiftState.Shift -> copy(state = ShiftState.CapsLock)
            else -> this
        }
    }

    // Not an actual usable icon, but a pseudo-icon that shows up in help texts
    data object TransientShift : Action() {
        override val isModifier: Boolean = true

        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_rotate_right_24)

        override val title: Translated = translated(R.string.action_transientShift)
        override val description: Translated = translated(R.string.action_transientShift_desc)
    }

    /**
     * Shift the case for the current word up/down one step (lower <-> Title <-> UPPER )
     */
    data class ToggleWordCase(val direction: CaseChangeDirection, val locale: Locale) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual = when (direction) {
            CaseChangeDirection.Up -> ActionVisual.Icon(R.drawable.baseline_keyboard_double_arrow_up_24)
            CaseChangeDirection.Down -> ActionVisual.Icon(R.drawable.baseline_keyboard_double_arrow_down_24)
        }

        override val title: Translated = translated(
            when (direction) {
                CaseChangeDirection.Up -> R.string.action_toggleWordCase_up
                CaseChangeDirection.Down -> R.string.action_toggleWordCase_down
            },
        )
        override val description: Translated = translated(
            when (direction) {
                CaseChangeDirection.Up -> R.string.action_toggleWordCase_desc_up
                CaseChangeDirection.Down -> R.string.action_toggleWordCase_desc_down
            },
        )
    }

    data object ToggleCtrl : Action() {
        override val isModifier: Boolean = true

        override fun visual(keyboardState: KeyboardState): ActionVisual = when {
            keyboardState.modifiers?.ctrl ?: true
                    || keyboardState.modifierSettings?.stickyModifiers?.value ?: false ->
                ActionVisual.Label("ctrl")

            else -> ActionVisual.None
        }

        override fun tint(modifier: ModifierState?): ActionTint = when {
            modifier?.ctrl ?: true -> ActionTint.Active
            else -> ActionTint.Dim
        }

        override val title: Translated = translated(R.string.action_toggleCtrl)
        override val description: Translated = translated(R.string.action_toggleCtrl_desc)
    }

    data object ToggleAlt : Action() {
        override val isModifier: Boolean = true

        override fun visual(keyboardState: KeyboardState): ActionVisual = when {
            keyboardState.modifiers?.alt ?: true
                    || keyboardState.modifierSettings?.stickyModifiers?.value ?: false ->
                ActionVisual.Label("alt")

            else -> ActionVisual.None
        }

        override fun tint(modifier: ModifierState?): ActionTint = when {
            modifier?.alt ?: true -> ActionTint.Active
            else -> ActionTint.Dim
        }

        override val title: Translated = translated(R.string.action_toggleAlt)
        override val description: Translated = translated(R.string.action_toggleAlt_desc)
    }

    data object ToggleZalgo : Action() {
        override val isModifier: Boolean = true

        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.outline_cell_merge_24)

        override val title: Translated = translated(R.string.action_toggleZalgo)
        override val description: Translated = translated(R.string.action_toggleZalgo_desc)

        override fun tint(modifier: ModifierState?): ActionTint = when {
            modifier?.zalgo ?: false -> ActionTint.Active
            else -> ActionTint.Dim
        }
    }

    data object ToggleSelect : Action() {
        override val isModifier: Boolean = true

        override fun visual(keyboardState: KeyboardState): ActionVisual =
            when {
                keyboardState.modifiers?.select ?: false
                    -> ActionVisual.Icon(R.drawable.outline_text_select_end_24)

                else -> ActionVisual.Icon(R.drawable.outline_text_select_start_24)
            }

        override val title: Translated = translated(R.string.action_toggleSelect)
        override val description: Translated = translated(R.string.action_toggleSelect_desc)

        override fun tint(modifier: ModifierState?): ActionTint = when {
            modifier?.select ?: false -> ActionTint.Active
            else -> ActionTint.Normal
        }
    }

    data object Cut : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_content_cut_24)

        override val title: Translated = translated(R.string.action_cut)
        override val description: Translated = translated(R.string.action_cut_desc)
    }

    data object Copy : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_content_copy_24)

        override val title: Translated = translated(R.string.action_copy)
        override val description: Translated = translated(R.string.action_copy_desc)
    }

    data object Paste : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_content_paste_24)

        override val title: Translated = translated(R.string.action_paste)
        override val description: Translated = translated(R.string.action_paste_desc)
    }

    data object Undo : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.outline_undo_24)

        override val title: Translated = translated(R.string.action_undo)
        override val description: Translated = translated(R.string.action_undo_desc)
    }

    data object Redo : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.outline_redo_24)

        override val title: Translated = translated(R.string.action_redo)
        override val description: Translated = translated(R.string.action_redo_desc)
    }

    data object Settings : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_settings_24)

        override val title: Translated = translated(R.string.action_settings)
        override val description: Translated = translated(R.string.action_settings_desc)
    }

    data class SwitchLetterLayer(val direction: SearchDirection) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_language_24)

        override val title: Translated = translated(
            when (direction) {
                SearchDirection.Backwards -> R.string.action_switchLetterLayer_backwards
                SearchDirection.Forwards -> R.string.action_switchLetterLayer_forwards
            },
        )
        override val description: Translated = translated(
            when (direction) {
                SearchDirection.Backwards -> R.string.action_switchLetterLayer_desc_backwards
                SearchDirection.Forwards -> R.string.action_switchLetterLayer_desc_forwards
            },
        )
    }

    data object SwitchSystemKeyboard : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_keyboard_24)

        override val title: Translated = translated(R.string.action_switchSystemKeyboard)
        override val description: Translated = translated(R.string.action_switchSystemKeyboard_desc)
    }

    /**
     * Toggles which layer is "active"
     *
     * Intent: "I want easy access to the opposite layer with my current hand"
     */
    data object ToggleActiveLayer : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_flip_camera_android_24)

        override val title: Translated = translated(R.string.action_toggleActiveLayer)
        override val description: Translated = translated(R.string.action_toggleActiveLayer_desc)

        override fun shift(locale: Locale): Action = ToggleHandedness
    }

    /**
     * Toggles between left- and right-handed mode
     *
     * Intent: "I want to switch the active hand comfortably"
     */
    data object ToggleHandedness : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_flip_camera_android_24)

        override val title: Translated = translated(R.string.action_toggleHandedness)
        override val description: Translated = translated(R.string.action_toggleHandedness_desc)
    }

    /**
     * Toggles whether a numbers layer is "active"
     *
     * Intent: "I want easy access to the numbers layer"
     */
    data object ToggleNumbersLayer : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            when {
                keyboardState.enabledLayers.toggleNumbers?.hasNumbers == false ->
                    ActionVisual.Icon(R.drawable.baseline_abc_24)

                else -> ActionVisual.Icon(R.drawable.baseline_123_24)
            }

        override val title: Translated = translated(R.string.action_toggleNumbersLayer)
        override val description: Translated = translated(R.string.action_toggleNumbersLayer_desc)
    }

    data class AdjustCellHeight(val amount: Float) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual = when {
            amount >= 0 -> ActionVisual.Icon(R.drawable.baseline_zoom_in_24)
            else -> ActionVisual.Icon(R.drawable.baseline_zoom_out_24)
        }

        override val title: Translated = translated(
            when {
                amount >= 0 -> R.string.action_adjustCellHeight_increase
                else -> R.string.action_adjustCellHeight_decrease
            },
        )
        override val description: Translated = translated(
            when {
                amount >= 0 -> R.string.action_adjustCellHeight_desc_increase
                else -> R.string.action_adjustCellHeight_desc_decrease
            },
        )
    }

    data object SelectAll : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_select_all_24)

        override val title: Translated = translated(R.string.action_selectAll)
        override val description: Translated = translated(R.string.action_selectAll_desc)
    }

    data class OpenEmojiKeyboard(val search: Boolean = false) : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_emoji_emotions_24)

        override val title: Translated = translated(
            when {
                search -> R.string.action_emoji_search
                else -> R.string.action_emoji
            },
        )
        override val description: Translated = translated(
            when {
                search -> R.string.action_emoji_desc_search
                else -> R.string.action_emoji_desc
            },
        )

        override fun shift(locale: Locale): Action = OpenEmojiKeyboard(search = true)
    }

    data object OpenClipboardHistory : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_history_edu_24)

        override val title: Translated = translated(R.string.action_clipboardHistory)
        override val description: Translated = translated(R.string.action_clipboardHistory_desc)
    }

    data object ToggleShowSymbols : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.toggle_show_symbols)

        override val title: Translated = translated(R.string.action_toggleShowSymbols)
        override val description: Translated = translated(R.string.action_toggleShowSymbols_desc)

        override fun shift(locale: Locale): Action = ToggleShowLetters
    }

    data object ToggleShowLetters : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.toggle_show_letters)

        override val title: Translated = translated(R.string.action_toggleShowLetters)
        override val description: Translated = translated(R.string.action_toggleShowLetters_desc)
    }

    data object EnableVoiceMode : Action() {
        override fun visual(keyboardState: KeyboardState): ActionVisual =
            ActionVisual.Icon(R.drawable.baseline_mic_24)

        override val title: Translated = translated(R.string.action_enableVoiceMode)
        override val description: Translated = translated(R.string.action_enableVoiceMode_desc)
    }

    companion object {
        val Space = Text(
            " ",
            visualOverride = ActionVisual.Icon(R.drawable.baseline_space_bar_24),
            hidden = { true },
        )
    }
}

enum class FastActionType {
    Delete,
}

enum class ActionClass {
    Letter, Number, Symbol, Other,
}

sealed class ActionVisual {
    data class Icon(val resource: Int, val modifier: Modifier = Modifier) : ActionVisual()
    data class Label(
        val label: String,
        val directionOverride: TextDirection? = null
    ) : ActionVisual()

    data object None : ActionVisual()
}

enum class ActionTint {
    Normal,
    Active,
    Dim,
}

enum class TextBoundary {
    Character,
    Word,
    Line;

    fun breakIterator(): BreakIterator = when (this) {
        Character -> BreakIterator.getCharacterInstance()
        Word -> BreakIterator.getWordInstance()
        Line -> HardLineBreakIterator()
    }
}

enum class TextDirection {
    LeftToRight,
    RightToLeft,
}

enum class SearchDirection(val factor: Int) {
    Backwards(factor = -1),
    Forwards(factor = 1),
}

enum class CaseChangeDirection {
    Up,
    Down;
}

enum class Direction {
    TOP_LEFT, TOP, TOP_RIGHT,
    LEFT, CENTER, RIGHT,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT;

    fun title(): String = when (this) {
        TOP_LEFT -> "Northwest"
        TOP -> "North"
        TOP_RIGHT -> "Northeast"
        LEFT -> "West"
        CENTER -> "Tap"
        RIGHT -> "East"
        BOTTOM_LEFT -> "Southwest"
        BOTTOM -> "South"
        BOTTOM_RIGHT -> "Southeast"
    }

    fun angleFromTop(): Int = when (this) {
        TOP -> 0
        TOP_RIGHT -> 45
        RIGHT -> 90
        BOTTOM_RIGHT -> 135
        BOTTOM -> 180
        BOTTOM_LEFT -> 225
        LEFT -> 270
        TOP_LEFT -> 315
        CENTER -> 0
    }

    fun isCorner(): Boolean = when (this) {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT -> true
        else -> false
    }
}

interface Gesture {
    data class Flick(
        val direction: Direction,
        val longHold: Boolean,
        val longSwipe: Boolean,
        val shift: Boolean
    ) :
        Gesture {
        override fun normalizeToFlick(
            clockwiseCircleBehaviour: ModifierBehaviourOption,
            counterClockwiseCircleBehaviour: ModifierBehaviourOption,
            longHoldBehaviour: ModifierBehaviourOption,
        ): Flick = when {
            longHold -> when (longHoldBehaviour) {
                ModifierBehaviourOption.Digit -> this
                ModifierBehaviourOption.Shift -> copy(longHold = false, shift = true)
            }

            else -> this
        }

        fun resolveAction(key: KeyM): Action? = when {
            longHold -> key.holdAction
            else -> when {
                shift -> key.transientShift?.actions?.get(direction)
                    ?: key.shift?.actions?.get(direction)

                else -> null
            } ?: when {
                longSwipe -> key.longActions[direction]
                else -> null
            } ?: key.actions[direction]
        }
    }

    data class Circle(val direction: CircleDirection) : Gesture {
        override fun normalizeToFlick(
            clockwiseCircleBehaviour: ModifierBehaviourOption,
            counterClockwiseCircleBehaviour: ModifierBehaviourOption,
            longHoldBehaviour: ModifierBehaviourOption,
        ): Flick {
            val circleBehaviour = when (direction) {
                CircleDirection.Clockwise -> clockwiseCircleBehaviour
                CircleDirection.CounterClockwise -> counterClockwiseCircleBehaviour
            }
            return when (circleBehaviour) {
                ModifierBehaviourOption.Digit ->
                    Flick(Direction.CENTER, longHold = true, longSwipe = false, shift = false)

                ModifierBehaviourOption.Shift ->
                    Flick(Direction.CENTER, longHold = false, longSwipe = false, shift = true)
            }
        }
    }

    fun normalizeToFlick(
        clockwiseCircleBehaviour: ModifierBehaviourOption,
        counterClockwiseCircleBehaviour: ModifierBehaviourOption,
        longHoldBehaviour: ModifierBehaviourOption,
    ): Flick

    companion object {
        val Tap =
            Flick(
                direction = Direction.CENTER,
                longHold = false, longSwipe = false, shift = false,
            )

        val names = Direction.entries
            .flatMap {
                listOf(
                    "flick.${it.name}" to Flick(
                        direction = it,
                        longHold = false,
                        longSwipe = false,
                        shift = false,
                    ),
                    "flick.${it.name}.shift" to Flick(
                        direction = it,
                        longHold = false,
                        longSwipe = false,
                        shift = true,
                    ),
                )
            }
            .toMap() +
                CircleDirection.entries.associate { "circle.${it.name}" to Circle(it) }
    }
}

enum class CircleDirection {
    Clockwise,
    CounterClockwise,
}
