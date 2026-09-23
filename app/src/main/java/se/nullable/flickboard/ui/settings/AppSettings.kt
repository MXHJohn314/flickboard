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
import androidx.compose.ui.res.stringResource
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
        label = R.string.setting_settingsVersion,
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
        label = R.string.setting_hasCompletedTutorial,
        defaultValue = false,
    )

    val letterLayers = SettingMeta.EnumList(
        // Renaming this would reset the people's selected layer..
        key = "layout",
        label = R.string.setting_layout,
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
        label = R.string.setting_activeLetterLayerIndex,
        defaultValue = 0,
    )

    val secondaryLetterLayer = SettingMeta.Enum(
        key = "secondaryLetterLayer",
        label = R.string.setting_secondaryLetterLayer,
        description = R.string.setting_secondaryLetterLayer_desc,
        defaultValue = LetterLayerOption.English,
        options = LetterLayerOption.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        writePreviewSettings = { ctx ->
            enabledLayers.writeTo(ctx, EnabledLayers.DoubleLetters)
        },
    )

    val numericLayer = SettingMeta.Enum(
        key = "numericLayer",
        label = R.string.setting_numericLayer,
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
        label = R.string.setting_enabledLayers,
        defaultValue = EnabledLayers.All,
        options = EnabledLayers.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
    )

    val enabledLayersLandscape = SettingMeta.Enum(
        key = "enabledLayersLandscape",
        label = R.string.setting_enabledLayersLandscape,
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
        label = R.string.setting_handedness,
        defaultValue = Handedness.RightHanded,
        options = Handedness.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
    )

    val invertSwipeDelete = SettingMeta.Enum(
        key = "invertSwipeDelete",
        label = R.string.setting_invertSwipeDelete,
        defaultValue = InvertSwipeDelete.Never,
        options = InvertSwipeDelete.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        previewOverride = { Text(stringResource(it.description)) },
    )


    val landscapeLocation = SettingMeta.FloatSlider(
        key = "landscapeLocation",
        label = R.string.setting_landscapeLocation,
        defaultValue = 0F,
        range = -1F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val landscapeScale = SettingMeta.FloatSlider(
        key = "landscapeScale",
        label = R.string.setting_landscapeScale,
        defaultValue = 1F,
        range = 0.2F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val landscapeSplit = SettingMeta.FloatSlider(
        key = "landscapeSplit",
        label = R.string.setting_landscapeSplit,
        defaultValue = 0F,
        range = 0F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val landscapeControlSection = SettingMeta.Enum(
        key = "landscapeControlSection",
        label = R.string.setting_landscapeControlSection,
        defaultValue = ControlSectionOption.Single,
        options = ControlSectionOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ControlSectionOption>(it) },
        previewForceLandscape = true,
    )

    val portraitLocation = SettingMeta.FloatSlider(
        key = "portraitLocation",
        label = R.string.setting_portraitLocation,
        defaultValue = 0F,
        range = -1F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val portraitScale = SettingMeta.FloatSlider(
        key = "portraitScale",
        label = R.string.setting_portraitScale,
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
        label = R.string.setting_showLetters,
        description = R.string.setting_showLetters_desc,
        defaultValue = true,
    )

    val showSymbols = SettingMeta.Bool(
        key = "showSymbols",
        label = R.string.setting_showSymbols,
        description = R.string.setting_showSymbols_desc,
        defaultValue = true,
    )

    val enableToggleShowSymbolsGesture = SettingMeta.Bool(
        key = "enableToggleShowSymbolsGesture",
        label = R.string.setting_enableToggleShowSymbolsGesture,
        defaultValue = true,
    )

    val showNumbers = SettingMeta.Bool(
        key = "showNumbers",
        label = R.string.setting_showNumbers,
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
        label = R.string.setting_enableHiddenActions,
        defaultValue = true,
    )

    val keyboardMargin = SettingMeta.FloatSlider(
        key = "keyboardMargin",
        label = R.string.setting_keyboardMargin,
        defaultValue = 0F,
        range = 0F..8F,
    )

    val keyboardMarginBottomPortrait = SettingMeta.FloatSlider(
        key = "keyboardMarginBottomPortrait",
        label = R.string.setting_keyboardMarginBottomPortrait,
        defaultValue = 0F,
        range = 0F..250F,
    )

    val keyRoundness = SettingMeta.FloatSlider(
        key = "keyRoundness",
        label = R.string.setting_keyRoundness,
        defaultValue = 0F,
        range = 0F..0.5F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val keyGap = SettingMeta.FloatSlider(
        key = "keyGap",
        label = R.string.setting_keyGap,
        defaultValue = 1F,
        range = 0F..2F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val keyOutline = SettingMeta.FloatSlider(
        key = "keyOutline",
        label = R.string.setting_keyOutline,
        defaultValue = 0F,
        range = 0F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val actionVisualBiasCenter = SettingMeta.FloatSlider(
        key = "actionVisualBiasCenter",
        label = R.string.setting_actionVisualBiasCenter,
        defaultValue = 1.5F,
        range = 1F..2F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val actionVisualScale = SettingMeta.FloatSlider(
        key = "actionVisualScale",
        label = R.string.setting_actionVisualScale,
        defaultValue = 1F,
        range = 0.5F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val keyColour = SettingMeta.Colour(
        key = "keyColour",
        label = R.string.setting_keyColour,
    )
    val keyColourChroma = SettingMeta.FloatSlider(
        key = "keyColourChroma",
        label = R.string.setting_keyColourChroma,
        defaultValue = 20F,
        range = 0F..100F,
        description = R.string.setting_keyColour_attrib_desc,
    )
    val keyColourTone = SettingMeta.Enum(
        key = "keyColourTone",
        label = R.string.setting_keyColourTone,
        defaultValue = MaterialToneMode.System,
        options = MaterialToneMode.entries,
        tryEnumValueOfT = { tryEnumValueOf<MaterialToneMode>(it) },
        description = R.string.setting_keyColour_attrib_desc,
    )

    val keyOpacity = SettingMeta.FloatSlider(
        key = "keyOpacity",
        label = R.string.setting_keyOpacity,
        defaultValue = 0.7F,
        range = 0F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val backgroundOpacity = SettingMeta.FloatSlider(
        key = "backgroundOpacity",
        label = R.string.setting_backgroundOpacity,
        defaultValue = 1F,
        range = 0F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val backgroundImage = SettingMeta.Image(
        key = "backgroundImage",
        label = R.string.setting_backgroundImage,
        cacheFileName = "background-image.jpeg",
        scalingTarget = ImageScalingTarget.DeviceDisplay,
    )

    val enablePointerTrail = SettingMeta.Bool(
        key = "enablePointerTrail",
        label = R.string.setting_enablePointerTrail,
        defaultValue = true,
    )

    val enableFastActions = SettingMeta.Bool(
        key = "enableFastActions",
        label = R.string.setting_enableFastActions,
        description = R.string.setting_enableFastActions_desc,
        defaultValue = true,
    )

    val enableLongSwipes = SettingMeta.Bool(
        key = "enableLongSwipes",
        label = R.string.setting_enableLongSwipes,
        description = R.string.setting_enableLongSwipes_desc,
        defaultValue = false,
    )

    val longSwipeThreshold = SettingMeta.FloatSlider(
        key = "longSwipeThreshold",
        label = R.string.setting_longSwipeThreshold,
        range = 0.5F..4F,
        defaultValue = 1.5F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val enableAdvancedModifiers = SettingMeta.Bool(
        key = "enableAdvancedModifiers",
        label = R.string.setting_enableAdvancedModifiers,
        description = R.string.setting_enableAdvancedModifiers_desc,
        defaultValue = true,
    )

    val stickyModifiers = SettingMeta.Bool(
        key = "stickyModifiers",
        label = R.string.setting_stickyModifiers,
        description = R.string.setting_stickyModifiers_desc,
        defaultValue = false,
    )

    val modifierSettings: ModifierSettings
        @Composable
        get() = ModifierSettings(stickyModifiers = localAppSetting(stickyModifiers))

    val periodOnDoubleSpace = SettingMeta.Bool(
        key = "periodOnDoubleSpace",
        label = R.string.setting_periodOnDoubleSpace,
        description = R.string.setting_periodOnDoubleSpace_desc,
        defaultValue = false,
    )

    // Deprecated options only used to migrate values to {counterC,c}lockwiseCircleBehaviour
    private val digitOnClockwiseCircle = SettingMeta.Bool(
        key = "digitOnClockwiseCircle",
        label = R.string.setting_digitOnClockwiseCircle,
        defaultValue = false,
    )
    private val digitOnCounterClockwiseCircle = SettingMeta.Bool(
        key = "digitOnCounterClockwiseCircle",
        label = R.string.setting_digitOnCounterClockwiseCircle,
        defaultValue = false,
    )

    val clockwiseCircleBehaviour = SettingMeta.Enum(
        key = "clockwiseCircleBehaviour",
        label = R.string.setting_clockwiseCircleBehaviour,
        defaultValue = ModifierBehaviourOption.Shift,
        options = ModifierBehaviourOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ModifierBehaviourOption>(it) },
        previewOverride = {},
    )

    val counterClockwiseCircleBehaviour = SettingMeta.Enum(
        key = "counterClockwiseCircleBehaviour",
        label = R.string.setting_counterClockwiseCircleBehaviour,
        defaultValue = ModifierBehaviourOption.Shift,
        options = ModifierBehaviourOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ModifierBehaviourOption>(it) },
        previewOverride = {},
    )

    val longHoldThresholdMillis = SettingMeta.FloatSlider(
        key = "longHoldThresholdMillis",
        label = R.string.setting_longHoldThresholdMillis,
        // This is theoretically possible to modify with the `settings`
        // but there's no good event to read for updating this anyway.
        defaultValue = ViewConfiguration.getLongPressTimeout().toFloat(),
        range = 20F..1000F,
        render = { "${it.roundToInt()}ms" },
    )

    val longHoldBehaviour = SettingMeta.Enum(
        key = "longHoldBehaviour",
        label = R.string.setting_longHoldBehaviour,
        defaultValue = ModifierBehaviourOption.Digit,
        options = ModifierBehaviourOption.entries,
        tryEnumValueOfT = { tryEnumValueOf<ModifierBehaviourOption>(it) },
        previewOverride = {},
    )

    val disabledDeadkeys = SettingMeta.Text(
        key = "disabledDeadkeys",
        label = R.string.setting_disabledDeadkeys,
        defaultValue = "",
        description = R.string.setting_disabledDeadkeys_desc,
        placeholder = "(none)",
    )

    val keyHeight = SettingMeta.FloatSlider(
        key = "keyHeight",
        label = R.string.setting_keyHeight,
        defaultValue = 72F,
        range = 48F..512F,
    )

    val swipeThreshold = SettingMeta.FloatSlider(
        key = "swipeThreshold",
        label = R.string.setting_swipeThreshold,
        description = R.string.setting_swipeThreshold_desc,
        defaultValue = 8F,
        range = 8F..96F,
    )

    val fastSwipeThreshold = SettingMeta.FloatSlider(
        key = "fastSwipeTreshold",
        label = R.string.setting_fastSwipeTreshold,
        description = R.string.setting_fastSwipeTreshold_desc,
        defaultValue = 16F,
        range = 8F..96F,
    )

    val gestureRecognizer = SettingMeta.Enum(
        key = "gestureRecognizer",
        label = R.string.setting_gestureRecognizer,
        defaultValue = GestureRecognizer.Default,
        options = GestureRecognizer.entries,
        tryEnumValueOfT = ::tryEnumValueOf,
        previewOverride = { Text(stringResource(it.description)) },
    )

    val circleJaggednessThreshold = SettingMeta.FloatSlider(
        key = "circleJaggednessThreshold",
        label = R.string.setting_circleJaggednessThreshold,
        description = R.string.setting_circleJaggednessThreshold_desc,
        defaultValue = .5F,
        range = .01F..1F,
        render = SettingMeta.FloatSlider.Companion::percentage,
    )

    val circleDiscontinuityThreshold = SettingMeta.FloatSlider(
        key = "circleDiscontinuityThreshold",
        label = R.string.setting_circleDiscontinuityThreshold,
        description = R.string.setting_circleDiscontinuityThreshold_desc,
        defaultValue = PiF,
        range = 0F..PiF,
        render = SettingMeta.FloatSlider.Companion::angle,
    )

    val circleAngleThreshold = SettingMeta.FloatSlider(
        key = "circleAngleThreshold",
        label = R.string.setting_circleAngleThreshold,
        description = R.string.setting_circleAngleThreshold_desc,
        defaultValue = 1.5F * PiF,
        range = 1.5F * PiF..3F * PiF,
        render = SettingMeta.FloatSlider.Companion::angle,
    )

    val enableHapticFeedbackOnGestureStart = SettingMeta.Bool(
        key = "enableHapticFeedbackOnGestureStart",
        label = R.string.setting_enableHapticFeedbackOnGestureStart,
        defaultValue = false,
    )

    val enableHapticFeedbackOnGestureSuccess = SettingMeta.Bool(
        key = "enableHapticFeedback",
        label = R.string.setting_enableHapticFeedback,
        defaultValue = true,
    )

    val enableVisualFeedback = SettingMeta.Bool(
        key = "enableVisualFeedback",
        label = R.string.setting_enableVisualFeedback,
        defaultValue = true,
    )

    val disableVisualFeedbackForPasswords = SettingMeta.Bool(
        key = "disableVisualFeedbackForPasswords",
        label = R.string.setting_disableVisualFeedbackForPasswords,
        defaultValue = true,
    )

    val visualFeedbackInvertColourScheme = SettingMeta.Bool(
        key = "visualFeedbackInvertColourScheme",
        label = R.string.setting_visualFeedbackInvertColourScheme,
        defaultValue = false,
    )

    val enableKeyboardPreview = SettingMeta.Bool(
        key = "enableKeyboardPreview",
        label = R.string.setting_enableKeyboardPreview,
        defaultValue = true,
    )

    val dropLastGesturePoint = SettingMeta.Bool(
        key = "dropLastGesturePoint",
        label = R.string.setting_dropLastGesturePoint,
        defaultValue = false,
        description = R.string.setting_dropLastGesturePoint_desc,
    )

    val ignoreJumpsLongerThanPx = SettingMeta.FloatSlider(
        key = "ignoreJumpsLongerThanPx",
        label = R.string.setting_ignoreJumpsLongerThanPx,
        defaultValue = 600F,
        range = 16F..600F,
        description = R.string.setting_ignoreJumpsLongerThanPx_desc,
    )

    val flicksMustBeLongerThanSeconds = SettingMeta.FloatSlider(
        key = "flicksMustBeLongerThanSeconds",
        label = R.string.setting_flicksMustBeLongerThanSeconds,
        defaultValue = 0F,
        range = 0F..1F,
        description = R.string.setting_flicksMustBeLongerThanSeconds_desc,
        render = { String.format(locale = Locale.getDefault(), "%.2fs", it) },
    )

    val noReverseRtlBrackets = SettingMeta.Bool(
        key = "noReverseRtlBrackets",
        label = R.string.setting_noReverseRtlBrackets,
        defaultValue = isSamsungDevice,
        description = R.string.setting_noReverseRtlBrackets_desc,
    )

    val enableCustomHaptics = SettingMeta.Bool(
        key = "enableCustomHaptics",
        label = R.string.setting_enableCustomHaptics,
        defaultValue = false,
        description = R.string.setting_enableCustomHaptics_desc,
    )

    val customHapticsDuration = SettingMeta.FloatSlider(
        key = "customHapticsDuration",
        label = R.string.setting_customHapticsDuration,
        defaultValue = 30F,
        // Android doesn't allow a completely zero duration, see
        // https://developer.android.com/reference/kotlin/android/os/VibrationEffect?hl=en#createoneshot
        range = 1F..300F,
        description = R.string.setting_customHapticsDuration_desc,
        render = { "${it.roundToInt()}ms" },
    )

    // Pseudo-option used to store history
    val emojiHistory =
        SettingMeta.Text(
            key = "emojiHistory",
            label = R.string.setting_emojiHistory,
            defaultValue = "",
        )

    val saveEmojiHistory = SettingMeta.Bool(
        key = "saveEmojiHistory",
        label = R.string.setting_saveEmojiHistory,
        defaultValue = false,
        description = R.string.setting_saveEmojiHistory_desc,
        onChange = { ctx, enabled ->
            // Reset history on disable
            if (!enabled) {
                emojiHistory.resetIn(ctx)
            }
        },
    )

    val saveClipboardHistory = SettingMeta.Bool(
        key = "saveClipboardHistory",
        label = R.string.setting_saveClipboardHistory,
        defaultValue = false,
        description = R.string.setting_saveClipboardHistory_desc,
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
        label = R.string.setting_clipboardHistoryShowPinned,
        defaultValue = true,
    )
    val clipboardHistoryShowHistory = SettingMeta.Bool(
        key = "clipboardHistoryShowHistory",
        label = R.string.setting_clipboardHistoryShowHistory,
        defaultValue = true,
    )
    val clipboardHistoryShowTips = SettingMeta.Bool(
        key = "clipboardHistoryShowTips",
        label = R.string.setting_clipboardHistoryShowTips,
        defaultValue = true,
    )

    val zalgoSubstitutionsShowCustom = SettingMeta.Bool(
        key = "zalgoSubstitutionsShowCustom",
        label = R.string.setting_zalgoSubstitutionsShowCustom,
        defaultValue = true,
    )

    val zalgoSubstitutionsShowDefault = SettingMeta.Bool(
        key = "zalgoSubstitutionsShowDefault",
        label = R.string.setting_zalgoSubstitutionsShowDefault,
        defaultValue = false,
    )

    val zalgoSubstitutionsShowTips = SettingMeta.Bool(
        key = "zalgoSubstitutionsShowTips",
        label = R.string.setting_zalgoSubstitutionsShowTips,
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

