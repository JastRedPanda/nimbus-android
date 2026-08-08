package com.nimbus.weather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.data.model.CurrentWeather
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = cityName,
                style = MaterialTheme.typography.titleLarge
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
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(weatherDescriptionRes(current.weatherCode)),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${stringResource(R.string.feels_like)} ${current.apparentTemperature.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherDetailItem(
                    label = stringResource(R.string.precipitation),
                    value = "${current.precipitation} mm"
                )
                WeatherDetailItem(
                    label = stringResource(R.string.humidity),
                    value = "${current.humidity.toInt()}%"
                )
                WeatherDetailItem(
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
                    label = stringResource(R.string.wind),
                    value = "${current.windSpeed.toInt()} ${stringResource(R.string.wind_ms)} ${windDirection(current.windDirection)}"
                )
                WeatherDetailItem(
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
                    label = stringResource(R.string.sunrise),
                    value = formatTime(sunrise)
                )
                WeatherDetailItem(
                    label = stringResource(R.string.sunset),
                    value = formatTime(sunset)
                )
            }
        }
    }
}

@Composable
private fun WeatherDetailItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
