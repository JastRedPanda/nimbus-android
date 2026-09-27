package com.nimbus.weather.ui.theme

import android.graphics.Bitmap
import android.graphics.PorterDuff
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * «Мокрое стекло» поверх фона при дожде/грозе: слой росы, по которому
 * ползут вниз капли-бегуны, оставляя затухающие мокрые следы.
 *
 * Следы живут в отдельном битмапе: каждый кадр он чуть гасится
 * ([TRAIL_KEEP] через DST_IN), бегуны домазывают новые отрезки.
 * Роса и бегуны — пулы без аллокаций в кадре, ~30 fps.
 */
private const val FRAME_STEP_NANOS = 33_333_333L
private const val DEW_COUNT = 150
private const val RUNNER_COUNT = 5
private const val TRAIL_KEEP = 0.985f
private const val MAX_RUNNER_DP = 8f

/**
 * Объёмная капля как на мокром стекле: тёмная кромка, светлая линза
 * со смещением вверх, тёмный мениск снизу, светлый ободок справа и блик.
 * Крупные капли слегка вытянуты вертикально. Без аллокаций.
 */
private fun DrawScope.drawGlassDrop(
    cx: Float,
    cy: Float,
    r: Float,
    tall: Boolean,
    alphaScale: Float = 1f
) {
    val ry = if (tall) r * 1.15f else r
    val edge = Color.Black.copy(alpha = (0.45f * alphaScale).coerceAtMost(1f))
    // 1. Тёмная кромка — контур и нижняя тень капли.
    drawOval(
        color = edge,
        topLeft = Offset(cx - r, cy - ry),
        size = Size(r * 2f, ry * 2f)
    )
    // 2. Линза — просвечивающий фон, центр выше (свет идёт сверху).
    drawOval(
        color = Color.White.copy(alpha = (0.16f * alphaScale).coerceAtMost(1f)),
        topLeft = Offset(cx - r * 0.8f, cy - ry * 0.8f - r * 0.2f),
        size = Size(r * 1.6f, ry * 1.6f)
    )
    // 3. Тёмный мениск снизу.
    drawArc(
        color = edge,
        startAngle = 25f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.8f, cy - ry * 0.8f),
        size = Size(r * 1.6f, ry * 1.6f),
        style = Stroke(width = (r * 0.14f).coerceAtLeast(1f))
    )
    // 4. Светлый ободок справа.
    drawArc(
        color = Color.White.copy(alpha = (0.5f * alphaScale).coerceAtMost(1f)),
        startAngle = -40f,
        sweepAngle = 50f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.8f, cy - ry * 0.8f),
        size = Size(r * 1.6f, ry * 1.6f),
        style = Stroke(width = (r * 0.1f).coerceAtLeast(1f))
    )
    // 5. Блик сверху слева.
    drawCircle(
        color = Color.White.copy(alpha = (0.85f * alphaScale).coerceAtMost(1f)),
        radius = r * 0.18f,
        center = Offset(cx - r * 0.32f, cy - ry * 0.38f)
    )
}

private class Dew(
    var x: Float = -1f,
    var y: Float = -1f,
    var r: Float = 0f,
    var alpha: Float = 0f
)

private class Runner(
    var x: Float = 0f,
    var y: Float = 0f,
    var r: Float = 0f,
    var base: Float = 0f,
    var phase: Float = 0f,
    var swayAmp: Float = 0f
)

@Composable
fun GlassRainOverlay(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    var frame by remember { mutableLongStateOf(0L) }

    val dew = remember { List(DEW_COUNT) { Dew() } }
    val runners = remember { List(RUNNER_COUNT) { Runner() } }
    // Битмап следов пересоздаётся под размер Canvas, старый recycled.
    val trail = remember { arrayOfNulls<Bitmap>(1) }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            if (now - last >= FRAME_STEP_NANOS) {
                last = now
                frame++
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val random = Random(1234567L)
        val nowMs = frame * 33L

        val dewMin = with(density) { 1.dp.toPx() }
        val dewMax = with(density) { 3.5.dp.toPx() }
        val runnerMin = with(density) { 4.dp.toPx() }
        val runnerMax = with(density) { MAX_RUNNER_DP.dp.toPx() }

        // Росе раздаём места один раз (метка x < 0 — «не посажена»).
        // 8% — крупные статичные капли, как на мокром стекле.
        dew.forEach { d ->
            if (d.x < 0f) {
                d.x = random.nextFloat() * w
                d.y = random.nextFloat() * h
                d.r = if (random.nextFloat() < 0.08f) {
                    runnerMin + random.nextFloat() * (runnerMax - runnerMin) * 0.5f
                } else {
                    dewMin + random.nextFloat() * (dewMax - dewMin)
                }
                d.alpha = 0.7f + random.nextFloat() * 0.6f
            }
            // Медленный рост, капля не больше бегуна-новичка.
            d.r = (d.r + with(density) { 0.004.dp.toPx() }).coerceAtMost(runnerMin)
        }

        // Бегуны: стоп-старт через синус, виляние, рост за счёт росы.
        runners.forEach { r ->
            if (r.r == 0f) {
                r.x = random.nextFloat() * w
                r.y = -runnerMax - random.nextFloat() * h * 0.3f
                r.r = runnerMin + random.nextFloat() * (runnerMax - runnerMin) * 0.4f
                r.base = with(density) { (140.dp.toPx() + random.nextFloat() * 120.dp.toPx()) } / 30f
                r.phase = random.nextFloat() * 6.28f
                r.swayAmp = with(density) { (6.dp.toPx() + random.nextFloat() * 10.dp.toPx()) }
            }
            val gate = 0.25f + 0.75f * abs(sin(nowMs / 1400f + r.phase))
            val prevX = r.x
            val prevY = r.y
            r.y += r.base * gate * (r.r / runnerMin)
            r.x += r.swayAmp * sin(r.y / h * 9f + r.phase) / 30f
            // Поглощение росы по пути: бегун растёт.
            dew.forEach { d ->
                if (d.x >= 0f) {
                    val dx = d.x - r.x
                    val dy = d.y - r.y
                    if (dx * dx + dy * dy < (r.r + d.r) * (r.r + d.r)) {
                        r.r = (r.r + d.r * 0.15f).coerceAtMost(runnerMax)
                        d.x = -1f
                    }
                }
            }
            // Мазок следа в битмап.
            trailBitmap(w.toInt(), h.toInt(), trail)?.let { bmp ->
                val canvas = android.graphics.Canvas(bmp)
                val p = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(77, 255, 255, 255)
                    strokeWidth = r.r * 0.8f
                    strokeCap = android.graphics.Paint.Cap.ROUND
                }
                canvas.drawLine(prevX, prevY, r.x, r.y, p)
            }
            if (r.y - r.r > h) r.r = 0f
        }

        // Гасим следы и выводим слой.
        trailBitmap(w.toInt(), h.toInt(), trail)?.let { bmp ->
            val canvas = android.graphics.Canvas(bmp)
            val fade = android.graphics.Paint().apply {
                xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                alpha = (TRAIL_KEEP * 255).toInt()
            }
            canvas.drawRect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(), fade)
            drawIntoCanvas { c: ComposeCanvas ->
                c.nativeCanvas.drawBitmap(bmp, 0f, 0f, null)
            }
        }

        // Роса — объёмные капли.
        dew.forEach { d ->
            if (d.x < 0f) return@forEach
            drawGlassDrop(d.x, d.y, d.r, tall = d.r >= runnerMin, alphaScale = d.alpha)
        }
        // Бегуны — крупные вытянутые капли.
        runners.forEach { r ->
            if (r.r == 0f || r.y < -r.r) return@forEach
            drawGlassDrop(r.x, r.y, r.r, tall = true)
        }
    }
}

/** Битмап следов под текущий размер; при смене размера старый recycled. */
private fun trailBitmap(w: Int, h: Int, holder: Array<Bitmap?>): Bitmap? {
    val current = holder[0]
    if (current != null && !current.isRecycled && current.width == w && current.height == h) {
        return current
    }
    runCatching { current?.recycle() }
    return runCatching {
        Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            .also { holder[0] = it }
    }.getOrNull()
}
