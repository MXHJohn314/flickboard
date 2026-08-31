package se.nullable.flickboard.ui.settings

import android.content.SharedPreferences
import android.view.ViewConfiguration
import androidx.annotation.StringRes
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
import se.nullable.flickboard.ui.settings.AppSettings.enabledLayersForCurrentOrientation
import se.nullable.flickboard.ui.util.ResettableState
import se.nullable.flickboard.ui.util.isSamsungDevice
import se.nullable.flickboard.util.MaterialToneMode
import se.nullable.flickboard.util.PiF
import se.nullable.flickboard.util.tryEnumValueOf
import java.util.Locale
import kotlin.math.roundToInt

val LocalSettingsContext = staticCompositionLocalOf<SettingsContext> {
    error("Tried to use LocalSettingsContext without an AppSettingsProvider")
}

@Composable
fun AppSettingsProvider(prefs: SharedPreferences? = null, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSettingsContext provides SettingsContext(
            prefs = prefs ?: SettingsContext.appPrefsForContext(LocalContext.current),
            coroutineScope = rememberCoroutineScope(),
            appDatabase = LocalAppDatabase.current,
        ),
    ) {
        content()
    }
}

@Composable
fun <T> localAppSetting(meta: SettingProjection<T>): ResettableState<T> =
    LocalSettingsContext.current[meta].state

@Composable
inline fun <T> localAppSetting(meta: (AppSettings) -> SettingProjection<T>): ResettableState<T> =
    localAppSetting(meta(AppSettings))

@Suppress("MemberVisibilityCanBePrivate")
object AppSettings {
    val settingsVersion = SettingMeta.Integer(
        key = "settingsVersion",
        label = "Settings format version",
        // This should always stay at 0 to
        defaultValue = 0,
    )

    /**
     * Attempt to upgrade settings from older versions.
     *
     * Always call this in its own [SettingsContext.edit] transaction,
     * since it needs to be able to see the latest old values.
     */
    fun migrateSettings(ctx: SettingsContext.Editor) {
        val oldVersion = settingsVersion.readFrom(ctx.reader)

        if (oldVersion <= 0) {
            fun modifierBehaviourOptionFromIsDigit(isDigit: Boolean) = when {
                isDigit -> ModifierBehaviourOption.Digit
                else -> ModifierBehaviourOption.Shift
            }
            if (digitOnClockwiseCircle.isSet(ctx.reader)
                // clockwiseCircleBehaviour was added before the settings migration system,
                // so we need to still respect manual changes made in between
                && !clockwiseCircleBehaviour.isSet(ctx.reader)
            ) {
                clockwiseCircleBehaviour.writeTo(
                    ctx,
                    modifierBehaviourOptionFromIsDigit(digitOnClockwiseCircle.readFrom(ctx.reader)),
                )
            }
            digitOnClockwiseCircle.resetIn(ctx)
            if (digitOnCounterClockwiseCircle.isSet(ctx.reader)
                // counterClockwiseCircleBehaviour was added before the settings migration system,
                // so we need to still respect manual changes made in between
                && !counterClockwiseCircleBehaviour.isSet(ctx.reader)
            ) {
                counterClockwiseCircleBehaviour.writeTo(
                    ctx,
                    modifierBehaviourOptionFromIsDigit(digitOnCounterClockwiseCircle.readFrom(ctx.reader)),
                )
            }
            digitOnCounterClockwiseCircle.resetIn(ctx)
        }

        val currentVersion = 1
        if (oldVersion != currentVersion) {
            settingsVersion.writeTo(ctx, currentVersion)
        }
    }

    val hasCompletedTutorial = SettingMeta.Bool(
        key = "hasCompletedTutorial",
        label = "Has completed tutorial",
        defaultValue = false,
    )

    val letterLayers = SettingMeta.EnumList(
        // Renaming this would reset the people's selected layer..
        key = "layout",
        label = "Letter layouts",
        defaultValue = listOf(LetterLayerOption.English),
        options = LetterLayerOption.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        writePreviewSettings = { ctx ->
            if (enabledLayers.readFrom(ctx.reader) == EnabledLayers.Numbers) {
                enabledLayers.writeTo(ctx, EnabledLayers.Letters)
            }
        },
    )

    val activeLetterLayerIndex = SettingMeta.Integer(
        key = "activeLetterLayerIndex",
        label = "Active letter layer index",
        defaultValue = 0,
    )

    val secondaryLetterLayer = SettingMeta.Enum(
        key = "secondaryLetterLayer",
        label = "Secondary letter layout",
        description = "Only applies when using double letter layers",
        defaultValue = LetterLayerOption.English,
        options = LetterLayerOption.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        writePreviewSettings = { ctx ->
            enabledLayers.writeTo(ctx, EnabledLayers.DoubleLetters)
        },
    )

    val numericLayer = SettingMeta.Enum(
        key = "numericLayer",
        label = "Number layout",
        defaultValue = NumericLayerOption.Phone,
        options = NumericLayerOption.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        writePreviewSettings = { ctx ->
            if (enabledLayers.readFrom(ctx.reader) == EnabledLayers.Letters) {
                enabledLayers.writeTo(ctx, EnabledLayers.Numbers)
            }
        },
    )

    val enabledLayers = SettingMeta.Enum(
        key = "enabledLayers",
        label = "Enabled layers",
        defaultValue = EnabledLayers.All,
        options = EnabledLayers.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
    )

    val enabledLayersLandscape = SettingMeta.Enum(
        key = "enabledLayersLandscape",
        label = "Enabled layers (landscape)",
        defaultValue = EnabledLayersLandscape.Inherit,
        options = EnabledLayersLandscape.entries,
        tryEnumValueOfT = EnabledLayersLandscape.Companion::tryEnumValueOf,
        previewForceLandscape = true,
    )

    /**
     * Used for actions that need to modify this setting depending on the active context.
     *
     * Should not be used for UI rendering, as [SettingProjection] does not include state tracking,
     * nor does this function try to track whether the projection "mode" is still valid. For these
     * use cases, prefer [enabledLayersForCurrentOrientation].
     *
     * [Setting.currentValue] may be null if the projection is outdated. Note that this
     * is *not exhaustive*.
     */
    fun enabledLayersProjectionForOrientation(
        ctx: SettingsContext,
        displayLimits: DisplayLimits?
    ): SettingProjection<EnabledLayers?> =
        run {
            val landscapeSetting =
                enabledLayersLandscape.tryMap(
                    get = { setting ->
                        (setting as? EnabledLayersLandscape.Set)?.setting
                    },
                    set = { EnabledLayersLandscape.Set(it) },
                )
            when {
                ctx[landscapeSetting].currentValue != null
                        && displayLimits?.isLandscape ?: false -> landscapeSetting

                else -> enabledLayers.tryMap(
                    get = { it },
                    set = { it },
                )
            }
        }

    val enabledLayersForCurrentOrientation: State<EnabledLayers>
        @Composable get() = when {
            LocalDisplayLimits.current?.isLandscape == true -> {
                val landscape = localAppSetting(enabledLayersLandscape)
                val portrait = localAppSetting(enabledLayers)
                remember {
                    derivedStateOf {
                        when (val landscapeValue = landscape.value) {
                            is EnabledLayersLandscape.Set -> landscapeValue.setting
                            EnabledLayersLandscape.Inherit -> portrait.value
                        }
                    }
                }
            }

            else -> localAppSetting(enabledLayers)
        }

    val handedness = SettingMeta.Enum(
        key = "handedness",
        label = "Handedness",
        defaultValue = Handedness.RightHanded,
        options = Handedness.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
    )

    val invertSwipeDelete = SettingMeta.Enum(
        key = "invertSwipeDelete",
        label = "Invert delete-swipe direction",
        defaultValue = InvertSwipeDelete.Never,
        options = InvertSwipeDelete.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        previewOverride = { Text(it.description) },
    )


    val landscapeLocation = SettingMeta.FloatSlider(
        key = "landscapeLocation",
        label = "Landscape location",
        defaultValue = 0F,
        range = -1F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val landscapeScale = SettingMeta.FloatSlider(
        key = "landscapeScale",
        label = "Landscape scale",
        defaultValue = 1F,
        range = 0.2F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val landscapeSplit = SettingMeta.FloatSlider(
        key = "landscapeSplit",
        label = "Landscape split",
        defaultValue = 0F,
        range = 0F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val landscapeControlSection = SettingMeta.Enum(
        key = "landscapeControlSection",
        label = "Control section mode in landscape",
        defaultValue = ControlSectionOption.Single,
        options = ControlSectionOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ControlSectionOption>(it) },
        previewForceLandscape = true,
    )

    val portraitLocation = SettingMeta.FloatSlider(
        key = "portraitLocation",
        label = "Portrait location",
        defaultValue = 0F,
        range = -1F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val portraitScale = SettingMeta.FloatSlider(
        key = "portraitScale",
        label = "Portrait scale",
        defaultValue = 1F,
        range = 0.2F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val currentLocation: Float
        @Composable get() = when {
            LocalDisplayLimits.current?.isLandscape
                ?: false -> localAppSetting(landscapeLocation).value

            else -> localAppSetting(portraitLocation).value
        }

    val currentScale: Float
        @Composable get() = when {
            LocalDisplayLimits.current?.isLandscape
                ?: false -> localAppSetting(landscapeScale).value

            else -> localAppSetting(portraitScale).value
        }

    val showLetters = SettingMeta.Bool(
        key = "showLetters",
        label = "Show letters",
        description = "Can also be toggled by swiping up from spacebar",
        defaultValue = true,
    )

    val showSymbols = SettingMeta.Bool(
        key = "showSymbols",
        label = "Show symbols",
        description = "Can also be toggled by swiping up-then-down from spacebar",
        defaultValue = true,
    )

    val enableToggleShowSymbolsGesture = SettingMeta.Bool(
        key = "enableToggleShowSymbolsGesture",
        label = "Enable toggle show symbols/letters gestures",
        defaultValue = true,
    )

    val showNumbers = SettingMeta.Bool(
        key = "showNumbers",
        label = "Show numbers",
        defaultValue = true,
    )

    val shownActionClasses: State<Set<ActionClass>>
        @Composable get() {
            val showLetters = localAppSetting(showLetters)
            val showSymbols = localAppSetting(showSymbols)
            val showNumbers = localAppSetting(showNumbers)
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

    val enableHiddenActions = SettingMeta.Bool(
        key = "enableHiddenActions",
        label = "Enable hidden actions",
        defaultValue = true,
    )

    val keyboardMargin = SettingMeta.FloatSlider(
        key = "keyboardMargin",
        label = "Keyboard margin",
        defaultValue = 0F,
        range = 0F..8F,
    )

    val keyboardMarginBottomPortrait = SettingMeta.FloatSlider(
        key = "keyboardMarginBottomPortrait",
        label = "Keyboard bottom margin (portrait)",
        defaultValue = 0F,
        range = 0F..250F,
    )

    val keyRoundness = SettingMeta.FloatSlider(
        key = "keyRoundness",
        label = "Key roundness",
        defaultValue = 0F,
        range = 0F..0.5F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val keyGap = SettingMeta.FloatSlider(
        key = "keyGap",
        label = "Key gap",
        defaultValue = 1F,
        range = 0F..2F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val keyOutline = SettingMeta.FloatSlider(
        key = "keyOutline",
        label = "Key outline",
        defaultValue = 0F,
        range = 0F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val actionVisualBiasCenter = SettingMeta.FloatSlider(
        key = "actionVisualBiasCenter",
        label = "Center key label scale",
        defaultValue = 1.5F,
        range = 1F..2F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val actionVisualScale = SettingMeta.FloatSlider(
        key = "actionVisualScale",
        label = "Key label scale",
        defaultValue = 1F,
        range = 0.5F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val keyColour = SettingMeta.Colour(
        key = "keyColour",
        label = "Key colour",
    )
    val keyColourChroma = SettingMeta.FloatSlider(
        key = "keyColourChroma",
        label = "Key colour saturation",
        defaultValue = 20F,
        range = 0F..100F,
        description = "Only applies when using a custom colour",
    )
    val keyColourTone = SettingMeta.Enum(
        key = "keyColourTone",
        label = "Key colour brightness",
        defaultValue = MaterialToneMode.System,
        options = MaterialToneMode.entries,
        tryEnumValueOfT = { tryEnumValueOf<MaterialToneMode>(it) },
        description = "Only applies when using a custom colour",
    )

    val keyOpacity = SettingMeta.FloatSlider(
        key = "keyOpacity",
        label = "Key opacity",
        defaultValue = 0.7F,
        range = 0F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val backgroundOpacity = SettingMeta.FloatSlider(
        key = "backgroundOpacity",
        label = "Background opacity",
        defaultValue = 1F,
        range = 0F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val backgroundImage = SettingMeta.Image(
        key = "backgroundImage",
        label = "Background image",
        cacheFileName = "background-image.jpeg",
        scalingTarget = ImageScalingTarget.DeviceDisplay,
    )

    val enablePointerTrail = SettingMeta.Bool(
        key = "enablePointerTrail",
        label = "Enable pointer trail",
        defaultValue = true,
    )

    val enableFastActions = SettingMeta.Bool(
        key = "enableFastActions",
        label = "Enable fast actions",
        description = "Allows certain actions to be performed before the tap is released",
        defaultValue = true,
    )

    val enableLongSwipes = SettingMeta.Bool(
        key = "enableLongSwipes",
        label = "Enable long swipes",
        description = "Enables shortcut actions on extra long swipes",
        defaultValue = false,
    )

    val longSwipeThreshold = SettingMeta.FloatSlider(
        key = "longSwipeThreshold",
        label = "Long swipe threshold",
        range = 0.5F..4F,
        defaultValue = 1.5F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val enableAdvancedModifiers = SettingMeta.Bool(
        key = "enableAdvancedModifiers",
        label = "Enable advanced modifiers",
        description = "Allows ctrl and alt modifiers to be toggled by swiping diagonally from space",
        defaultValue = true,
    )

    val stickyModifiers = SettingMeta.Bool(
        key = "stickyModifiers",
        label = "Sticky modifiers",
        description = "When disabled, ctrl and alt modifiers are automatically cleared after typing a letter. When enabled, they must be cleared manually.",
        defaultValue = false,
    )

    val modifierSettings: ModifierSettings
        @Composable
        get() = ModifierSettings(stickyModifiers = localAppSetting(stickyModifiers))

    val periodOnDoubleSpace = SettingMeta.Bool(
        key = "periodOnDoubleSpace",
        label = "Period on double space",
        description = "Convert \"  \" into \". \"",
        defaultValue = false,
    )

    // Deprecated options only used to migrate values to {counterC,c}lockwiseCircleBehaviour
    private val digitOnClockwiseCircle = SettingMeta.Bool(
        key = "digitOnClockwiseCircle",
        label = "Type digit on clockwise circle",
        defaultValue = false,
    )
    private val digitOnCounterClockwiseCircle = SettingMeta.Bool(
        key = "digitOnCounterClockwiseCircle",
        label = "Type digit on counter-clockwise circle",
        defaultValue = false,
    )

    val clockwiseCircleBehaviour = SettingMeta.Enum(
        key = "clockwiseCircleBehaviour",
        label = "Clockwise circle behaviour",
        defaultValue = ModifierBehaviourOption.Shift,
        options = ModifierBehaviourOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ModifierBehaviourOption>(it) },
        previewOverride = {},
    )

    val counterClockwiseCircleBehaviour = SettingMeta.Enum(
        key = "counterClockwiseCircleBehaviour",
        label = "Counter-clockwise circle behaviour",
        defaultValue = ModifierBehaviourOption.Shift,
        options = ModifierBehaviourOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ModifierBehaviourOption>(it) },
        previewOverride = {},
    )

    val longHoldThresholdMillis = SettingMeta.FloatSlider(
        key = "longHoldThresholdMillis",
        label = "Long hold duration threshold",
        // This is theoretically possible to modify with the `settings`
        // but there's no good event to read for updating this anyway.
        defaultValue = ViewConfiguration.getLongPressTimeout().toFloat(),
        range = 20F..1000F,
        render = { "${it.roundToInt()}ms" },
    )

    val longHoldBehaviour = SettingMeta.Enum(
        key = "longHoldBehaviour",
        label = "Long hold behaviour",
        defaultValue = ModifierBehaviourOption.Digit,
        options = ModifierBehaviourOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ModifierBehaviourOption>(it) },
        previewOverride = {},
    )

    val disabledDeadkeys = SettingMeta.Text(
        key = "disabledDeadkeys",
        label = "Disabled deadkeys",
        defaultValue = "",
        description = "Special characters that should never be merged into the previous character",
        placeholder = "(none)",
    )

    val keyHeight = SettingMeta.FloatSlider(
        key = "keyHeight",
        label = "Key height",
        defaultValue = 72F,
        range = 48F..512F,
    )

    val swipeThreshold = SettingMeta.FloatSlider(
        key = "swipeThreshold",
        label = "Swipe threshold",
        description = "How far you need to drag before a tap becomes a swipe",
        defaultValue = 8F,
        range = 8F..96F,
    )

    val fastSwipeThreshold = SettingMeta.FloatSlider(
        key = "fastSwipeTreshold",
        label = "Fast swipe threshold",
        description = "How far you need to drag between each fast action tick",
        defaultValue = 16F,
        range = 8F..96F,
    )

    val gestureRecognizer = SettingMeta.Enum(
        key = "gestureRecognizer",
        "Gesture recognizer",
        defaultValue = GestureRecognizer.Default,
        options = GestureRecognizer.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        previewOverride = { Text(it.description) },
    )

    val circleJaggednessThreshold = SettingMeta.FloatSlider(
        key = "circleJaggednessThreshold",
        label = "Circle jaggedness threshold",
        description = "How smooth a circle's radius must be",
        defaultValue = .5F,
        range = .01F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val circleDiscontinuityThreshold = SettingMeta.FloatSlider(
        key = "circleDiscontinuityThreshold",
        label = "Circle discontinuity threshold",
        description = "How much a circle's angle is allowed to jump",
        defaultValue = PiF,
        range = 0F..PiF,
        render = SettingMeta.FloatSlider.Companion::angle,
    )

    val circleAngleThreshold = SettingMeta.FloatSlider(
        key = "circleAngleThreshold",
        label = "Circle angle threshold",
        description = "How full the circle must be to be detected",
        defaultValue = 1.5F * PiF,
        range = 1.5F * PiF..3F * PiF,
        render = SettingMeta.FloatSlider.Companion::angle,
    )

    val enableHapticFeedbackOnGestureStart = SettingMeta.Bool(
        key = "enableHapticFeedbackOnGestureStart",
        label = "Vibrate on gesture start",
        defaultValue = false,
    )

    val enableHapticFeedbackOnGestureSuccess = SettingMeta.Bool(
        key = "enableHapticFeedback",
        label = "Vibrate on gesture finish",
        defaultValue = true,
    )

    val enableVisualFeedback = SettingMeta.Bool(
        key = "enableVisualFeedback",
        label = "Highlight taken actions",
        defaultValue = true,
    )

    val disableVisualFeedbackForPasswords = SettingMeta.Bool(
        key = "disableVisualFeedbackForPasswords",
        label = "Disable visual feedback (and pointer trails) for password fields",
        defaultValue = true,
    )

    val visualFeedbackInvertColourScheme = SettingMeta.Bool(
        key = "visualFeedbackInvertColourScheme",
        label = "Make visual highlight extra prominent",
        defaultValue = false,
    )

    val enableKeyboardPreview = SettingMeta.Bool(
        key = "enableKeyboardPreview",
        label = "Enable keyboard preview",
        defaultValue = true,
    )

    val dropLastGesturePoint = SettingMeta.Bool(
        key = "dropLastGesturePoint",
        label = "Ignore last gesture point",
        defaultValue = false,
        description = "This can help against some devices that insert erroneous motion at the end of gestures",
    )

    val ignoreJumpsLongerThanPx = SettingMeta.FloatSlider(
        key = "ignoreJumpsLongerThanPx",
        label = "Ignore jumps larger than limit",
        defaultValue = 600F,
        range = 16F..600F,
        description = "This can help against some devices that insert erroneous motion in the middle of " +
                "gestures, at the cost of sometimes causing misinput if the device lags",
    )

    val flicksMustBeLongerThanSeconds = SettingMeta.FloatSlider(
        key = "flicksMustBeLongerThanSeconds",
        label = "Swipes must be longer than limit",
        defaultValue = 0F,
        range = 0F..1F,
        description = "All gestures shorter than the limit will be forcibly interpreted as taps, rather than swipes",
        render = { String.format(locale = Locale.getDefault(), "%.2fs", it) },
    )

    val noReverseRtlBrackets = SettingMeta.Bool(
        key = "noReverseRtlBrackets",
        label = "Do not reverse brackets in right-to-left languages",
        defaultValue = isSamsungDevice,
        description = "This is required for the correct brackets to be typed on some devices (especially Samsung)",
    )

    val enableCustomHaptics = SettingMeta.Bool(
        key = "enableCustomHaptics",
        label = "Enable custom vibration",
        defaultValue = false,
        description = "This is required for some devices that ship broken vibration profiles that aren't strong enough to notice",
    )

    val customHapticsDuration = SettingMeta.FloatSlider(
        key = "customHapticsDuration",
        label = "Custom vibration duration",
        defaultValue = 30F,
        range = 0F..300F,
        description = "Only used when custom vibration is enabled",
        render = { "${it.roundToInt()}ms" },
    )

    // Pseudo-option used to store history
    val emojiHistory =
        SettingMeta.Text(
            key = "emojiHistory",
            label = "Emoji history",
            defaultValue = "",
        )

    val saveEmojiHistory = SettingMeta.Bool(
        key = "saveEmojiHistory",
        label = "Remember recent emojis",
        defaultValue = false,
        description = "History is only saved locally, and will not be shared.",
        onChange = { ctx, enabled ->
            // Reset history on disable
            if (!enabled) {
                emojiHistory.resetIn(ctx)
            }
        },
    )

    val saveClipboardHistory = SettingMeta.Bool(
        key = "saveClipboardHistory",
        label = "Save clipboard history",
        defaultValue = false,
        description = "History is only saved locally, and will not be shared.\n" +
                "Clipboard entries marked as \"sensitive\" will not be stored," +
                " but this requires the sharing app to know that it is sensitive.",
        onChange = { ctx, enabled ->
            // Reset history on disable
            if (!enabled) {
                ctx.reader.coroutineScope.launch {
                    ctx.reader.appDatabase.value.clipboardEntryDao().clearUnpinned()
                }
            }
        },
    )

    // Section display toggles that are intentionally not listed as regular settings
    val clipboardHistoryShowPinned = SettingMeta.Bool(
        key = "clipboardHistoryShowPinned",
        label = "Clipboard History: Show Pinned",
        defaultValue = true,
    )
    val clipboardHistoryShowHistory = SettingMeta.Bool(
        key = "clipboardHistoryShowHistory",
        label = "Clipboard History: Show History",
        defaultValue = true,
    )
    val clipboardHistoryShowTips = SettingMeta.Bool(
        key = "clipboardHistoryShowTips",
        label = "Clipboard History: Show Tips",
        defaultValue = true,
    )

    val zalgoSubstitutionsShowCustom = SettingMeta.Bool(
        key = "zalgoSubstitutionsShowCustom",
        label = "Zalgo Substitutions: Show Custom",
        defaultValue = true,
    )

    val zalgoSubstitutionsShowDefault = SettingMeta.Bool(
        key = "zalgoSubstitutionsShowDefault",
        label = "Zalgo Substitutions: Show Default",
        defaultValue = false,
    )

    val zalgoSubstitutionsShowTips = SettingMeta.Bool(
        key = "zalgoSubstitutionsShowTips",
        label = "Zalgo Substitutions: Show Tips",
        defaultValue = true,
    )

    val all =
        listOf(
            SettingsSection(
                key = "system",
                label = R.string.settingsGroup_system,
                icon = R.drawable.baseline_warning_24,
                hidden = true,
                settings = listOf(
                    settingsVersion,
                    hasCompletedTutorial,
                    activeLetterLayerIndex,
                ),
            ),
            SettingsSection(
                key = "deprecated",
                label = R.string.settingsGroup_deprecated,
                icon = R.drawable.baseline_warning_24,
                hidden = true,
                settings = listOf(
                    digitOnClockwiseCircle, digitOnCounterClockwiseCircle,
                ),
            ),
            SettingsSection(
                key = "layout",
                label = R.string.settingsGroup_layout,
                icon = R.drawable.baseline_apps_24,
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
                key = "aesthetics",
                label = R.string.settingsGroup_aesthetics,
                icon = R.drawable.baseline_palette_24,
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
                label = R.string.settingsGroup_behaviour,
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
                key = "feedback",
                label = R.string.settingsGroup_feedback,
                icon = R.drawable.baseline_vibration_24,
                settings = listOf(
                    enableHapticFeedbackOnGestureStart,
                    enableHapticFeedbackOnGestureSuccess,
                    enableVisualFeedback,
                    visualFeedbackInvertColourScheme,
                    enablePointerTrail,
                    disableVisualFeedbackForPasswords,
                ),
            ),
            SettingsSection(
                key = "workarounds",
                label = R.string.settingsGroup_workarounds,
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
                label = R.string.settingsGroup_privacy,
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
    @param:StringRes val label: Int,
    val icon: Int,
    val settings: List<SettingMeta<*>>,
    val hidden: Boolean = false,
)

