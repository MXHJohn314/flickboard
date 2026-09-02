package se.nullable.flickboard.ui.emoji

import android.annotation.SuppressLint
import android.icu.lang.UCharacter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.content.res.getStringOrThrow
import androidx.core.content.res.use
import se.nullable.flickboard.R
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.emoji.EmojiCategory
import se.nullable.flickboard.model.emoji.EmojiGroup
import se.nullable.flickboard.model.emoji.EmojiList
import se.nullable.flickboard.ui.LocalEmojiChecker
import se.nullable.flickboard.ui.LocalEmojiDb
import se.nullable.flickboard.ui.LocalEmojiTrie
import se.nullable.flickboard.ui.OnAction
import se.nullable.flickboard.ui.settings.localAppSetting
import se.nullable.flickboard.ui.util.defaultFocus
import se.nullable.flickboard.util.unique
import androidx.emoji2.emojipicker.R as Emoji2R

@Composable
fun EmojiKeyboard(
    onAction: OnAction,
    navigateUp: () -> Unit,
    defaultTab: EmojiTab? = null,
) {
    val saveHistory = localAppSetting { it.saveEmojiHistory }
    val emojiHistory = localAppSetting { it.emojiHistory }
    val emojis = emojiList()
    val searchQuery = remember { mutableStateOf("") }
    val selectedTab = remember(emojis) {
        mutableStateOf(
            defaultTab ?: when {
                emojiHistory.value.isBlank() -> EmojiTab.Category(0)
                else -> EmojiTab.Recent
            },
        )
    }

    val tabScrollState = rememberScrollState()
    val emojiSize = 32.dp
    val emojiPadding = 8.dp
    val emojiItemSize = emojiSize + emojiPadding * 3
    val emojiSizeSp = with(LocalDensity.current) { emojiSize.toSp() }
    val emojiTextModifier = Modifier.padding(emojiPadding)
    val emojiTextStyle = LocalTextStyle.current.merge(
        fontSize = emojiSizeSp,
        textAlign = TextAlign.Center,
    )
    Column {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = navigateUp,
                    Modifier.padding(8.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.returnToKeyboard),
                    )
                }
                // Ideally this would be a ScrollableTabRow, but it enforces a massive minimum width
                Row(
                    modifier = Modifier
                        .weight(1F)
                        .horizontalScroll(tabScrollState),
                ) {
                    NarrowTab(
                        selected = selectedTab.value == EmojiTab.Search,
                        onClick = { selectedTab.value = EmojiTab.Search },
                        icon = {
                            Icon(
                                painterResource(R.drawable.baseline_search_24),
                                contentDescription = stringResource(R.string.emoji_search),
                                Modifier.padding(8.dp),
                            )
                        },
                    )
                    if (saveHistory.value) {
                        NarrowTab(
                            selected = selectedTab.value == EmojiTab.Recent,
                            onClick = { selectedTab.value = EmojiTab.Recent },
                            icon = {
                                Icon(
                                    painterResource(R.drawable.baseline_history_24),
                                    contentDescription = stringResource(R.string.emoji_history),
                                    Modifier.padding(8.dp),
                                )
                            },
                        )
                    } else {
                        if (selectedTab.value == EmojiTab.Recent) {
                            selectedTab.value = EmojiTab.Category(0)
                        }
                    }
                    emojis.categories.forEachIndexed { index, emojiCategory ->
                        NarrowTab(
                            selected = (selectedTab.value as? EmojiTab.Category)?.index == index,
                            onClick = { selectedTab.value = EmojiTab.Category(index) },
                            icon = {
                                Icon(
                                    painterResource(emojiCategory.iconId),
                                    contentDescription = emojiCategory.name,
                                    Modifier.padding(8.dp),
                                )
                            },
                        )
                    }
                }
                Button(
                    onClick = { onAction.onAction(Action.Delete(), key = null, gesture = null) },
                    Modifier.padding(8.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.baseline_backspace_24),
                        stringResource(R.string.action_delete_backwardsCharacter),
                    )
                }
            }
        }

        if (selectedTab.value == EmojiTab.Search) {
            TextField(
                searchQuery.value,
                searchQuery::value::set,
                Modifier
                    .fillMaxWidth()
                    .defaultFocus(),
                label = { Text(stringResource(R.string.emoji_search)) },
                singleLine = true,
                keyboardActions = KeyboardActions { this.defaultKeyboardAction(ImeAction.Done) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }

        val emojiChecker = LocalEmojiChecker.current.value
        val tabEmojis = when (val tab = selectedTab.value) {
            is EmojiTab.Category -> emojis.categories[tab.index].emojis

            EmojiTab.Search -> {
                val emojiDb = LocalEmojiDb.current.value
                LocalEmojiTrie.current.value.matches(searchQuery.value)
                    .map(emojiDb::groupForBaseEmoji)
            }

            EmojiTab.Recent -> {
                val emojiDb = LocalEmojiDb.current.value
                // Don't reshuffle recents while it's still visible
                remember {
                    emojiHistory.value.split('\n')
                        .filter { it.isNotBlank() }
                        .map(emojiDb::groupForBaseEmoji)
                }
            }
        }.filter { emojiChecker.hasEmoji(it.baseVariant) }
        val emojiGridColumns = GridCells.Adaptive(emojiItemSize)
        LazyVerticalGrid(
            columns = emojiGridColumns,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            items(tabEmojis) { emoji ->
                // dragAndDropSource modifier seems to break normal automatic updating
                key(emoji) {
                    Box {
                        val primaryVariant = emoji.baseVariant
                        val hasSecondaryVariants = emoji.variants != null
                        val showVariantPopup = remember { mutableStateOf(false) }
                        if (showVariantPopup.value) {
                            Popup(
                                Alignment.TopEnd,
                                onDismissRequest = { showVariantPopup.value = false },
                            ) {
                                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                                    LazyVerticalGrid(emojiGridColumns) {
                                        items(emoji.variants?.value ?: emptyList()) { variant ->
                                            EmojiVariant(
                                                variant,
                                                onAction = onAction,
                                                onClick = { showVariantPopup.value = false },
                                                onLongClick = null,
                                                emojiTextStyle = emojiTextStyle,
                                                emojiTextModifier = emojiTextModifier,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        EmojiVariant(
                            primaryVariant,
                            onAction = onAction,
                            onClick = {},
                            onLongClick = { showVariantPopup.value = true }
                                .takeIf { hasSecondaryVariants },
                            emojiTextStyle = emojiTextStyle,
                            emojiTextModifier = emojiTextModifier,
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiVariant(
    variant: String,
    onAction: OnAction,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    emojiTextStyle: TextStyle,
    emojiTextModifier: Modifier,
) {
    val saveHistory = localAppSetting { it.saveEmojiHistory }
    val emojiHistory = localAppSetting { it.emojiHistory }
    Box(
        Modifier
            .combinedClickable(onLongClick = onLongClick) {
                if (saveHistory.value) {
                    emojiHistory.value =
                        "$variant\n${
                            emojiHistory.value
                                // Remove duplicates
                                .replace("$variant\n", "")
                        }"
                            // Limit history length
                            .let { history ->
                                history
                                    .findAnyOf(listOf("\n"), startIndex = 50)
                                    ?.let { (i) -> history.substring(0..i) }
                                    ?: history
                            }
                }
                onAction.onAction(
                    Action.Text(variant),
                    key = null,
                    gesture = null,
                )
                onClick()
            },
    ) {
        if (onLongClick != null) {
            Icon(
                painterResource(R.drawable.corner),
                stringResource(R.string.emoji_hasVariants),
                Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .size(12.dp)
                    .padding(2.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            variant,
            style = emojiTextStyle,
            modifier = emojiTextModifier,
        )
    }
}

@Composable
fun NarrowTab(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        icon = {
            Box {
                icon()
                if (selected) {
                    TabRowDefaults.SecondaryIndicator(Modifier.align(Alignment.BottomCenter))
                }
            }
        },
        modifier = Modifier.width(IntrinsicSize.Max),
    )
}

@Composable
@Preview
fun EmojiKeyboardPreview() {
    EmojiKeyboard(onAction = { _, _, _ -> true }, navigateUp = {})
}

sealed class EmojiTab {
    data object Search : EmojiTab()
    data object Recent : EmojiTab()
    data class Category(val index: Int) : EmojiTab()
}

@SuppressLint("PrivateResource")
@Composable
fun emojiList(): EmojiList {
    // Piggyback off the emoji list from emoji2-emojipicker,
    // but reimplement it to use Compose and control rendering.
    // Compose doesn't currently support loading non-string arrays, so we need to drop down to Ye Olde
    // context/resources API.
    val resources = LocalResources.current
    return remember(resources) {
        val categoryIconIds =
            resources.obtainTypedArray(Emoji2R.array.emoji_categories_icons)
                .use { icons ->
                    IntArray(icons.length(), icons::getResourceIdOrThrow)
                }
        val categoryNames =
            resources.obtainTypedArray(Emoji2R.array.category_names)
                .use { names ->
                    Array(names.length(), names::getStringOrThrow)
                }

        val categories =
            resources.obtainTypedArray(Emoji2R.array.emoji_by_category_raw_resources_gender_inclusive)
                .use { categories ->
                    List(categories.length()) { i ->
                        val categoryId = categories.getResourceIdOrThrow(i)
                        val categoryCsv = resources.openRawResource(categoryId).use {
                            it.readBytes().decodeToString()
                        }
                        EmojiCategory(
                            name = categoryNames[i],
                            iconId = categoryIconIds[i],
                            emojis = categoryCsv
                                .split('\n')
                                .filter { it.isNotEmpty() }
                                .map { variantsStr ->
                                    val variants = variantsStr.split(',').unique()
                                    val codePoint = UCharacter.codePointAt(variants.first(), 0)
                                    EmojiGroup(
                                        names = listOfNotNull(
                                            UCharacter.getName(codePoint),
                                            UCharacter.getNameAlias(codePoint),
                                        ),
                                        baseVariant = variants[0],
                                        variants = variants.takeIf { it.size > 1 }?.let(::lazyOf),
                                    )
                                },
                        )
                    }
                }
        EmojiList(categories)
    }
}