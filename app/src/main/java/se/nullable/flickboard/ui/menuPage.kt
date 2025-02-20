package se.nullable.flickboard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.nullable.flickboard.R

@Composable
private fun MenuPageItem(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
fun MenuPageLink(
    onClick: () -> Unit,
    icon: Painter?,
    label: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    Box(modifier.clickable(onClick = onClick)) {
        MenuPageItem {
            if (icon != null) {
                Icon(icon, contentDescription = null, iconModifier.padding(end = 8.dp))
            }
            Text(label, Modifier.weight(1F))
            Icon(Icons.AutoMirrored.Default.ArrowForward, null, Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun MenuPageUriLink(
    uri: String,
    icon: Painter? = null,
    label: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    MenuPageLink(
        onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        },
        icon = icon,
        label = label,
        modifier = modifier,
        iconModifier = iconModifier,
    )
}

@Composable
@Preview
fun MenuPageLinkPreview() {
    Surface {
        Column {
            MenuPageLink(
                onClick = {},
                icon = painterResource(R.drawable.baseline_image_search_24),
                label = "Section",
            )
            MenuPageLink(
                onClick = {},
                icon = null,
                label = "Section",
            )
        }
    }
}