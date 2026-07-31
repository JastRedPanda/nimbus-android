package com.nimbus.weather.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.ThemeMode
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import com.nimbus.weather.util.weatherDescription
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SMALL_BREAKPOINT: Dp = 200.dp

private fun isDarkTheme(context: Context, themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> {
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }
}

class ClockTempWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsDataStore(context)
        val weather = WidgetUpdateManager.getCachedWeather()
        val useFeelsLike = settings.useFeelsLike.first()
        val tempUnit = settings.tempUnit.first()
        val themeMode = settings.themeMode.first()
        val dark = isDarkTheme(context, themeMode)
        val textColor = if (dark) Color.White else Color.Black
        val bgColor = if (dark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)

        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val tempText = if (weather?.current != null) {
            val t = if (useFeelsLike) weather.current.apparentTemperature
            else weather.current.temperature
            "${t.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}"
        } else "--°"

        provideContent {
            val isCompact = LocalSize.current.width < SMALL_BREAKPOINT

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .background(bgColor),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCompact) {
                    Text(
                        text = tempText,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = ColorProvider(textColor)
                        )
                    )
                } else {
                    Text(
                        text = timeText,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = ColorProvider(textColor)
                        )
                    )
                    Text(
                        text = tempText,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = ColorProvider(textColor)
                        )
                    )
                    if (weather?.current != null) {
                        Text(
                            text = weatherDescription(context, weather.current.weatherCode),
                            style = TextStyle(fontSize = 12.sp, color = ColorProvider(textColor))
                        )
                    }
                }
            }
        }
    }
}

class ClockTempWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockTempWidget()
}
