package com.nimbus.weather.widget

import android.content.Context
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.formatDayOfWeek
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import com.nimbus.weather.util.weatherDescription
import kotlinx.coroutines.flow.first
import kotlin.math.min

private val TALL_BREAKPOINT: Dp = 140.dp

class TempForecastWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsDataStore(context)
        val weather = WidgetUpdateManager.getCachedWeather()
        val useFeelsLike = settings.useFeelsLike.first()
        val tempUnit = settings.tempUnit.first()
        val themeMode = settings.themeMode.first()
        val dark = isDarkTheme(context, themeMode)
        val palette = settings.readWidgetPalette(dark)

        provideContent {
            val isTall = LocalSize.current.height >= TALL_BREAKPOINT
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(palette.background)
            ) {
                if (weather?.current != null) {
                    val t = if (useFeelsLike) weather.current.apparentTemperature
                    else weather.current.temperature
                    Text(
                        text = "${t.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = ColorProvider(palette.text)
                        )
                    )
                    Text(
                        text = weatherDescription(context, weather.current.weatherCode),
                        style = TextStyle(fontSize = 11.sp, color = ColorProvider(palette.text))
                    )
                }

                weather?.daily?.let { daily ->
                    val maxDays = min(daily.time.size, if (isTall) 7 else 4)
                    for (i in 0 until maxDays) {
                        val tmax = daily.temperatureMax.getOrElse(i) { 0.0 }
                        val tmin = daily.temperatureMin.getOrElse(i) { 0.0 }
                        Row(
                            modifier = GlanceModifier.padding(top = 1.dp, bottom = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDayOfWeek(daily.time[i]),
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(fontSize = 10.sp, color = ColorProvider(palette.text))
                            )
                            Text(
                                text = "${tmax.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}",
                                modifier = GlanceModifier.padding(start = 2.dp, end = 2.dp),
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = ColorProvider(palette.text)
                                )
                            )
                            Text(
                                text = "${tmin.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}",
                                style = TextStyle(fontSize = 9.sp, color = ColorProvider(palette.text))
                            )
                        }
                    }
                }
            }
        }
    }
}

class TempForecastWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TempForecastWidget()
}
