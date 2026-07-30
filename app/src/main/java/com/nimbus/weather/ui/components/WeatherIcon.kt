package com.nimbus.weather.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nimbus.weather.util.weatherIcon

@Composable
fun WeatherIcon(
    code: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Icon(
        imageVector = weatherIcon(code),
        contentDescription = null,
        modifier = modifier.size(size)
    )
}
