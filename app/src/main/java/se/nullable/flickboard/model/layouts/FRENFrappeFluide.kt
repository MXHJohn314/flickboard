package se.nullable.flickboard.model.layouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.Direction
import se.nullable.flickboard.model.KeyM
import se.nullable.flickboard.model.Layer
import se.nullable.flickboard.model.Layout
import se.nullable.flickboard.ui.KeyboardLayoutPreview

val FR_EN_FRAPPEFLUIDE_MAIN_LAYER = Layer(
    keyRows = listOf(
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("u"),
                    Direction.TOP_LEFT to Action.Text("<"),
                    Direction.TOP to Action.Text("["),
                    Direction.TOP_RIGHT to Action.Text("#"),
                    Direction.LEFT to Action.Text("^"),
                    Direction.RIGHT to Action.Text("û"),
                    Direction.BOTTOM_LEFT to Action.Text("ll"),
                    Direction.BOTTOM to Action.Text("b"),
                    Direction.BOTTOM_RIGHT to Action.Text("qu"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("n"),
                    Direction.TOP_LEFT to Action.Text("ff"),
                    Direction.TOP to Action.Text("ï"),
                    Direction.TOP_RIGHT to Action.Text("`"),
                    Direction.LEFT to Action.Text("mm"),
                    Direction.RIGHT to Action.Text("ù"),
                    Direction.BOTTOM_LEFT to Action.Text("d"),
                    Direction.BOTTOM to Action.Text("m"),
                    Direction.BOTTOM_RIGHT to Action.Text("f"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("i"),
                    Direction.TOP_LEFT to Action.Text("@"),
                    Direction.TOP to Action.Text("]"),
                    Direction.TOP_RIGHT to Action.Text(">"),
                    Direction.LEFT to Action.Text("+"),
                    Direction.RIGHT to Action.Text("gh"),
                    Direction.BOTTOM_LEFT to Action.Text("c"),
                    Direction.BOTTOM to Action.Text("î"),
                    Direction.BOTTOM_RIGHT to Action.Text("ô"),
                ),
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("e"),
                    Direction.TOP_LEFT to Action.Text("œ"),
                    Direction.TOP to Action.Text("q"),
                    Direction.TOP_RIGHT to Action.Text("ê"),
                    Direction.LEFT to Action.Text("("),
                    Direction.RIGHT to Action.Text("é"),
                    Direction.BOTTOM_LEFT to Action.Text("\u202F"),
                    Direction.BOTTOM to Action.Text("z"),
                    Direction.BOTTOM_RIGHT to Action.Text("è"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("a"),
                    Direction.TOP_LEFT to Action.Text("k"),
                    Direction.TOP to Action.Text("o"),
                    Direction.TOP_RIGHT to Action.Text("â"),
                    Direction.LEFT to Action.Text("v"),
                    Direction.RIGHT to Action.Text("y"),
                    Direction.BOTTOM_LEFT to Action.Text("g"),
                    Direction.BOTTOM to Action.Text("x"),
                    Direction.BOTTOM_RIGHT to Action.Text("à"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("t"),
                    Direction.TOP_LEFT to Action.Text("ç"),
                    Direction.TOP_RIGHT to Action.Text("/"),
                    Direction.LEFT to Action.Text("h"),
                    Direction.RIGHT to Action.Text(")"),
                    Direction.BOTTOM_LEFT to Action.Text("p"),
                    Direction.BOTTOM_RIGHT to Action.Text("\u00A0"),
                ),
            ),
        ),
        listOf(
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("r"),
                    Direction.TOP_LEFT to Action.Text("·"),
                    Direction.TOP to Action.Text("j"),
                    Direction.TOP_RIGHT to Action.Text("ch"),
                    Direction.LEFT to Action.Text("“"),
                    Direction.RIGHT to Action.Text("="),
                    Direction.BOTTOM_LEFT to Action.Text("« "),
                    Direction.BOTTOM to Action.Text("!"),
                    Direction.BOTTOM_RIGHT to Action.Text("?"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("s"),
                    Direction.TOP_LEFT to Action.Text("pp"),
                    Direction.TOP to Action.Text("oo"),
                    Direction.TOP_RIGHT to Action.Text("-"),
                    Direction.LEFT to Action.Text("*"),
                    Direction.RIGHT to Action.Text(":"),
                    Direction.BOTTOM_LEFT to Action.Text(","),
                    Direction.BOTTOM to Action.Text("."),
                    Direction.BOTTOM_RIGHT to Action.Text(";"),
                ),
            ),
            KeyM(
                actions = mapOf(
                    Direction.CENTER to Action.Text("l"),
                    Direction.TOP_LEFT to Action.Text("’"),
                    Direction.TOP to Action.Text("w"),
                    Direction.TOP_RIGHT to Action.Text("\""),
                    Direction.LEFT to Action.Text("'"),
                    Direction.RIGHT to Action.Text("”"),
                    Direction.BOTTOM_LEFT to Action.Text("…"),
                    Direction.BOTTOM to Action.Text("_"),
                    Direction.BOTTOM_RIGHT to Action.Text(" »"),
                ),
            ),
        ),
        listOf(KEY_SPACE),
    ),
)

val FR_EN_FRAPPEFLUIDE = Layout(
    mainLayer = FR_EN_FRAPPEFLUIDE_MAIN_LAYER,
    controlLayer = CONTROL_MESSAGEASE_LAYER,
)

@Composable
@Preview
fun FrEnFrappeFluidePreview() {
    KeyboardLayoutPreview(layout = Layout(FR_EN_FRAPPEFLUIDE_MAIN_LAYER))
}

@Composable
@Preview
fun FrEnFrappeFluideFullPreview() {
    KeyboardLayoutPreview(layout = FR_EN_FRAPPEFLUIDE)
}