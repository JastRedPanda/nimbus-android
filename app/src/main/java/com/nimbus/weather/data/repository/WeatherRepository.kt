package com.nimbus.weather.data.repository

import android.content.Context
import com.nimbus.weather.data.api.ApiClient
import com.nimbus.weather.data.model.AirQualityResponse
import com.nimbus.weather.data.model.GeocodingResult
import com.nimbus.weather.data.model.WeatherResponse

class WeatherRepository {

    private val weatherApi = ApiClient.weatherApi
    private val geocodingApi = ApiClient.geocodingApi
    private val airQualityApi = ApiClient.airQualityApi

    @Volatile
    private var cache: WeatherCache? = null

    @Volatile
    var showingCachedWeather: Boolean = false
        private set

    @Volatile
    private var ttlHours: Int = 4

    fun setTtlHours(hours: Int) {
        ttlHours = hours.coerceAtLeast(1)
    }

    fun initCache(context: Context) {
        if (cache == null) {
            cache = WeatherCache(context).also { it.setTtlHours(ttlHours) }
        }
    }

    private fun getCache(context: Context): WeatherCache {
        val existing = cache
        if (existing != null) return existing
        val created = WeatherCache(context).also { it.setTtlHours(ttlHours) }
        cache = created
        return created
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
        val radiusKm = 25.0
        fun bestMatch(results: List<GeocodingResult>): GeocodingResult? {
            val distances = results.map { it to distanceKm(it.latitude, it.longitude, lat, lon) }
            return distances.filter { it.second <= radiusKm }.minByOrNull { it.second }?.first
        }
        bestMatch(geocodingApi.searchCities(name = name, language = toLang).results.orEmpty())?.let { return it.name }
        val canonical = bestMatch(geocodingApi.searchCities(name = name, language = "en").results.orEmpty())
            ?: return null
        return bestMatch(geocodingApi.searchCities(name = canonical.name, language = toLang).results.orEmpty())?.name
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return 2 * earthRadiusKm * Math.asin(Math.sqrt(a))
    }
}
