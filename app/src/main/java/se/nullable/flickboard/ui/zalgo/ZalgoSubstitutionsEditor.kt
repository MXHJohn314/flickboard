package se.nullable.flickboard.ui.zalgo

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
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
import se.nullable.flickboard.ui.util.PreviewLayoutDirectionProvider
import se.nullable.flickboard.ui.util.modify

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZalgoSubstitutionsEditor(
    navigationIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val manager = LocalZalgoManager.current.value
    val substitutions = manager.substitutions.collectAsState(emptyList())
    val composingNew = remember { mutableStateOf(false) }
    Scaffold(
        modifier,
        topBar = {
            TopAppBar(
                title = { Text("Zalgo Macros") },
                navigationIcon = { navigationIcon() },
                actions = {
                    IconButton({ composingNew.value = true; println("hiiii") }) {
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
            substitutions.value,
            saveSubstitution = { manager.substitutionDao.upsert(it) },
            deleteSubstitution = { manager.substitutionDao.deleteOrRevert(it) },
            composingNew = composingNew.value,
            closeNew = { composingNew.value = false },
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun ZalgoSubstitutionList(
    substitutions: List<ZalgoSubstitution>,
    saveSubstitution: suspend (ZalgoSubstitution) -> Unit,
    deleteSubstitution: suspend (ZalgoSubstitution) -> Unit,
    composingNew: Boolean,
    closeNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    LaunchedEffect(composingNew) {
        if (composingNew) {
            listState.animateScrollToItem(0)
        }
    }
    LazyColumn(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        state = listState,
    ) {
        if (composingNew) {
            item(contentType = "new") {
                Card(
                    Modifier
                        .animateItem()
                        .padding(horizontal = 8.dp),
                ) {
                    ZalgoSubstitutionEditor(
                        ZalgoSubstitution("", "", isDefault = true),
                        onSave = {
                            coroutineScope.launch {
                                saveSubstitution(it)
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

        items(substitutions, key = { it.from }) { substitution ->
            val editing = remember { mutableStateOf(false) }
            Card(
                Modifier
                    .animateItem()
                    .padding(horizontal = 8.dp),
            ) {
                AnimatedContent(editing.value) { isEditing ->
                    when {
                        isEditing -> ZalgoSubstitutionEditor(
                            substitution,
                            onSave = {
                                coroutineScope.launch {
                                    saveSubstitution(it)
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

                        else -> ZalgoSubstitutionViewer(
                            substitution,
                            onEdit = { editing.value = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZalgoSubstitutionViewer(
    substitution: ZalgoSubstitution,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.padding(start = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val zalgoIconId = "join"
            val fromWithIcons = remember(substitution.from) {
                buildAnnotatedString {
                    substitution.from.forEachIndexed { i, chr ->
                        if (i > 0) {
                            appendInlineContent(zalgoIconId, alternateText = " ")
                        }
                        append(chr)
                    }
                }
            }
            Text(
                fromWithIcons,
                inlineContent = mapOf(
                    zalgoIconId to InlineTextContent(
                        Placeholder(
                            width = 1.em,
                            height = 1.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                    ) {
                        Icon(
                            painterResource(R.drawable.outline_cell_merge_24),
                            contentDescription = "and",
                            Modifier.fillMaxSize(),
                        )
                    },
                ),
            )
            Icon(
                painterResource(R.drawable.outline_arrow_right_alt_24),
                contentDescription = "becomes",
            )
            Text(
                substitution.to,
            )
        }
        Row {
            IconButton(onClick = { onEdit() }) {
                Icon(
                    painterResource(R.drawable.baseline_edit_24),
                    contentDescription = "Edit",
                )
            }
        }
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
                    substitutions = listOf(
                        // WE GOT THE..
                        ZalgoSubstitution("ae", "æ"),
                        ZalgoSubstitution("o/", "ø"),
                        ZalgoSubstitution("a0", "å"),
                    ),
                    saveSubstitution = {},
                    deleteSubstitution = {},
                    composingNew = true,
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
    val fromFocusRequester = FocusRequester()
    Column(modifier) {
        TextField(
            edited.value.from,
            onValueChange = { edited.modify { copy(from = it) } },
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
            onValueChange = { edited.modify { copy(to = it) } },
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
                ZalgoSubstitution("ae", "æ"),
                onSave = {},
                onCancel = {},
                onDelete = {},
            )
        }
    }
}