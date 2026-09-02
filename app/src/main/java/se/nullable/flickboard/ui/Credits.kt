package se.nullable.flickboard.ui

import android.content.Intent
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import se.nullable.flickboard.R
import se.nullable.flickboard.ui.theme.Typography

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Credits(modifier: Modifier = Modifier) {
    val dependencyInfo = LocalDependencyInfo.current
    LazyColumn(modifier) {
        stickyHeader { CreditHeader(stringResource(R.string.credits_by)) }
        item {
            CreditEntry(
                "Natalie Klestrup Röijezon",
                uri = "https://natkr.com/",
                role = listOf(
                    stringResource(R.string.credits_role_code),
                    stringResource(R.string.credits_role_maintainer),
                ).joinToString(separator = ", "),
            )
        }

        stickyHeader { CreditHeader(stringResource(R.string.credits_contributors)) }
        item {
            CreditEntry(
                "Anarchtism",
                uri = "https://codeberg.org/Anarchtism",
                role = listOf(
                    stringResource(R.string.credits_role_docs),
                    stringResource(R.string.credits_role_testing),
                ).joinToString(separator = ", "),
            )
        }
        item {
            CreditEntry(
                "MisterE",
                uri = "https://codeberg.org/mistere123",
                role = stringResource(R.string.credits_role_logo),
            )
        }
        item {
            CreditEntry(
                "Jesper Holmberg",
                uri = "https://github.com/strindberg",
                role = stringResource(R.string.credits_role_code),
            )
        }
        item {
            CreditEntry(
                "aalong",
                uri = "https://github.com/aalong",
                role = stringResource(R.string.credits_role_code),
            )
        }

        stickyHeader { CreditHeader(stringResource(R.string.credits_role_layout)) }
        item {
            CreditEntry(
                "AhmadMoradDoctor",
                role = stringResource(R.string.letterLayerOption_arabic),
            )
        }
        item {
            CreditEntry(
                "Ali8101",
                uri = "https://github.com/Ali8101",
                role = stringResource(R.string.letterLayerOption_arabic),
            )
        }
        item {
            CreditEntry(
                "ben-igel",
                uri = "https://github.com/ben-igel",
                role = stringResource(R.string.letterLayerOption_germanEsperanto),
            )
        }
        item {
            CreditEntry(
                "rommeswi",
                uri = "https://github.com/rommeswi",
                role = stringResource(R.string.letterLayerOption_german),
            )
        }
        item {
            CreditEntry(
                "hupfdule",
                uri = "https://github.com/hupfdule",
                role = stringResource(R.string.letterLayerOption_german),
            )
        }
        item {
            CreditEntry(
                "Untrusem",
                uri = "https://untrusem.codeberg.page/",
                stringResource(R.string.letterLayerOption_englishThumbKey),
            )
        }
        item {
            CreditEntry(
                "Ariel Kanterewicz",
                uri = "https://github.com/asdkant",
                stringResource(R.string.letterLayerOption_spanish),
            )
        }
        item {
            CreditEntry(
                "aldonator",
                uri = "https://codeberg.org/aldonator",
                stringResource(R.string.letterLayerOption_spanish),
            )
        }
        item {
            CreditEntry(
                "Tirbod",
                uri = "https://github.com/tirbod",
                stringResource(R.string.letterLayerOption_persian),
            )
        }
        item {
            CreditEntry(
                "Eve Redero",
                uri = "https://github.com/everedero",
                stringResource(R.string.letterLayerOption_french),
            )
        }
        item {
            CreditEntry(
                "AntoineÐ",
                uri = "https://github.com/Atn-D",
                stringResource(R.string.letterLayerOption_frenchPunc),
            )
        }
        item {
            CreditEntry(
                "Crazy Unicorn",
                uri = "https://github.com/Crazy-Unicooorn",
                stringResource(R.string.letterLayerOption_frenchExt),
            )
        }
        item {
            CreditEntry(
                "romdudk",
                uri = "https://github.com/romdudk",
                stringResource(R.string.letterLayerOption_hebrew),
            )
        }
        item {
            CreditEntry(
                "allthelazors",
                uri = "https://github.com/allthelazors",
                listOf(
                    stringResource(
                        R.string.letterLayerOption_hungarian,
                        R.string.letterLayerOption_italian,
                        R.string.letterLayerOption_portuguese,
                        R.string.letterLayerOption_turkish,
                    ),
                ).joinToString(separator = ", "),
            )
        }
        item {
            CreditEntry(
                "Petter S. Storvik",
                uri = "https://github.com/allthelazors",
                stringResource(R.string.letterLayerOption_norwegianThumbKey),
            )
        }
        item {
            CreditEntry(
                "rmitura",
                uri = "https://github.com/rmitura",
                stringResource(R.string.letterLayerOption_polishRmitura),
            )
        }
        item {
            CreditEntry(
                "Yuri Khan",
                uri = "https://github.com/yurikhan",
                listOf(
                    stringResource(
                        R.string.letterLayerOption_ukrainian,
                        R.string.letterLayerOption_russian,
                    ),
                ).joinToString(", "),
            )
        }
        item {
            CreditEntry(
                "Vitalii",
                uri = "https://github.com/Crandel",
                stringResource(R.string.letterLayerOption_ukrainianRussian),
            )
        }
        item {
            CreditEntry(
                "Geert Hendrickx",
                uri = "https://geert.hendrickx.be/",
                stringResource(R.string.letterLayerOption_russianPhonetic),
            )
        }
        item {
            CreditEntry(
                "orosmoln",
                uri = "https://github.com/orosmoln",
                stringResource(R.string.letterLayerOption_swedishDe),
            )
        }

        stickyHeader { CreditHeader(stringResource(R.string.credits_specialThanks)) }
        item {
            CreditEntry(
                "ExIdeas",
                uri = "https://www.exideas.com/",
                role = stringResource(R.string.credits_role_exideas),
            )
        }
        item {
            CreditEntry(
                "dessalines",
                uri = "https://github.com/dessalines/thumb-key",
                role = stringResource(R.string.credits_role_thumbKey),
            )
        }

        stickyHeader { CreditHeader(stringResource(R.string.credits_dependencies)) }
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
            Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri())) }
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
            Icon(
                Icons.AutoMirrored.Default.ArrowForward,
                stringResource(R.string.open),
                Modifier.padding(start = 8.dp),
            )
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