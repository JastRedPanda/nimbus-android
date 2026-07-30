package com.nimbus.weather.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
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
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockTempWidget : GlanceAppWidget() {

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
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text(
                        text = timeFormat.format(Date()),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

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
                                fontSize = 36
                            )
                        )
                        Text(
                            text = weatherDescription(context, weather.current.weatherCode),
                            style = TextStyle(fontSize = 12)
                        )
                    }
                }
            }
        }
    }
}

class ClockTempWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ClockTempWidget()
}
