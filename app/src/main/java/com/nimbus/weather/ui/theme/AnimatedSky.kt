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

/**
 * Клуб облака: центр + два субкруга со сдвигом — дают рваный край
 * вместо идеального круга. Тёмные и светлые кластеры рисуются отдельно.
 */
private class CloudPuff(
    var x: Float = 0f,
    var y: Float = 0f,
    var r: Float = 0f,
    var speed: Float = 0f,
    var phase: Float = 0f,
    var alpha: Float = 0f,
    var dx1: Float = 0f,
    var dy1: Float = 0f,
    var s1: Float = 1f,
    var dx2: Float = 0f,
    var dy2: Float = 0f,
    var s2: Float = 1f,
    var dx3: Float = 0f,
    var dy3: Float = 0f,
    var s3: Float = 1f
)

@Composable
fun AnimatedSky(
    weatherCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val effect = skyEffectFor(weatherCode, isDay)
    if (effect == SkyEffect.NONE) return

    val density = LocalDensity.current
    var frame by remember(effect) { mutableLongStateOf(0L) }

    val particles = remember(effect) {
        val count = when (effect) {
            SkyEffect.RAIN -> 90
            SkyEffect.SNOW -> 140
            SkyEffect.THUNDER -> 70
            SkyEffect.FOG -> 7
            SkyEffect.STARS -> 60
            else -> 0
        }
        List(count) { Particle() }
    }
    val cloudDark = remember(effect) { List(12) { CloudPuff() } }
    val cloudLight = remember(effect) { List(12) { CloudPuff() } }
    // Молния: время следующей вспышки и её длительность, мс
    val lightning = remember(effect) { longArrayOf(0L, 0L) }
    // Точки разряда: главный канал (до 16 точек) + до 3 веток по 6 точек.
    // Генерируются раз за вспышку, рисуются каждый кадр вспышки.
    val boltPts = remember(effect) { FloatArray(32) }
    val boltN = remember(effect) { intArrayOf(0) }
    val branchPts = remember(effect) { FloatArray(36) }
    val branchN = remember(effect) { intArrayOf(0) }

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
        if (w <= 0f || h <= 0f) return@Canvas
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
                        lightning[0] = nowMs + 2500L + random.nextLong(3500L)
                        lightning[1] = nowMs + 220L
                        // Новый разряд: ломаная сверху вниз + ветки.
                        var x = random.nextFloat() * w
                        var y = -20f
                        var n = 0
                        boltPts[n++] = x
                        boltPts[n++] = y
                        val bottom = h * (0.7f + random.nextFloat() * 0.25f)
                        while (y < bottom && n < 30) {
                            x = (x + (random.nextFloat() - 0.5f) * 90f).coerceIn(0f, w)
                            y += 50f + random.nextFloat() * 70f
                            boltPts[n++] = x
                            boltPts[n++] = y
                        }
                        boltN[0] = n / 2
                        // Ветки от случайных точек канала, вбок-вниз.
                        var b = 0
                        val branches = 1 + random.nextInt(3)
                        repeat(branches) {
                            if (b + 12 > branchPts.size) return@repeat
                            val from = 2 * (1 + random.nextInt((boltN[0] - 1).coerceAtLeast(1)))
                            var bx = boltPts[from]
                            var by = boltPts[from + 1]
                            val dir = if (random.nextBoolean()) 1f else -1f
                            repeat(6) {
                                bx = (bx + dir * (20f + random.nextFloat() * 40f)).coerceIn(0f, w)
                                by += 30f + random.nextFloat() * 50f
                                branchPts[b++] = bx
                                branchPts[b++] = by
                            }
                        }
                        branchN[0] = b / 2
                    }
                    if (nowMs < lightning[1]) {
                        val blink = if ((nowMs / 60) % 2 == 0L) 1f else 0.35f
                        drawRect(Color.White.copy(alpha = 0.10f * blink))
                        val glowWidth = with(density) { 9.dp.toPx() }
                        val coreWidth = with(density) { 2.5.dp.toPx() }
                        val glow = Color(0xFFB388FF).copy(alpha = 0.30f * blink)
                        val core = Color.White.copy(alpha = 0.95f * blink)
                        fun stroke(
                            pts: FloatArray, from: Int, count: Int, color: Color, width: Float
                        ) {
                            var i = from * 2
                            val last = (from + count) * 2
                            while (i + 3 < last) {
                                drawLine(
                                    color = color,
                                    start = Offset(pts[i], pts[i + 1]),
                                    end = Offset(pts[i + 2], pts[i + 3]),
                                    strokeWidth = width
                                )
                                i += 2
                            }
                        }
                        var v = 0
                        while (v + 6 <= branchN[0]) {
                            stroke(branchPts, v, 6, glow, glowWidth)
                            stroke(branchPts, v, 6, core, coreWidth)
                            v += 6
                        }
                        if (v < branchN[0]) {
                            stroke(branchPts, v, branchN[0] - v, glow, glowWidth)
                            stroke(branchPts, v, branchN[0] - v, core, coreWidth)
                        }
                        stroke(boltPts, 0, boltN[0], glow, glowWidth)
                        stroke(boltPts, 0, boltN[0], core, coreWidth)
                    }
                }
            }
            SkyEffect.SNOW -> {
                // Два плана глубины: дальние мелкие и чёткие, ближние
                // крупные и бледные (боке). Общий снос ветром.
                val windPx = with(density) { 15.dp.toPx() } / 30f
                particles.forEachIndexed { i, p ->
                    val far = i % 3 != 2
                    if (p.speed == 0f) {
                        p.x = random.nextFloat() * w
                        p.y = random.nextFloat() * h
                        p.speed = with(density) {
                            if (far) {
                                60.dp.toPx() + random.nextFloat() * 50.dp.toPx()
                            } else {
                                120.dp.toPx() + random.nextFloat() * 80.dp.toPx()
                            }
                        } / 30f
                        p.size = with(density) {
                            if (far) {
                                1.dp.toPx() + random.nextFloat() * 1.dp.toPx()
                            } else {
                                2.5f.dp.toPx() + random.nextFloat() * 2.dp.toPx()
                            }
                        }
                        p.phase = random.nextFloat() * 6.28f
                        p.alpha = if (far) {
                            0.5f + random.nextFloat() * 0.2f
                        } else {
                            0.45f + random.nextFloat() * 0.35f
                        }
                    }
                    p.y += p.speed
                    p.x += windPx * (if (far) 0.6f else 1f)
                    if (p.y > h + p.size) {
                        p.y = -p.size
                        p.x = random.nextFloat() * w
                    }
                    if (p.x > w + p.size) p.x = -p.size
                    val swayAmp = with(density) {
                        if (far) 12.dp.toPx() else 26.dp.toPx()
                    }
                    val sway = swayAmp * sin(nowMs / 900f + p.phase)
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha),
                        radius = p.size,
                        center = Offset(p.x + sway, p.y)
                    )
                }
            }
            SkyEffect.FOG -> {
                // Молоко: сплошная вуаль во весь экран + крупные медленные
                // волны плотности. Ночью вуаль тёмная, иначе белый текст
                // на светлом тумане не читается.
                val veil = if (isDay) Color.White else Color.Black
                drawRect(color = veil.copy(alpha = if (isDay) 0.28f else 0.35f))
                val driftPx = with(density) { 7.dp.toPx() } / 30f
                particles.forEachIndexed { i, p ->
                    if (p.size == 0f) {
                        p.size = with(density) {
                            140.dp.toPx() + random.nextFloat() * 140.dp.toPx()
                        }
                        p.x = random.nextFloat() * w
                        p.y = random.nextFloat() * h
                        p.speed = driftPx * (0.6f + random.nextFloat() * 0.8f) *
                            if (i % 2 == 0) 1f else -1f
                        p.phase = random.nextFloat() * 6.28f
                        p.alpha = 0.05f + random.nextFloat() * 0.04f
                    }
                    p.x += p.speed
                    if (p.x - p.size > w) p.x = -p.size
                    if (p.x + p.size < 0f) p.x = w + p.size
                    val breathe = p.alpha * (0.8f + 0.2f * sin(nowMs / 2200f + p.phase))
                    drawCircle(
                        color = veil.copy(alpha = breathe),
                        radius = p.size,
                        center = Offset(p.x, p.y)
                    )
                }
            }
            SkyEffect.CLOUDS -> {
                // Клубы: тёмные сгущения + светлые просветы со сдвигом,
                // дрейф в одну сторону (ветер) + медленное «дыхание».
                fun seed(c: CloudPuff, dark: Boolean) {
                    c.r = with(density) { 60.dp.toPx() + random.nextFloat() * 100.dp.toPx() }
                    c.x = random.nextFloat() * w
                    c.y = random.nextFloat() * h
                    c.speed = with(density) {
                        (4.dp.toPx() + random.nextFloat() * 8.dp.toPx())
                    } / 30f
                    c.phase = random.nextFloat() * 6.28f
                    c.alpha = if (dark) {
                        0.10f + random.nextFloat() * 0.06f
                    } else {
                        0.08f + random.nextFloat() * 0.06f
                    }
                    fun sub(): Triple<Float, Float, Float> = Triple(
                        (random.nextFloat() - 0.5f) * c.r,
                        (random.nextFloat() - 0.5f) * c.r * 0.6f,
                        0.5f + random.nextFloat() * 0.3f
                    )
                    val a = sub()
                    c.dx1 = a.first
                    c.dy1 = a.second
                    c.s1 = a.third
                    val b = sub()
                    c.dx2 = b.first
                    c.dy2 = b.second
                    c.s2 = b.third
                    val d = sub()
                    c.dx3 = d.first
                    c.dy3 = d.second
                    c.s3 = d.third
                }
                // Мягкий круг: бледный внешний + плотная середина,
                // край не читается окружностью.
                fun softCircle(center: Offset, r: Float, color: Color) {
                    drawCircle(color, r, center)
                    drawCircle(
                        color.copy(alpha = (color.alpha * 1.8f).coerceAtMost(1f)),
                        r * 0.6f,
                        center
                    )
                }
                fun drawPuff(c: CloudPuff, dark: Boolean) {
                    if (c.r == 0f) seed(c, dark)
                    c.x += c.speed
                    if (c.x - c.r * 1.6f > w) c.x = -c.r * 1.6f
                    val breathe = 1f + 0.06f * sin(nowMs / 2400f + c.phase)
                    var alpha = c.alpha * (0.85f + 0.15f * sin(nowMs / 1700f + c.phase))
                    // Ночью светлые просветы почти гаснут, тёмные густеют.
                    alpha *= if (dark) {
                        if (isDay) 1f else 1.2f
                    } else {
                        if (isDay) 1f else 0.4f
                    }
                    val color = if (dark) {
                        Color.Black.copy(alpha = alpha.coerceAtMost(1f))
                    } else {
                        Color.White.copy(alpha = alpha)
                    }
                    val r = c.r * breathe
                    val center = Offset(c.x, c.y)
                    softCircle(center, r, color)
                    softCircle(Offset(c.x + c.dx1, c.y + c.dy1), r * c.s1, color)
                    softCircle(Offset(c.x + c.dx2, c.y + c.dy2), r * c.s2, color)
                    softCircle(Offset(c.x + c.dx3, c.y + c.dy3), r * c.s3, color)
                }
                cloudDark.forEach { drawPuff(it, dark = true) }
                cloudLight.forEach { drawPuff(it, dark = false) }
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
