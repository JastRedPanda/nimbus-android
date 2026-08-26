package com.nimbus.weather.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
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
import com.nimbus.weather.MainActivity
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.repository.WeatherCache
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val WIDGET_PADDING_DP = 8

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
            val tempText = weather?.current?.let { current ->
                val t = if (renderData.useFeelsLike) current.apparentTemperature
                else current.temperature
                "${t.toCelsiusOrFahrenheit(renderData.tempUnit).toInt()}${renderData.tempUnit.displayString()}"
            } ?: "--°"

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
            val tempSp = baseSp * 0.92f
            val dateSp = baseSp

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(WIDGET_PADDING_DP.dp)
                    .cornerRadius(16.dp)
                    .background(renderData.palette.background),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity<MainActivity>()),
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

class ClockTempWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockTempWidget()
}
