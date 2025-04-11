package se.nullable.flickboard.ui.settings

import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import se.nullable.flickboard.R
import se.nullable.flickboard.ui.ColourPicker
import se.nullable.flickboard.ui.ConfiguredKeyboard
import se.nullable.flickboard.ui.DisplayLimits
import se.nullable.flickboard.ui.FlickBoardParent
import se.nullable.flickboard.ui.ProvideDisplayLimits
import java.io.FileOutputStream
import kotlin.random.Random

@Composable
fun BoolSetting(setting: Setting.Bool) {
    val state = setting.state
    Box(
        modifier = Modifier.clickable {
            setting.currentValue = !state.value
        },
    ) {
        SettingRow {
            SettingLabel(setting)
            Switch(
                checked = state.value,
                onCheckedChange = { setting.currentValue = it },
            )
        }
    }
}

@Composable
fun TextSetting(setting: Setting.Text) {
    val state = setting.state
    SettingRow {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingLabel(setting)
            }
            Row {
                TextField(
                    value = state.value,
                    onValueChange = { setting.currentValue = it },
                    placeholder = { setting.placeholder?.let { Text(it) } },
                    modifier = Modifier.weight(1F),
                )
                IconButton(onClick = { setting.resetToDefault() }) {
                    Icon(painterResource(R.drawable.baseline_clear_24), "Reset to default")
                }
            }
        }
    }
}

@Composable
fun FloatSliderSetting(setting: Setting.FloatSlider) {
    val state = setting.state
    SettingRow {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingLabel(setting)
                Text(text = setting.render(state.value))
            }
            Row {
                Slider(
                    value = state.value,
                    onValueChange = { setting.currentValue = it },
                    valueRange = setting.range,
                    modifier = Modifier.weight(1F),
                )
                IconButton(onClick = { setting.resetToDefault() }) {
                    Icon(painterResource(R.drawable.baseline_clear_24), "Reset to default")
                }
            }
        }
    }
}

@Composable
fun <T : Labeled> EnumListSetting(setting: Setting.EnumList<T>) {
    fun toggleOption(option: T, add: Boolean? = null) {
        val old = setting.currentValue
        setting.currentValue = when {
            add ?: !old.contains(option) -> old + option
            else -> old - option
        }
    }
    BaseEnumSetting(
        setting,
        valueLabel = { it.singleOrNull()?.label ?: "${it.size} enabled" },
        options = setting.options,
        optionSelectionControl = { selected, option ->
            Switch(
                checked = selected.contains(option),
                onCheckedChange = { toggleOption(option, add = it) },
            )
        },
        optionIsSelected = List<T>::contains,
        onOptionSelected = { toggleOption(it) },
        collapseOnOptionSelected = false,
        writePreviewSettings = { readPrefs, prefs, option ->
            setting.writePreviewSettings(readPrefs, prefs)
            setting.writeTo(prefs, listOf(option))
        },
        previewOverride = null,
        previewForceLandscape = false,
    )
}

@Composable
fun <T : Labeled> EnumSetting(setting: Setting.Enum<T>) {
    if (setting.options.size > 1) {
        BaseEnumSetting(
            setting,
            valueLabel = { it.label },
            options = setting.options,
            optionSelectionControl = { selected, option ->
                RadioButton(
                    selected = selected == option,
                    onClick = { setting.currentValue = option },
                )
            },
            optionIsSelected = { selected, option -> option == selected },
            onOptionSelected = { setting.currentValue = it },
            collapseOnOptionSelected = true,
            writePreviewSettings = { readPrefs, prefs, option ->
                setting.writePreviewSettings(readPrefs, prefs)
                setting.writeTo(prefs, option)
            },
            previewOverride = setting.previewOverride,
            previewForceLandscape = setting.previewForceLandscape,
        )
    }
}

@Composable
fun ImageSetting(setting: Setting.Image) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        var uri = it
        if (uri != null) {
            val cachedFile = context.filesDir.resolve("background-image")
            // Copy image to ensure that that it'll stay present,
            // and that we will keep permission to access it.
            // takePersistableUriPermission is not always granted
            // by all implementations.
            context.contentResolver.openInputStream(uri).use { input ->
                FileOutputStream(cachedFile).use { output ->
                    input?.copyTo(output)
                }
            }
            uri = Uri.fromFile(cachedFile).buildUpon()
                // Bust caches and make sure clients load it
                .fragment(Random.Default.nextLong().toString())
                .build()
        }
        setting.currentValue = uri
    }
    SettingRow {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SettingLabel(setting)
            Row {
                IconButton(
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) {
                    Icon(painterResource(R.drawable.baseline_image_search_24), "Set image")
                }
                IconButton(onClick = { setting.currentValue = null }) {
                    Icon(painterResource(R.drawable.baseline_clear_24), "Clear")
                }
            }
        }
    }
}

@Composable
fun ColourSetting(setting: Setting.Colour) {
    val state = setting.state
    val expanded = remember { mutableStateOf(false) }
    SettingRow {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingLabel(setting)
                Row {
                    state.value?.let {
                        // Not actually a button, but we want to share the same sizing...
                        IconButton(onClick = {}, enabled = false) {
                            Image(
                                ColorPainter(it), "Selected colour",
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .align(Alignment.CenterVertically),
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            expanded.value = !expanded.value
                        },
                    ) {
                        Icon(painterResource(R.drawable.baseline_color_lens_24), "Set colour")
                    }
                    IconButton(onClick = { setting.currentValue = null }) {
                        Icon(painterResource(R.drawable.baseline_clear_24), "Clear")
                    }
                }
            }
            AnimatedVisibility(expanded.value, Modifier.align(Alignment.CenterHorizontally)) {
                ColourPicker(
                    onColourSelected = { setting.currentValue = it },
                    Modifier
                        .sizeIn(maxHeight = 200.dp)
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Labeled, V : Any> BaseEnumSetting(
    setting: Setting<V>,
    valueLabel: (V) -> String,
    options: List<T>,
    optionSelectionControl: @Composable (V, T) -> Unit,
    optionIsSelected: (V, T) -> Boolean,
    onOptionSelected: (T) -> Unit,
    collapseOnOptionSelected: Boolean,
    writePreviewSettings: (SharedPreferences, SharedPreferences.Editor, T) -> Unit,
    previewOverride: (@Composable (T) -> Unit)?,
    previewForceLandscape: Boolean,
) {
    val sheetState = rememberModalBottomSheetState()
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun collapse() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                expanded = false
            }
        }
    }
    Box(Modifier.clickable { expanded = true }) {
        SettingRow {
            SettingLabel(setting)
            Row {
                Text(valueLabel(setting.state.value))
                val angle: Float by animateFloatAsState(
                    when {
                        expanded -> 180F
                        else -> 0F
                    },
                    label = "angle",
                )
                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.rotate(angle))
            }
        }
        if (expanded) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { expanded = false },
            ) {
                LazyColumn(
                    Modifier
                        // Don't propagate any insets to the previews
                        .consumeWindowInsets(WindowInsets.safeContent),
                ) {
                    items(options, key = { it.toString() }) { option ->
                        val appSettings = LocalAppSettings.current
                        val isSelected = optionIsSelected(setting.state.value, option)
                        val select = {
                            onOptionSelected(option)
                            if (collapseOnOptionSelected) {
                                collapse()
                            }
                        }
                        Card(
                            onClick = select,
                            colors = when {
                                isSelected -> CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                )

                                else -> CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                )
                            },
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    Modifier.padding(bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = option.label,
                                        fontSize = 16.sp,
                                        modifier = Modifier.weight(1F),
                                    )
                                    optionSelectionControl(setting.state.value, option)
                                }
                                val prefs = remember(appSettings, setting, option) {
                                    MockedSharedPreferences(appSettings.ctx.prefs)
                                        .also { it.edit { writePreviewSettings(it, this, option) } }
                                }
                                FlickBoardParent(prefs) {
                                    ProvideDisplayLimits(
                                        DisplayLimits.calculateCurrent().let {
                                            it.copy(isLandscape = previewForceLandscape || it.isLandscape)
                                        },
                                    ) {
                                        when {
                                            previewOverride != null -> previewOverride(option)
                                            else -> ConfiguredKeyboard(
                                                onAction = null,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
fun RowScope.SettingLabel(setting: Setting<*>) {
    Column(
        Modifier
            .weight(1f, false)
            .padding(end = 8.dp),
    ) {
        Text(text = setting.label)
        val description = setting.description
        if (description != null) {
            Text(
                text = description,
                softWrap = true,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7F),
                fontWeight = FontWeight.Light,
            )
        }
    }
}
