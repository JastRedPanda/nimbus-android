package com.nimbus.weather.util

import com.nimbus.weather.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeUtilsTest {

    @Test
    fun `clear sky`() {
        assertEquals(R.string.wmo_0, weatherDescriptionRes(0))
    }

    @Test
    fun `mainly clear`() {
        assertEquals(R.string.wmo_1, weatherDescriptionRes(1))
    }

    @Test
    fun `partly cloudy`() {
        assertEquals(R.string.wmo_2, weatherDescriptionRes(2))
    }

    @Test
    fun `overcast`() {
        assertEquals(R.string.wmo_3, weatherDescriptionRes(3))
    }

    @Test
    fun `fog`() {
        assertEquals(R.string.wmo_45, weatherDescriptionRes(45))
        assertEquals(R.string.wmo_48, weatherDescriptionRes(48))
    }

    @Test
    fun `drizzle`() {
        assertEquals(R.string.wmo_51, weatherDescriptionRes(51))
        assertEquals(R.string.wmo_55, weatherDescriptionRes(55))
    }

    @Test
    fun `rain`() {
        assertEquals(R.string.wmo_61, weatherDescriptionRes(61))
        assertEquals(R.string.wmo_65, weatherDescriptionRes(65))
    }

    @Test
    fun `snow`() {
        assertEquals(R.string.wmo_71, weatherDescriptionRes(71))
        assertEquals(R.string.wmo_75, weatherDescriptionRes(75))
    }

    @Test
    fun `thunderstorm`() {
        assertEquals(R.string.wmo_95, weatherDescriptionRes(95))
        assertEquals(R.string.wmo_99, weatherDescriptionRes(99))
    }

    @Test
    fun `unknown code defaults to clear`() {
        assertEquals(R.string.wmo_0, weatherDescriptionRes(999))
    }

    @Test
    fun `icons follow day and night`() {
        assertEquals(R.drawable.ic_weather_clear_day, weatherIcon(0, isDay = true))
        assertEquals(R.drawable.ic_weather_clear_night, weatherIcon(0, isDay = false))
        assertEquals(R.drawable.ic_weather_partly_day, weatherIcon(2, isDay = true))
        assertEquals(R.drawable.ic_weather_partly_night, weatherIcon(2, isDay = false))
    }

    @Test
    fun `icons cover all groups`() {
        assertEquals(R.drawable.ic_weather_overcast, weatherIcon(3))
        assertEquals(R.drawable.ic_weather_fog, weatherIcon(45))
        assertEquals(R.drawable.ic_weather_rain, weatherIcon(61))
        assertEquals(R.drawable.ic_weather_sleet, weatherIcon(66))
        assertEquals(R.drawable.ic_weather_snow, weatherIcon(71))
        assertEquals(R.drawable.ic_weather_thunderstorm, weatherIcon(95))
        assertEquals(R.drawable.ic_weather_clear_day, weatherIcon(999))
    }
}
