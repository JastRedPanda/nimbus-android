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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.withFrameNanos
import kotlin.math.roundToInt
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
 * Мягкий спрайт клуба 64×64: value noise с радиальной маской —
 * края гарантированно прозрачные, штампы не дают швов и пузырей.
 * Цвет запекается параметрами, плотность — в альфе.
 */
private const val PUFF_SPRITE_PX = 64

private fun generatePuffSprite(
    seed: Long,
    r: Int,
    g: Int,
    b: Int
): android.graphics.Bitmap {
    val size = PUFF_SPRITE_PX
    val random = Random(seed)
    val period = 4
    val grid = FloatArray(period * period) { random.nextFloat() }
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val fx = x.toFloat() / size * period
            val fy = y.toFloat() / size * period
            val x0 = fx.toInt() % period
            val y0 = fy.toInt() % period
            val x1 = (x0 + 1) % period
            val y1 = (y0 + 1) % period
            val tx = fx - fx.toInt()
            val ty = fy - fy.toInt()
            val sx = tx * tx * (3f - 2f * tx)
            val sy = ty * ty * (3f - 2f * ty)
            val a = grid[y0 * period + x0]
            val bb = grid[y0 * period + x1]
            val c = grid[y1 * period + x0]
            val d = grid[y1 * period + x1]
            val v = (a + (bb - a) * sx) + ((c + (d - c) * sx) - (a + (bb - a) * sx)) * sy
            val nx = (x + 0.5f) / size - 0.5f
            val ny = (y + 0.5f) / size - 0.5f
            val dist = kotlin.math.sqrt(nx * nx + ny * ny) * 2f
            val mask = (1f - dist.coerceIn(0f, 1f)).let { it * it * (3f - 2f * it) }
            val t = ((v - 0.3f) / 0.4f).coerceIn(0f, 1f)
            val s = t * t * (3f - 2f * t) * mask
            pixels[y * size + x] = android.graphics.Color.argb(
                (s * 255).toInt(), r, g, b
            )
        }
    }
    return android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        .also { it.setPixels(pixels, 0, size, 0, 0, size, size) }
}

private class PuffStamp(
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 0f,
    var speed: Float = 0f,
    var phase: Float = 0f,
    var alpha: Float = 0f
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
    // Спрайты клубов: серая тень дня, чёрная тень ночи, белая просветка.
    val spriteGray = remember(effect) { generatePuffSprite(1234567L, 0x61, 0x61, 0x61) }
    val spriteBlack = remember(effect) { generatePuffSprite(1234567L, 0, 0, 0) }
    val spriteWhite = remember(effect) { generatePuffSprite(7654321L, 255, 255, 255) }
    val stampsDark = remember(effect) { List(16) { PuffStamp() } }
    val stampsLight = remember(effect) { List(16) { PuffStamp() } }
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
                // Клубы — штампы из мягкого спрайта: цельные объекты,
                // швам неоткуда взяться. Тёмные и светлые со встречным
                // дрейфом — перетекание. Днём тень серая, ночью чёрная.
                fun seed(s: PuffStamp, dark: Boolean) {
                    s.scale = with(density) { 150.dp.toPx() + random.nextFloat() * 250.dp.toPx() }
                    s.x = random.nextFloat() * w - s.scale * 0.25f
                    s.y = random.nextFloat() * h - s.scale * 0.25f
                    s.speed = with(density) {
                        (6.dp.toPx() + random.nextFloat() * 6.dp.toPx())
                    } / 30f * if (dark) 1f else -1f
                    s.phase = random.nextFloat() * 6.28f
                    s.alpha = if (dark) {
                        0.30f + random.nextFloat() * 0.15f
                    } else {
                        0.35f + random.nextFloat() * 0.15f
                    }
                }
                fun drawStamps(
                    stamps: List<PuffStamp>,
                    sprite: android.graphics.Bitmap,
                    dark: Boolean
                ) {
                    val img = sprite.asImageBitmap()
                    stamps.forEach { s ->
                        if (s.scale == 0f) seed(s, dark)
                        s.x += s.speed
                        val m = s.scale * 0.5f
                        if (s.speed > 0f && s.x - m > w) s.x = -m
                        if (s.speed < 0f && s.x + m < 0f) s.x = w + m
                        val breathe = 0.85f + 0.15f * sin(nowMs / 1700f + s.phase)
                        val px = s.scale.roundToInt().coerceAtLeast(1)
                        drawImage(
                            image = img,
                            dstOffset = androidx.compose.ui.unit.IntOffset(
                                (s.x - m).roundToInt(), (s.y - m).roundToInt()
                            ),
                            dstSize = androidx.compose.ui.unit.IntSize(px, px),
                            alpha = (s.alpha * breathe).coerceIn(0f, 1f)
                        )
                    }
                }
                if (isDay) {
                    drawStamps(stampsDark, spriteGray, dark = true)
                    drawStamps(stampsLight, spriteWhite, dark = false)
                } else {
                    drawStamps(stampsDark, spriteBlack, dark = true)
                    drawStamps(stampsLight, spriteWhite, dark = false)
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
