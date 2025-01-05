package se.nullable.flickboard.ui.clipboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import se.nullable.flickboard.model.clipboard.clipboardHistorySize
import se.nullable.flickboard.ui.LocalAppDatabase
import se.nullable.flickboard.ui.OnAction
import se.nullable.flickboard.ui.theme.PageTitle
import se.nullable.flickboard.ui.util.directionNames
import se.nullable.flickboard.ui.util.items

@Composable
fun ClipboardHistoryViewer(
    onAction: OnAction,
    snackbarHostState: SnackbarHostState,
    navigateUp: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val entryDao = LocalAppDatabase.current.value.clipboardEntryDao()
    val entries = remember(coroutineScope, entryDao) {
        Pager(PagingConfig(pageSize = 10), pagingSourceFactory = entryDao::getAll)
            .flow.cachedIn(coroutineScope)
    }.collectAsLazyPagingItems()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val directionNames = directionNames()

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
                    entryDao.insertOrTouch(entry)
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

                // The header is important not just for visual clarity, but it also provides space
                // where the *previous* item (or the header) is considered the top item, so that if
                // the visible top item is deleted and restored it will reappear within the visible
                // area.
                clipboardHistoryHeader("History")

                if (entries.loadState.isIdle && entries.itemCount == 0) {
                    item {
                        ClipboardEntryCard(
                            "The history is empty, go and copy something!",
                            onClick = null,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                items(entries, key = { it.id }, contentType = { "history entry" }) { entry ->
                    val swipeState =
                        when {
                            entry == null ->
                                rememberSwipeToDismissBoxState(confirmValueChange = { false })

                            else -> {
                                val currentEntry = rememberUpdatedState(entry)
                                rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        when (it) {
                                            SwipeToDismissBoxValue.Settled -> true

                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                coroutineScope.launch {
                                                    val e = currentEntry.value
                                                    entryDao.delete(e)
                                                    undoableDeletedEntries.add(e)
                                                }
                                                true
                                            }

                                            SwipeToDismissBoxValue.EndToStart -> {
                                                clipboardManager.setText(AnnotatedString(entry.clipData))
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(0)
                                                }
                                                // API 33+ provides its own copy toast, so we don't need to provide one there
                                                // see https://developer.android.com/develop/ui/compose/touch-input/copy-and-paste#feedback_to_copying_content
                                                if (android.os.Build.VERSION.SDK_INT < 33) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            "Copied \"${entry.clipData}\"",
                                                            withDismissAction = true,
                                                        )
                                                    }
                                                }
                                                false // It's not deleted, so return to its original position
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    SwipeToDismissBox(
                        swipeState,
                        backgroundContent = {
                            val colour = animateColorAsState(
                                when (swipeState.targetValue) {
                                    SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
                                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer
                                },
                                label = "swipe background colour",
                            )
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .background(colour.value)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AnimatedVisibility(
                                    swipeState.targetValue == SwipeToDismissBoxValue.StartToEnd,
                                    enter = fadeIn(), exit = fadeOut(),
                                ) {
                                    Icon(
                                        painterResource(R.drawable.baseline_delete_outline_24),
                                        "Delete",
                                    )
                                }
                                Spacer(Modifier.weight(1F))
                                AnimatedVisibility(
                                    swipeState.targetValue == SwipeToDismissBoxValue.EndToStart,
                                    enter = fadeIn(), exit = fadeOut(),
                                ) {
                                    Icon(
                                        painterResource(R.drawable.baseline_content_copy_24),
                                        "Copy",
                                    )
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

                clipboardHistoryHeader("Tips")
                val tips = listOf(
                    "Any text copied (while clipboard history is enabled) will be saved here.",
                    "Only the $clipboardHistorySize most recent items will be kept.",
                    "An item can be inserted by tapping it.",
                    "Items can be deleted from the history by swiping them to the ${directionNames.end}",
                    "Items can be copied by swiping them to the ${directionNames.start}.",
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