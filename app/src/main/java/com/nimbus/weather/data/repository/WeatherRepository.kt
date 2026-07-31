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

    fun initCache(context: Context) {
        if (cache == null) {
            cache = WeatherCache(context)
        }
    }

    private fun getCache(context: Context): WeatherCache {
        val existing = cache
        if (existing != null) return existing
        val created = WeatherCache(context)
        cache = created
        return created
    }

    suspend fun getWeather(lat: Double, lon: Double, context: Context? = null): WeatherResponse {
        val c = context?.let { getCache(it) }
        return try {
            val response = weatherApi.getForecast(latitude = lat, longitude = lon)
            c?.cacheWeather(response)
            response
        } catch (e: Exception) {
            val cached = c?.getCachedWeather()
            if (cached != null) cached else throw e
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

    suspend fun searchCities(query: String): List<GeocodingResult> {
        return geocodingApi.searchCities(name = query).results.orEmpty()
    }
}
