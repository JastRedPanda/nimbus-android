package com.nimbus.weather.widget

import android.content.Context
import android.text.TextPaint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.model.WeatherResponse
import com.nimbus.weather.data.repository.WeatherCache
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.TemperatureUnit
import com.nimbus.weather.util.ThemeMode
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MIN_FONT_SP = 10f
private const val MAX_FONT_SP = 400f
private const val TEMP_RATIO = 0.92f
private const val GAP_EM = 0.55f

private data class WidgetRenderData(
    val fontScale: Int = SettingsDataStore.DEFAULT_WIDGET_FONT_SCALE,
    val dateFormat: String = SettingsDataStore.DEFAULT_WIDGET_DATE_FORMAT,
    val useFeelsLike: Boolean = false,
    val tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val palette: WidgetPalette = WidgetPalette.defaultLight
)

class ClockTempWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsDataStore(context)
        val weather = WidgetUpdateManager.getCachedWeather()
            ?: runCatching { WeatherCache(context).getCachedWeather() }.getOrNull()
        val renderDataFlow = renderDataFlow(context, settings)

        provideContent {
            val ctx = LocalContext.current
            val density = ctx.resources.displayMetrics.density
            val systemFontScale = ctx.resources.configuration.fontScale
            val widthDp = LocalSize.current.width.value
            val renderData by renderDataFlow.collectAsState(initial = WidgetRenderData())
            val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
                while (true) {
                    delay(30_000)
                    value = System.currentTimeMillis()
                }
            }

            val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
            val dateText = if (renderData.dateFormat == "text") {
                SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(nowMillis))
            } else {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(nowMillis))
            }
            val tempText = if (weather?.current != null) {
                val t = if (renderData.useFeelsLike) weather.current.apparentTemperature
                else weather.current.temperature
                "${t.toCelsiusOrFahrenheit(renderData.tempUnit).toInt()}${renderData.tempUnit.displayString()}"
            } else "--°"

            val padPx = 16f * density
            val availPx = (widthDp * density - padPx).coerceAtLeast(10f)
            val multiplier = systemFontScale * (100f / renderData.fontScale)

            val (baseSp, showDate) = fitBaseSp(
                timeText = timeText,
                dateText = dateText,
                tempText = tempText,
                availPx = availPx,
                density = density,
                multiplier = multiplier
            )
            val timeSp = baseSp
            val tempSp = baseSp * TEMP_RATIO
            val dateSp = baseSp

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .cornerRadius(16.dp)
                    .background(renderData.palette.background),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeText,
                        modifier = GlanceModifier.defaultWeight(),
                        maxLines = 1,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = timeSp.sp,
                            color = ColorProvider(renderData.palette.text),
                            textAlign = TextAlign.Start
                        )
                    )
                    if (showDate) {
                        Text(
                            text = dateText,
                            maxLines = 1,
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = dateSp.sp,
                                color = ColorProvider(renderData.palette.text),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    Text(
                        text = tempText,
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        maxLines = 1,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = tempSp.sp,
                            color = ColorProvider(renderData.palette.text),
                            textAlign = TextAlign.End
                        )
                    )
                }
            }
        }
    }
}

private fun renderDataFlow(context: Context, settings: SettingsDataStore): Flow<WidgetRenderData> {
    val paletteFlow = combine(
        settings.themeMode,
        settings.widgetBgColor,
        settings.widgetBgAlpha,
        settings.widgetTextColor
    ) { theme, bg, alpha, text ->
        resolveWidgetPalette(
            dark = isDarkTheme(context, theme),
            bgColorHex = bg,
            bgAlpha = alpha,
            textOption = text
        )
    }
    return combine(
        paletteFlow,
        settings.widgetDateFormat,
        settings.widgetFontScale,
        settings.useFeelsLike,
        settings.tempUnit
    ) { palette, dateFormat, fontScale, useFeelsLike, tempUnit ->
        WidgetRenderData(
            fontScale = fontScale,
            dateFormat = dateFormat,
            useFeelsLike = useFeelsLike,
            tempUnit = tempUnit,
            palette = palette
        )
    }
}

private fun fitBaseSp(
    timeText: String,
    dateText: String,
    tempText: String,
    availPx: Float,
    density: Float,
    multiplier: Float
): Pair<Float, Boolean> {
    val paint = TextPaint()

    fun totalWidth(sp: Float, withDate: Boolean): Float {
        fun widthOf(text: String, ratio: Float): Float {
            paint.textSize = sp * ratio * density * multiplier
            return paint.measureText(text)
        }

        val timeWidth = widthOf(timeText, 1f)
        val tempWidth = widthOf(tempText, TEMP_RATIO)
        val dateWidth = if (withDate) widthOf(dateText, 1f) else 0f
        val gaps = (if (withDate) 2 else 1) * sp * GAP_EM * density * multiplier
        return timeWidth + tempWidth + dateWidth + gaps
    }

    fun maxSp(withDate: Boolean): Float {
        var low = MIN_FONT_SP
        var high = MAX_FONT_SP
        var best = MIN_FONT_SP
        repeat(36) {
            val mid = (low + high) / 2f
            if (totalWidth(mid, withDate) <= availPx) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }
        return best
    }

    val withDateBest = maxSp(withDate = true)
    if (withDateBest > MIN_FONT_SP) return withDateBest to true
    return maxSp(withDate = false) to false
}

class ClockTempWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockTempWidget()
}