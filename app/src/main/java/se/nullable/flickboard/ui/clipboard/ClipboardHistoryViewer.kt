package se.nullable.flickboard.ui.clipboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import se.nullable.flickboard.R
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.ui.LocalAppDatabase
import se.nullable.flickboard.ui.OnAction
import se.nullable.flickboard.ui.theme.BodyPlaceholder

@Composable
fun ClipboardHistoryViewer(
    onAction: OnAction,
    snackbarHostState: SnackbarHostState,
    navigateUp: () -> Unit,
) {
    val entryDao = LocalAppDatabase.current.value.clipboardEntryDao()
    val entries = entryDao.getAll().collectAsState(emptyList())
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth()) {
                    Button(
                        onClick = navigateUp,
                        Modifier.padding(8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Return to keyboard")
                    }
                }
            }
            Box {
                LazyColumn(
                    Modifier
                        .height(300.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(entries.value, key = { it.id }) { entry ->
                        val swipeState =
                            rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    when (it) {
                                        SwipeToDismissBoxValue.Settled -> true

                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            coroutineScope.launch {
                                                entryDao.delete(entry)
                                                if (snackbarHostState.showSnackbar(
                                                        "Deleted",
                                                        actionLabel = "Undo",
                                                        withDismissAction = true,
                                                    ) == SnackbarResult.ActionPerformed
                                                ) {
                                                    entryDao.insertOrTouch(entry)
                                                }
                                            }
                                            true
                                        }

                                        SwipeToDismissBoxValue.EndToStart -> {
                                            clipboardManager.setText(AnnotatedString(entry.clipData))
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
                            Modifier
                                .animateItem(),
                        ) {
                            OutlinedCard(
                                onClick = {
                                    onAction.onAction(
                                        Action.Text(entry.clipData, hidden = true),
                                        key = null,
                                        gesture = null,
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                            ) {
                                Text(entry.clipData, Modifier.padding(16.dp))
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.safeDrawingPadding())
                    }
                }
                if (entries.value.isEmpty()) {
                    BodyPlaceholder(
                        "The clipboard history is empty, go and copy something!",
                        Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}