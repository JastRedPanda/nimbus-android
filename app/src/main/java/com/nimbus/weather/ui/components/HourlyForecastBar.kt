package com.nimbus.weather.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.ui.theme.rememberSkyTextColors
import com.nimbus.weather.util.TemperatureUnit
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.formatHour
import com.nimbus.weather.util.isDayNow
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import com.nimbus.weather.util.windDirection

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

/** Ширина колонки часа и зазор — общие для ленты и спарклайна, чтобы точки легли под метки. */
private val HourlyChipWidth = 56.dp
private val HourlyChipGap = 8.dp

/** Высота графика: одна линия, поэтому ниже, чем двухполосный недельный. */
private val ChartHeight = 34.dp

/**
 * Почасовой прогноз в том же языке, что и недельный обзор:
 * прозрачные колонки без капсул, метаданные сверху, спарклайн
 * посередине, значение под ним.
 *
 * Лента и график живут в одном горизонтальном скролле — точки
 * едут вместе с метками времени и всегда стоят под ними.
 */
@Composable
fun HourlyForecastBar(
    hourly: List<HourlyForecastData>,
    tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    sunrise: String = "",
    sunset: String = "",
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        val t = rememberSkyTextColors()
        Column {
            Text(
                text = stringResource(R.string.hourly_forecast),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = t.title
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    HourlyTopRow(hourly = hourly, sunrise = sunrise, sunset = sunset)
                    Spacer(modifier = Modifier.height(4.dp))
                    TempSparkline(hourly = hourly, tempUnit = tempUnit)
                    Spacer(modifier = Modifier.height(4.dp))
                    HourlyBottomRow(hourly = hourly, tempUnit = tempUnit)
                }
            }
        }
    }
}

@Composable
private fun HourlyTopRow(
    hourly: List<HourlyForecastData>,
    sunrise: String,
    sunset: String
) {
    val t = rememberSkyTextColors()
    Row(horizontalArrangement = Arrangement.spacedBy(HourlyChipGap)) {
        hourly.forEach { h ->
            Column(
                modifier = Modifier.width(HourlyChipWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatHour(h.time),
                    style = ChipLabelText,
                    color = t.subtle
                )
                WeatherIcon(
                    code = h.weatherCode,
                    isDay = isDayNow(h.time, sunrise, sunset),
                    size = 22.dp
                )
                PrecipitationLabel(precipitation = h.precipitation)
            }
        }
    }
}

@Composable
private fun HourlyBottomRow(
    hourly: List<HourlyForecastData>,
    tempUnit: TemperatureUnit
) {
    val t = rememberSkyTextColors()
    val windUnit = stringResource(R.string.wind_ms)
    Row(horizontalArrangement = Arrangement.spacedBy(HourlyChipGap)) {
        hourly.forEach { h ->
            Column(
                modifier = Modifier.width(HourlyChipWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${h.temperature.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}",
                    style = ChipValueText,
                    fontWeight = FontWeight.SemiBold,
                    color = t.title,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                // Колонка часа шире недельной, поэтому скорость и направление
                // помещаются в одну строку без обрезки по краям.
                Text(
                    text = "${h.windSpeed.toInt()} $windUnit ${windDirection(h.windDirection)}",
                    style = ChipMetaText,
                    color = t.subtle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth()
                )
                HumidityLabel(humidity = h.humidity)
            }
        }
    }
}

@Composable
private fun HumidityLabel(humidity: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Opacity,
            contentDescription = null,
            tint = Color(0xFF4FC3F7),
            modifier = Modifier.size(9.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "${humidity.toInt()}%",
            style = ChipMetaText,
            color = Color(0xFF4FC3F7)
        )
    }
}

@Composable
private fun PrecipitationLabel(precipitation: Double) {
    // Без капсул резерв под пустую строку не нужен: колонки выровнены
    // по верху, а график и температура живут в отдельных рядах.
    if (precipitation <= 0) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WaterDrop,
            contentDescription = null,
            tint = Color(0xFF64B5F6),
            modifier = Modifier.size(9.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "${precipitation.toInt()} mm",
            style = ChipMetaText,
            color = Color(0xFF64B5F6)
        )
    }
}

@Composable
private fun TempSparkline(
    hourly: List<HourlyForecastData>,
    tempUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    if (hourly.size < 2) return
    val ring = rememberSkyTextColors().title
    val temps = remember(hourly, tempUnit) {
        hourly.map { it.temperature.toCelsiusOrFahrenheit(tempUnit) }
    }
    // Цельсии для цвета: порог мороза — 0°C при любой единице отображения
    val tempsC = remember(hourly) { hourly.map { it.temperature } }
    val min = temps.minOrNull() ?: return
    val max = temps.maxOrNull() ?: return
    val chartWidth = HourlyChipWidth * hourly.size + HourlyChipGap * (hourly.size - 1)

    Spacer(
        modifier = modifier
            .width(chartWidth)
            .height(ChartHeight)
            .drawWithCache {
                val h = size.height
                val chipW = HourlyChipWidth.toPx()
                val gap = HourlyChipGap.toPx()
                val padTop = 5.dp.toPx()
                val padBottom = 5.dp.toPx()
                // Центр i-й колонки — точка строго под её меткой времени
                fun x(i: Int) = i * (chipW + gap) + chipW / 2f
                fun y(v: Double): Float {
                    if (max == min) return h / 2f
                    val f = ((v - min) / (max - min)).toFloat()
                    return padTop + (1 - f) * (h - padTop - padBottom)
                }
                val pts = temps.indices.map { Offset(x(it), y(temps[it])) }
                val series = buildSparklineSeries(
                    points = pts,
                    pointColor = { tempLineColor(tempsC[it]) },
                    // Сегмент красится по средней температуре концов —
                    // переход через ноль виден по смене цвета
                    segmentColor = { tempLineColor((tempsC[it] + tempsC[it + 1]) / 2) }
                )
                val strokeWidth = 2.dp.toPx()
                val dotRadius = 2.5.dp.toPx()
                val ringRadius = 4.dp.toPx()
                onDrawBehind {
                    drawSparkline(series, ring, strokeWidth, dotRadius, ringRadius)
                }
            }
    )
}
