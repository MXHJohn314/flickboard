package se.nullable.flickboard.model.layouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.Direction
import se.nullable.flickboard.model.KeyM
import se.nullable.flickboard.model.Layer
import se.nullable.flickboard.model.Layout
import se.nullable.flickboard.ui.KeyboardLayoutPreview

val ES_CA_MESSAGEASE_MAIN_LAYER = Layer(
    keyRows = listOf(
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("a"),
                    Direction.LEFT to Action.Text("à"),
                    Direction.BOTTOM to Action.Text("á"),
                    Direction.BOTTOM_RIGHT to Action.Text("v"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("n"),
                    Direction.TOP to Action.Text("ñ"),
                    Direction.BOTTOM to Action.Text("l"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("i"),
                    Direction.TOP_LEFT to Action.Text("í"),
                    Direction.RIGHT to Action.Text("ï"),
                    Direction.BOTTOM_LEFT to Action.Text("x"),
                ),
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("d"),
                    Direction.TOP to Action.Text("ú"),
                    Direction.BOTTOM to Action.Text("ç"),
                    Direction.RIGHT to Action.Text("k"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("o"),
                    Direction.TOP_LEFT to Action.Text("q"),
                    Direction.TOP to Action.Text("u"),
                    Direction.TOP_RIGHT to Action.Text("p"),
                    Direction.LEFT to Action.Text("c"),
                    Direction.RIGHT to Action.Text("b"),
                    Direction.BOTTOM_LEFT to Action.Text("g"),
                    Direction.BOTTOM to Action.Text("m"),
                    Direction.BOTTOM_RIGHT to Action.Text("j"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("r"),
                    Direction.LEFT to Action.Text("h"),
                ),
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("t"),
                    Direction.TOP_LEFT to Action.Text("è"),
                    Direction.TOP to Action.Text("ú"),
                    Direction.TOP_RIGHT to Action.Text("y"),
                    Direction.LEFT to Action.Text("ò"),
                    Direction.BOTTOM to Action.Text("ü"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("e"),
                    Direction.TOP to Action.Text("w"),
                    Direction.LEFT to Action.Text("é"),
                    Direction.RIGHT to Action.Text("z"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("s"),
                    Direction.TOP_LEFT to Action.Text("f"),
                    Direction.RIGHT to Action.Text("ó"),
                ),
            ),
        ),
        listOf(KEY_SPACE),
    ),
)

val ES_CA_MESSAGEASE = Layout(
    mainLayer = ES_CA_MESSAGEASE_MAIN_LAYER,
    controlLayer = CONTROL_MESSAGEASE_LAYER,
)

@Composable
@Preview
fun EsCaKeyboardPreview() {
    KeyboardLayoutPreview(layout = Layout(ES_CA_MESSAGEASE_MAIN_LAYER))
}

@Composable
@Preview
fun EsCaFullKeyboardPreview() {
    KeyboardLayoutPreview(layout = ES_CA_MESSAGEASE)
}
