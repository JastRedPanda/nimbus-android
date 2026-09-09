package com.nimbus.weather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.data.model.CurrentWeather
import com.nimbus.weather.ui.theme.LocalGlassDark
import com.nimbus.weather.ui.theme.skyTextColors
import com.nimbus.weather.util.TemperatureUnit
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.formatTime
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import com.nimbus.weather.util.weatherDescriptionRes
import com.nimbus.weather.util.windDirection

@Composable
fun CurrentWeatherCard(
    current: CurrentWeather,
    cityName: String,
    sunrise: String,
    sunset: String,
    tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        val t = skyTextColors(LocalGlassDark.current)
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = cityName,
                style = MaterialTheme.typography.titleLarge,
                color = t.title
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                WeatherIcon(
                    code = current.weatherCode,
                    size = 64.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                val animatedTemp by animateIntAsState(
                    targetValue = current.temperature.toCelsiusOrFahrenheit(tempUnit).toInt(),
                    animationSpec = tween(durationMillis = 600),
                    label = "temp"
                )
                Text(
                    text = "$animatedTemp${tempUnit.displayString()}",
                    style = MaterialTheme.typography.displayLarge,
                    color = t.title
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(weatherDescriptionRes(current.weatherCode)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = t.body
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${stringResource(R.string.feels_like)} ${current.apparentTemperature.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = t.subtle
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherDetailItem(
                    icon = Icons.Default.WaterDrop,
                    iconTint = Color(0xFF64B5F6),
                    label = stringResource(R.string.precipitation),
                    value = "${current.precipitation} mm"
                )
                WeatherDetailItem(
                    icon = Icons.Default.Opacity,
                    iconTint = Color(0xFF4FC3F7),
                    label = stringResource(R.string.humidity),
                    value = "${current.humidity.toInt()}%"
                )
                WeatherDetailItem(
                    icon = Icons.Default.WbSunny,
                    iconTint = Color(0xFFFFD54F),
                    label = stringResource(R.string.uv_index),
                    value = "${current.uvIndex.toInt()} (${stringResource(current.uvIndex.uvCategory())})"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherDetailItem(
                    icon = Icons.Default.Air,
                    iconTint = Color(0xFF81D4FA),
                    label = stringResource(R.string.wind),
                    value = "${current.windSpeed.toInt()} ${stringResource(R.string.wind_ms)} ${windDirection(current.windDirection)}"
                )
                WeatherDetailItem(
                    icon = Icons.Default.Compress,
                    iconTint = Color(0xFFCE93D8),
                    label = stringResource(R.string.pressure),
                    value = "${current.pressure.toInt()} ${stringResource(R.string.pressure_hpa)}"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherDetailItem(
                    icon = Icons.Default.WbSunny,
                    iconTint = Color(0xFFFFB74D),
                    label = stringResource(R.string.sunrise),
                    value = formatTime(sunrise)
                )
                WeatherDetailItem(
                    icon = Icons.Default.WbTwilight,
                    iconTint = Color(0xFFFF8A65),
                    label = stringResource(R.string.sunset),
                    value = formatTime(sunset)
                )
            }
        }
    }
}

@Composable
private fun WeatherDetailItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    val t = skyTextColors(LocalGlassDark.current)
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = t.title
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = t.subtle
        )
    }
}
