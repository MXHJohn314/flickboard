package se.nullable.flickboard.ui

import android.annotation.SuppressLint
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.nullable.flickboard.R
import se.nullable.flickboard.ui.settings.AppSettings
import se.nullable.flickboard.ui.settings.LocalSettingsContext

@Composable
fun BetaMenu(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsCtx = LocalSettingsContext.current
    Column(modifier) {
        val itemModifier = Modifier.padding(8.dp)
        Text(stringResource(R.string.beta_openHint), itemModifier)
        Text(stringResource(R.string.beta_disclaimer), itemModifier)
        val settingsMime = "application/json"
        val exportPicker =
            rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(settingsMime)) { uri ->
                if (uri == null) {
                    return@rememberLauncherForActivityResult
                }
                JsonWriter(context.contentResolver.openOutputStream(uri)!!.bufferedWriter())
                    .use { json ->
                        json.beginObject()
                        AppSettings.all.forEach { section ->
                            section.settings.forEach { setting ->
                                if (setting.isSet(settingsCtx)) {
                                    json.name(setting.key)
                                    setting.exportToJson(settingsCtx, json)
                                }
                            }
                        }
                        json.endObject()
                    }
            }
        Button(onClick = { exportPicker.launch("flickboard-settings.json") }, itemModifier) {
            Text(stringResource(R.string.beta_exportSettings))
        }
        val importPicker =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri == null) {
                    return@rememberLauncherForActivityResult
                }
                val settingsByKey = AppSettings.all
                    .flatMap { it.settings }
                    .associateBy { it.key }
                try {
                    settingsCtx.edit { editPrefs ->
                        editPrefs.prefsEditor.clear()
                        JsonReader(context.contentResolver.openInputStream(uri)!!.bufferedReader())
                            .use { json ->
                                json.beginObject()
                                while (json.peek() != JsonToken.END_OBJECT) {
                                    val key = json.nextName()
                                    val setting = settingsByKey[key]
                                    if (setting != null) {
                                        try {
                                            setting.importFromJson(editPrefs, json)
                                        } catch (err: Exception) {
                                            Log.w("BetaMenu", "Import failed for key $key", err)
                                        }
                                    } else {
                                        Log.w("BetaMenu", "Unable to import unknown key $key")
                                        json.skipValue()
                                    }
                                }
                                json.endObject()
                            }
                    }
                    settingsCtx.edit { AppSettings.migrateSettings(it) }
                } catch (err: Exception) {
                    Log.w("BetaMenu", "Import failed", err)
                    // We don't care about refreshing here, since we only use it immediately
                    @SuppressLint("LocalContextGetResourceValueCall")
                    Toast.makeText(
                        context,
                        context.getString(R.string.beta_importFailed), Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        val showImportConfirmationDialog = remember { mutableStateOf(false) }
        if (showImportConfirmationDialog.value) {
            AlertDialog(
                icon = { Icon(painterResource(id = R.drawable.baseline_warning_24), "Warning") },
                title = { Text(stringResource(R.string.beta_importConfirm)) },
                text = { Text(stringResource(R.string.beta_importWillOverwrite)) },
                onDismissRequest = { showImportConfirmationDialog.value = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showImportConfirmationDialog.value = false
                            importPicker.launch(arrayOf(settingsMime))
                        },
                    ) {
                        Text(stringResource(R.string.beta_import))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportConfirmationDialog.value = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
        Button(onClick = { showImportConfirmationDialog.value = true }, itemModifier) {
            Text(stringResource(R.string.beta_importSettings))
        }
    }
}

