package com.nimbus.weather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimbus.weather.util.TemperatureUnit
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.formatHour
import com.nimbus.weather.util.toCelsiusOrFahrenheit

data class HourlyForecastData(
    val time: String,
    val temperature: Double,
    val precipitation: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val windDirection: Double,
    val humidity: Double,
    val apparentTemperature: Double,
    val uvIndex: Double
)

@Composable
fun HourlyForecastBar(
    hourly: List<HourlyForecastData>,
    tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(hourly) { h ->
                HourlyColumn(h, tempUnit)
            }
        }
    }
}

@Composable
private fun HourlyColumn(
    h: HourlyForecastData,
    tempUnit: TemperatureUnit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Text(
            text = formatHour(h.time),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        WeatherIcon(code = h.weatherCode, size = 24.dp)
        Text(
            text = "${h.temperature.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (h.precipitation > 0) {
            Text(
                text = "${h.precipitation.toInt()} mm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "",
                modifier = Modifier.height(12.dp)
            )
        }
    }
}
