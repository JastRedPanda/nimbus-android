package com.nimbus.weather.data.repository

import android.content.Context
import com.nimbus.weather.data.api.ApiClient
import com.nimbus.weather.data.model.AirQualityResponse
import com.nimbus.weather.data.model.GeocodingResult
import com.nimbus.weather.data.model.WeatherResponse
import java.util.concurrent.ConcurrentHashMap

class WeatherRepository {

    private companion object {
        const val DEFAULT_TTL_HOURS = 4
    }

    private val weatherApi = ApiClient.weatherApi
    private val geocodingApi = ApiClient.geocodingApi
    private val airQualityApi = ApiClient.airQualityApi

    @Volatile
    private var cache: WeatherCache? = null

    @Volatile
    var showingCachedWeather: Boolean = false
        private set

    @Volatile
    private var ttlHours: Int = DEFAULT_TTL_HOURS

    fun setTtlHours(hours: Int) {
        ttlHours = hours.coerceAtLeast(1)
        cache?.setTtlHours(ttlHours)
    }

    private fun getCache(context: Context): WeatherCache {
        val existing = cache
        if (existing != null) return existing
        return WeatherCache(context).also {
            it.setTtlHours(ttlHours)
            cache = it
        }
    }

    suspend fun getWeather(lat: Double, lon: Double, context: Context? = null): WeatherResponse {
        val c = context?.let { getCache(it) }
        return try {
            val response = weatherApi.getForecast(latitude = lat, longitude = lon)
            showingCachedWeather = false
            c?.cacheWeather(response)
            response
        } catch (e: Exception) {
            val cached = c?.getCachedWeather()
            if (cached != null) {
                showingCachedWeather = true
                cached
            } else {
                showingCachedWeather = false
                throw e
            }
        }
    }

    suspend fun getAirQuality(lat: Double, lon: Double, context: Context? = null): AirQualityResponse {
        val c = context?.let { getCache(it) }
        return try {
            val response = airQualityApi.getAirQuality(latitude = lat, longitude = lon)
            c?.cacheAqi(response)
            response
        } catch (e: Exception) {
            val cached = c?.getCachedAqi()
            if (cached != null) cached else throw e
        }
    }

    suspend fun searchCities(query: String, language: String = "ru"): List<GeocodingResult> {
        return geocodingApi.searchCities(name = query, language = language).results.orEmpty()
    }

    suspend fun translateCityName(name: String, lat: Double, lon: Double, toLang: String): String? {
        val key = TranslationKey(name, toLang, lat, lon)
        translationCache[key]?.let { return it }
        if (key in translationMisses) return null

        val radiusKm = 25.0
        fun bestMatch(results: List<GeocodingResult>): GeocodingResult? {
            var best: GeocodingResult? = null
            var bestDist = Double.MAX_VALUE
            for (r in results) {
                val d = distanceKm(r.latitude, r.longitude, lat, lon)
                if (d <= radiusKm && d < bestDist) {
                    best = r
                    bestDist = d
                }
            }
            return best
        }

        val targetResults = geocodingApi.searchCities(name = name, language = toLang).results.orEmpty()
        bestMatch(targetResults)?.let {
            translationCache[key] = it.name
            return it.name
        }
        val canonical = bestMatch(
            geocodingApi.searchCities(name = name, language = "en").results.orEmpty()
        ) ?: run {
            translationMisses.add(key)
            return null
        }
        val result = bestMatch(
            geocodingApi.searchCities(name = canonical.name, language = toLang).results.orEmpty()
        )?.name
        if (result != null) translationCache[key] = result else translationMisses.add(key)
        return result
    }

    private data class TranslationKey(
        val name: String,
        val lang: String,
        val lat: Double,
        val lon: Double
    )

    private val translationCache = ConcurrentHashMap<TranslationKey, String>()
    private val translationMisses = ConcurrentHashMap.newKeySet<TranslationKey>()

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return 2 * earthRadiusKm * Math.asin(Math.sqrt(a))
    }
}
