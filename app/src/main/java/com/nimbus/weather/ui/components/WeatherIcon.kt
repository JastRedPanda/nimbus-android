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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nimbus.weather.util.weatherIcon

fun weatherCodePulse(code: Int): Boolean = when (code) {
    in 51..67, in 71..86, 95, 96, 99 -> true
    else -> false
}

@Composable
fun WeatherIcon(
    code: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    AnimatedContent(
        targetState = code,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
        label = "weather_icon"
    ) { targetCode ->
        if (weatherCodePulse(targetCode)) {
            val transition = rememberInfiniteTransition(label = "weather_pulse")
            val scale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "scale"
            )
            Icon(
                imageVector = weatherIcon(targetCode),
                contentDescription = null,
                modifier = modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        } else {
            Icon(
                imageVector = weatherIcon(targetCode),
                contentDescription = null,
                modifier = modifier.size(size)
            )
        }
    }
}