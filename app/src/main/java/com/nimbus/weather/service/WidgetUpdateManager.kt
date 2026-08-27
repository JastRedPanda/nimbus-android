package com.nimbus.weather.service

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.model.WeatherResponse
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.widget.ClockTempWidget
import kotlinx.coroutines.flow.first

object WidgetUpdateManager {

    private var cachedWeather: WeatherResponse? = null
    private var cachedForCity: String? = null

    fun getCachedWeather(): WeatherResponse? = cachedWeather

    /**
     * Notify that a fresh weather response arrived. Cached only when it
     * matches the widget's target city (first favourite, or current if no
     * favourites). Otherwise discarded — the next refresh will pull it.
     */
    suspend fun updateAllWidgets(context: Context, response: WeatherResponse, cityName: String) {
        val target = resolveTargetCity(context)
        if (target != null && target.name != cityName) {
            refreshAllWidgets(context)
            return
        }
        cachedWeather = response
        cachedForCity = cityName
        refreshAllWidgets(context)
    }

    suspend fun refreshAllWidgets(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(ClockTempWidget::class.java)
        ids.forEach { id ->
            runCatching { ClockTempWidget().update(context, id) }
                .onFailure { Log.w("WidgetUpdateManager", "update failed for $id", it) }
        }
    }

    /**
     * Loads (and caches) weather for the widget's target city — first
     * favourite, or the current city when no favourites. Falls back silently
     * to whatever is already cached.
     */
    suspend fun updateFromTargetCity(context: Context) {
        val target = resolveTargetCity(context) ?: return
        if (cachedForCity == target.name) {
            refreshAllWidgets(context)
            return
        }
        runCatching {
            val repository = WeatherRepository()
            val settings = SettingsDataStore(context)
            val interval = settings.updateIntervalHours.first()
            repository.setTtlHours(interval * 2)
            val response = repository.getWeather(target.lat, target.lon, context)
            cachedWeather = response
            cachedForCity = target.name
            refreshAllWidgets(context)
        }.onFailure {
            Log.w("WidgetUpdateManager", "widget target refresh failed", it)
            refreshAllWidgets(context)
        }
    }

    private suspend fun resolveTargetCity(context: Context): SettingsDataStore.FavouriteCity? {
        val settings = SettingsDataStore(context)
        val favourites = settings.favouriteCities.first()
        if (favourites.isNotEmpty()) return favourites.first()
        val current = settings.getLocationSnapshot()
        if (current.name.isBlank()) return null
        return SettingsDataStore.FavouriteCity(
            name = current.name,
            lat = current.lat,
            lon = current.lon,
            tz = current.tz,
            localNames = current.localNames
        )
    }
}
