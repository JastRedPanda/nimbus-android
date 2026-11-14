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
    private var cachedLat: Double? = null
    private var cachedLon: Double? = null
    private var cachedTimestamp: Long = 0L

    fun getCachedWeather(): WeatherResponse? = cachedWeather

    /**
     * Notify that a fresh weather response arrived. Cached only when it
     * matches the widget's target city (first favourite, or current if no
     * favourites). Otherwise discarded — the next refresh will pull it.
     * Город сравниваем по координатам: имя зависит от языка и переименований,
     * а координаты — нет.
     */
    suspend fun updateAllWidgets(context: Context, response: WeatherResponse) {
        val target = resolveTargetCity(context)
        if (target != null && !WeatherCache.coordsMatch(
                target.lat, target.lon, response.latitude, response.longitude
            )
        ) {
            if (cachedWeather == null || !cachedMatches(target)) {
                updateFromTargetCity(context)
            } else {
                refreshAllWidgets(context)
            }
            return
        }
        cachedWeather = response
        cachedLat = response.latitude
        cachedLon = response.longitude
        cachedTimestamp = System.currentTimeMillis()
        runCatching {
            WeatherCache(context).cacheWidgetWeather(response, response.latitude, response.longitude)
        }
        markWeatherUpdated(context)
        refreshAllWidgets(context)
    }

    private fun cachedMatches(target: SettingsDataStore.FavouriteCity): Boolean {
        val lat = cachedLat ?: return false
        val lon = cachedLon ?: return false
        return WeatherCache.coordsMatch(lat, lon, target.lat, target.lon)
    }

    suspend fun refreshAllWidgets(context: Context) {
        renderWidgets(context, widgetIds(context))
    }

    /** Есть ли на рабочем столе хотя бы один экземпляр виджета. */
    fun hasWidgets(context: Context): Boolean = widgetIds(context).isNotEmpty()

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
        // Без виджетов обновлять нечего: не тратим сеть и батарею.
        if (widgetIds(context).isEmpty()) return
        val target = resolveTargetCity(context) ?: return
        val now = System.currentTimeMillis()
        val settings = SettingsDataStore(context)
        val interval = settings.updateIntervalHours.first()
        val maxAgeMillis = interval * 60 * 60 * 1000L
        val isStale = (now - cachedTimestamp) > maxAgeMillis

        if (!force && cachedWeather != null && cachedMatches(target) && !isStale) {
            refreshAllWidgets(context)
            return
        }
        runCatching {
            val repository = WeatherRepository()
            repository.setTtlHours(interval * 2)
            val response = repository.getWeather(target.lat, target.lon, context)
            cachedWeather = response
            cachedLat = target.lat
            cachedLon = target.lon
            cachedTimestamp = now
            WeatherCache(context).cacheWidgetWeather(response, target.lat, target.lon)
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
