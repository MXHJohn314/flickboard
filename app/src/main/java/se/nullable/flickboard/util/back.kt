package se.nullable.flickboard.util

import android.annotation.SuppressLint
import android.os.Build
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.RequiresApi

/**
 * An [OnBackInvokedCallback] that forwards to [OnBackPressedDispatcher].
 *
 * This is basically equivalent to what [OnBackPressedDispatcher.setOnBackInvokedDispatcher] uses
 * internally, but allows us to control when and how it's registered.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun onBackPressedDispatcherWrapperCallback(onBackPressedDispatcher: OnBackPressedDispatcher): OnBackInvokedCallback =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            object : OnBackAnimationCallback {
                override fun onBackInvoked() {
                    onBackPressedDispatcher.onBackPressed()
                }

                @SuppressLint("VisibleForTests")
                override fun onBackStarted(backEvent: BackEvent) {
                    onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(backEvent))
                }

                @SuppressLint("VisibleForTests")
                override fun onBackProgressed(backEvent: BackEvent) {
                    onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(backEvent))
                }

                @SuppressLint("VisibleForTests")
                override fun onBackCancelled() {
                    onBackPressedDispatcher.dispatchOnBackCancelled()
                }
            }

        else -> OnBackInvokedCallback(onBackPressedDispatcher::onBackPressed)
    }