package com.nimbus.weather.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.withFrameNanos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Живой фон главного экрана: частицы на Canvas поверх градиента неба.
 *
 * Дешевле GIF/WebP-слоя: ноль веса APK, частицы рисуются в цветах текущей
 * палитры (белый с альфой ложится на любое небо), лимиты зафиксированы
 * (десятки частиц, ~30 fps), вне экрана Compose сам останавливает
 * перерисовку. Без аллокаций в кадре — пул частиц мутируется на месте.
 */
enum class SkyEffect {
    NONE, RAIN, SNOW, THUNDER, FOG, CLOUDS, STARS
}

/** Маппинг WMO-кода в эффект — те же границы, что в [SkyPalette]. */
fun skyEffectFor(weatherCode: Int, isDay: Boolean): SkyEffect {
    return when {
        weatherCode >= 95 -> SkyEffect.THUNDER
        weatherCode in 71..77 || weatherCode in 85..86 -> SkyEffect.SNOW
        weatherCode in 51..67 || weatherCode in 80..82 -> SkyEffect.RAIN
        weatherCode == 45 || weatherCode == 48 -> SkyEffect.FOG
        weatherCode >= 2 -> SkyEffect.CLOUDS
        isDay -> SkyEffect.NONE
        else -> SkyEffect.STARS
    }
}

private const val FRAME_STEP_NANOS = 33_333_333L // ~30 fps для фона достаточно

private class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var speed: Float = 0f,
    var size: Float = 0f,
    var phase: Float = 0f,
    var alpha: Float = 1f
)

@Composable
fun AnimatedSky(
    weatherCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val effect = skyEffectFor(weatherCode, isDay)
    if (effect == SkyEffect.NONE) return

    val context = LocalContext.current
    val density = LocalDensity.current
    var frame by remember(effect) { mutableLongStateOf(0L) }

    val particles = remember(effect) {
        val count = when (effect) {
            SkyEffect.RAIN -> 90
            SkyEffect.SNOW -> 70
            SkyEffect.THUNDER -> 70
            SkyEffect.FOG -> 5
            SkyEffect.CLOUDS -> 4
            SkyEffect.STARS -> 60
            SkyEffect.NONE -> 0
        }
        List(count) { Particle() }
    }
    // Молния: время следующей вспышки и её длительность, мс
    val lightning = remember(effect) { longArrayOf(0L, 0L) }

    LaunchedEffect(effect) {
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
        if (w <= 0f || h <= 0f || particles.isEmpty()) return@Canvas
        val random = Random(effect.ordinal * 1_000_003L + 7L)
        val nowMs = frame * 33L

        when (effect) {
            SkyEffect.RAIN, SkyEffect.THUNDER -> {
                val speedPx = with(density) { 900.dp.toPx() } / 30f
                val lenPx = with(density) { 14.dp.toPx() }
                val slant = lenPx * 0.25f
                val driftPx = speedPx * 0.08f
                particles.forEach { p ->
                    if (p.speed == 0f) {
                        p.x = random.nextFloat() * (w + slant)
                        p.y = random.nextFloat() * h
                        p.speed = 0.7f + random.nextFloat() * 0.6f
                    }
                    p.y += speedPx * p.speed
                    p.x -= driftPx * p.speed
                    if (p.y > h + lenPx) {
                        p.y = -lenPx
                        p.x = random.nextFloat() * (w + slant)
                    }
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(p.x, p.y),
                        end = Offset(p.x + slant * 0.2f, p.y - lenPx),
                        strokeWidth = with(density) { 1.5.dp.toPx() }
                    )
                }
                if (effect == SkyEffect.THUNDER) {
                    if (nowMs >= lightning[0]) {
                        lightning[0] = nowMs + 2000L + random.nextLong(3000L)
                        lightning[1] = nowMs + 160L
                    }
                    if (nowMs < lightning[1]) {
                        val blink = if ((nowMs / 60) % 2 == 0L) 0.22f else 0.08f
                        drawRect(Color.White.copy(alpha = blink))
                    }
                }
            }
            SkyEffect.SNOW -> {
                particles.forEach { p ->
                    if (p.speed == 0f) {
                        p.x = random.nextFloat() * w
                        p.y = random.nextFloat() * h
                        p.speed = with(density) { (60.dp.toPx() + random.nextFloat() * 80.dp.toPx()) } / 30f
                        p.size = with(density) { (1.5f.dp.toPx() + random.nextFloat() * 2.5f.dp.toPx()) }
                        p.phase = random.nextFloat() * 6.28f
                    }
                    p.y += p.speed
                    if (p.y > h + p.size) {
                        p.y = -p.size
                        p.x = random.nextFloat() * w
                    }
                    val sway = with(density) { 18.dp.toPx() } * sin(nowMs / 900f + p.phase)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = p.size,
                        center = Offset(p.x + sway, p.y)
                    )
                }
            }
            SkyEffect.FOG, SkyEffect.CLOUDS -> {
                val baseAlpha = if (effect == SkyEffect.FOG) 0.07f else 0.09f
                val driftPx = with(density) {
                    (if (effect == SkyEffect.FOG) 8.dp.toPx() else 14.dp.toPx())
                } / 30f
                particles.forEachIndexed { i, p ->
                    if (p.size == 0f) {
                        p.size = with(density) {
                            (90.dp.toPx() + random.nextFloat() * 90.dp.toPx())
                        }
                        p.x = random.nextFloat() * w
                        p.y = h * (0.15f + 0.7f * random.nextFloat())
                        p.speed = driftPx * (0.6f + random.nextFloat() * 0.8f) *
                            if (i % 2 == 0) 1f else -1f
                    }
                    p.x += p.speed
                    if (p.x - p.size > w) p.x = -p.size
                    if (p.x + p.size < 0f) p.x = w + p.size
                    drawCircle(
                        color = Color.White.copy(alpha = baseAlpha),
                        radius = p.size,
                        center = Offset(p.x, p.y)
                    )
                }
            }
            SkyEffect.STARS -> {
                particles.forEach { p ->
                    if (p.size == 0f) {
                        p.x = random.nextFloat() * w
                        p.y = random.nextFloat() * h * 0.7f
                        p.size = with(density) {
                            (0.8f.dp.toPx() + random.nextFloat() * 1.4f.dp.toPx())
                        }
                        p.phase = random.nextFloat() * 6.28f
                        p.speed = 0.5f + random.nextFloat() * 1.5f
                    }
                    val twinkle = 0.45f + 0.35f * sin(nowMs / 1000f * p.speed + p.phase)
                    drawCircle(
                        color = Color.White.copy(alpha = twinkle),
                        radius = p.size,
                        center = Offset(p.x, p.y)
                    )
                }
            }
            SkyEffect.NONE -> Unit
        }
    }
}
