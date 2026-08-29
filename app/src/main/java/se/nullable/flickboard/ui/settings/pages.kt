package se.nullable.flickboard.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.nullable.flickboard.BuildConfig
import se.nullable.flickboard.MainActivitySharedElement
import se.nullable.flickboard.R
import se.nullable.flickboard.model.FieldType
import se.nullable.flickboard.ui.ConfiguredKeyboard
import se.nullable.flickboard.ui.FlickBoardParent
import se.nullable.flickboard.ui.MenuPageLink
import se.nullable.flickboard.ui.MenuPageUriLink
import se.nullable.flickboard.ui.excludeFromBottomInset
import se.nullable.flickboard.ui.help.OnboardingPrompt
import se.nullable.flickboard.ui.theme.Title
import se.nullable.flickboard.ui.util.modify

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsHomePage(
    onNavigateToSection: (SettingsSection) -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateToBetaMenu: () -> Unit,
    onNavigateToCredits: () -> Unit,
    onNavigateToDescriber: () -> Unit,
    onNavigateToZalgoEditor: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val tryText = remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    Column(modifier) {
        LazyColumn(Modifier.weight(1F)) {
            item {
                OnboardingPrompt()
            }
            item {
                MenuPageLink(
                    onClick = onNavigateToTutorial,
                    icon = painterResource(R.drawable.baseline_checklist_24),
                    label = "Tutorial",
                )
            }
            item {
                MenuPageLink(
                    onClick = onNavigateToDescriber,
                    icon = painterResource(R.drawable.baseline_help_24),
                    label = "What Does This Do?",
                )
            }
            item {
                Title("Settings")
            }
            items(AppSettings.all, key = { it.key }) { section ->
                if (!section.hidden) {
                    MenuPageLink(
                        onClick = { onNavigateToSection(section) },
                        icon = painterResource(section.icon),
                        label = section.label,
                    )
                }
            }
            item {
                MenuPageLink(
                    onClick = onNavigateToZalgoEditor,
                    icon = painterResource(R.drawable.outline_cell_merge_24),
                    label = "Zalgo Macros",
                )
            }
            item {
                TextField(
                    value = tryText.value,
                    onValueChange = { tryText.value = it },
                    label = {
                        Text(
                            "Type here to try FlickBoard",
                            Modifier.testTag("testPad"),
                        )
                    },
                    textStyle = TextStyle(textDirection = TextDirection.Content),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                )
            }
            item {
                Title("About")
                // These constants will depend on the build config
                @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
                val variant = when {
                    BuildConfig.BUILD_TYPE == "release" && (BuildConfig.FLAVOR == "plain" || BuildConfig.FLAVOR == "screengrab") -> ""
                    else -> " (${BuildConfig.BUILD_TYPE})"
                }
                val hiddenBetaTaps = remember { mutableIntStateOf(0) }
                val betaMenuHintToast = remember { Toast.makeText(context, "", Toast.LENGTH_SHORT) }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val tapsToOpenBetaMenu = 7
                            when (val taps = ++hiddenBetaTaps.intValue) {
                                tapsToOpenBetaMenu -> {
                                    hiddenBetaTaps.intValue = 0
                                    betaMenuHintToast.cancel()
                                    onNavigateToBetaMenu()
                                }

                                in 4..tapsToOpenBetaMenu -> {
                                    betaMenuHintToast.cancel()
                                    betaMenuHintToast.setText("${tapsToOpenBetaMenu - taps} more taps to open the seeeecret beta menu")
                                    betaMenuHintToast.show()
                                }
                            }
                        },
                ) {
                    Text(
                        "FlickBoard v${BuildConfig.VERSION_NAME}$variant",
                        modifier = Modifier.padding(8.dp),
                    )
                }
                // These constants will depend on the build config
                @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
                if (BuildConfig.FLAVOR == "beta") {
                    MenuPageLink(
                        onClick = { onNavigateToBetaMenu() },
                        icon = painterResource(R.drawable.baseline_bug_report_24),
                        label = "Beta Options",
                    )
                }
                MenuPageLink(
                    onClick = { onNavigateToCredits() },
                    icon = painterResource(R.drawable.baseline_people_24),
                    label = "Credits",
                )
                MenuPageUriLink(
                    uri = "https://zulip.flickboard.natkr.com",
                    icon = painterResource(R.drawable.baseline_chat_24),
                    label = "Discuss on Zulip",
                )
                MenuPageUriLink(
                    uri = "https://codeberg.org/natkr/flickboard",
                    icon = painterResource(id = R.drawable.baseline_code_24),
                    label = "View Source on Codeberg",
                )
                MenuPageUriLink(
                    uri = "https://codeberg.org/natkr/flickboard/issues",
                    icon = painterResource(id = R.drawable.baseline_bug_report_24),
                    label = "Report Bugs on Codeberg",
                )
            }
        }
        SettingsKeyboardPreview(sharedTransitionScope, animatedVisibilityScope)
    }

}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsSectionPage(
    section: SettingsSection,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .weight(1F),
        ) {
            section.settings.forEach { setting ->
                when (setting) {
                    is SettingMeta.Bool -> BoolSetting(setting)
                    is SettingMeta.Integer -> {} // Not rendered right now, implement if used anywhere
                    is SettingMeta.Text -> TextSetting(setting)
                    is SettingMeta.FloatSlider -> FloatSliderSetting(setting)
                    is SettingMeta.EnumList<*> -> EnumListSetting(setting)
                    is SettingMeta.Enum -> EnumSetting(setting)
                    is SettingMeta.Image -> ImageSetting(setting)
                    is SettingMeta.Colour -> ColourSetting(setting)
                }
            }
        }
        SettingsKeyboardPreview(sharedTransitionScope, animatedVisibilityScope)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsKeyboardPreview(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val enable = localAppSetting { it.enableKeyboardPreview }
    with(sharedTransitionScope) {
        Box {
            Column {
                val realKeyboardVisible = WindowInsets.isImeVisible
                val softwareKeyboardController = LocalSoftwareKeyboardController.current
                Surface(
                    Modifier
                        .clickable {
                            when {
                                realKeyboardVisible -> softwareKeyboardController?.hide()
                                else -> enable.modify { !it }
                            }
                        }
                        .sharedElement(
                            rememberSharedContentState(MainActivitySharedElement.SettingsKeyboardPreviewHeader.toString()),
                            animatedVisibilityScope,
                        ),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when {
                            realKeyboardVisible -> {
                                Text(
                                    text = "Keyboard active",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(8.dp),
                                )
                                Icon(
                                    painterResource(R.drawable.baseline_keyboard_hide_24),
                                    "close",
                                    Modifier
                                        .padding(8.dp),
                                )
                            }

                            else -> {
                                Text(
                                    text = "Preview keyboard",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(8.dp),
                                )
                                val hideIconAngle = animateFloatAsState(
                                    when {
                                        enable.value -> 0F
                                        else -> 180F
                                    },
                                    label = "hideIconAngle",
                                )
                                Icon(
                                    painterResource(R.drawable.baseline_arrow_drop_down_24),
                                    when {
                                        enable.value -> "hide"
                                        else -> "show"
                                    },
                                    Modifier
                                        .padding(8.dp)
                                        .rotate(hideIconAngle.value),
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    enable.value,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Box {
                        ConfiguredKeyboard(
                            onAction = { _, _, _ -> true }, // Keyboard provides internal visual feedback if enabled
                            fieldType = FieldType.Regular,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clipToBounds()
                                .excludeFromBottomInset()
                                // sharedElement can't encompass AnimatedVisibility, or the size animation breaks
                                .sharedElement(
                                    rememberSharedContentState(MainActivitySharedElement.SettingsKeyboardPreview.toString()),
                                    animatedVisibilityScope,
                                ),
                        )
                        @Suppress("RemoveRedundantQualifierName")
                        androidx.compose.animation.AnimatedVisibility(
                            realKeyboardVisible,
                            Modifier.matchParentSize(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Box(
                                Modifier
                                    .background(MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.9F))
                                    // Block tap input for convered nodes
                                    .pointerInput(Unit) {},
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
@Preview
private fun SettingsHomePreview() {
    FlickBoardParent {
        Surface {
            SharedTransitionLayout {
                AnimatedVisibility(true) {
                    SettingsHomePage(
                        onNavigateToSection = {},
                        onNavigateToTutorial = {},
                        onNavigateToBetaMenu = {},
                        onNavigateToCredits = {},
                        onNavigateToDescriber = {},
                        onNavigateToZalgoEditor = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
@Preview
private fun SettingsSectionPagePreview() {
    FlickBoardParent {
        Surface {
            SharedTransitionLayout {
                AnimatedVisibility(true) {
                    SettingsSectionPage(
                        AppSettings.all[0],
                        this@SharedTransitionLayout,
                        this@AnimatedVisibility,
                        Modifier.width(1000.dp),
                    )
                }
            }
        }
    }
}

