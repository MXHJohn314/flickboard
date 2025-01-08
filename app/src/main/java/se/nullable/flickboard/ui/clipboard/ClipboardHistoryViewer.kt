package se.nullable.flickboard.ui.clipboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
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
import se.nullable.flickboard.ui.theme.PageTitle
import se.nullable.flickboard.ui.util.items
import se.nullable.flickboard.ui.util.reverse

@Composable
fun ClipboardHistoryViewer(
    onAction: OnAction,
    snackbarHostState: SnackbarHostState,
    navigateUp: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val entryDao = LocalAppDatabase.current.value.clipboardEntryDao()
    val pinnedEntries = remember(coroutineScope, entryDao) {
        Pager(PagingConfig(pageSize = 10), pagingSourceFactory = { entryDao.getAll(pinned = true) })
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
    LaunchedEffect(undoableDeletedEntries.lastOrNull()) {
        // Specifically not storing entry here, since we don't want to capture a specific value
        // until the user has responded.
        if (undoableDeletedEntries.isNotEmpty()) {
            val snackbarResult = snackbarHostState.showSnackbar(
                "Deleted",
                actionLabel = "Undo",
                withDismissAction = true,
            )
            val entry = undoableDeletedEntries.lastOrNull()
            if (entry != null) {
                if (snackbarResult == SnackbarResult.ActionPerformed) {
                    entryDao.insertOrTouch(entry.copy(generation = entry.generation + 1))
                }
                undoableDeletedEntries.remove(entry)
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = navigateUp,
                        Modifier.padding(8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Return to keyboard")
                    }

                    PageTitle("Clipboard")
                }
            }
            LazyColumn(
                Modifier
                    .height(300.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = listState,
            ) {
                // Anchor for scrolling to the top of the list
                item {}

                clipboardHistoryHeader("Pinned")

                if (pinnedEntries.loadState.isIdle && pinnedEntries.itemCount == 0) {
                    item {
                        ClipboardEntryCard(
                            "You have no pinned items.",
                            onClick = null,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                items(
                    pinnedEntries,
                    key = { it.id },
                    contentType = { "history entry" },
                ) { entry ->
                    ClipboardEntryItem(
                        entry,
                        entryDao = entryDao,
                        onAction = onAction,
                        onEntryDeleted = undoableDeletedEntries::add,
                        snackbarHostState = snackbarHostState,
                    )
                }

                // The header is important not just for visual clarity, but it also provides space
                // where the *previous* item (or the header) is considered the top item, so that if
                // the visible top item is deleted and restored it will reappear within the visible
                // area.
                clipboardHistoryHeader("History")

                if (unpinnedEntries.loadState.isIdle && unpinnedEntries.itemCount == 0) {
                    item {
                        ClipboardEntryCard(
                            "The history is empty, go and copy something!",
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
                        onEntryDeleted = undoableDeletedEntries::add,
                        snackbarHostState = snackbarHostState,
                    )
                }

                clipboardHistoryHeader("Tips")
                val tips = listOf(
                    "Any text copied (while clipboard history is enabled) will be saved here.",
                    "Only the $clipboardHistorySize most recent unpinned items will be kept.",
                    "An item can be inserted by tapping it.",
                    "Items can be pinned, copied, or deleted by swiping them to the side.",
                )
                items(tips) { tip ->
                    ClipboardEntryCard(tip, onClick = null)
                }

                item {
                    Spacer(Modifier.safeDrawingPadding())
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.clipboardHistoryHeader(label: String) {
    stickyHeader {
        Surface(
            Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7F),
        ) { Text(label, Modifier.padding(8.dp)) }
    }
}

@Composable
private fun LazyItemScope.ClipboardEntryItem(
    entry: ClipboardEntry?,
    entryDao: ClipboardEntryDao,
    onAction: OnAction,
    onEntryDeleted: (ClipboardEntry) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val swipeState = key(entry?.id, entry?.generation) {
        rememberSwipeToDismissBoxState()
    }
    SwipeToDismissBox(
        swipeState,
        // We can't show popup windows (like bottom sheets or dropdown menus) from IMEs,
        // so instead, use the swipe-to-dismiss background as a makeshift "menu".
        backgroundContent = {
            AnimatedVisibility(
                swipeState.targetValue != SwipeToDismissBoxValue.Settled,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val currentEntry = rememberUpdatedState(entry)
                Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = when (swipeState.dismissDirection) {
                            SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                            else -> Arrangement.Start.reverse()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // FIXME: SwipeToDismissBox doesn't allow swiping an item *back*, once it
                        //  has been dismissed.
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    swipeState.dismiss(SwipeToDismissBoxValue.Settled)
                                }
                            },
                        ) {
                            Icon(
                                painterResource(R.drawable.baseline_undo_24),
                                "Undo",
                            )
                        }
                        IconButton(
                            onClick = {
                                currentEntry.value?.let {
                                    coroutineScope.launch {
                                        entryDao.setPinned(id = it.id, pinned = !it.pinned)
                                    }
                                    coroutineScope.launch {
                                        swipeState.dismiss(SwipeToDismissBoxValue.Settled)
                                    }
                                }
                            },
                        ) {
                            when {
                                entry?.pinned == true -> Icon(
                                    painterResource(R.drawable.baseline_bookmark_remove_24),
                                    "Unpin",
                                )

                                else -> Icon(
                                    painterResource(R.drawable.baseline_bookmark_add_24),
                                    "Pin",
                                )
                            }
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
                        IconButton(
                            onClick = {
                                currentEntry.value?.let {
                                    clipboardManager.setText(AnnotatedString(it.clipData))
                                    coroutineScope.launch {
                                        swipeState.dismiss(SwipeToDismissBoxValue.Settled)
                                    }
                                    // TODO: does it still make sense to scroll here?
//                                coroutineScope.launch {
//                                    listState.animateScrollToItem(0)
//                                }
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
                    }
                }
            }
        },
        Modifier.animateItem(),
    ) {
        ClipboardEntryCard(
            clipData = entry?.clipData ?: "(loading)",
            onClick = {
                if (entry != null) {
                    onAction.onAction(
                        Action.Text(entry.clipData, hidden = true),
                        key = null,
                        gesture = null,
                    )
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
) {
    OutlinedCard(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Text(clipData, Modifier.padding(16.dp))
    }
}