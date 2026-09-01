package se.nullable.flickboard.ui.clipboard

import android.content.ClipData
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import se.nullable.flickboard.R
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.clipboard.ClipboardEntry
import se.nullable.flickboard.model.clipboard.ClipboardEntryDao
import se.nullable.flickboard.ui.OnAction
import se.nullable.flickboard.ui.layout.SwipeToOptionsBox
import se.nullable.flickboard.ui.layout.centerVerticallyInVisibleBounds
import se.nullable.flickboard.ui.layout.rememberSwipeToOptionsBoxState
import se.nullable.flickboard.ui.layout.reverse
import se.nullable.flickboard.ui.util.defaultFocus

@Composable
fun ClipboardEntryCard(
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
                            Icon(
                                painterResource(R.drawable.baseline_clear_24),
                                stringResource(R.string.cancel),
                            )
                        }
                        IconButton(onClick = { onEditDone(editedClipData.value.text) }) {
                            Icon(
                                painterResource(R.drawable.baseline_check_24),
                                stringResource(R.string.save),
                            )
                        }
                    }
                }
            }


            else -> OutlinedCard(
                onClick = { onClick?.invoke() },
                enabled = onClick != null,
                modifier = cardModifier,
            ) {
                Text(
                    clipData, Modifier.padding(16.dp),
                    // LazyColumn crashes if the *layout area* for an item is too large
                    // (~400 paragraphs of https://getlorem.com/ seems to trigger it, for example).
                    // This is a pretty conservative limit that leaves a huge safety margin,
                    // but too large values also make it difficult to scroll through.
                    // TODO: Provide a separate "expanded view" instead.
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun LazyItemScope.ClipboardEntryItem(
    entry: ClipboardEntry?,
    entryDao: ClipboardEntryDao,
    onAction: OnAction,
    onEntryDeleted: (ClipboardEntry) -> Unit,
    snackbarHostState: SnackbarHostState,
    allowPinToggle: Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
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
                            val resources = LocalResources.current
                            IconButton(
                                onClick = {
                                    currentEntry.value?.let {
                                        coroutineScope.launch {
                                            clipboard.setClipEntry(
                                                ClipEntry(
                                                    ClipData.newPlainText(
                                                        it.clipData,
                                                        it.clipData,
                                                    ),
                                                ),
                                            )

                                            // API 33+ provides its own copy toast, so we don't need to provide one there
                                            // see https://developer.android.com/develop/ui/compose/touch-input/copy-and-paste#feedback_to_copying_content
                                            if (android.os.Build.VERSION.SDK_INT < 33) {
                                                snackbarHostState.showSnackbar(
                                                    resources.getString(
                                                        R.string.clipboard_copied,
                                                        it.clipData,
                                                    ),
                                                    withDismissAction = true,
                                                )
                                            }
                                        }
                                        coroutineScope.launch {
                                            swipeState.animateReset()
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    painterResource(R.drawable.baseline_content_copy_24),
                                    stringResource(R.string.copy),
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
                                        stringResource(R.string.clipboard_unpin),
                                    )

                                    else -> Icon(
                                        painterResource(R.drawable.outline_keep_24),
                                        stringResource(R.string.clipboard_pin),
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
                                    stringResource(R.string.edit),
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
                                    stringResource(R.string.delete),
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
            clipData = entry?.clipData ?: stringResource(R.string.loadingPlaceholder),
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
