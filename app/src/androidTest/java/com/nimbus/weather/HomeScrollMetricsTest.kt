package com.nimbus.weather

import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.FrameMetricsAggregator
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nimbus.weather.data.local.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Замер кадров при прокрутке главного экрана.
 *
 * `adb shell input` на MIUI запрещён (INJECT_EVENTS для чужого UID),
 * поэтому касания инжектятся изнутри instrumentation-процесса —
 * `sendPointerSync` работает в своём приложении без разрешений.
 * Метрики собирает [FrameMetricsAggregator] и печатает их в logcat
 * под тегом NIMBUS_PERF.
 */
@RunWith(AndroidJUnit4::class)
class HomeScrollMetricsTest {

    @Test
    fun scrollFrameMetrics() {
        measure("scroll") { instrumentation ->
            repeat(12) {
                swipe(instrumentation, fromY = 1_900f, toY = 700f)
                swipe(instrumentation, fromY = 700f, toY = 1_900f)
            }
        }
    }

    /**
     * То же самое, но с развёрнутым подробным прогнозом: в списке на семь
     * плиток больше, и прокрутка идёт через них.
     */
    @Test
    fun expandedScrollFrameMetrics() {
        measure(
            label = "scroll_expanded",
            before = { instrumentation ->
                repeat(4) { swipe(instrumentation, fromY = 1_900f, toY = 500f) }
                SystemClock.sleep(800)
                expandSpoiler(instrumentation)
                // Опускаемся к плиткам — замер должен идти через них.
                repeat(4) { swipe(instrumentation, fromY = 1_900f, toY = 500f) }
                SystemClock.sleep(800)
            }
        ) { instrumentation ->
            repeat(12) {
                swipe(instrumentation, fromY = 1_900f, toY = 700f)
                swipe(instrumentation, fromY = 700f, toY = 1_900f)
            }
        }
    }

    @Test
    fun idleFrameMetrics() {
        measure("idle") { SystemClock.sleep(10_000) }
    }

    private fun measure(
        label: String,
        before: ((android.app.Instrumentation) -> Unit)? = null,
        action: (android.app.Instrumentation) -> Unit
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Фон-небо рисуется каждый кадр и перекрывает вклад списка в метрику.
        // На время замера выключаем анимацию неба, потом возвращаем как было.
        val settings = SettingsDataStore(instrumentation.targetContext)
        val skyWasEnabled = runBlocking { settings.skyAnimationEnabled.first() }
        runBlocking { settings.setSkyAnimationEnabled(false) }
        try {
            measureWithStaticSky(label, before, action, instrumentation)
        } finally {
            runBlocking { settings.setSkyAnimationEnabled(skyWasEnabled) }
        }
    }

    private fun measureWithStaticSky(
        label: String,
        before: ((android.app.Instrumentation) -> Unit)?,
        action: (android.app.Instrumentation) -> Unit,
        instrumentation: android.app.Instrumentation
    ) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Погода грузится из сети/кэша — даём экрану устояться.
            SystemClock.sleep(8_000)

            before?.invoke(instrumentation)

            val aggregator = FrameMetricsAggregator(ALL_METRICS)
            scenario.onActivity { aggregator.add(it) }
            Log.i(TAG, "aggregator started: $label")
            try {
                action(instrumentation)
                SystemClock.sleep(500)
            } finally {
                val all = arrayOfNulls<Array<SparseIntArray?>>(1)
                scenario.onActivity { _ ->
                    all[0] = aggregator.stop()
                }
                val result = all[0]
                if (result == null) {
                    Log.i(TAG, "$label buckets=null")
                    writeResult(label, "buckets=null")
                } else {
                    writeResult(label, reportAll(label, result))
                }
            }
        }
    }

    private fun expandSpoiler(instrumentation: android.app.Instrumentation) {
        val title = instrumentation.targetContext.getString(R.string.forecast_7_days)
        repeat(5) {
            val node = findText(instrumentation.uiAutomation.rootInActiveWindow, title)
            if (node != null) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                if (bounds.bottom > 0 && bounds.top < 2400) {
                    Log.i(TAG, "spoiler bounds=$bounds")
                    tap(instrumentation, bounds.centerX().toFloat(), bounds.centerY().toFloat())
                    SystemClock.sleep(900)
                    return
                }
            }
            swipe(instrumentation, 1_900f, 600f)
            SystemClock.sleep(600)
        }
    }

    private fun findText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        node ?: return null
        if (node.text?.toString() == text) return node
        for (i in 0 until node.childCount) {
            findText(node.getChild(i), text)?.let { return it }
        }
        return null
    }

    private fun tap(instrumentation: android.app.Instrumentation, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        send(instrumentation, downTime, downTime, MotionEvent.ACTION_DOWN, x, y)
        SystemClock.sleep(60)
        send(instrumentation, downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y)
    }

    private fun swipe(instrumentation: android.app.Instrumentation, fromY: Float, toY: Float) {
        val x = 540f
        val downTime = SystemClock.uptimeMillis()
        send(instrumentation, downTime, downTime, MotionEvent.ACTION_DOWN, x, fromY)
        val steps = 18
        val stepDelayMs = 16L
        for (i in 1..steps) {
            SystemClock.sleep(stepDelayMs)
            val y = fromY + (toY - fromY) * i / steps
            send(instrumentation, downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, x, y)
        }
        SystemClock.sleep(stepDelayMs)
        send(instrumentation, downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, toY)
    }

    private fun send(
        instrumentation: android.app.Instrumentation,
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
        y: Float
    ) {
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        instrumentation.sendPointerSync(event)
        event.recycle()
    }

    private fun reportAll(label: String, buckets: Array<SparseIntArray?>): String {
        val sb = StringBuilder()
        buckets.forEachIndexed { index, arr ->
            if (arr == null || arr.size() == 0) return@forEachIndexed
            val name = METRIC_NAMES.getOrElse(index) { "m$index" }
            sb.append(reportPart(label, name, arr)).append("; ")
        }
        val line = sb.toString().ifBlank { "$label no frames captured" }
        Log.i(TAG, line)
        return line
    }

    private fun reportPart(label: String, name: String, buckets: SparseIntArray): String {
        var total = 0
        var janky = 0
        for (i in 0 until buckets.size()) {
            val count = buckets.valueAt(i)
            total += count
            if (buckets.keyAt(i) > 16) janky += count
        }
        if (total == 0) return "$label $name none"
        fun percentile(p: Double): Int {
            val target = (total * p).toInt().coerceAtLeast(1)
            var acc = 0
            for (i in 0 until buckets.size()) {
                acc += buckets.valueAt(i)
                if (acc >= target) return buckets.keyAt(i)
            }
            return buckets.keyAt(buckets.size() - 1)
        }
        return "$name n=$total over16=${pct(janky, total)} " +
            "p50=${percentile(0.50)} p90=${percentile(0.90)} p99=${percentile(0.99)}"
    }

    private fun writeResult(label: String, text: String) {
        runCatching {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val file = java.io.File(context.getExternalFilesDir(null), "perf_$label.txt")
            file.writeText(text)
            Log.i(TAG, "written to ${file.absolutePath}")
        }.onFailure { Log.i(TAG, "write failed: ${it.message}") }
    }

    private fun pct(part: Int, total: Int): String = "%.1f%%".format(part * 100.0 / total)

    private companion object {
        const val TAG = "NIMBUS_PERF"

        val ALL_METRICS = FrameMetricsAggregator.EVERY_DURATION

        val METRIC_NAMES = arrayOf(
            "total", "input", "layout", "draw", "sync", "command", "swap", "delay", "anim"
        )
    }
}
