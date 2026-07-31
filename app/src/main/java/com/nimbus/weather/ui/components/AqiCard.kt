package com.nimbus.weather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.data.model.AirQualityCurrent

@Composable
fun AqiCard(
    aqi: AirQualityCurrent,
    modifier: Modifier = Modifier
) {
    val aqiValue = (aqi.europeanAqi ?: aqi.usAqi ?: 0.0).toInt()
    val color = aqiColor(aqiValue)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.air_quality),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$aqiValue",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (aqi.pm25 != null) {
                    AqiItem(label = "PM2.5", value = "${aqi.pm25.toInt()}")
                }
                if (aqi.pm10 != null) {
                    AqiItem(label = "PM10", value = "${aqi.pm10.toInt()}")
                }
                if (aqi.ozone != null) {
                    AqiItem(label = "O\u2083", value = "${aqi.ozone.toInt()}")
                }
            }
        }
    }
}

@Composable
private fun AqiItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

private fun aqiColor(value: Int): Color {
    return when {
        value <= 20 -> Color(0xFF4CAF50)
        value <= 40 -> Color(0xFFFFEB3B)
        value <= 60 -> Color(0xFFFF9800)
        value <= 80 -> Color(0xFFF44336)
        value <= 100 -> Color(0xFF9C27B0)
        else -> Color(0xFF880E4F)
    }
}
