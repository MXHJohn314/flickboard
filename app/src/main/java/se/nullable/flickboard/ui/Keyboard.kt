package se.nullable.flickboard.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import se.nullable.flickboard.R
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.ActionClass
import se.nullable.flickboard.model.EnterActionOverride
import se.nullable.flickboard.model.FieldType
import se.nullable.flickboard.model.Gesture
import se.nullable.flickboard.model.KeyM
import se.nullable.flickboard.model.KeyboardState
import se.nullable.flickboard.model.Layer
import se.nullable.flickboard.model.Layout
import se.nullable.flickboard.model.ModifierState
import se.nullable.flickboard.model.ShiftState
import se.nullable.flickboard.model.TextDirection
import se.nullable.flickboard.model.layouts.CONTROL_MESSAGEASE_LAYER_DUAL_SIDED_OVERLAY
import se.nullable.flickboard.model.layouts.CONTROL_MESSAGEASE_LAYER_MINI_NUMBERS_OVERLAY
import se.nullable.flickboard.model.layouts.CONTROL_MESSAGEASE_LAYER_REVERSE_BACKWARDS_OVERLAY
import se.nullable.flickboard.model.layouts.EN_MESSAGEASE
import se.nullable.flickboard.model.layouts.OVERLAY_ADVANCED_MODIFIERS_MESSAGEASE_LAYER
import se.nullable.flickboard.model.layouts.OVERLAY_LONG_SWIPE_LAYER
import se.nullable.flickboard.model.layouts.OVERLAY_TOGGLE_SYMBOLS_MESSAGEASE_LAYER
import se.nullable.flickboard.model.layouts.overlayMessageaseLayer
import se.nullable.flickboard.model.layouts.spacer
import se.nullable.flickboard.ui.layout.Grid
import se.nullable.flickboard.ui.settings.AppSettings
import se.nullable.flickboard.ui.settings.AppSettingsProvider
import se.nullable.flickboard.ui.settings.ControlSectionOption
import se.nullable.flickboard.ui.settings.EnabledLayers
import se.nullable.flickboard.ui.settings.Handedness
import se.nullable.flickboard.ui.settings.LayoutType
import se.nullable.flickboard.ui.settings.LocalSettingsContext
import se.nullable.flickboard.ui.settings.localAppSetting
import se.nullable.flickboard.ui.theme.LocalKeyboardTheme
import se.nullable.flickboard.ui.util.userImagePainter

// Returns false if action could not be processed
//typealias OnAction = (Action, KeyM?, Gesture?) -> Boolean
fun interface OnAction {
    // Returns false if action could not be processed
    fun onAction(action: Action, key: KeyM?, gesture: Gesture?): Boolean
}

@Composable
fun Keyboard(
    layout: Layout,
    onAction: OnAction?,
    fieldType: FieldType,
    modifier: Modifier = Modifier,
    enterActionOverride: EnterActionOverride? = null,
    onModifierStateUpdated: (ModifierState) -> Unit = {},
    showAllModifiers: Boolean = false,
    overrideEnabledLayers: EnabledLayers? = null,
    allowFastActions: Boolean = true,
    allowHideSettings: Boolean = true,
    highlightedAction: Action? = null,
) {
    val settingsCtx = LocalSettingsContext.current
    val enabledLayersForCurrentOrientation = AppSettings.enabledLayersForCurrentOrientation
    val overrideEnabledLayersState = rememberUpdatedState(overrideEnabledLayers)
    val enabledLayers = remember {
        derivedStateOf {
            when (val override = overrideEnabledLayersState.value) {
                null -> enabledLayersForCurrentOrientation.value
                else -> override
            }
        }
    }
    val landscapeSplit = settingsCtx[AppSettings.landscapeSplit].state
    val landscapeControlSection = settingsCtx[AppSettings.landscapeControlSection].state
    val numericLayer = settingsCtx[AppSettings.numericLayer].state
    val secondaryLetterLayer = settingsCtx[AppSettings.secondaryLetterLayer].state
    val handedness = settingsCtx[AppSettings.handedness].state
    val invertSwipeDelete = settingsCtx[AppSettings.invertSwipeDelete].state
    val backgroundOpacity = settingsCtx[AppSettings.backgroundOpacity].state
    val enablePointerTrailSetting = settingsCtx[AppSettings.enablePointerTrail].state
    val disablePointerTrailForPasswords =
        settingsCtx[AppSettings.disableVisualFeedbackForPasswords].state
    val enablePointerTrail = enablePointerTrailSetting.value &&
            !(fieldType == FieldType.Password && disablePointerTrailForPasswords.value)
    val shownActionClasses = when {
        allowHideSettings -> AppSettings.shownActionClasses
        else -> remember { derivedStateOf { ActionClass.entries.toSet() } }
    }
    val enableHiddenActions = settingsCtx[AppSettings.enableHiddenActions].state
    val enableAdvancedModifiers = settingsCtx[AppSettings.enableAdvancedModifiers].state
    val modifierSettings = AppSettings.modifierSettings
    val enableToggleShowSymbols = settingsCtx[AppSettings.enableToggleShowSymbolsGesture].state
    val keyGap = settingsCtx[AppSettings.keyGap].state
    val backgroundImage = settingsCtx[AppSettings.backgroundImage].state
    val keyboardMargin = settingsCtx[AppSettings.keyboardMargin].state
    val keyboardMarginBottomPortrait = settingsCtx[AppSettings.keyboardMarginBottomPortrait].state
    val noReverseRtlBrackets = settingsCtx[AppSettings.noReverseRtlBrackets].state
    var modifierState: ModifierState by remember { mutableStateOf(ModifierState()) }
    LaunchedEffect(modifierState) {
        onModifierStateUpdated(modifierState)
    }
    val keyboardState = KeyboardState(
        modifiers = modifierState.takeUnless { showAllModifiers },
        modifierSettings = AppSettings.modifierSettings,
        enterOverride = enterActionOverride,
        enabledLayers = enabledLayers.value,
    )
    val layoutState = rememberUpdatedState(layout)
    val mergedFullSizedNumericLayer =
        remember {
            derivedStateOf {
                numericLayer.value.fullSizedLayer(layoutState.value)
                    .mergeFallback(layoutState.value.symbolLayer)
                    .mergeOverlay(OVERLAY_LONG_SWIPE_LAYER)
            }
        }
    val mergedMiniNumericLayer =
        remember {
            derivedStateOf {
                numericLayer.value.miniLayer(layoutState.value)
                    .mergeFallback(layoutState.value.miniSymbolLayer)
                    .let { it.setShift(it.autoShift(layoutState.value.locale)) }
            }
        }

    fun letterLayerByShiftState(layout: Layout): Map<ShiftState, Layer> {
        val overlay = overlayMessageaseLayer(layout.locale)
            .mergeOverlay(
                OVERLAY_ADVANCED_MODIFIERS_MESSAGEASE_LAYER
                    .takeIf { enableAdvancedModifiers.value },
            )
            .mergeOverlay(
                OVERLAY_TOGGLE_SYMBOLS_MESSAGEASE_LAYER
                    .takeIf { enableToggleShowSymbols.value },
            )
        val shift = layout.shiftLayer
            .mergeFallback(
                overlay.mergeFallback(
                    mergedFullSizedNumericLayer.value,
                    holdForFallback = true,
                )
                    .autoShift(layout.locale),
            )
        return mapOf(
            ShiftState.Normal to layout.mainLayer
                .mergeFallback(
                    overlay.mergeFallback(
                        mergedFullSizedNumericLayer.value,
                        holdForFallback = true,
                    ),
                )
                .setShift(shift),

            ShiftState.Shift to shift,

            // Don't shift numeric layer in caps lock
            ShiftState.CapsLock to layout.shiftLayer
                .mergeFallback(
                    overlay
                        .autoShift(layout.locale)
                        .mergeFallback(
                            mergedFullSizedNumericLayer.value,
                            holdForFallback = true,
                        ),
                ),
        ).mapValues {
            it.value.filterActions(
                shownActionClasses = shownActionClasses.value,
                enableHiddenActions = enableHiddenActions.value,
            )
        }
    }

    val layersByShiftState = remember {
        derivedStateOf {
            letterLayerByShiftState(layoutState.value)
        }
    }
    val secondaryLettersLayersByShiftState = remember {
        derivedStateOf {
            letterLayerByShiftState(secondaryLetterLayer.value.layout)
        }
    }
    val controlLayer = remember {
        derivedStateOf {
            layoutState.value.controlLayer
                ?.mergeOverlay(
                    CONTROL_MESSAGEASE_LAYER_REVERSE_BACKWARDS_OVERLAY
                        .takeIf {
                            invertSwipeDelete.value.forHandedness(handedness.value)
                        },
                )
                ?.mergeOverlay(
                    when (enabledLayers.value.type) {
                        LayoutType.Single -> null
                        LayoutType.Dual -> CONTROL_MESSAGEASE_LAYER_DUAL_SIDED_OVERLAY
                        LayoutType.MiniNumbers -> CONTROL_MESSAGEASE_LAYER_MINI_NUMBERS_OVERLAY
                    },
                )
                ?.let { it.setShift(it.autoShift(layoutState.value.locale)) }
        }
    }
    val isLandscape = rememberUpdatedState(LocalDisplayLimits.current?.isLandscape ?: false)
    var globalPosition: Offset by remember { mutableStateOf(Offset.Zero) }
    val pointerTrailState = rememberUpdatedState(
        when {
            enablePointerTrail -> rememberPointerTrailState()
            else -> null
        },
    )
    val keyboardDescription = stringResource(R.string.app_keyboard)
    BoxWithConstraints(
        modifier
            .onGloballyPositioned { globalPosition = it.positionInRoot() }
            .semantics {
                this.contentDescription = keyboardDescription
            },
    ) {
        var desiredWidth = this.maxWidth
        LocalDisplayLimits.current?.let { limits ->
            val maxMargin = (desiredWidth - limits.portraitWidth).coerceAtLeast(0.dp)
            // Enforce portrait aspect ratio in landscape mode
            desiredWidth -= maxMargin
        }
        desiredWidth *= AppSettings.currentScale
        val splitScale = when {
            isLandscape.value -> landscapeSplit.value
            else -> 0F
        }
        val excessWidth = maxWidth - desiredWidth
        val splitWidth = rememberUpdatedState(excessWidth * splitScale)
        val sideMargin = excessWidth - splitWidth.value
        val thisWidth = maxWidth - sideMargin

        val layer by remember {
            derivedStateOf {
                val activeLayer = layersByShiftState.value[modifierState.shift]!!
                val activeControlLayer = when (modifierState.shift) {
                    ShiftState.Shift -> controlLayer.value?.autoShift(layoutState.value.locale)
                    ShiftState.Normal, ShiftState.CapsLock -> controlLayer.value
                }
                val controlSection = when {
                    isLandscape.value -> landscapeControlSection.value
                    else -> ControlSectionOption.Single
                }
                listOfNotNull(
                    activeControlLayer.takeIf { controlSection == ControlSectionOption.DoubleOutside },
                    when (enabledLayers.value) {
                        EnabledLayers.All -> mergedFullSizedNumericLayer.value
                        EnabledLayers.AllMiniNumbers -> mergedMiniNumericLayer.value
                        EnabledLayers.DoubleLetters -> secondaryLettersLayersByShiftState.value[modifierState.shift]!!
                        else -> null
                    },
                    activeControlLayer.takeIf { controlSection == ControlSectionOption.DoubleInside },
                    when {
                        !isLandscape.value -> null
                        splitWidth.value > 0.01.dp -> spacer(splitWidth.value)
                        else -> null
                    },
                    activeControlLayer.takeUnless { controlSection == ControlSectionOption.DoubleOutside },
                    when (enabledLayers.value) {
                        EnabledLayers.AllMiniNumbersMiddle -> mergedMiniNumericLayer.value
                        else -> null
                    },
                    when (enabledLayers.value) {
                        EnabledLayers.Numbers -> mergedFullSizedNumericLayer.value
                        EnabledLayers.Letters, EnabledLayers.DoubleLetters, EnabledLayers.All,
                        EnabledLayers.AllMiniNumbers, EnabledLayers.AllMiniNumbersMiddle,
                        EnabledLayers.AllMiniNumbersOpposite -> activeLayer
                    },
                    when (enabledLayers.value) {
                        EnabledLayers.AllMiniNumbersOpposite -> mergedMiniNumericLayer.value
                        else -> null
                    },
                    activeControlLayer.takeIf { controlSection == ControlSectionOption.DoubleOutside },
                )
                    .let {
                        when (handedness.value) {
                            Handedness.RightHanded -> it
                            Handedness.LeftHanded -> it.asReversed()
                        }
                    }
                    .fold(Layer.empty, Layer::chain)
                    .let {
                        when (layoutState.value.textDirection) {
                            TextDirection.LeftToRight -> it
                            TextDirection.RightToLeft -> when {
                                noReverseRtlBrackets.value -> it
                                else -> it.flipBrackets()
                            }
                        }
                    }
            }
        }

        val backgroundColor = LocalKeyboardTheme.current.surfaceColour
        val backgroundImagePainter = backgroundImage.value?.let { userImagePainter(it) }
        val backgroundPainter = backgroundImagePainter ?: ColorPainter(backgroundColor)
        Image(
            backgroundPainter,
            null,
            contentScale = ContentScale.Crop,
            alpha = backgroundOpacity.value,
            modifier = Modifier.matchParentSize(),
        )
        Grid(
            modifier = Modifier
                .width(thisWidth)
                .align(
                    BiasAbsoluteAlignment(
                        horizontalBias = AppSettings.currentLocation,
                        verticalBias = 0F,
                    ),
                )
                .padding(keyboardMargin.value.dp)
                .padding(
                    bottom = when {
                        isLandscape.value -> 0.dp
                        else -> keyboardMarginBottomPortrait.value.dp
                    },
                )
                .safeDrawingPadding(),
            columnGap = keyGap.value.dp,
            rowGap = keyGap.value.dp,
            rows = layer.keyRows.map { row ->
                {
                    row.forEach { key ->
                        androidx.compose.runtime.key(key) {
                            val keyPosition = remember { mutableStateOf(Offset.Zero) }
                            val keyPointerTrailState = remember {
                                derivedStateOf {
                                    pointerTrailState.value?.forKey(keyPosition.value)
                                }
                            }
                            Key(
                                key,
                                onAction = onAction?.let { wrappedOnAction ->
                                    { action, key, gesture ->
                                        modifierState = when (action) {
                                            is Action.ToggleShift -> modifierState.copy(shift = action.state)
                                            Action.ToggleCtrl -> modifierState.copy(ctrl = !modifierState.ctrl)
                                            Action.ToggleAlt -> modifierState.copy(alt = !modifierState.alt)
                                            Action.ToggleZalgo -> modifierState.copy(zalgo = !modifierState.zalgo)
                                            Action.ToggleSelect -> modifierState.copy(select = !modifierState.select)
                                            is Action.Jump, is Action.JumpLineKeepPos, is Action.FastDelete -> modifierState

                                            else -> when {
                                                action.isHiddenAction -> modifierState
                                                else -> modifierState.next(modifierSettings)
                                            }
                                        }
                                        wrappedOnAction.onAction(action, key, gesture)
                                    }
                                },
                                keyboardState = keyboardState,
                                fieldType = fieldType,
                                modifier = when {
                                    key.fixedWidth != null -> Modifier.fixedWidth(key.fixedWidth)
                                    else -> Modifier.colspan(key.colspan)
                                }
                                    .onGloballyPositioned {
                                        keyPosition.value = it.positionInRoot() - globalPosition
                                    },
                                trailState = keyPointerTrailState,
                                layoutTextDirection = layout.textDirection,
                                allowFastActions = allowFastActions,
                                highlightedAction = highlightedAction,
                            )
                        }
                    }
                }
            },
        )
        pointerTrailState.value?.also {
            PointerTrail(Modifier.matchParentSize(), it)
        }
    }
}

@Composable
fun ConfiguredKeyboard(
    onAction: OnAction?,
    fieldType: FieldType,
    modifier: Modifier = Modifier,
    enterActionOverride: EnterActionOverride? = null,
    onModifierStateUpdated: (ModifierState) -> Unit = {},
    overrideEnabledLayers: EnabledLayers? = null,
    allowFastActions: Boolean = true,
    allowHideSettings: Boolean = true,
    highlightedAction: Action? = null,
) {
    val enabledLetterLayers = localAppSetting { it.letterLayers }.value
    Keyboard(
        layout = enabledLetterLayers.getOrNull(localAppSetting { it.activeLetterLayerIndex }.value)?.layout
            ?: enabledLetterLayers.firstOrNull()?.layout
            ?: EN_MESSAGEASE,
        onAction = onAction,
        fieldType = fieldType,
        modifier = modifier,
        enterActionOverride = enterActionOverride,
        onModifierStateUpdated = onModifierStateUpdated,
        overrideEnabledLayers = overrideEnabledLayers,
        allowFastActions = allowFastActions,
        allowHideSettings = allowHideSettings,
        highlightedAction = highlightedAction,
    )
}

@Composable
fun KeyboardLayoutPreview(layout: Layout, showAllModifiers: Boolean = false) {
    FlickBoardParent {
        Keyboard(
            layout = layout,
            showAllModifiers = showAllModifiers,
            onAction = { _, _, _ -> true },
            fieldType = FieldType.Regular,
        )
    }
}

@Composable
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
fun KeyboardPreview() {
    var lastAction by remember { mutableStateOf<Action?>(null) }
    FlickBoardParent {
        Surface {
            Column {
                Row {
                    Text(text = "Tapped: $lastAction")
                }
                ConfiguredKeyboard(
                    onAction = { action, _, _ ->
                        lastAction = action
                        true
                    },
                    fieldType = FieldType.Regular,
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 1024, heightDp = 500)
fun PlayKeyboardPreview() {
    var lastAction by remember { mutableStateOf<Action?>(null) }
    FlickBoardParent {
        Surface {
            val settingsCtx = LocalSettingsContext.current
            AppSettingsProvider(
                prefs = settingsCtx.mock {
                    AppSettings.keyHeight.writeTo(it, 128F)
                }.prefs,
            ) {
                ConfiguredKeyboard(
                    onAction = { action, _, _ ->
                        lastAction = action
                        true
                    },
                    fieldType = FieldType.Regular,
                )
            }
        }
    }
}
