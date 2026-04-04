package se.nullable.flickboard.model.layouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.Direction
import se.nullable.flickboard.model.KeyM
import se.nullable.flickboard.model.Layer
import se.nullable.flickboard.model.Layout
import se.nullable.flickboard.ui.KeyboardLayoutPreview

val BG_HLONRNO_MAIN_LAYER = Layer(
    keyRows = listOf(
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("н"),
                    Direction.TOP to Action.Text("ѝ"),
                    Direction.TOP_RIGHT to Action.Text("ь"),
                    Direction.BOTTOM_RIGHT to Action.Text("ф"),
                )
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("и"),
                    Direction.BOTTOM to Action.Text("д"),
                )
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("е"),
                    Direction.TOP_LEFT to Action.Text("ю"),
                    Direction.TOP_RIGHT to Action.Text("э"),
                    Direction.BOTTOM_LEFT to Action.Text("з"),
                    Direction.BOTTOM_RIGHT to Action.Text("ы"),
                )
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("о"),
                    Direction.TOP to Action.Text("х"),
                    Direction.LEFT to Action.Text("п"),
                )
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("а"),
                    Direction.TOP_LEFT to Action.Text("ц"),
                    Direction.TOP to Action.Text("у"),
                    Direction.TOP_RIGHT to Action.Text("я"),
                    Direction.LEFT to Action.Text("г"),
                    Direction.RIGHT to Action.Text("б"),
                    Direction.BOTTOM_LEFT to Action.Text("м"),
                    Direction.BOTTOM to Action.Text("ж"),
                    Direction.BOTTOM_RIGHT to Action.Text("к"),
                )
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("р"),
                    Direction.RIGHT to Action.Text("ч"),
                )
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("с"),
                    Direction.TOP_RIGHT to Action.Text("й"),
                )
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("т"),
                    Direction.TOP to Action.Text("л"),
                    Direction.LEFT to Action.Text("щ"),
                    Direction.RIGHT to Action.Text("ъ"),
                )
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("в"),
                    Direction.TOP_LEFT to Action.Text("ш"),
                )
            ),
        ),
        listOf(KEY_SPACE),
    ),
)

val BG_HLONRNO = Layout(
    mainLayer = BG_HLONRNO_MAIN_LAYER,
    controlLayer = CONTROL_MESSAGEASE_LAYER,
)

@Composable
@Preview
private fun BgHlonrnoKeyboardPreview() {
    KeyboardLayoutPreview(layout = Layout(BG_HLONRNO_MAIN_LAYER))
}

@Composable
@Preview
private fun BgHlonrnoFullKeyboardPReview() {
    KeyboardLayoutPreview(layout = BG_HLONRNO, showAllModifiers = true)
}