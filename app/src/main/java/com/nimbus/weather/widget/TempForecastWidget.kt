package com.nimbus.weather.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.dp
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.weatherDescription
import com.nimbus.weather.util.formatDayOfWeek
import kotlinx.coroutines.flow.first

class TempForecastWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weather = WidgetUpdateManager.getCachedWeather()
        val settings = SettingsDataStore(context)
        val useFeelsLike = settings.useFeelsLike.first()

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(8.dp)
                ) {
                    if (weather?.current != null) {
                        val temp = if (useFeelsLike) {
                            weather.current.apparentTemperature
                        } else {
                            weather.current.temperature
                        }
                        Text(
                            text = "${temp.toInt()}°",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32
                            )
                        )
                        Text(
                            text = weatherDescription(context, weather.current.weatherCode),
                            style = TextStyle(fontSize = 12)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    weather?.daily?.let { daily ->
                        val days = daily.time.zip(
                            daily.weatherCode.zip(daily.temperatureMax.zip(daily.temperatureMin))
                        )
                        Row(
                            modifier = GlanceModifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            days.take(7).forEach { (date, codeAndTemps) ->
                                val (code, temps) = codeAndTemps
                                val (tMax, tMin) = temps
                                Column(
                                    modifier = GlanceModifier.padding(horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = formatDayOfWeek(date),
                                        style = TextStyle(fontSize = 10)
                                    )
                                    Text(
                                        text = "${tMax.toInt()}°",
                                        style = TextStyle(
                                            fontSize = 11,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "${tMin.toInt()}°",
                                        style = TextStyle(fontSize = 10)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class TempForecastWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TempForecastWidget()
}
