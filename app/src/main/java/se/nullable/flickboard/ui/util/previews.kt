package se.nullable.flickboard.ui.util

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.LayoutDirection

class PreviewLayoutDirectionProvider : PreviewParameterProvider<LayoutDirection> {
    override val values: Sequence<LayoutDirection> = LayoutDirection.entries.asSequence()
}

@Preview
@Composable
fun PreviewLayoutDirectionProviderPreview(
    @PreviewParameter(PreviewLayoutDirectionProvider::class)
    direction: LayoutDirection,
) {
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Row {
            Text("a")
            Text("b")
            Text("c")
        }
    }
}