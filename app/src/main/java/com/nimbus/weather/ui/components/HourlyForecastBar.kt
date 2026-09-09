package com.nimbus.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.ui.theme.LocalGlassDark
import com.nimbus.weather.ui.theme.skyTextColors
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

/** Ширина чипа часа и зазор — общие для ленты и спарклайна, чтобы точки легли под метки. */
private val HourlyChipWidth = 56.dp
private val HourlyChipGap = 8.dp

@Composable
fun HourlyForecastBar(
    hourly: List<HourlyForecastData>,
    tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        val t = skyTextColors(LocalGlassDark.current)
        Column {
            Text(
                text = stringResource(R.string.hourly_forecast),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = t.title
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Линия и чипы в одном горизонтальном скролле:
            // точки строго под метками времени, едут вместе
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    TempSparkline(hourly = hourly, tempUnit = tempUnit)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(HourlyChipGap)) {
                        hourly.forEach { h ->
                            HourlyColumn(h, tempUnit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TempSparkline(
    hourly: List<HourlyForecastData>,
    tempUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    if (hourly.size < 2) return
    val temps = hourly.map { it.temperature.toCelsiusOrFahrenheit(tempUnit) }
    // Цельсии для цвета: порог мороза — 0°C при любой единице отображения
    val tempsC = hourly.map { it.temperature }
    val min = temps.minOrNull() ?: return
    val max = temps.maxOrNull() ?: return

    Canvas(
        modifier = modifier
            .width(HourlyChipWidth * hourly.size + HourlyChipGap * (hourly.size - 1))
            .height(56.dp)
    ) {
        val h = size.height
        val chipW = HourlyChipWidth.toPx()
        val gap = HourlyChipGap.toPx()
        val padTop = 8.dp.toPx()
        val padBottom = 8.dp.toPx()
        val n = temps.size
        // Центр i-го чипа — точка строго под его меткой времени
        fun x(i: Int) = i * (chipW + gap) + chipW / 2f
        fun y(v: Double): Float {
            if (max == min) return h / 2f
            val f = ((v - min) / (max - min)).toFloat()
            return padTop + (1 - f) * (h - padTop - padBottom)
        }
        val pts = temps.indices.map { Offset(x(it), y(temps[it])) }
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)

        // Каждый сегмент сплайна красится по средней температуре
        // его концов — переход через ноль виден по смене цвета
        for (i in 0 until n - 1) {
            val p0 = pts.getOrElse(i - 1) { pts[i] }
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p3 = pts.getOrElse(i + 2) { pts[i + 1] }
            val seg = Path().apply {
                moveTo(p1.x, p1.y)
                cubicTo(
                    p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
                    p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
                    p2.x, p2.y
                )
            }
            val segColor = tempLineColor((tempsC[i] + tempsC[i + 1]) / 2)
            val segFill = Path().apply {
                addPath(seg)
                lineTo(p2.x, h)
                lineTo(p1.x, h)
                close()
            }
            drawPath(
                path = segFill,
                brush = Brush.verticalGradient(
                    listOf(segColor.copy(alpha = 0.35f), Color.Transparent)
                )
            )
            drawPath(path = seg, color = segColor, style = stroke)
        }
        pts.forEachIndexed { i, pt ->
            drawCircle(tempLineColor(tempsC[i]), radius = 2.dp.toPx(), center = pt)
        }
    }
}

/**
 * Цвет линии по температуре: ноль и ниже — синий (мороз),
 * выше нуля — оранжево-красный (тепло). Чем дальше от нуля,
 * тем насыщеннее. Порог — 0°C при любой единице отображения.
 */
private fun tempLineColor(celsius: Double): Color {
    return if (celsius <= 0) {
        val f = ((-celsius) / 20.0).coerceIn(0.0, 1.0).toFloat()
        lerp(Color(0xFF8FCBFF), Color(0xFF1565C0), f)
    } else {
        val f = (celsius / 30.0).coerceIn(0.0, 1.0).toFloat()
        lerp(Color(0xFFFFB74D), Color(0xFFD32F2F), f)
    }
}

@Composable
private fun HourlyColumn(
    h: HourlyForecastData,
    tempUnit: TemperatureUnit
) {
    val isDark = LocalGlassDark.current
    val t = skyTextColors(isDark)
    val chipShape = RoundedCornerShape(12.dp)
    val chipBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.40f)
    val chipBorder = Brush.verticalGradient(
        if (isDark) listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))
        else listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.15f))
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(HourlyChipWidth)
            .clip(chipShape)
            .background(chipBg)
            .border(width = 0.5.dp, brush = chipBorder, shape = chipShape)
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = formatHour(h.time),
            style = MaterialTheme.typography.labelSmall,
            color = t.subtle
        )
        WeatherIcon(code = h.weatherCode, size = 24.dp)
        Text(
            text = "${h.temperature.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = t.title
        )
        if (h.precipitation > 0) {
            Text(
                text = "${h.precipitation.toInt()} mm",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64B5F6)
            )
        } else {
            Text(
                text = "",
                modifier = Modifier.height(12.dp)
            )
        }
    }
}
