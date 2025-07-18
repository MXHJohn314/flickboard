package se.nullable.flickboard.ui.settings

import android.content.SharedPreferences
import android.view.ViewConfiguration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import se.nullable.flickboard.R
import se.nullable.flickboard.model.ActionClass
import se.nullable.flickboard.model.ModifierSettings
import se.nullable.flickboard.ui.DisplayLimits
import se.nullable.flickboard.ui.LocalAppDatabase
import se.nullable.flickboard.ui.LocalDisplayLimits
import se.nullable.flickboard.ui.util.isSamsungDevice
import se.nullable.flickboard.util.MaterialToneMode
import se.nullable.flickboard.util.PiF
import se.nullable.flickboard.util.tryEnumValueOf
import java.util.Locale
import kotlin.math.roundToInt

val LocalAppSettings = staticCompositionLocalOf<AppSettings> {
    error("Tried to use LocalAppSettings without an AppSettingsProvider")
}

@Composable
fun AppSettingsProvider(prefs: SharedPreferences? = null, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppSettings provides AppSettings(
            SettingsContext(
                prefs = prefs ?: SettingsContext.appPrefsForContext(LocalContext.current),
                coroutineScope = rememberCoroutineScope(),
                appDatabase = LocalAppDatabase.current,
            ),
        ),
    ) {
        content()
    }
}

@Suppress("MemberVisibilityCanBePrivate")
class AppSettings(val ctx: SettingsContext) {
    // Intentionally not rendered
    val hasCompletedTutorial = Setting.Bool(
        key = "hasCompletedTutorial",
        label = "Has completed tutorial",
        defaultValue = false,
        ctx = ctx,
    )

    val letterLayers = Setting.EnumList(
        // Renaming this would reset the people's selected layer..
        key = "layout",
        label = "Letter layouts",
        defaultValue = listOf(LetterLayerOption.English),
        options = LetterLayerOption.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        ctx = ctx,
        writePreviewSettings = { readPrefs, prefs ->
            if (enabledLayers.readFrom(readPrefs) == EnabledLayers.Numbers) {
                enabledLayers.writeTo(prefs, EnabledLayers.Letters)
            }
        },
    )

    // Intentionally not surfaced as a settings option
    val activeLetterLayerIndex = Setting.Integer(
        key = "activeLetterLayerIndex",
        label = "Active letter layer index",
        defaultValue = 0,
        ctx = ctx,
    )

    val secondaryLetterLayer = Setting.Enum(
        key = "secondaryLetterLayer",
        label = "Secondary letter layout",
        description = "Only applies when using double letter layers",
        defaultValue = LetterLayerOption.English,
        options = LetterLayerOption.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        ctx = ctx,
        writePreviewSettings = { _, prefs ->
            enabledLayers.writeTo(prefs, EnabledLayers.DoubleLetters)
        },
    )

    val numericLayer = Setting.Enum(
        key = "numericLayer",
        label = "Number layout",
        defaultValue = NumericLayerOption.Phone,
        options = NumericLayerOption.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        ctx = ctx,
        writePreviewSettings = { readPrefs, prefs ->
            if (enabledLayers.readFrom(readPrefs) == EnabledLayers.Letters) {
                enabledLayers.writeTo(prefs, EnabledLayers.Numbers)
            }
        },
    )

    val enabledLayers = Setting.Enum(
        key = "enabledLayers",
        label = "Enabled layers",
        defaultValue = EnabledLayers.All,
        options = EnabledLayers.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        ctx = ctx,
    )

    val enabledLayersLandscape = Setting.Enum(
        key = "enabledLayersLandscape",
        label = "Enabled layers (landscape)",
        defaultValue = EnabledLayersLandscape.Inherit,
        options = EnabledLayersLandscape.entries,
        tryEnumValueOfT = EnabledLayersLandscape.Companion::tryEnumValueOf,
        ctx = ctx,
        previewForceLandscape = true,
    )

    /**
     * Used for actions that need to modify this setting depending on the active context.
     *
     * Should not be used for UI rendering, as [SettingProjection] does not include state tracking,
     * nor does this function try to track whether the projection "mode" is still valid. For these
     * use cases, prefer [enabledLayersForCurrentOrientation].
     *
     * [SettingProjection.currentValue] may be null if the projection is outdated. Note that this
     * is *not exhaustive*.
     */
    fun enabledLayersProjectionForOrientation(displayLimits: DisplayLimits?): SettingProjection<EnabledLayers?> =
        run {
            val landscapeSetting =
                enabledLayersLandscape.tryMap(
                    get = { setting ->
                        (setting as? EnabledLayersLandscape.Set)?.setting
                    },
                    set = { _, it ->
                        EnabledLayersLandscape.Set(it)
                    },
                )
            when {
                landscapeSetting.currentValue != null
                        && displayLimits?.isLandscape ?: false -> landscapeSetting

                else -> enabledLayers.tryMap(
                    get = { it },
                    set = { _, it -> it },
                )
            }
        }

    val enabledLayersForCurrentOrientation: State<EnabledLayers>
        @Composable get() = when {
            LocalDisplayLimits.current?.isLandscape == true -> {
                val landscape = enabledLayersLandscape.state
                val portrait = enabledLayers.state
                remember {
                    derivedStateOf {
                        when (val landscapeValue = landscape.value) {
                            is EnabledLayersLandscape.Set -> landscapeValue.setting
                            EnabledLayersLandscape.Inherit -> portrait.value
                        }
                    }
                }
            }

            else -> enabledLayers.state
        }

    val handedness = Setting.Enum(
        key = "handedness",
        label = "Handedness",
        defaultValue = Handedness.RightHanded,
        options = Handedness.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        ctx = ctx,
    )

    val invertSwipeDelete = Setting.Enum(
        key = "invertSwipeDelete",
        label = "Invert delete-swipe direction",
        defaultValue = InvertSwipeDelete.Never,
        options = InvertSwipeDelete.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        ctx = ctx,
        previewOverride = { Text(it.description) },
    )


    val landscapeLocation = Setting.FloatSlider(
        key = "landscapeLocation",
        label = "Landscape location",
        defaultValue = 0F,
        range = -1F..1F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val landscapeScale = Setting.FloatSlider(
        key = "landscapeScale",
        label = "Landscape scale",
        defaultValue = 1F,
        range = 0.2F..1F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val landscapeSplit = Setting.FloatSlider(
        key = "landscapeSplit",
        label = "Landscape split",
        defaultValue = 0F,
        range = 0F..1F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val landscapeControlSection = Setting.Enum(
        key = "landscapeControlSection",
        label = "Control section mode in landscape",
        defaultValue = ControlSectionOption.Single,
        options = ControlSectionOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ControlSectionOption>(it) },
        ctx = ctx,
        previewForceLandscape = true,
    )

    val portraitLocation = Setting.FloatSlider(
        key = "portraitLocation",
        label = "Portrait location",
        defaultValue = 0F,
        range = -1F..1F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val portraitScale = Setting.FloatSlider(
        key = "portraitScale",
        label = "Portrait scale",
        defaultValue = 1F,
        range = 0.2F..1F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val currentLocation: Float
        @Composable get() = when {
            LocalDisplayLimits.current?.isLandscape ?: false -> landscapeLocation.state.value
            else -> portraitLocation.state.value
        }

    val currentScale: Float
        @Composable get() = when {
            LocalDisplayLimits.current?.isLandscape ?: false -> landscapeScale.state.value
            else -> portraitScale.state.value
        }

    val showLetters = Setting.Bool(
        key = "showLetters",
        label = "Show letters",
        defaultValue = true,
        ctx = ctx,
    )

    val showSymbols = Setting.Bool(
        key = "showSymbols",
        label = "Show symbols",
        defaultValue = true,
        ctx = ctx,
    )

    val enableToggleShowSymbolsGesture = Setting.Bool(
        key = "enableToggleShowSymbolsGesture",
        label = "Enable toggle show symbols/letters gestures",
        defaultValue = true,
        ctx = ctx,
        description = "To use, swipe up from spacebar",
    )

    val showNumbers = Setting.Bool(
        key = "showNumbers",
        label = "Show numbers",
        defaultValue = true,
        ctx = ctx,
    )

    val shownActionClasses: State<Set<ActionClass>>
        @Composable get() {
            val showLetters = showLetters.state
            val showSymbols = showSymbols.state
            val showNumbers = showNumbers.state
            return remember {
                derivedStateOf {
                    setOfNotNull(
                        ActionClass.Other,
                        ActionClass.Letter.takeIf { showLetters.value },
                        ActionClass.Symbol.takeIf { showSymbols.value },
                        ActionClass.Number.takeIf { showNumbers.value },
                    )
                }
            }
        }

    val enableHiddenActions = Setting.Bool(
        key = "enableHiddenActions",
        label = "Enable hidden actions",
        defaultValue = true,
        ctx = ctx,
    )

    val keyboardMargin = Setting.FloatSlider(
        key = "keyboardMargin",
        label = "Keyboard margin",
        defaultValue = 0F,
        range = 0F..8F,
        ctx = ctx,
    )

    val keyboardMarginBottomPortrait = Setting.FloatSlider(
        key = "keyboardMarginBottomPortrait",
        label = "Keyboard bottom margin (portrait)",
        defaultValue = 0F,
        range = 0F..100F,
        ctx = ctx,
    )

    val keyRoundness = Setting.FloatSlider(
        key = "keyRoundness",
        label = "Key roundness",
        defaultValue = 0F,
        range = 0F..0.5F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val keyGap = Setting.FloatSlider(
        key = "keyGap",
        label = "Key gap",
        defaultValue = 1F,
        range = 0F..2F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val keyOutline = Setting.FloatSlider(
        key = "keyOutline",
        label = "Key outline",
        defaultValue = 0F,
        range = 0F..1F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val actionVisualBiasCenter = Setting.FloatSlider(
        key = "actionVisualBiasCenter",
        label = "Center key label scale",
        defaultValue = 1.5F,
        range = 1F..2F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val actionVisualScale = Setting.FloatSlider(
        key = "actionVisualScale",
        label = "Key label scale",
        defaultValue = 1F,
        range = 0.5F..1F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val keyColour = Setting.Colour(
        key = "keyColour",
        label = "Key colour",
        ctx = ctx,
    )
    val keyColourChroma = Setting.FloatSlider(
        key = "keyColourChroma",
        label = "Key colour saturation",
        defaultValue = 20F,
        range = 0F..100F,
        ctx = ctx,
        description = "Only applies when using a custom colour",
    )
    val keyColourTone = Setting.Enum(
        key = "keyColourTone",
        label = "Key colour brightness",
        defaultValue = MaterialToneMode.System,
        options = MaterialToneMode.entries,
        tryEnumValueOfT = { tryEnumValueOf<MaterialToneMode>(it) },
        ctx = ctx,
        description = "Only applies when using a custom colour",
    )

    val keyOpacity = Setting.FloatSlider(
        key = "keyOpacity",
        label = "Key opacity",
        defaultValue = 0.7F,
        range = 0F..1F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val backgroundOpacity = Setting.FloatSlider(
        key = "backgroundOpacity",
        label = "Background opacity",
        defaultValue = 1F,
        range = 0F..1F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val backgroundImage = Setting.Image(
        key = "backgroundImage",
        label = "Background image",
        ctx = ctx,
        cacheFileName = "background-image.jpeg",
        scalingTarget = ImageScalingTarget.DeviceDisplay,
    )

    val enablePointerTrail = Setting.Bool(
        key = "enablePointerTrail",
        label = "Enable pointer trail",
        defaultValue = true,
        ctx = ctx,
    )

    val enableFastActions = Setting.Bool(
        key = "enableFastActions",
        label = "Enable fast actions",
        description = "Allows certain actions to be performed before the tap is released",
        defaultValue = true,
        ctx = ctx,
    )

    val enableLongSwipes = Setting.Bool(
        key = "enableLongSwipes",
        label = "Enable long swipes",
        description = "Enables shortcut actions on extra long swipes",
        defaultValue = false,
        ctx = ctx,
    )

    val longSwipeThreshold = Setting.FloatSlider(
        key = "longSwipeThreshold",
        label = "Long swipe threshold",
        range = 0.5F..4F,
        defaultValue = 1.5F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val enableAdvancedModifiers = Setting.Bool(
        key = "enableAdvancedModifiers",
        label = "Enable advanced modifiers",
        description = "Allows ctrl and alt modifiers to be toggled by swiping diagonally from space",
        defaultValue = true,
        ctx = ctx,
    )

    val stickyModifiers = Setting.Bool(
        key = "stickyModifiers",
        label = "Sticky modifiers",
        description = "When disabled, ctrl and alt modifiers are automatically cleared after typing a letter. When enabled, they must be cleared manually.",
        defaultValue = false,
        ctx = ctx,
    )

    val modifierSettings: ModifierSettings
        @Composable
        get() = ModifierSettings(stickyModifiers = stickyModifiers.state)

    val periodOnDoubleSpace = Setting.Bool(
        key = "periodOnDoubleSpace",
        label = "Period on double space",
        description = "Convert \"  \" into \". \"",
        defaultValue = false,
        ctx = ctx,
    )

    // Deprecated options only used to migrate values to {counterC,c}lockwiseCircleBehaviour
    private val digitOnClockwiseCircle = Setting.Bool(
        key = "digitOnClockwiseCircle",
        label = "Type digit on clockwise circle",
        defaultValue = false,
        ctx = ctx,
    )
    private val digitOnCounterClockwiseCircle = Setting.Bool(
        key = "digitOnCounterClockwiseCircle",
        label = "Type digit on counter-clockwise circle",
        defaultValue = false,
        ctx = ctx,
    )

    val clockwiseCircleBehaviour = Setting.Enum(
        key = "clockwiseCircleBehaviour",
        label = "Clockwise circle behaviour",
        defaultValue = when {
            // Migration from the old format, okay since nobody should be writing to it anymore
            digitOnClockwiseCircle.currentValue -> ModifierBehaviourOption.Digit
            else -> ModifierBehaviourOption.Shift
        },
        options = ModifierBehaviourOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ModifierBehaviourOption>(it) },
        ctx = ctx,
        previewOverride = {},
    )

    val counterClockwiseCircleBehaviour = Setting.Enum(
        key = "counterClockwiseCircleBehaviour",
        label = "Counter-clockwise circle behaviour",
        defaultValue = when {
            // Migration from the old format, okay since nobody should be writing to it anymore
            digitOnCounterClockwiseCircle.currentValue -> ModifierBehaviourOption.Digit
            else -> ModifierBehaviourOption.Shift
        },
        options = ModifierBehaviourOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ModifierBehaviourOption>(it) },
        ctx = ctx,
        previewOverride = {},
    )

    val longHoldThresholdMillis = Setting.FloatSlider(
        key = "longHoldThresholdMillis",
        label = "Long hold duration threshold",
        // This is theoretically possible to modify with the `settings`
        // but there's no good event to read for updating this anyway.
        defaultValue = ViewConfiguration.getLongPressTimeout().toFloat(),
        range = 20F..1000F,
        ctx = ctx,
        render = { "${it.roundToInt()}ms" },
    )

    val longHoldBehaviour = Setting.Enum(
        key = "longHoldBehaviour",
        label = "Long hold behaviour",
        defaultValue = ModifierBehaviourOption.Digit,
        options = ModifierBehaviourOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ModifierBehaviourOption>(it) },
        ctx = ctx,
        previewOverride = {},
    )

    val disabledDeadkeys = Setting.Text(
        key = "disabledDeadkeys",
        label = "Disabled deadkeys",
        defaultValue = "",
        ctx = ctx,
        description = "Special characters that should never be merged into the previous character",
        placeholder = "(none)",
    )

    val keyHeight = Setting.FloatSlider(
        key = "keyHeight",
        label = "Key height",
        defaultValue = 72F,
        range = 48F..512F,
        ctx = ctx,
    )

    val swipeThreshold = Setting.FloatSlider(
        key = "swipeThreshold",
        label = "Swipe threshold",
        description = "How far you need to drag before a tap becomes a swipe",
        defaultValue = 8F,
        range = 8F..96F,
        ctx = ctx,
    )

    val fastSwipeThreshold = Setting.FloatSlider(
        key = "fastSwipeTreshold",
        label = "Fast swipe threshold",
        description = "How far you need to drag between each fast action tick",
        defaultValue = 16F,
        range = 8F..96F,
        ctx = ctx,
    )

    val gestureRecognizer = Setting.Enum(
        key = "gestureRecognizer",
        "Gesture recognizer",
        defaultValue = GestureRecognizer.Default,
        options = GestureRecognizer.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        ctx = ctx,
        previewOverride = { Text(it.description) },
    )

    val circleJaggednessThreshold = Setting.FloatSlider(
        key = "circleJaggednessThreshold",
        label = "Circle jaggedness threshold",
        description = "How smooth a circle's radius must be",
        defaultValue = .5F,
        range = .01F..1F,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::percentage,
    )

    val circleDiscontinuityThreshold = Setting.FloatSlider(
        key = "circleDiscontinuityThreshold",
        label = "Circle discontinuity threshold",
        description = "How much a circle's angle is allowed to jump",
        defaultValue = PiF,
        range = 0F..PiF,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::angle,
    )

    val circleAngleThreshold = Setting.FloatSlider(
        key = "circleAngleThreshold",
        label = "Circle angle threshold",
        description = "How full the circle must be to be detected",
        defaultValue = 1.5F * PiF,
        range = 1.5F * PiF..3F * PiF,
        ctx = ctx,
        render = Setting.FloatSlider.Companion::angle,
    )

    val enableHapticFeedbackOnGestureStart = Setting.Bool(
        key = "enableHapticFeedbackOnGestureStart",
        label = "Vibrate on gesture start",
        defaultValue = false,
        ctx = ctx,
    )

    val enableHapticFeedbackOnGestureSuccess = Setting.Bool(
        key = "enableHapticFeedback",
        label = "Vibrate on gesture finish",
        defaultValue = true,
        ctx = ctx,
    )

    val enableVisualFeedback = Setting.Bool(
        key = "enableVisualFeedback",
        label = "Highlight taken actions",
        defaultValue = true,
        ctx = ctx,
    )

    val visualFeedbackInvertColourScheme = Setting.Bool(
        key = "visualFeedbackInvertColourScheme",
        label = "Make visual highlight extra prominent",
        defaultValue = false,
        ctx = ctx,
    )

    val enableKeyboardPreview = Setting.Bool(
        key = "enableKeyboardPreview",
        label = "Enable keyboard preview",
        defaultValue = true,
        ctx = ctx,
    )

    val dropLastGesturePoint = Setting.Bool(
        key = "dropLastGesturePoint",
        label = "Ignore last gesture point",
        defaultValue = false,
        ctx = ctx,
        description = "This can help against some devices that insert erroneous motion at the end of gestures",
    )

    val ignoreJumpsLongerThanPx = Setting.FloatSlider(
        key = "ignoreJumpsLongerThanPx",
        label = "Ignore jumps larger than limit",
        defaultValue = 600F,
        range = 16F..600F,
        ctx = ctx,
        description = "This can help against some devices that insert erroneous motion in the middle of " +
                "gestures, at the cost of sometimes causing misinput if the device lags",
    )

    val flicksMustBeLongerThanSeconds = Setting.FloatSlider(
        key = "flicksMustBeLongerThanSeconds",
        label = "Swipes must be longer than limit",
        defaultValue = 0F,
        range = 0F..1F,
        ctx = ctx,
        description = "All gestures shorter than the limit will be forcibly interpreted as taps, rather than swipes",
        render = { String.format(locale = Locale.getDefault(), "%.2fs", it) },
    )

    val noReverseRtlBrackets = Setting.Bool(
        key = "noReverseRtlBrackets",
        label = "Do not reverse brackets in right-to-left languages",
        defaultValue = isSamsungDevice,
        ctx = ctx,
        description = "This is required for the correct brackets to be typed on some devices (especially Samsung)",
    )

    val enableCustomHaptics = Setting.Bool(
        key = "enableCustomHaptics",
        label = "Enable custom vibration",
        defaultValue = false,
        ctx = ctx,
        description = "This is required for some devices that ship broken vibration profiles that aren't strong enough to notice",
    )

    val customHapticsDuration = Setting.FloatSlider(
        key = "customHapticsDuration",
        label = "Custom vibration duration",
        defaultValue = 30F,
        range = 0F..300F,
        ctx = ctx,
        description = "Only used when custom vibration is enabled",
        render = { "${it.roundToInt()}ms" },
    )

    // Pseudo-option used to store history
    val emojiHistory =
        Setting.Text(key = "emojiHistory", label = "Emoji history", defaultValue = "", ctx = ctx)

    val saveEmojiHistory = Setting.Bool(
        key = "saveEmojiHistory",
        label = "Remember recent emojis",
        defaultValue = false,
        ctx = ctx,
        description = "History is only saved locally, and will not be shared.",
        onChange = {
            // Reset history on disable
            if (!it) {
                emojiHistory.currentValue = ""
            }
        },
    )

    val saveClipboardHistory = Setting.Bool(
        key = "saveClipboardHistory",
        label = "Save clipboard history",
        defaultValue = false,
        ctx = ctx,
        description = "History is only saved locally, and will not be shared.\n" +
                "Clipboard entries marked as \"sensitive\" will not be stored," +
                " but this requires the sharing app to know that it is sensitive.",
        onChange = {
            // Reset history on disable
            if (!it) {
                ctx.coroutineScope.launch {
                    ctx.appDatabase.value.clipboardEntryDao().clearUnpinned()
                }
            }
        },
    )

    // Section display toggles that are intentionally not listed as regular settings
    val clipboardHistoryShowPinned = Setting.Bool(
        key = "clipboardHistoryShowPinned",
        label = "Clipboard History: Show Pinned",
        defaultValue = true,
        ctx = ctx,
    )
    val clipboardHistoryShowHistory = Setting.Bool(
        key = "clipboardHistoryShowHistory",
        label = "Clipboard History: Show History",
        defaultValue = true,
        ctx = ctx,
    )
    val clipboardHistoryShowTips = Setting.Bool(
        key = "clipboardHistoryShowTips",
        label = "Clipboard History: Show Tips",
        defaultValue = true,
        ctx = ctx,
    )

    val all =
        listOf(
            SettingsSection(
                key = "layout", label = "Layout", icon = R.drawable.baseline_apps_24,
                settings = listOf(
                    letterLayers,
                    secondaryLetterLayer,
                    numericLayer,
                    enabledLayers,
                    enabledLayersLandscape,
                    handedness,
                    invertSwipeDelete,
                    keyHeight,
                    keyboardMargin,
                    landscapeLocation,
                    landscapeScale,
                    landscapeSplit,
                    landscapeControlSection,
                    portraitLocation,
                    portraitScale,
                    keyboardMarginBottomPortrait,
                ),
            ),
            SettingsSection(
                key = "aesthetics", label = "Aesthetics", icon = R.drawable.baseline_palette_24,
                settings = listOf(
                    showLetters,
                    showSymbols,
                    enableToggleShowSymbolsGesture,
                    showNumbers,
                    enableHiddenActions,
                    keyRoundness,
                    keyGap,
                    keyOutline,
                    actionVisualBiasCenter,
                    actionVisualScale,
                    keyColour,
                    keyColourChroma,
                    keyColourTone,
                    keyOpacity,
                    backgroundOpacity,
                    backgroundImage,
                ),
            ),
            SettingsSection(
                key = "behaviour",
                label = "Behaviour",
                icon = R.drawable.baseline_app_settings_alt_24,
                settings = listOf(
                    enableFastActions,
                    enableLongSwipes,
                    longSwipeThreshold,
                    enableAdvancedModifiers,
                    stickyModifiers,
                    periodOnDoubleSpace,
                    clockwiseCircleBehaviour,
                    counterClockwiseCircleBehaviour,
                    longHoldThresholdMillis,
                    longHoldBehaviour,
                    disabledDeadkeys,
                    swipeThreshold,
                    fastSwipeThreshold,
                    gestureRecognizer,
                    circleJaggednessThreshold,
                    circleDiscontinuityThreshold,
                    circleAngleThreshold,
                ),
            ),
            SettingsSection(
                key = "feedback", label = "Feedback", icon = R.drawable.baseline_vibration_24,
                settings = listOf(
                    enableHapticFeedbackOnGestureStart,
                    enableHapticFeedbackOnGestureSuccess,
                    enableVisualFeedback,
                    visualFeedbackInvertColourScheme,
                    enablePointerTrail,
                ),
            ),
            SettingsSection(
                key = "workarounds",
                label = "Workarounds",
                icon = R.drawable.baseline_bug_report_24,
                settings = listOf(
                    dropLastGesturePoint,
                    ignoreJumpsLongerThanPx,
                    flicksMustBeLongerThanSeconds,
                    noReverseRtlBrackets,
                    enableCustomHaptics,
                    customHapticsDuration,
                ),
            ),
            SettingsSection(
                key = "privacy",
                label = "Privacy",
                icon = R.drawable.baseline_fingerprint_24,
                settings = listOf(
                    saveEmojiHistory,
                    saveClipboardHistory,
                ),
            ),
        )
}

data class SettingsSection(
    val key: String,
    val label: String,
    val icon: Int,
    val settings: List<Setting<*>>
)

