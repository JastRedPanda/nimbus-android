package com.nimbus.weather.data.repository

import android.content.Context
import com.nimbus.weather.data.model.CurrentWeather
import com.nimbus.weather.data.model.WeatherResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WeatherCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var weatherCache: WeatherCache

    private val sampleWeather = WeatherResponse(
        latitude = 50.45,
        longitude = 30.52,
        timezone = "Europe/Kiev",
        current = CurrentWeather(
            time = "2026-09-09T06:00",
            temperature = 22.5,
            humidity = 55.0,
            apparentTemperature = 21.0,
            precipitation = 0.0,
            weatherCode = 0,
            pressure = 1013.0,
            windSpeed = 3.2,
            windDirection = 180.0,
            windGusts = 5.0,
            uvIndex = 3.0
        )
    )

    @Before
    fun setup() {
        context = mockk()
        every { context.cacheDir } returns tempFolder.root
        weatherCache = WeatherCache(context)
    }

    @Test
    fun `cacheWeather saves and reads weather data`() {
        weatherCache.cacheWeather(sampleWeather)

        val result = weatherCache.getCachedWeather(allowExpired = false)
        assertNotNull(result)
        assertEquals(22.5, result?.current?.temperature ?: 0.0, 0.01)
    }

    @Test
    fun `expired cache does not delete file and returns data when allowExpired is true`() {
        weatherCache.cacheWeather(sampleWeather)

        // Искусственно устанавливаем временную метку в прошлое (более 10 часов назад)
        val metaFile = tempFolder.root.resolve("cache_meta.txt")
        metaFile.writeText((System.currentTimeMillis() - 10 * 3600 * 1000L).toString())

        // Свежий экземпляр кэша считывает метаданные из файла
        val freshCache = WeatherCache(context)
        freshCache.setTtlHours(2)

        assertNull(freshCache.getCachedWeather(allowExpired = false))
        val fallback = freshCache.getCachedWeather(allowExpired = true)
        assertNotNull(fallback)
        assertEquals(22.5, fallback?.current?.temperature ?: 0.0, 0.01)
    }

    @Test
    fun `cacheWidgetWeather saves isolated widget weather`() {
        weatherCache.cacheWidgetWeather(sampleWeather)

        val widgetResult = weatherCache.getCachedWidgetWeather(allowExpired = true)
        assertNotNull(widgetResult)
        assertEquals(22.5, widgetResult?.current?.temperature ?: 0.0, 0.01)
    }
}
