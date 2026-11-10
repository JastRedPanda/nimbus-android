package com.nimbus.weather.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nimbus.weather.util.weatherIcon

fun weatherCodePulse(code: Int): Boolean = when (code) {
    in 51..67, in 71..86, 95, 96, 99 -> true
    else -> false
}

/**
 * Общая фаза пульсации иконок на экране.
 *
 * В почасовой ленте и недельном блоке одновременно живут десятки иконок.
 * Своя [rememberInfiniteTransition] у каждой — это столько же независимых
 * анимаций, каждая будит Choreographer. Провайдер задаёт одно значение
 * на экран; чтение происходит в draw-фазе через `graphicsLayer`, поэтому
 * рекомпозиции нет. Без провайдера иконка пульсирует своей анимацией.
 */
val LocalWeatherIconPulse = compositionLocalOf<State<Float>?> { null }

@Composable
fun WeatherIcon(
    code: Int,
    isDay: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    AnimatedContent(
        targetState = code to isDay,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
        label = "weather_icon"
    ) { (targetCode, targetDay) ->
        // Цветные векторы рисуются как есть, без tint.
        val iconModifier = modifier.size(size)
        if (weatherCodePulse(targetCode)) {
            val shared = LocalWeatherIconPulse.current
            // Значение читается внутри graphicsLayer, то есть в draw-фазе:
            // общая анимация не вызывает рекомпозицию ни одной иконки.
            val scale: () -> Float = if (shared != null) {
                { shared.value }
            } else {
                val transition = rememberInfiniteTransition(label = "weather_pulse")
                val own = transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                    label = "scale"
                )
                val fallback: () -> Float = { own.value }
                fallback
            }
            Image(
                painter = painterResource(weatherIcon(targetCode, targetDay)),
                contentDescription = null,
                colorFilter = null,
                modifier = iconModifier.graphicsLayer {
                    val value = scale()
                    scaleX = value
                    scaleY = value
                }
            )
        } else {
            Image(
                painter = painterResource(weatherIcon(targetCode, targetDay)),
                contentDescription = null,
                colorFilter = null,
                modifier = iconModifier
            )
        }
    }
}
