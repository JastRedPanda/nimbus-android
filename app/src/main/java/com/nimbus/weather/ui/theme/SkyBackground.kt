package com.nimbus.weather.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Флаг «небо тёмное» для хрома прямо поверх неба
 * (топбар, заголовки секций, лоадер).
 * Провайдится в HomeScreen из погоды и времени суток.
 */
val LocalSkyDark = compositionLocalOf { true }

/**
 * Флаг «стекло тёмное» для карточек: диктуется темой приложения,
 * а не небом. Тёмная тема — тонировка в тёмное, светлая — в белое.
 * Провайдится в HomeScreen из яркости surface текущей colorScheme.
 */
val LocalGlassDark = compositionLocalOf { true }

/**
 * Три уровня текста поверх неба: заголовки, основной, приглушённый.
 */
data class SkyTextColors(
    val title: Color,
    val body: Color,
    val subtle: Color
)

/**
 * Палитра текста под яркость неба.
 * На тёмном — белый набор (как на макетах), на светлом — чернильный.
 */
fun skyTextColors(dark: Boolean): SkyTextColors = if (dark) {
    SkyTextColors(
        title = Color.White,
        body = Color.White.copy(alpha = 0.85f),
        subtle = Color.White.copy(alpha = 0.60f)
    )
} else {
    val ink = Color(0xFF102027)
    SkyTextColors(
        title = ink,
        body = ink.copy(alpha = 0.85f),
        subtle = ink.copy(alpha = 0.60f)
    )
}

/**
 * Динамические палитры «неба» для фона главного экрана.
 * Подбираются по WMO-коду погоды и флагу «день/ночь».
 */
object SkyPalette {

    /* ─── Ночные ─── */

    /** Ясная ночь — глубокий индиго → чернильный */
    private val NightClear = listOf(
        Color(0xFF0B0E2D),
        Color(0xFF121845),
        Color(0xFF1B2566)
    )

    /** Облачная ночь — тёмно-серо-синяя */
    private val NightOvercast = listOf(
        Color(0xFF0D1117),
        Color(0xFF161B22),
        Color(0xFF21262D)
    )

    /** Ночная гроза — фиолетово-электрический */
    private val NightThunder = listOf(
        Color(0xFF0D0221),
        Color(0xFF1A0533),
        Color(0xFF2D0A4E)
    )

    /* ─── Дневные ─── */

    /** Ясный день — лазурно-небесный */
    private val DayClear = listOf(
        Color(0xFF1565C0),
        Color(0xFF1E88E5),
        Color(0xFF42A5F5),
        Color(0xFF90CAF9)
    )

    /** Облачный день — тёплый светло-серый (днём светлый, хром светлый) */
    private val DayOvercast = listOf(
        Color(0xFF9E9E9E),
        Color(0xFFBDBDBD),
        Color(0xFFE0E0E0),
        Color(0xFFF5F5F5)
    )

    /** Дождь днём — средне-серая мгла (темнее облачности, но день) */
    private val DayRain = listOf(
        Color(0xFF546E7A),
        Color(0xFF607D8B),
        Color(0xFF78909C),
        Color(0xFF90A4AE)
    )

    /** Гроза днём — светло-сиреневая (характер kept, хром светлый) */
    private val DayThunder = listOf(
        Color(0xFF7986CB),
        Color(0xFF9FA8DA),
        Color(0xFFC5CAE9),
        Color(0xFFE8EAF6)
    )

    /** Снег (день) — холодно-белёсый */
    private val DaySnow = listOf(
        Color(0xFF78909C),
        Color(0xFF90A4AE),
        Color(0xFFB0BEC5),
        Color(0xFFCFD8DC)
    )

    /** Ночной снег */
    private val NightSnow = listOf(
        Color(0xFF1C2331),
        Color(0xFF263040),
        Color(0xFF374357)
    )

    /** Туман */
    private val Fog = listOf(
        Color(0xFF546E7A),
        Color(0xFF78909C),
        Color(0xFFB0BEC5)
    )

    /**
     * Тёмное ли небо: ночью — всегда; днём тёмный хром только на ясном
     * небе (насыщенно-синий верх), всё остальное днём светлое.
     */
    fun isDarkSky(weatherCode: Int, isDay: Boolean): Boolean {
        if (!isDay) return true
        return weatherCode in 0..1
    }

    /**
     * Подбирает [Brush] вертикального градиента неба
     * по WMO-коду погоды и признаку дня/ночи.
     *
     * @param weatherCode WMO weather code
     * @param isDay true — дневное время
     */
    fun resolveSkyBrush(weatherCode: Int, isDay: Boolean): Brush {
        val colors = when {
            // Гроза (WMO 95–99)
            weatherCode >= 95 -> if (isDay) DayThunder else NightThunder

            // Снег / снежные зёрна (WMO 71–77, 85–86)
            weatherCode in 71..77 || weatherCode in 85..86 ->
                if (isDay) DaySnow else NightSnow

            // Дождь / морось (WMO 51–67, 80–82)
            weatherCode in 51..67 || weatherCode in 80..82 ->
                if (isDay) DayRain else NightOvercast

            // Туман (WMO 45, 48)
            weatherCode == 45 || weatherCode == 48 -> Fog

            // Облачно (WMO 2–3)
            weatherCode >= 2 -> if (isDay) DayOvercast else NightOvercast

            // Ясно / малооблачно (WMO 0–1)
            else -> if (isDay) DayClear else NightClear
        }
        return Brush.verticalGradient(colors)
    }
}
