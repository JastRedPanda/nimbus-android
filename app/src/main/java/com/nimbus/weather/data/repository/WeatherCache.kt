package com.nimbus.weather.data.repository

import android.content.Context
import com.nimbus.weather.data.model.AirQualityResponse
import com.nimbus.weather.data.model.WeatherResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class WeatherCache(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val weatherFile: File get() =
        File(context.cacheDir, "weather_cache.json").apply { parentFile?.mkdirs() }

    private val aqiFile: File get() =
        File(context.cacheDir, "aqi_cache.json").apply { parentFile?.mkdirs() }

    private val metaFile: File get() =
        File(context.cacheDir, "cache_meta.txt").apply { parentFile?.mkdirs() }

    @Volatile private var cachedExpiry: Long = Long.MIN_VALUE
    private val metaAccessLock = Any()

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
        val now = System.currentTimeMillis()
        synchronized(metaAccessLock) {
            val cached = cachedExpiry
            if (cached != Long.MIN_VALUE && now - cached <= TTL_MILLIS) return false
            val timestamp = try {
                metaFile.readText().toLongOrNull() ?: return true.also { cachedExpiry = Long.MIN_VALUE }
            } catch (_: Exception) { return true.also { cachedExpiry = Long.MIN_VALUE } }
            cachedExpiry = timestamp
            return (now - timestamp) > TTL_MILLIS
        }
    }

    private fun updateTimestamp() {
        val now = System.currentTimeMillis()
        synchronized(metaAccessLock) {
            cachedExpiry = now
        }
        try { metaFile.writeText(now.toString()) } catch (_: Exception) {}
    }

    fun clear() {
        weatherFile.delete()
        aqiFile.delete()
        metaFile.delete()
        synchronized(metaAccessLock) {
            cachedExpiry = Long.MIN_VALUE
        }
    }

    fun setTtlHours(hours: Int) {
        ttlHours = hours.coerceAtLeast(1)
    }

    private var ttlHours: Int = 4

    private val TTL_MILLIS: Long get() = ttlHours * 60 * 60 * 1000L
}
