package se.nullable.flickboard.ui.settings

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.geometry.Size
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
import se.nullable.flickboard.ui.LocalDisplayLimits
import se.nullable.flickboard.ui.util.ScalingMode
import se.nullable.flickboard.ui.util.loadBitmap
import java.io.FileOutputStream
import kotlin.random.Random

private const val LOG_TAG = "settings.items"

@Composable
fun BoolSetting(meta: SettingMeta.Bool) {
    val setting = localAppSetting(meta)
    val state = setting.state
    Box(
        modifier = Modifier.clickable {
            setting.currentValue = !state.value
        },
    ) {
        SettingRow {
            SettingLabel(meta)
            Switch(
                checked = state.value,
                onCheckedChange = { setting.currentValue = it },
            )
        }
    }
}

@Composable
fun TextSetting(meta: SettingMeta.Text) {
    val setting = localAppSetting(meta)
    val state = setting.state
    SettingRow {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingLabel(meta)
            }
            Row {
                TextField(
                    value = state.value,
                    onValueChange = { setting.currentValue = it },
                    placeholder = { meta.placeholder?.let { Text(it) } },
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
fun FloatSliderSetting(meta: SettingMeta.FloatSlider) {
    val setting = localAppSetting(meta)
    val state = setting.state
    SettingRow {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingLabel(meta)
                Text(text = meta.render(state.value))
            }
            Row {
                Slider(
                    value = state.value,
                    onValueChange = { setting.currentValue = it },
                    valueRange = meta.range,
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
fun <T : Labeled> EnumListSetting(meta: SettingMeta.EnumList<T>) {
    val setting = localAppSetting(meta)
    fun toggleOption(option: T, add: Boolean? = null) {
        setting.modify { old ->
            when {
                add ?: !old.contains(option) -> old + option
                else -> old - option
            }
        }
    }
    BaseEnumSetting(
        meta, setting,
        valueLabel = { it.singleOrNull()?.label ?: "${it.size} enabled" },
        options = meta.options,
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
            meta.writePreviewSettings(readPrefs, prefs)
            meta.writeTo(prefs, listOf(option))
        },
        previewOverride = null,
        previewForceLandscape = false,
    )
}

@Composable
fun <T : Labeled> EnumSetting(meta: SettingMeta.Enum<T>) {
    val setting = localAppSetting(meta)
    if (meta.options.size > 1) {
        BaseEnumSetting(
            meta, setting,
            valueLabel = { it.label },
            options = meta.options,
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
                meta.writePreviewSettings(readPrefs, prefs)
                meta.writeTo(prefs, option)
            },
            previewOverride = meta.previewOverride,
            previewForceLandscape = meta.previewForceLandscape,
        )
    }
}

@Composable
fun ImageSetting(meta: SettingMeta.Image) {
    val setting = localAppSetting(meta)
    val context = LocalContext.current
    val displayLimits = LocalDisplayLimits.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        var uri = it
        if (uri != null) {
            val cachedFile = context.filesDir.resolve(meta.cacheFileName)
            // Copy image to ensure that that it'll stay present,
            // and that we will keep permission to access it.
            // takePersistableUriPermission is not always granted
            // by all implementations.
            // This also lets us pre-scale the image to something that makes sense for the device.
            val successfullyCompressed = when {
                else -> when (meta.scalingTarget) {
                    ImageScalingTarget.Original -> false

                    ImageScalingTarget.DeviceDisplay -> when {
                        displayLimits == null ->
                            throw RuntimeException("Can't scale file to display without knowing its bounds")

                        else -> {
                            val image = loadBitmap(
                                context.contentResolver,
                                uri,
                                // Scale to a square, since we don't know the final orientation that
                                // the keyboard will be rendered in.
                                targetSize = displayLimits.largestDimensionPixels.toFloat()
                                    .let { px -> Size(px, px) },
                                scalingMode = ScalingMode.Conservative,
                            )
                            FileOutputStream(cachedFile).use { output ->
                                image.compress(Bitmap.CompressFormat.JPEG, 80, output)
                            }.also { success ->
                                if (!success) {
                                    Log.e(
                                        LOG_TAG,
                                        "Failed to compress selected image, saving original image instead",
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (!successfullyCompressed) {
                context.contentResolver.openInputStream(uri).use { input ->
                    FileOutputStream(cachedFile).use { output ->
                        input?.copyTo(output)
                    }
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
            SettingLabel(meta)
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
fun ColourSetting(meta: SettingMeta.Colour) {
    val setting = localAppSetting(meta)
    val state = setting.state
    val expanded = remember { mutableStateOf(false) }
    SettingRow {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingLabel(meta)
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
    meta: SettingMeta<V>,
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
            SettingLabel(meta)
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
                                FlickBoardParent(
                                    prefs = prefs,
                                    displayLimits =
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
fun RowScope.SettingLabel(setting: SettingMeta<*>) {
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
