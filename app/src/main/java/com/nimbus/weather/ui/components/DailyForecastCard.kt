package com.nimbus.weather.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.util.TemperatureUnit
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.formatDayOfWeek
import com.nimbus.weather.util.formatTime
import com.nimbus.weather.util.isToday
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import com.nimbus.weather.util.weatherDescriptionRes
import com.nimbus.weather.util.windDirection

data class DailyForecastData(
    val date: String,
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double,
    val feelsLikeMax: Double,
    val feelsLikeMin: Double,
    val sunrise: String,
    val sunset: String,
    val precipitation: Double,
    val precipProbability: Int,
    val windMax: Double,
    val windGusts: Double,
    val windDirection: Double,
    val uvIndexMax: Double = 0.0
)

@Composable
fun DailyForecastCard(
    day: DailyForecastData,
    tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val tmax = day.tempMax.toCelsiusOrFahrenheit(tempUnit).toInt()
            val tmin = day.tempMin.toCelsiusOrFahrenheit(tempUnit).toInt()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dayLabel = if (isToday(day.date)) {
                    stringResource(R.string.today)
                } else {
                    formatDayOfWeek(day.date)
                }

                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(60.dp)
                )

                WeatherIcon(code = day.weatherCode, size = 32.dp)

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(weatherDescriptionRes(day.weatherCode)),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "$tmax/$tmin${tempUnit.displayString()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                DailyDetailItem(
                    label = stringResource(R.string.temperature),
                    value = "$tmax/$tmin${tempUnit.displayString()}",
                    modifier = Modifier.weight(1f),
                    align = Alignment.Start
                )
                DailyDetailItem(
                    label = stringResource(R.string.precipitation),
                    value = "${day.precipitation} mm (${day.precipProbability}%)",
                    modifier = Modifier.weight(1f),
                    align = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                DailyDetailItem(
                    label = stringResource(R.string.wind),
                    value = "${day.windMax.toInt()}/${day.windGusts.toInt()} ${stringResource(R.string.wind_ms)} ${windDirection(day.windDirection)}",
                    modifier = Modifier.weight(1f),
                    align = Alignment.Start
                )
                DailyDetailItem(
                    label = stringResource(R.string.uv_index),
                    value = "${day.uvIndexMax.toInt()} (${stringResource(day.uvIndexMax.uvCategory())})",
                    modifier = Modifier.weight(1f),
                    align = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                DailyDetailItem(
                    label = stringResource(R.string.sunrise),
                    value = formatTime(day.sunrise),
                    modifier = Modifier.weight(1f),
                    align = Alignment.Start
                )
                DailyDetailItem(
                    label = stringResource(R.string.sunset),
                    value = formatTime(day.sunset),
                    modifier = Modifier.weight(1f),
                    align = Alignment.End
                )
            }
        }
    }
}

internal fun Double.uvCategory(): Int {
    return when {
        this <= 2.0 -> R.string.uv_low
        this <= 5.0 -> R.string.uv_moderate
        this <= 7.0 -> R.string.uv_high
        this <= 10.0 -> R.string.uv_very_high
        else -> R.string.uv_extreme
    }
}

@Composable
private fun DailyDetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
