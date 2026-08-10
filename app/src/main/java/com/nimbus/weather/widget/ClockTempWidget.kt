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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.ThemeMode
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SMALL_BREAKPOINT: Dp = 160.dp

class ClockTempWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsDataStore(context)
        val weather = WidgetUpdateManager.getCachedWeather()
        val useFeelsLike = settings.useFeelsLike.first()
        val tempUnit = settings.tempUnit.first()
        val themeMode = settings.themeMode.first()
        val dark = isDarkTheme(context, themeMode)
        val palette = settings.readWidgetPalette(dark)

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
                    .cornerRadius(16.dp)
                    .background(palette.background),
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
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompact) 18.sp else 65.sp,
                            color = ColorProvider(palette.text)
                        )
                    )
                    Text(
                        text = tempText,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompact) 22.sp else 68.sp,
                            color = ColorProvider(palette.text)
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
