package com.nimbus.weather.service

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.nimbus.weather.data.model.WeatherResponse
import com.nimbus.weather.widget.ClockTempWidget

object WidgetUpdateManager {

    private var cachedWeather: WeatherResponse? = null

    fun getCachedWeather(): WeatherResponse? = cachedWeather

    suspend fun updateAllWidgets(context: Context, response: WeatherResponse) {
        cachedWeather = response
        refreshAllWidgets(context)
    }

    suspend fun refreshAllWidgets(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(ClockTempWidget::class.java).forEach { id ->
            ClockTempWidget().update(context, id)
        }
    }
}
