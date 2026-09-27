package com.nimbus.weather.service

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.model.WeatherResponse
import com.nimbus.weather.data.repository.WeatherCache
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.widget.ClockTempWidgetReceiver
import com.nimbus.weather.widget.buildWidgetViews
import kotlinx.coroutines.flow.first

object WidgetUpdateManager {

    private var cachedWeather: WeatherResponse? = null
    private var cachedForCity: String? = null
    private var cachedTimestamp: Long = 0L

    fun getCachedWeather(): WeatherResponse? = cachedWeather

    /**
     * Notify that a fresh weather response arrived. Cached only when it
     * matches the widget's target city (first favourite, or current if no
     * favourites). Otherwise discarded — the next refresh will pull it.
     */
    suspend fun updateAllWidgets(context: Context, response: WeatherResponse, cityName: String) {
        val target = resolveTargetCity(context)
        if (target != null && target.name != cityName) {
            if (cachedWeather == null || cachedForCity != target.name) {
                updateFromTargetCity(context)
            } else {
                refreshAllWidgets(context)
            }
            return
        }
        cachedWeather = response
        cachedForCity = cityName
        cachedTimestamp = System.currentTimeMillis()
        runCatching { WeatherCache(context).cacheWidgetWeather(response) }
        markWeatherUpdated(context)
        refreshAllWidgets(context)
    }

    suspend fun refreshAllWidgets(context: Context) {
        renderWidgets(context, widgetIds(context))
    }

    suspend fun renderWidgets(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetIds.forEach { id ->
            runCatching {
                appWidgetManager.updateAppWidget(id, buildWidgetViews(context, id))
            }.onFailure { Log.w("WidgetUpdateManager", "update failed for $id", it) }
        }
    }

    /**
     * Loads (and caches) weather for the widget's target city — first
     * favourite, or the current city when no favourites. Falls back silently
     * to whatever is already cached.
     */
    suspend fun updateFromTargetCity(context: Context, force: Boolean = false) {
        val target = resolveTargetCity(context) ?: return
        val now = System.currentTimeMillis()
        val settings = SettingsDataStore(context)
        val interval = settings.updateIntervalHours.first()
        val maxAgeMillis = interval * 60 * 60 * 1000L
        val isStale = (now - cachedTimestamp) > maxAgeMillis

        if (!force && cachedForCity == target.name && cachedWeather != null && !isStale) {
            refreshAllWidgets(context)
            return
        }
        runCatching {
            val repository = WeatherRepository()
            repository.setTtlHours(interval * 2)
            val response = repository.getWeather(target.lat, target.lon, context)
            cachedWeather = response
            cachedForCity = target.name
            cachedTimestamp = now
            WeatherCache(context).cacheWidgetWeather(response)
            markWeatherUpdated(context)
            refreshAllWidgets(context)
        }.onFailure {
            Log.w("WidgetUpdateManager", "widget target refresh failed", it)
            refreshAllWidgets(context)
        }
    }

    private fun widgetIds(context: Context): IntArray {

        return runCatching {
            AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, ClockTempWidgetReceiver::class.java)
            )
        }.getOrNull() ?: intArrayOf()
    }

    private suspend fun markWeatherUpdated(context: Context) {
        runCatching {
            SettingsDataStore(context).setLastWeatherUpdateMillis(System.currentTimeMillis())
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
