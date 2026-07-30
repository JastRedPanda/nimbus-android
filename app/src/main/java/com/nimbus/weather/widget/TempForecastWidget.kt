package com.nimbus.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.nimbus.weather.R
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.formatDayOfWeek
import com.nimbus.weather.util.weatherDescription
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TempForecastWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val weather = WidgetUpdateManager.getCachedWeather()
        val useFeelsLike = runBlocking {
            SettingsDataStore(context).useFeelsLike.first()
        }

        val views = RemoteViews(context.packageName, R.layout.widget_temp_forecast)

        if (weather?.current != null) {
            val temp = if (useFeelsLike) weather.current.apparentTemperature
            else weather.current.temperature
            views.setTextViewText(R.id.widget_temp, "${temp.toInt()}°")
            views.setTextViewText(R.id.widget_desc, weatherDescription(context, weather.current.weatherCode))
        }

        weather?.daily?.let { daily ->
            val maxDays = minOf(daily.time.size, 7)
            for (i in 0 until maxDays) {
                val date = daily.time[i]
                val tMax = daily.temperatureMax.getOrElse(i) { 0.0 }
                val tMin = daily.temperatureMin.getOrElse(i) { 0.0 }

                val dayId = context.resources.getIdentifier("widget_day_$i", "id", context.packageName)
                val maxId = context.resources.getIdentifier("widget_max_$i", "id", context.packageName)
                val minId = context.resources.getIdentifier("widget_min_$i", "id", context.packageName)

                views.setTextViewText(dayId, formatDayOfWeek(date))
                views.setTextViewText(maxId, "${tMax.toInt()}°")
                views.setTextViewText(minId, "${tMin.toInt()}°")
            }
        }

        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
