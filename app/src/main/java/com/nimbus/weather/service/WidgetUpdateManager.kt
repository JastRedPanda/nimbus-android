package com.nimbus.weather.service

import android.content.Context
import com.nimbus.weather.data.model.WeatherResponse
import com.nimbus.weather.util.Constants

object WidgetUpdateManager {

    private var cachedWeather: WeatherResponse? = null

    fun updateAllWidgets(context: Context, response: WeatherResponse) {
        cachedWeather = response
        val intent = android.content.Intent(Constants.WIDGET_UPDATE_ACTION).apply {
            `package` = context.packageName
        }
        context.sendBroadcast(intent)
    }

    fun getCachedWeather(): WeatherResponse? = cachedWeather
}
