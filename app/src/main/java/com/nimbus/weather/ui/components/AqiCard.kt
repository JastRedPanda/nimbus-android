package com.nimbus.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.data.model.AirQualityCurrent
import com.nimbus.weather.ui.theme.LocalGlassDark
import com.nimbus.weather.ui.theme.skyTextColors

@Composable
fun AqiCard(
    aqi: AirQualityCurrent,
    modifier: Modifier = Modifier
) {
    val aqiValue = (aqi.europeanAqi ?: aqi.usAqi ?: 0.0).toInt()
    val color = aqiColor(aqiValue)

    GlassCard(modifier = modifier) {
        val t = skyTextColors(LocalGlassDark.current)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.air_quality),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = t.title
                )
                Text(
                    text = "$aqiValue",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Неоновая шкала AQI
            AqiScaleBar(value = aqiValue)

            Spacer(modifier = Modifier.height(8.dp))

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

/**
 * Неоновая горизонтальная шкала AQI: градиент от зелёного к бордовому
 * с белой точкой-маркером текущего значения.
 */
@Composable
private fun AqiScaleBar(value: Int) {
    val scaleColors = listOf(
        Color(0xFF4CAF50),
        Color(0xFFCDDC39),
        Color(0xFFFFEB3B),
        Color(0xFFFF9800),
        Color(0xFFF44336),
        Color(0xFF880E4F)
    )
    val fraction = (value.coerceIn(0, 100) / 100f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(scaleColors))
        )
        // Маркер текущего значения
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .padding(end = 6.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun AqiItem(label: String, value: String) {
    val t = skyTextColors(LocalGlassDark.current)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = t.title
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = t.subtle
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
