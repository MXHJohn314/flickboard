package se.nullable.flickboard.model.layouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import se.nullable.flickboard.R
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.ActionVisual
import se.nullable.flickboard.model.Direction
import se.nullable.flickboard.model.KeyM
import se.nullable.flickboard.model.Layer
import se.nullable.flickboard.model.Layout
import se.nullable.flickboard.model.SearchDirection
import se.nullable.flickboard.model.ShiftState
import se.nullable.flickboard.model.TextBoundary
import se.nullable.flickboard.ui.KeyboardLayoutPreview

// Medulla: a 4x4 MessagEase-style layout for English, with its controls along the top
// and Space in the middle row. Each key holds its own digit, and U gestures (transientShift)
// type each symbol's companion.
// It places its own symbols and controls, so it doesn't use the shared overlays or the
// control section (see Layout.selfContained).
val EN_MEDULLA_MAIN_LAYER = Layer(
    keyRows = listOf(
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.ToggleActiveLayer,
                ),
            ),
            KeyM(
                actions = mapOf(
                    // A second Backspace: its tap was empty.
                    Direction.CENTER to Action.Delete(),
                    Direction.TOP_LEFT to Action.Escape,
                ),
                holdAction = Action.Settings,
            ),
            KeyM(
                actions = mapOf(
                    Direction.TOP to Action.Copy,
                    Direction.TOP_RIGHT to Action.ToggleEmojiMode,
                    Direction.LEFT to Action.Cut,
                    Direction.RIGHT to Action.ToggleHandedness,
                    Direction.BOTTOM_LEFT to Action.EnableVoiceMode,
                    Direction.BOTTOM to Action.Paste,
                    Direction.BOTTOM_RIGHT to Action.SwitchLetterLayer(SearchDirection.Forwards),
                ),
                transientShift = KeyM(
                    actions = mapOf(
                        Direction.CENTER to Action.SelectAll,
                        Direction.BOTTOM_RIGHT to Action.SwitchSystemKeyboard,
                    ),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Enter(),
                    Direction.TOP_LEFT to Action.ToggleAlt,
                    Direction.TOP_RIGHT to Action.ToggleCtrl,
                ),
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("i"),
                    // Go, as GridKeys puts it when its toolbar is hidden.
                    Direction.BOTTOM to Action.Enter(),
                    Direction.TOP_LEFT to Action.Text("["),
                    Direction.TOP to Action.Text("("),
                    Direction.TOP_RIGHT to Action.Text("{"),
                    Direction.BOTTOM_LEFT to Action.Text(
                        "\n",
                        visualOverride = Action.Enter().visual(null),
                    ),
                    Direction.BOTTOM_RIGHT to Action.Text("f"),
                ),
                holdAction = Action.Text("1"),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("a"),
                    Direction.TOP_LEFT to Action.Text("'"),
                    Direction.TOP to Action.Text("!"),
                    Direction.TOP_RIGHT to Action.Text("/"),
                    Direction.BOTTOM_LEFT to Action.Text("w"),
                    Direction.BOTTOM to Action.Text("y"),
                ),
                transientShift = KeyM(
                    actions = mapOf(
                        Direction.TOP_LEFT to Action.Text("'"),
                        Direction.TOP to Action.Text("¡"),
                        Direction.TOP_RIGHT to Action.Text("—"),
                    ),
                ),
                holdAction = Action.Text("2"),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("n"),
                    Direction.TOP_LEFT to Action.Text("\\"),
                    Direction.TOP to Action.Text("?"),
                    Direction.TOP_RIGHT to Action.Text("\""),
                    Direction.BOTTOM_LEFT to Action.Text("v"),
                    Direction.BOTTOM to Action.Text("p"),
                    Direction.BOTTOM_RIGHT to Action.Text("x"),
                ),
                transientShift = KeyM(
                    actions = mapOf(
                        Direction.TOP_LEFT to Action.Text("|"),
                        Direction.TOP to Action.Text("¿"),
                    ),
                ),
                holdAction = Action.Text("3"),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Delete(),
                    Direction.TOP_LEFT to Action.Text("}"),
                    Direction.TOP to Action.Text(")"),
                    Direction.TOP_RIGHT to Action.Text("]"),
                    Direction.RIGHT to Action.Delete(direction = SearchDirection.Forwards),
                    Direction.BOTTOM_LEFT to Action.Text("b"),
                ),
                transientShift = KeyM(
                    actions = mapOf(
                        Direction.TOP_LEFT to Action.Delete(boundary = TextBoundary.Word),
                        Direction.LEFT to Action.Delete(boundary = TextBoundary.Word),
                        Direction.TOP_RIGHT to Action.Delete(
                            direction = SearchDirection.Forwards,
                            boundary = TextBoundary.Word,
                        ),
                        Direction.RIGHT to Action.Delete(
                            direction = SearchDirection.Forwards,
                            boundary = TextBoundary.Word,
                        ),
                        Direction.BOTTOM_RIGHT to Action.Delete(
                            direction = SearchDirection.Forwards,
                            boundary = TextBoundary.Word,
                        ),
                    ),
                ),
                holdAction = Action.Delete(),
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("t"),
                    // Undo and Redo (Ctrl+Z, Ctrl+Y), as GridKeys puts them when its toolbar is hidden.
                    Direction.LEFT to Action.Text("z", ctrl = true, visualOverride = ActionVisual.Label("↶")),
                    Direction.RIGHT to Action.Text("y", ctrl = true, visualOverride = ActionVisual.Label("↷")),
                    Direction.TOP to Action.ToggleShift(ShiftState.Shift),
                    Direction.TOP_RIGHT to Action.Text("~"),
                    Direction.BOTTOM to Action.ToggleShift(ShiftState.Normal),
                    Direction.BOTTOM_LEFT to Action.Text("<"),
                    Direction.BOTTOM_RIGHT to Action.Text("_"),
                ),
                transientShift = KeyM(
                    actions = mapOf(
                        Direction.BOTTOM_RIGHT to Action.Text("¬"),
                    ),
                ),
                holdAction = Action.Text("4"),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text(" "),
                    Direction.TOP_LEFT to Action.Text(";"),
                    Direction.TOP to Action.Text("h"),
                    Direction.TOP_RIGHT to Action.Text(":"),
                    Direction.LEFT to Action.Text("c"),
                    Direction.RIGHT to Action.Text("m"),
                    Direction.BOTTOM_LEFT to Action.Text(","),
                    Direction.BOTTOM to Action.Text("g"),
                    Direction.BOTTOM_RIGHT to Action.Text("."),
                ),
                transientShift = KeyM(
                    actions = mapOf(
                        Direction.TOP_LEFT to Action.Text(";"),
                        Direction.TOP_RIGHT to Action.Text(","),
                        Direction.BOTTOM_LEFT to Action.Text(","),
                        Direction.BOTTOM_RIGHT to Action.Text("…"),
                    ),
                ),
                colspan = 2,
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("r"),
                    Direction.TOP_LEFT to Action.Text("q"),
                    Direction.TOP to Action.ToggleShift(ShiftState.Shift),
                    Direction.TOP_RIGHT to Action.Text(
                        "\t",
                        forceRawKeyEvent = true,
                        visualOverride = ActionVisual.Icon(R.drawable.baseline_keyboard_tab_24),
                    ),
                    Direction.RIGHT to Action.Text("`"),
                    Direction.BOTTOM to Action.ToggleShift(ShiftState.Normal),
                    Direction.BOTTOM_LEFT to Action.Text("*"),
                    Direction.BOTTOM_RIGHT to Action.Text(">"),
                    Direction.LEFT to Action.Text("|"),
                ),
                transientShift = KeyM(
                    actions = mapOf(
                        Direction.RIGHT to Action.Text("\\"),
                        Direction.BOTTOM_LEFT to Action.Text("†"),
                    ),
                ),
                holdAction = Action.Text("0"),
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("d"),
                    Direction.TOP to Action.JumpLineKeepPos(SearchDirection.Backwards),
                    Direction.TOP_RIGHT to Action.Text("j"),
                    Direction.LEFT to Action.Jump(SearchDirection.Backwards),
                    Direction.RIGHT to Action.Jump(SearchDirection.Forwards),
                    Direction.BOTTOM_LEFT to Action.Text("="),
                    Direction.BOTTOM to Action.JumpLineKeepPos(SearchDirection.Forwards),
                    Direction.BOTTOM_RIGHT to Action.Text("+"),
                ),
                transientShift = KeyM(
                    actions = mapOf(
                        Direction.BOTTOM_LEFT to Action.Text("±"),
                        Direction.BOTTOM_RIGHT to Action.Text("×"),
                    ),
                ),
                holdAction = Action.Text("7"),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("e"),
                    Direction.TOP to Action.Text("u"),
                    Direction.BOTTOM_LEFT to Action.Text("-"),
                    Direction.BOTTOM to Action.Text("@"),
                    Direction.BOTTOM_RIGHT to Action.Text("#"),
                ),
                transientShift = KeyM(
                    actions = mapOf(
                        Direction.BOTTOM_LEFT to Action.Text("+"),
                    ),
                ),
                holdAction = Action.Text("8"),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("o"),
                    Direction.TOP_LEFT to Action.Text("z"),
                    Direction.TOP to Action.Text("l"),
                    Direction.BOTTOM_LEFT to Action.Text("$"),
                    Direction.BOTTOM to Action.Text("%"),
                    Direction.BOTTOM_RIGHT to Action.Text("^"),
                ),
                transientShift = KeyM(
                    actions = mapOf(
                        Direction.BOTTOM_LEFT to Action.Text("¢"),
                        Direction.BOTTOM to Action.Text("‰"),
                    ),
                ),
                holdAction = Action.Text("9"),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("s"),
                    Direction.TOP_LEFT to Action.Text("k"),
                    Direction.TOP to Action.JumpLineKeepPos(SearchDirection.Backwards),
                    Direction.LEFT to Action.Jump(SearchDirection.Backwards),
                    Direction.RIGHT to Action.Jump(SearchDirection.Forwards),
                    Direction.BOTTOM_LEFT to Action.Text("&"),
                    Direction.BOTTOM to Action.JumpLineKeepPos(SearchDirection.Forwards),
                ),
                holdAction = Action.Text("0"),
            ),
        ),
    ),
)

val EN_MEDULLA = Layout(
    mainLayer = EN_MEDULLA_MAIN_LAYER,
    controlLayer = CONTROL_MESSAGEASE_LAYER,
    selfContained = true,
)

@Composable
@Preview
fun EnMedullaKeyboardPreview() {
    KeyboardLayoutPreview(layout = Layout(EN_MEDULLA_MAIN_LAYER, selfContained = true))
}

@Composable
@Preview
fun EnMedullaFullKeyboardPreview() {
    KeyboardLayoutPreview(layout = EN_MEDULLA, showAllModifiers = true)
}
