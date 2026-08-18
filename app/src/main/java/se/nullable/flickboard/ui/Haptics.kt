package se.nullable.flickboard.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.getSystemService
import se.nullable.flickboard.ui.settings.localAppSetting

interface Haptics {
    fun vibrate(reason: HapticFeedbackReason)

    companion object {
        val get: Haptics
            @Composable
            get() = run {
                when {
                    localAppSetting { it.enableCustomHaptics }.state.value -> {
                        val context = LocalContext.current
                        val durationMs = localAppSetting { it.customHapticsDuration }.state.value
                        remember(context, durationMs) {
                            AndroidCustomHaptics(context, durationMs = durationMs.toLong())
                        }
                    }

                    else -> {
                        val view = LocalView.current
                        remember(view) {
                            AndroidDefaultHaptics(view)
                        }
                    }
                }
            }
    }
}

enum class HapticFeedbackReason {
    GestureStart,
    GestureSuccess,
}

// Keyboard tap haptic event is not supported by Compose 1.7.x, see #254
//class ComposeHaptics(val hapticFeedback: HapticFeedback) : Haptics {
//    override fun vibrate(reason: HapticFeedbackReason) {
//        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
//    }
//}

// Workaround until ComposeHaptics can be reenabled
class AndroidDefaultHaptics(private val view: View) : Haptics {
    override fun vibrate(reason: HapticFeedbackReason) {
        view.performHapticFeedback(
            when (reason) {
                HapticFeedbackReason.GestureStart -> when {
                    android.os.Build.VERSION.SDK_INT >= 30 -> HapticFeedbackConstants.GESTURE_START
                    else -> HapticFeedbackConstants.KEYBOARD_TAP
                }

                HapticFeedbackReason.GestureSuccess -> HapticFeedbackConstants.KEYBOARD_TAP
            },
        )
    }
}

// Workaround for some devices that don't implement noticeable default haptics
class AndroidCustomHaptics(context: Context, private val durationMs: Long) : Haptics {
    private val vibrator = lazy { context.getSystemService<Vibrator>() }

    override fun vibrate(reason: HapticFeedbackReason) {
        vibrator.value?.vibrate(VibrationEffect.createOneShot(durationMs, 255))
    }
}