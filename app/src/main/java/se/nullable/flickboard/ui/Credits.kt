package se.nullable.flickboard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.nullable.flickboard.ui.theme.Typography

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Credits(modifier: Modifier = Modifier) {
    val dependencyInfo = LocalDependencyInfo.current
    LazyColumn(modifier) {
        stickyHeader { CreditHeader("By") }
        item {
            CreditEntry(
                "Natalie Klestrup Röijezon",
                uri = "https://natkr.com/",
                role = "Developer",
            )
        }

        stickyHeader { CreditHeader("Contributors") }
        item {
            CreditEntry(
                "Anarchtism",
                uri = "https://codeberg.org/Anarchtism",
                role = "Documentation and Testing",
            )
        }
        item {
            CreditEntry(
                "MisterE",
                uri = "https://codeberg.org/mistere123",
                role = "Logo",
            )
        }
        item {
            CreditEntry(
                "Jesper Holmberg",
                uri = "https://github.com/strindberg",
                role = "Capitalization improvements",
            )
        }
        item {
            CreditEntry("aalong", uri = "https://github.com/aalong", role = "Voice toggle")
        }

        stickyHeader { CreditHeader("Layout Contributors") }
        item {
            CreditEntry("AhmadMoradDoctor", role = "Arabic layout")
        }
        item {
            CreditEntry("Ali8101", uri = "https://github.com/Ali8101", role = "Arabic layout")
        }
        item {
            CreditEntry(
                "ben-igel",
                uri = "https://github.com/ben-igel",
                role = "German/Esperanto layout",
            )
        }
        item {
            CreditEntry(
                "rommeswi",
                uri = "https://github.com/rommeswi",
                role = "German layout",
            )
        }
        item {
            CreditEntry(
                "hupfdule",
                uri = "https://github.com/hupfdule",
                role = "German layout",
            )
        }
        item {
            CreditEntry(
                "moksh",
                uri = "https://moksh.codeberg.page/",
                "English Thumb-Key layout",
            )
        }
        item {
            CreditEntry(
                "Ariel Kanterewicz",
                uri = "https://github.com/asdkant",
                "Spanish layout",
            )
        }
        item {
            CreditEntry(
                "aldonator",
                uri = "https://codeberg.org/aldonator",
                "Spanish/English layout",
            )
        }
        item {
            CreditEntry(
                "Tirbod",
                uri = "https://github.com/tirbod",
                "Farsi layout",
            )
        }
        item {
            CreditEntry(
                "Eve Redero",
                uri = "https://github.com/everedero",
                "French layout",
            )
        }
        item {
            CreditEntry(
                "AntoineÐ",
                uri = "https://github.com/Atn-D",
                "French layout",
            )
        }
        item {
            CreditEntry(
                "Crazy Unicorn",
                uri = "https://github.com/Crazy-Unicooorn",
                "French-extended layout",
            )
        }
        item {
            CreditEntry(
                "romdudk",
                uri = "https://github.com/romdudk",
                "Hebrew layout",
            )
        }
        item {
            CreditEntry(
                "allthelazors",
                uri = "https://github.com/allthelazors",
                "Hungarian, Italian, Portuguese, & Turkish layouts",
            )
        }
        item {
            CreditEntry(
                "Petter S. Storvik",
                uri = "https://github.com/allthelazors",
                "Norwegian layout",
            )
        }
        item {
            CreditEntry(
                "rmitura",
                uri = "https://github.com/rmitura",
                "Polish layout",
            )
        }
        item {
            CreditEntry(
                "Yuri Khan",
                uri = "https://github.com/yurikhan",
                "Ukrainian & Russian layouts, punctuation",
            )
        }
        item {
            CreditEntry(
                "Vitalii",
                uri = "https://github.com/Crandel",
                "Ukrainian/Russian layout",
            )
        }
        item {
            CreditEntry(
                "Geert Hendrickx",
                uri = "https://geert.hendrickx.be/",
                "Russian phonetic layout",
            )
        }
        item {
            CreditEntry(
                "orosmoln",
                uri = "https://github.com/orosmoln",
                "Swedish/German layout",
            )
        }

        stickyHeader { CreditHeader("Special Thanks") }
        item {
            CreditEntry(
                "ExIdeas",
                uri = "https://www.exideas.com/",
                role = "MessagEase concept and layouts",
            )
        }
        item {
            CreditEntry(
                "dessalines",
                uri = "https://github.com/dessalines/thumb-key",
                role = "Thumb-Key layouts",
            )
        }

        stickyHeader { CreditHeader("Third-party components") }
        items(dependencyInfo.value) { dependency ->
            CreditEntry(
                dependency.name,
                uri = dependency.url,
                role = dependency.license,
            )
        }
    }
}

@Composable
fun CreditHeader(label: String) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Text(
            label,
            Modifier.padding(all = 8.dp),
            style = Typography.headlineSmall,
        )
    }
}

@Composable
fun CreditEntry(name: String, uri: String? = null, role: String? = null) {
    val clickModifier = when {
        uri != null -> {
            val context = LocalContext.current
            Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
        }

        else -> Modifier
    }
    Row(clickModifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1F)) {
            Text(name, style = Typography.labelLarge)
            if (role != null) {
                Text(role, Modifier.padding(start = 8.dp), style = Typography.labelMedium)
            }
        }
        if (uri != null) {
            Icon(Icons.AutoMirrored.Default.ArrowForward, "open", Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
@Preview
private fun CreditsPreview() {
    FlickBoardParent {
        Credits()
    }
}