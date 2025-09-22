package se.nullable.flickboard.benchmark

import android.content.ComponentName
import android.view.KeyEvent
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.onElement
import androidx.test.uiautomator.textAsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// These "benchmarks" have no defined endpoint,
// ONLY run them one by one, manually.
@RunWith(AndroidJUnit4::class)
class PerformanceHammerBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun hammerPreviewKeyTaps() = benchmarkRule.measureRepeated(
        packageName = "se.nullable.flickboard",
        metrics = listOf(StartupTimingMetric()),
        iterations = 1,
        startupMode = StartupMode.COLD,
        setupBlock = {
        },
    ) {
        startActivityAndWait()
        device.onElement { textAsString() == "SKIP" }.click()
        val targetKeys =
            "anihortes".map { device.onElement { viewIdResourceName == ("key(Type $it)") } }
        while (true) {
            targetKeys.forEach { targetKey ->
                targetKey.click()
            }
        }
    }

    @Test
    fun hammerKeySwipes() = benchmarkRule.measureRepeated(
        packageName = "se.nullable.flickboard",
        metrics = listOf(StartupTimingMetric()),
        iterations = 1,
        startupMode = StartupMode.COLD,
        setupBlock = {
        },
    ) {
        // Enable the keyboard
        val imeId = ComponentName(packageName, ".KeyboardService").flattenToShortString()
        device.executeShellCommand("ime enable $imeId")
        device.executeShellCommand("ime set $imeId")

        startActivityAndWait()
        device.onElement { textAsString() == "SKIP" }.click()
        val testPad = device.onElement { viewIdResourceName == "testPad" }
        testPad.click()
        val keyboard = device.onElement { viewIdResourceName == "flickboardKeyboardService" }
        val targetKeys =
            "anihortes".map { keyboard.onElement { viewIdResourceName == ("key(Type $it)") } }
        while (true) {
            targetKeys.forEach { targetKey ->
                targetKey.swipe(Direction.DOWN, 1F)
            }
            device.pressKeyCode(
                KeyEvent.KEYCODE_A,
                KeyEvent.normalizeMetaState(KeyEvent.META_CTRL_LEFT_ON),
            )
        }
    }
}