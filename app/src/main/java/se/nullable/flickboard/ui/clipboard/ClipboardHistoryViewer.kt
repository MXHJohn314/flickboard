package se.nullable.flickboard.ui.clipboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import se.nullable.flickboard.R
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.clipboard.ClipboardEntry
import se.nullable.flickboard.model.clipboard.ClipboardEntryDao
import se.nullable.flickboard.model.clipboard.clipboardHistorySize
import se.nullable.flickboard.ui.LocalAppDatabase
import se.nullable.flickboard.ui.OnAction
import se.nullable.flickboard.ui.layout.SwipeToOptionsBox
import se.nullable.flickboard.ui.layout.centerVerticallyInVisibleBounds
import se.nullable.flickboard.ui.layout.rememberSwipeToOptionsBoxState
import se.nullable.flickboard.ui.layout.reverse
import se.nullable.flickboard.ui.settings.LocalAppSettings
import se.nullable.flickboard.ui.theme.PageTitle
import se.nullable.flickboard.ui.util.defaultFocus
import se.nullable.flickboard.ui.util.items
import se.nullable.flickboard.ui.util.replaceWith
import java.time.Instant

@Composable
fun ClipboardHistoryViewer(
    onAction: OnAction,
    snackbarHostState: SnackbarHostState,
    navigateUp: () -> Unit,
) {
    val appSettings = LocalAppSettings.current
    val clipboardHistoryEnabled = appSettings.saveClipboardHistory.state
    val showPinned = appSettings.clipboardHistoryShowPinned.state
    val showHistory = appSettings.clipboardHistoryShowHistory.state
    val showTips = appSettings.clipboardHistoryShowTips.state

    val coroutineScope = rememberCoroutineScope()
    val entryDao = LocalAppDatabase.current.value.clipboardEntryDao()
    val pinnedEntries = remember(coroutineScope, entryDao) {
        Pager(
            PagingConfig(pageSize = 10),
            pagingSourceFactory = { entryDao.getAll(pinned = true) },
        )
            .flow.cachedIn(coroutineScope)
    }.collectAsLazyPagingItems()
    val unpinnedEntries = remember(coroutineScope, entryDao) {
        Pager(
            PagingConfig(pageSize = 10),
            pagingSourceFactory = { entryDao.getAll(pinned = false) },
        )
            .flow.cachedIn(coroutineScope)
    }.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    val undoableDeletedEntries = remember { mutableStateListOf<ClipboardEntry>() }
    LaunchedEffect(undoableDeletedEntries.lastOrNull(), undoableDeletedEntries.size) {
        // Specifically not storing entry here, since we don't want to capture a specific value
        // until the user has responded.
        if (undoableDeletedEntries.isNotEmpty()) {
            val snackbarResult = snackbarHostState.showSnackbar(
                "Deleted",
                actionLabel = "Undo",
                withDismissAction = true,
            )
            if (snackbarResult == SnackbarResult.ActionPerformed) {
                undoableDeletedEntries.forEach {
                    entryDao.insertOrTouch(it.copy(generation = it.generation + 1))
                }
            }
            undoableDeletedEntries.clear()
        }
    }

    val addingNewEntry = remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Return to keyboard")
                    }

                    PageTitle("Clipboard", Modifier.weight(1F))

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                undoableDeletedEntries.clear()
                                undoableDeletedEntries.addAll(
                                    entryDao.clearAndGetUnpinned(),
                                )
                            }
                        },
                        enabled = unpinnedEntries.itemCount > 0,
                    ) {
                        Icon(painterResource(R.drawable.outline_delete_sweep_24), "Delete all")
                    }

                    IconButton(onClick = { addingNewEntry.value = true }) {
                        Icon(painterResource(R.drawable.baseline_add_24), "Add")
                    }
                }
            }

            AnimatedVisibility(addingNewEntry.value) {
                ClipboardEntryCard(
                    "", onClick = null,
                    modifier = Modifier.padding(vertical = 8.dp),
                    editing = true,
                    onEditDone = { newClipData ->
                        addingNewEntry.value = false
                        if (newClipData != null && newClipData != "") {
                            coroutineScope.launch {
                                entryDao.insertOrTouch(
                                    ClipboardEntry(
                                        id = 0,
                                        lastAddedOn = Instant.now(),
                                        clipData = newClipData,
                                        pinned = true,
                                    ),
                                )
                            }
                        }
                    },
                )
            }

            LazyColumn(
                Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    // Elements outside of the list's bounds end up causing flashing forward
                    // briefly before animating out when removed from the view.
                    // In practice, this happens when collapsing a section while the middle of that
                    // section is rendered.
                    .clipToBounds(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = listState,
            ) {
                // Anchor for scrolling to the top of the list
                item {}

                if (pinnedEntries.itemCount > 0) {
                    clipboardHistoryHeader(
                        "Pinned",
                        expanded = showPinned.value,
                        onSetExpanded = appSettings.clipboardHistoryShowPinned::currentValue::set,
                    )
                }

                if (showPinned.value) {
                    items(
                        pinnedEntries,
                        key = { it.id },
                        contentType = { "history entry" },
                    ) { entry ->
                        ClipboardEntryItem(
                            entry,
                            entryDao = entryDao,
                            onAction = onAction,
                            onEntryDeleted = undoableDeletedEntries::replaceWith,
                            snackbarHostState = snackbarHostState,
                            // Don't allow unpinning when clipboard history is disabled,
                            // since it would introduce unpinned history items.
                            allowPinToggle = clipboardHistoryEnabled.value,
                        )
                    }
                }

                // The header is important not just for visual clarity, but it also provides space
                // where the *previous* item (or the header) is considered the top item, so that if
                // the visible top item is deleted and restored it will reappear within the visible
                // area.
                clipboardHistoryHeader(
                    "History",
                    expanded = showHistory.value,
                    onSetExpanded = appSettings.clipboardHistoryShowHistory::currentValue::set,
                )

                if (showHistory.value) {
                    if (unpinnedEntries.loadState.isIdle && unpinnedEntries.itemCount == 0) {
                        item {
                            ClipboardEntryCard(
                                when {
                                    clipboardHistoryEnabled.value ->
                                        "The history is empty, go and copy something!"

                                    else -> "Clipboard history is disabled."
                                },
                                onClick = null,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    items(
                        unpinnedEntries,
                        key = { it.id },
                        contentType = { "history entry" },
                    ) { entry ->
                        ClipboardEntryItem(
                            entry,
                            entryDao = entryDao,
                            onAction = onAction,
                            onEntryDeleted = undoableDeletedEntries::replaceWith,
                            snackbarHostState = snackbarHostState,
                            allowPinToggle = true,
                        )
                    }
                }

                clipboardHistoryHeader(
                    "Tips",
                    expanded = showTips.value,
                    onSetExpanded = appSettings.clipboardHistoryShowTips::currentValue::set,
                )
                if (showTips.value) {
                    val tips = listOf(
                        "Any text copied (while clipboard history is enabled) will be saved here.",
                        "Only the $clipboardHistorySize most recent unpinned items will be kept.",
                        "An item can be inserted by tapping it.",
                        "Items can be pinned, copied, or deleted by swiping them to the side.",
                    )
                    items(tips) { tip ->
                        ClipboardEntryCard(tip, onClick = null, Modifier.animateItem())
                    }
                }

                item {
                    Spacer(Modifier.safeDrawingPadding())
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.clipboardHistoryHeader(
    label: String,
    expanded: Boolean,
    onSetExpanded: (expanded: Boolean) -> Unit,
) {
    stickyHeader(key = label, contentType = "header") {
        Surface(
            Modifier
                .fillMaxWidth()
                .animateItem()
                .toggleable(expanded, onValueChange = onSetExpanded, role = Role.Checkbox),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7F),
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, Modifier.padding(8.dp))
                Spacer(Modifier.weight(1F))
                val hideIconAngle = animateFloatAsState(
                    when {
                        expanded -> 180F
                        else -> 0F
                    },
                    label = "hideIconAngle",
                )
                Icon(
                    painterResource(R.drawable.baseline_arrow_drop_down_24),
                    when {
                        expanded -> "Hide"
                        else -> "Show"
                    },
                    Modifier.rotate(hideIconAngle.value),
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.ClipboardEntryItem(
    entry: ClipboardEntry?,
    entryDao: ClipboardEntryDao,
    onAction: OnAction,
    onEntryDeleted: (ClipboardEntry) -> Unit,
    snackbarHostState: SnackbarHostState,
    allowPinToggle: Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val swipeState = key(entry?.id, entry?.generation) {
        rememberSwipeToOptionsBoxState()
    }
    val editing = remember { mutableStateOf(false) }
    if (swipeState.currentValue != SwipeToDismissBoxValue.Settled) {
        // Pin when in a non-default state, so that we don't lose the state when scrolled offscreen.
        val pinnable = LocalPinnableContainer.current
        DisposableEffect(pinnable) {
            val pinHandle = pinnable?.pin()
            onDispose {
                pinHandle?.release()
            }
        }
    }
    val swipeMarginOffset = 32.dp
    SwipeToOptionsBox(
        swipeState,
        swipeMarginOffset = swipeMarginOffset,
        allowSwipeGesture = !editing.value,
        // We can't show popup windows (like bottom sheets or dropdown menus) from IMEs,
        // so instead, use the swipe-to-dismiss background as a makeshift "menu".
        backgroundContent = {
            // Use Modifier.alpha instead of AnimatedVisibility to sync the animation state with the
            // swipe.
            if (swipeState.backgroundProgress > 0F) {
                Box(Modifier.alpha(swipeState.backgroundProgress)) {
                    val currentEntry = rememberUpdatedState(entry)
                    Surface(
                        Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .padding(swipeState.swipeDirectionPadding(swipeMarginOffset - 8.dp))
                                .centerVerticallyInVisibleBounds(),
                            horizontalArrangement = when (swipeState.swipeDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                                else -> Arrangement.Start.reverse()
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {
                                    currentEntry.value?.let {
                                        clipboardManager.setText(AnnotatedString(it.clipData))
                                        coroutineScope.launch {
                                            swipeState.animateReset()
                                        }
                                        // API 33+ provides its own copy toast, so we don't need to provide one there
                                        // see https://developer.android.com/develop/ui/compose/touch-input/copy-and-paste#feedback_to_copying_content
                                        if (android.os.Build.VERSION.SDK_INT < 33) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Copied \"${it.clipData}\"",
                                                    withDismissAction = true,
                                                )
                                            }
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    painterResource(R.drawable.baseline_content_copy_24),
                                    "Copy",
                                )
                            }
                            IconButton(
                                enabled = allowPinToggle,
                                onClick = {
                                    currentEntry.value?.let {
                                        coroutineScope.launch {
                                            entryDao.setPinned(id = it.id, pinned = !it.pinned)
                                        }
                                        coroutineScope.launch {
                                            swipeState.animateReset()
                                        }
                                    }
                                },
                            ) {
                                when {
                                    entry?.pinned == true -> Icon(
                                        painterResource(R.drawable.outline_keep_off_24),
                                        "Unpin",
                                    )

                                    else -> Icon(
                                        painterResource(R.drawable.outline_keep_24),
                                        "Pin",
                                    )
                                }
                            }
                            Spacer(Modifier.weight(1F))
                            IconButton(
                                onClick = {
                                    editing.value = true
                                    coroutineScope.launch { swipeState.animateReset() }
                                },
                            ) {
                                Icon(
                                    painterResource(R.drawable.baseline_edit_24),
                                    "Edit",
                                )
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        currentEntry.value?.let {
                                            entryDao.delete(it)
                                            onEntryDeleted(it)
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    painterResource(R.drawable.baseline_delete_outline_24),
                                    "Delete",
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier.animateItem(),
    ) {
        ClipboardEntryCard(
            clipData = entry?.clipData ?: "(loading)",
            onClick = {
                if (entry != null) {
                    onAction.onAction(
                        Action.Text(entry.clipData, hidden = { true }),
                        key = null,
                        gesture = null,
                    )
                }
            },
            editing = editing.value,
            onEditDone = { newClipData ->
                editing.value = false
                if (newClipData != null && entry != null) {
                    coroutineScope.launch {
                        entryDao.setClipData(entry.id, newClipData)
                    }
                }
            },
        )
    }
}

@Composable
private fun ClipboardEntryCard(
    clipData: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    onEditDone: ((String?) -> Unit) = {},
) {
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
    AnimatedContent(editing, modifier, label = "entry card state") { isEditing ->
        when {
            isEditing -> OutlinedCard(cardModifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val editedClipData = remember {
                        mutableStateOf(
                            TextFieldValue(
                                clipData,
                                selection = TextRange(clipData.length),
                            ),
                        )
                    }
                    TextField(
                        editedClipData.value,
                        onValueChange = editedClipData::value::set,
                        Modifier
                            .weight(1F)
                            .defaultFocus(),
                        maxLines = 4,
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onEditDone(
                                    editedClipData.value.text,
                                )
                            },
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    Row(
                        Modifier.centerVerticallyInVisibleBounds(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { onEditDone(null) }) {
                            Icon(painterResource(R.drawable.baseline_clear_24), "Cancel")
                        }
                        IconButton(onClick = { onEditDone(editedClipData.value.text) }) {
                            Icon(painterResource(R.drawable.baseline_check_24), "Save")
                        }
                    }
                }
            }


            else -> OutlinedCard(
                onClick = { onClick?.invoke() },
                enabled = onClick != null,
                modifier = cardModifier,
            ) {
                Text(clipData, Modifier.padding(16.dp))
            }
        }
    }
}