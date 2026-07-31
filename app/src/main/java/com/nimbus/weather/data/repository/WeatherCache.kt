package com.nimbus.weather.data.repository

import android.content.Context
import com.nimbus.weather.data.model.AirQualityResponse
import com.nimbus.weather.data.model.WeatherResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class WeatherCache(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val weatherFile: File get() =
        File(context.cacheDir, "weather_cache.json").apply { parentFile?.mkdirs() }

    private val aqiFile: File get() =
        File(context.cacheDir, "aqi_cache.json").apply { parentFile?.mkdirs() }

    private val metaFile: File get() =
        File(context.cacheDir, "cache_meta.txt").apply { parentFile?.mkdirs() }

    fun getCachedWeather(): WeatherResponse? {
        if (!weatherFile.exists()) return null
        if (isExpired()) { clear(); return null }
        return try {
            json.decodeFromString<WeatherResponse>(weatherFile.readText())
        } catch (_: Exception) { null }
    }

    fun getCachedAqi(): AirQualityResponse? {
        if (!aqiFile.exists()) return null
        if (isExpired()) { clear(); return null }
        return try {
            json.decodeFromString<AirQualityResponse>(aqiFile.readText())
        } catch (_: Exception) { null }
    }

    fun cacheWeather(response: WeatherResponse) {
        try {
            weatherFile.writeText(json.encodeToString(response))
            updateTimestamp()
        } catch (_: Exception) {}
    }

    fun cacheAqi(response: AirQualityResponse) {
        try {
            aqiFile.writeText(json.encodeToString(response))
            updateTimestamp()
        } catch (_: Exception) {}
    }

    private fun isExpired(): Boolean {
        return try {
            val timestamp = metaFile.readText().toLongOrNull() ?: return true
            (System.currentTimeMillis() - timestamp) > TTL_MILLIS
        } catch (_: Exception) { true }
    }

    private fun updateTimestamp() {
        try { metaFile.writeText(System.currentTimeMillis().toString()) } catch (_: Exception) {}
    }

    fun clear() {
        weatherFile.delete()
        aqiFile.delete()
        metaFile.delete()
    }

    fun setTtlHours(hours: Int) {
        ttlHours = hours.coerceAtLeast(1)
    }

    private var ttlHours: Int = 4

    private val TTL_MILLIS: Long get() = ttlHours * 60 * 60 * 1000L
}
