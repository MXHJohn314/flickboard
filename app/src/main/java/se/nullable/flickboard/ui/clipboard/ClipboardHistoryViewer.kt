package se.nullable.flickboard.ui.clipboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import se.nullable.flickboard.R
import se.nullable.flickboard.model.clipboard.ClipboardEntry
import se.nullable.flickboard.model.clipboard.clipboardHistorySize
import se.nullable.flickboard.ui.LocalAppDatabase
import se.nullable.flickboard.ui.OnAction
import se.nullable.flickboard.ui.settings.LocalAppSettings
import se.nullable.flickboard.ui.settings.localAppSetting
import se.nullable.flickboard.ui.theme.PageTitle
import se.nullable.flickboard.ui.util.collapsibleListHeader
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
    val settingsCtx = appSettings.ctx
    val clipboardHistoryEnabled = localAppSetting { it.saveClipboardHistory }
    val showPinned = localAppSetting { it.clipboardHistoryShowPinned }
    val showHistory = localAppSetting { it.clipboardHistoryShowHistory }
    val showTips = localAppSetting { it.clipboardHistoryShowTips }

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
                    collapsibleListHeader(
                        "Pinned",
                        expanded = showPinned.value,
                        onSetExpanded = settingsCtx[appSettings.clipboardHistoryShowPinned]::currentValue::set,
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
                collapsibleListHeader(
                    "History",
                    expanded = showHistory.value,
                    onSetExpanded = settingsCtx[appSettings.clipboardHistoryShowHistory]::currentValue::set,
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

                collapsibleListHeader(
                    "Tips",
                    expanded = showTips.value,
                    onSetExpanded = settingsCtx[appSettings.clipboardHistoryShowTips]::currentValue::set,
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

