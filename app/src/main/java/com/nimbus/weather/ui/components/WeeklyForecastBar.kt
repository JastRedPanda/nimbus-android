package com.nimbus.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nimbus.weather.R
import com.nimbus.weather.ui.theme.LocalGlassDark
import com.nimbus.weather.ui.theme.skyTextColors
import com.nimbus.weather.util.TemperatureUnit
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.formatDayMonth
import com.nimbus.weather.util.formatDayOfWeekShort
import com.nimbus.weather.util.isToday
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import com.nimbus.weather.util.windDirection

/** Высота полосы графика — как в почасовом. */
private val ChartHeight = 44.dp

/** Направление ветра — единственная строка блока с отрицательным трекингом: «15 м/с ПдЗх» иначе не влезает. */
private val WindDirectionText = ChipMetaText.copy(letterSpacing = (-0.3).sp)

/**
 * Компактный обзор недели в стиле почасового блока, но без скролла:
 * все 7 дней влезают в ширину карточки. Сверху — максимумы, снизу —
 * минимумы и ветер, между рядами — двухполосный спарклайн.
 *
 * Полосы независимы: верхний ряд нормируется сам по себе, нижний сам
 * по себе. Поэтому линии не слипаются при разбросе в пару градусов
 * и не пересекаются, когда максимум одного дня ниже минимума другого.
 */
@Composable
fun WeeklyForecastBar(
    days: List<DailyForecastData>,
    tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    GlassCard(modifier = modifier) {
        val t = skyTextColors(LocalGlassDark.current)
        Column {
            Text(
                text = stringResource(R.string.weekly_forecast),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = t.title
            )
            Spacer(modifier = Modifier.height(8.dp))
            WeeklyTopRow(days = days, tempUnit = tempUnit)
            Spacer(modifier = Modifier.height(6.dp))
            WeeklyTempChart(days = days, tempUnit = tempUnit)
            Spacer(modifier = Modifier.height(6.dp))
            WeeklyBottomRow(days = days, tempUnit = tempUnit)
        }
    }
}

@Composable
private fun WeeklyTopRow(days: List<DailyForecastData>, tempUnit: TemperatureUnit) {
    val t = skyTextColors(LocalGlassDark.current)
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            val today = isToday(day.date)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // День и дата в одной строке — иначе блок вырастает
                // на лишние 12 dp и перестаёт быть «как почасовой».
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDayOfWeekShort(day.date),
                        style = ChipLabelText,
                        fontWeight = if (today) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (today) t.title else t.subtle
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formatDayMonth(day.date),
                        style = ChipMetaText,
                        color = t.subtle
                    )
                }
                WeatherIcon(code = day.weatherCode, size = 22.dp)
                PrecipitationLabel(probability = day.precipProbability)
                Text(
                    text = "${day.tempMax.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}",
                    style = ChipValueText,
                    fontWeight = FontWeight.SemiBold,
                    color = t.title
                )
            }
        }
    }
}

@Composable
private fun WeeklyBottomRow(days: List<DailyForecastData>, tempUnit: TemperatureUnit) {
    val t = skyTextColors(LocalGlassDark.current)
    val windUnit = stringResource(R.string.wind_ms)
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${day.tempMin.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}",
                    style = ChipValueText,
                    fontWeight = FontWeight.SemiBold,
                    color = t.title
                )
                // Скорость и направление разными строками: одной строкой
                // «15 м/с ПдЗх» шире колонки и обрезается по краям карточки.
                Text(
                    text = "${day.windMax.toInt()} $windUnit",
                    style = ChipMetaText,
                    color = t.subtle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = windDirection(day.windDirection),
                    style = WindDirectionText,
                    color = t.subtle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PrecipitationLabel(probability: Int) {
    if (probability > 0) {
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
                text = "$probability%",
                style = ChipMetaText,
                color = Color(0xFF64B5F6)
            )
        }
    } else {
        // Пустая строка держит высоту — значения температуры
        // остаются на одной линии во всех колонках.
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WeeklyTempChart(
    days: List<DailyForecastData>,
    tempUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    if (days.size < 2) return
    val ring = skyTextColors(LocalGlassDark.current).title
    // Координаты — в выбранной единице, цвет — по цельсию:
    // порог «мороз/тепло» всегда 0 °C.
    val maxValues = days.map { it.tempMax.toCelsiusOrFahrenheit(tempUnit) }
    val minValues = days.map { it.tempMin.toCelsiusOrFahrenheit(tempUnit) }
    val maxCelsius = days.map { it.tempMax }
    val minCelsius = days.map { it.tempMin }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight)
    ) {
        val n = days.size
        val h = size.height
        val padTop = 5.dp.toPx()
        val padBottom = 5.dp.toPx()
        val bandGap = 8.dp.toPx()
        val band = (h - padTop - padBottom - bandGap) / 2f
        val topBand = padTop..(padTop + band)
        val bottomBand = (h - padBottom - band)..(h - padBottom)

        // Центр i-й колонки — точки строго под её метками.
        fun x(i: Int): Float = (i + 0.5f) * size.width / n

        fun yIn(value: Double, lo: Double, hi: Double, range: ClosedFloatingPointRange<Float>): Float {
            val span = hi - lo
            // Разброс меньше градуса — плоская линия по центру полосы,
            // без искусственного растягивания шума округления.
            if (span < 1.0) return (range.start + range.endInclusive) / 2f
            val f = ((value - lo) / span).toFloat()
            return range.start + (1 - f) * (range.endInclusive - range.start)
        }

        fun drawSeries(
            values: List<Double>,
            celsius: List<Double>,
            range: ClosedFloatingPointRange<Float>,
            colorFor: (Double) -> Color
        ) {
            val lo = values.minOrNull() ?: return
            val hi = values.maxOrNull() ?: return
            val pts = values.indices.map { Offset(x(it), yIn(values[it], lo, hi, range)) }
            drawSparkline(
                points = pts,
                ringColor = ring,
                strokeWidth = 2.dp.toPx(),
                dotRadius = 2.5.dp.toPx(),
                ringRadius = 4.dp.toPx(),
                pointColor = { colorFor(celsius[it]) },
                segmentColor = { colorFor((celsius[it] + celsius[it + 1]) / 2) }
            )
        }

        drawSeries(maxValues, maxCelsius, topBand, ::tempLineColor)
        drawSeries(minValues, minCelsius, bottomBand, ::coldLineColor)
    }
}
