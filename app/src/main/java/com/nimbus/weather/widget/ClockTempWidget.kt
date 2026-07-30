package com.nimbus.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.nimbus.weather.R
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.weatherDescription
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockTempWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val weather = WidgetUpdateManager.getCachedWeather()
        val useFeelsLike = runBlocking {
            SettingsDataStore(context).useFeelsLike.first()
        }

        val views = RemoteViews(context.packageName, R.layout.widget_clock_temp)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        views.setTextViewText(R.id.widget_time, timeFormat.format(Date()))

        if (weather?.current != null) {
            val temp = if (useFeelsLike) weather.current.apparentTemperature
            else weather.current.temperature
            views.setTextViewText(R.id.widget_temp, "${temp.toInt()}°")
            views.setTextViewText(R.id.widget_desc, weatherDescription(context, weather.current.weatherCode))
        }

        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
