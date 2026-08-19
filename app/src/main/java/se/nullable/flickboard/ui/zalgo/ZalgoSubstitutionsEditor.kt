package se.nullable.flickboard.ui.zalgo

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.launch
import se.nullable.flickboard.R
import se.nullable.flickboard.model.zalgo.ZalgoSubstitution
import se.nullable.flickboard.ui.FlickBoardParent
import se.nullable.flickboard.ui.LocalZalgoManager
import se.nullable.flickboard.ui.settings.localAppSetting
import se.nullable.flickboard.ui.util.PreviewLayoutDirectionProvider
import se.nullable.flickboard.ui.util.collapsibleListHeader
import se.nullable.flickboard.ui.util.modify
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZalgoSubstitutionsEditor(
    navigationIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val manager = LocalZalgoManager.current.value
    val customSubstitutions = manager.customSubstitutions.collectAsState(emptyList())
    val unshadowedDefaultSubstitutions = remember {
        val shadowedFroms = derivedStateOf {
            customSubstitutions.value.mapTo(mutableSetOf()) { it.from }
        }
        derivedStateOf {
            manager.defaultSubstitutions.filterNot { shadowedFroms.value.contains(it.from) }
        }
    }
    val composingNewId = remember { mutableStateOf<UUID?>(null) }
    Scaffold(
        modifier,
        topBar = {
            TopAppBar(
                title = { Text("Zalgo Macros") },
                navigationIcon = { navigationIcon() },
                actions = {
                    IconButton({ composingNewId.modify { it ?: UUID.randomUUID() } }) {
                        Icon(
                            painterResource(R.drawable.baseline_add_24),
                            contentDescription = "New",
                        )
                    }
                },
            )
        },
    ) { padding ->
        ZalgoSubstitutionList(
            customSubstitutions = customSubstitutions.value,
            defaultSubstitutions = unshadowedDefaultSubstitutions.value,
            saveSubstitution = { new, old -> manager.substitutionDao.upsert(new, old) },
            deleteSubstitution = { manager.substitutionDao.deleteOrRevert(it) },
            composingNewId = composingNewId.value,
            closeNew = { composingNewId.value = null },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        )
    }
}

@Composable
fun ZalgoSubstitutionList(
    customSubstitutions: List<ZalgoSubstitution>,
    defaultSubstitutions: List<ZalgoSubstitution>,
    saveSubstitution: suspend (new: ZalgoSubstitution, old: ZalgoSubstitution?) -> Unit,
    deleteSubstitution: suspend (ZalgoSubstitution) -> Unit,
    composingNewId: UUID?,
    closeNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showCustom = localAppSetting { it.zalgoSubstitutionsShowCustom }
    val showDefault = localAppSetting { it.zalgoSubstitutionsShowDefault }
    val showTips = localAppSetting { it.zalgoSubstitutionsShowTips }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    LaunchedEffect(composingNewId) {
        if (composingNewId != null) {
            listState.animateScrollToItem(0)
        }
    }

    val tips = remember {
        listOf(
            AnnotatedString("You can use Zalgo macros to enter long texts or type otherwise-missing characters."),
            buildAnnotatedString {
                append("Macros always have two characters.\n")
                append("To use a macro, type the first character, then ")
                appendInlineContent(ZalgoInlineContent.zalgoIconId, "the zalgo character")
                append(", and then the second character.")
            },
            buildAnnotatedString {
                appendInlineContent(ZalgoInlineContent.zalgoIconId, "Zalgo")
                append(" mode can also be used to combine arbitrary accents, like G")
                appendInlineContent(ZalgoInlineContent.zalgoIconId, "+")
                append("¨.")
            },
        )
    }

    LazyColumn(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        state = listState,
    ) {
        // Spacer item, compose tends to work better when the first item isn't moving around
        item {}

        collapsibleListHeader(
            "Custom",
            expanded = showCustom.value,
            onSetExpanded = { showCustom.value = it },
        )

        if (composingNewId != null) {
            item(key = composingNewId) {
                Card(
                    Modifier
                        .animateItem()
                        .padding(horizontal = 8.dp),
                ) {
                    val substitution = remember {
                        ZalgoSubstitution("", "", composingNewId, isDefault = true)
                    }
                    ZalgoSubstitutionEditor(
                        substitution,
                        onSave = {
                            coroutineScope.launch {
                                saveSubstitution(it, null)
                                closeNew()
                            }
                        },
                        onCancel = { closeNew() },
                        onDelete = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )

                }
            }
        }

        if (showCustom.value) {
            items(customSubstitutions, key = { it.visualId }) { substitution ->
                EditableZalgoSubstitution(
                    substitution,
                    saveSubstitution = saveSubstitution,
                    deleteSubstitution = deleteSubstitution,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        collapsibleListHeader(
            "Built-in",
            expanded = showDefault.value,
            onSetExpanded = { showDefault.value = it },
        )

        if (showDefault.value) {
            items(defaultSubstitutions, key = { it.visualId }) { substitution ->
                EditableZalgoSubstitution(
                    substitution,
                    saveSubstitution = saveSubstitution,
                    deleteSubstitution = deleteSubstitution,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        collapsibleListHeader(
            "Tips",
            expanded = showTips.value,
            onSetExpanded = { showTips.value = it },
        )

        if (showTips.value) {
            items(tips, key = { it.hashCode() }, contentType = { "tip" }) {
                ZalgoTipCard(it, Modifier.animateItem())
            }
        }
    }
}

@Composable
fun EditableZalgoSubstitution(
    substitution: ZalgoSubstitution,
    saveSubstitution: suspend (old: ZalgoSubstitution, new: ZalgoSubstitution) -> Unit,
    deleteSubstitution: suspend (ZalgoSubstitution) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val editing = remember { mutableStateOf(false) }
    Box(modifier.padding(horizontal = 8.dp)) {
        AnimatedContent(editing.value) { isEditing ->
            when {
                isEditing -> Card {
                    ZalgoSubstitutionEditor(
                        substitution,
                        onSave = {
                            coroutineScope.launch {
                                saveSubstitution(it, substitution)
                                editing.value = false
                            }
                        },
                        onCancel = { editing.value = false },
                        onDelete = {
                            coroutineScope.launch {
                                deleteSubstitution(substitution)
                                editing.value = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                }

                else -> Card(onClick = { editing.value = true }) {
                    ZalgoSubstitutionViewer(
                        substitution,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ZalgoSubstitutionViewer(
    substitution: ZalgoSubstitution,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlowRow(
            Modifier
                .weight(1F)
                .padding(end = 4.dp)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            val fromWithIcons = remember(substitution.from) {
                buildAnnotatedString {
                    substitution.from.forEachIndexed { i, chr ->
                        if (i > 0) {
                            appendInlineContent(ZalgoInlineContent.zalgoIconId, alternateText = " ")
                        }
                        append(chr)
                    }
                }
            }
            ElevatedCard(shape = MaterialTheme.shapes.extraSmall) {
                Text(
                    fromWithIcons,
                    Modifier.padding(4.dp),
                    inlineContent = ZalgoInlineContent.inlineContent,
                )
            }
            Icon(
                painterResource(R.drawable.outline_arrow_right_alt_24),
                contentDescription = "becomes",
            )
            ElevatedCard(shape = MaterialTheme.shapes.extraSmall) {
                Text(
                    substitution.to,
                    Modifier.padding(4.dp),
                )
            }
        }
        Icon(
            painterResource(R.drawable.baseline_edit_24),
            contentDescription = "Edit",
        )
    }
}

@Preview(widthDp = 200)
@Composable
fun ZalgoSubstitutionListPreview(
    @PreviewParameter(PreviewLayoutDirectionProvider::class)
    direction: LayoutDirection
) {
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        FlickBoardParent {
            Surface(Modifier.safeDrawingPadding()) {
                ZalgoSubstitutionList(
                    customSubstitutions = listOf(
                        ZalgoSubstitution(
                            "aa",
                            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                            remember { UUID.randomUUID() },
                        ),
                        // WE GOT THE..
                        ZalgoSubstitution("ae", "æ", remember { UUID.randomUUID() }),
                        ZalgoSubstitution("o/", "ø", remember { UUID.randomUUID() }),
                        ZalgoSubstitution("a0", "å", remember { UUID.randomUUID() }),
                    ),
                    defaultSubstitutions = listOf(
                        ZalgoSubstitution("øø", "Ø", remember { UUID.randomUUID() }),
                    ),
                    saveSubstitution = { _, _ -> },
                    deleteSubstitution = {},
                    composingNewId = remember { UUID.randomUUID() },
                    closeNew = {},
                )
            }
        }
    }
}

@Composable
fun ZalgoSubstitutionEditor(
    substitution: ZalgoSubstitution,
    onSave: (ZalgoSubstitution) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val edited = remember { mutableStateOf(substitution) }
    val validFrom = edited.value.from.length == 2
    val fromFocusRequester = remember { FocusRequester() }
    Column(modifier) {
        TextField(
            edited.value.from,
            onValueChange = { edited.modify { old -> old.copy(from = it) } },
            label = { Text("Macro (must be 2 characters)") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(fromFocusRequester),
            singleLine = true,
            isError = !validFrom,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        TextField(
            edited.value.to,
            onValueChange = { edited.modify { old -> old.copy(to = it) } },
            label = { Text("Replacement") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.End) {
            Spacer(Modifier.weight(1F))
            IconButton({ onDelete() }, enabled = !substitution.isDefault) {
                Icon(
                    painterResource(R.drawable.baseline_delete_outline_24),
                    "Delete",
                )
            }
            IconButton({ onCancel() }) {
                Icon(
                    painterResource(R.drawable.baseline_clear_24),
                    "Cancel",
                )
            }
            IconButton(
                {
                    when {
                        !validFrom -> fromFocusRequester.requestFocus()
                        else -> onSave(edited.value)
                    }
                },
            ) {
                Icon(
                    painterResource(R.drawable.baseline_check_24),
                    "Save",
                )
            }
        }
    }
}

@Preview(widthDp = 200)
@Composable
fun ZalgoSubstitutionEditorPreview() {
    FlickBoardParent {
        Surface(Modifier.safeDrawingPadding()) {
            ZalgoSubstitutionEditor(
                ZalgoSubstitution("ae", "æ", remember { UUID.randomUUID() }),
                onSave = {},
                onCancel = {},
                onDelete = {},
            )
        }
    }
}

@Composable
fun ZalgoTipCard(tip: AnnotatedString, modifier: Modifier = Modifier) {
    Card(
        onClick = {}, enabled = false,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
    ) {
        Text(
            tip, Modifier.padding(8.dp),
            inlineContent = ZalgoInlineContent.inlineContent,
        )
    }
}

@Preview(widthDp = 200)
@Composable
fun ZalgoTipCardPreview() {
    FlickBoardParent {
        Surface(Modifier.safeDrawingPadding()) {
            ZalgoTipCard(AnnotatedString("Did you know that FlickBoard will try to display tips here and there?"))
        }
    }
}

private object ZalgoInlineContent {
    const val zalgoIconId = "join"
    val inlineContent = mapOf(
        zalgoIconId to InlineTextContent(
            Placeholder(
                width = 1.em,
                height = 1.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            ),
        ) {
            Icon(
                painterResource(R.drawable.outline_cell_merge_24),
                contentDescription = "zalgo",
                Modifier.fillMaxSize(),
            )
        },
    )
}