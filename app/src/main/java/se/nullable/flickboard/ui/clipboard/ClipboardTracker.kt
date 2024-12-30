package se.nullable.flickboard.ui.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import se.nullable.flickboard.model.clipboard.ClipboardEntry
import se.nullable.flickboard.ui.LocalAppDatabase
import se.nullable.flickboard.ui.LocalAppSettings
import java.time.Instant

@Composable
fun ClipboardTracker() {
    if (LocalAppSettings.current.saveClipboardHistory.state.value) {
        val clipboardManager = LocalClipboardManager.current.nativeClipboard
        val context = LocalContext.current
        val entryDao = LocalAppDatabase.current.value.clipboardEntryDao()
        val coroutineScope = rememberCoroutineScope()
        DisposableEffect(clipboardManager, context, entryDao, coroutineScope) {
            val onClipChanged = { clipData: ClipData ->
                if (clipData.itemCount > 0 &&
                    // Never save sensitive data
                    clipData.description?.extras
                        ?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) != true
                ) {
                    val item = clipData.getItemAt(0)
                    val content = item.coerceToText(context).toString()
                    if (content.isNotEmpty()) {
                        coroutineScope.launch {
                            entryDao.insertOrTouch(
                                ClipboardEntry(
                                    id = 0,
                                    lastAddedOn = Instant.now(),
                                    clipData = content,
                                ),
                            )
                        }
                    }
                }
            }
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                clipboardManager.primaryClip?.let(onClipChanged)
            }
            clipboardManager.addPrimaryClipChangedListener(listener)
            onDispose {
                clipboardManager.removePrimaryClipChangedListener(listener)
            }
        }
    }
}