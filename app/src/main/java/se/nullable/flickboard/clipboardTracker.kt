package se.nullable.flickboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import se.nullable.flickboard.model.clipboard.ClipboardEntry
import se.nullable.flickboard.ui.settings.AppSettings
import java.time.Instant

suspend fun clipboardTracker(appSettings: AppSettings, context: Context) {
    appSettings.ctx[appSettings.saveClipboardHistory].watch.collectLatest { enabled ->
        if (enabled) {
            val clipboardManager = context.getSystemService<ClipboardManager>() ?: run {
                Log.e("clipboardTracker", "Could not obtain ClipboardManager")
                return@collectLatest
            }
            val entryDao = appSettings.ctx.appDatabase.value.clipboardEntryDao()
            coroutineScope {
                val listener = ClipboardManager.OnPrimaryClipChangedListener {
                    val clipData =
                        clipboardManager.primaryClip ?: return@OnPrimaryClipChangedListener
                    if (clipData.itemCount > 0 &&
                        // Never save sensitive data
                        clipData.description?.extras
                            ?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) != true
                    ) {
                        val item = clipData.getItemAt(0)
                        val content = item.coerceToText(context).toString()
                        if (content.isNotEmpty()) {
                            launch {
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
                try {
                    clipboardManager.addPrimaryClipChangedListener(listener)
                    // Save current clip in case it was copied while the listener was disabled
                    listener.onPrimaryClipChanged()
                    awaitCancellation()
                } finally {
                    clipboardManager.removePrimaryClipChangedListener(listener)
                }
            }
        }
    }
}