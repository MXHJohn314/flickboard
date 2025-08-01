package se.nullable.flickboard.model.layouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.Direction
import se.nullable.flickboard.model.KeyM
import se.nullable.flickboard.model.Layer
import se.nullable.flickboard.model.Layout
import se.nullable.flickboard.ui.KeyboardLayoutPreview

val GE_MESSAGEASE_MAIN_LAYER = Layer(
    keyRows = listOf(
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("ა"),
                    Direction.BOTTOM_RIGHT to Action.Text("ვ"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("ნ"),
                    Direction.RIGHT to Action.Text("ჩ"),
                    Direction.LEFT to Action.Text("ძ"),
                    Direction.BOTTOM to Action.Text("ლ"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("ი"),
                    Direction.BOTTOM_LEFT to Action.Text("ხ"),
                ),
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.TOP to Action.Text("ჭ"),
                    Direction.CENTER to Action.Text("ჰ"),
                    Direction.RIGHT to Action.Text("კ"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("ო"),
                    Direction.TOP_LEFT to Action.Text("ქ"),
                    Direction.TOP to Action.Text("უ"),
                    Direction.TOP_RIGHT to Action.Text("პ"),
                    Direction.LEFT to Action.Text("ც"),
                    Direction.RIGHT to Action.Text("ბ"),
                    Direction.BOTTOM_LEFT to Action.Text("გ"),
                    Direction.BOTTOM to Action.Text("დ"),
                    Direction.BOTTOM_RIGHT to Action.Text("ჯ"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("რ"),
                    Direction.LEFT to Action.Text("მ"),
                    Direction.BOTTOM_LEFT to Action.Text("ჟ"),
                ),
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("ტ"),
                    Direction.TOP to Action.Text("თ"),
                    Direction.TOP_RIGHT to Action.Text("ყ"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("ე"),
                    Direction.TOP to Action.Text("წ"),
                    Direction.RIGHT to Action.Text("ზ"),
                    Direction.LEFT to Action.Text("ღ"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("ს"),
                    Direction.TOP_LEFT to Action.Text("ფ"),
                    Direction.LEFT to Action.Text("შ"),
                ),
            ),
        ),
        listOf(KEY_SPACE),
    ),
)

val GE_MESSAGEASE = Layout(
    mainLayer = GE_MESSAGEASE_MAIN_LAYER,
    controlLayer = CONTROL_MESSAGEASE_LAYER,
)

@Composable
@Preview
fun GeKeyboardPreview() {
    KeyboardLayoutPreview(layout = Layout(GE_MESSAGEASE_MAIN_LAYER))
}

@Composable
@Preview
fun GeFullKeyboardPreview() {
    KeyboardLayoutPreview(layout = GE_MESSAGEASE, showAllModifiers = true)
}
