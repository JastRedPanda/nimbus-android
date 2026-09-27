package com.nimbus.weather.data.repository

import com.nimbus.weather.data.model.GeocodingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageChainTest {

    @Test
    fun `app language first, then english, then the rest`() {
        assertEquals(
            listOf("ru", "en", "uk", "cs"),
            WeatherRepository.buildLanguageChain("ru")
        )
        assertEquals(
            listOf("uk", "en", "ru", "cs"),
            WeatherRepository.buildLanguageChain("uk")
        )
    }

    @Test
    fun `no duplicates when app language is english`() {
        assertEquals(
            listOf("en", "ru", "uk", "cs"),
            WeatherRepository.buildLanguageChain("en")
        )
    }

    @Test
    fun `normalizeName transliterates cyrillic`() {
        assertEquals("kharkiv", WeatherRepository.normalizeName("Харків"))
        assertEquals("kharkov", WeatherRepository.normalizeName("Харьков"))
        assertEquals("kharkiv", WeatherRepository.normalizeName("Kharkiv"))
        assertEquals("kharkov", WeatherRepository.normalizeName("  Харьков! "))
    }

    @Test
    fun `exact match across alphabets`() {
        assertTrue(WeatherRepository.isExactMatch("Харків", "Kharkiv"))
        assertTrue(WeatherRepository.isExactMatch("Харьков", "Харьков"))
        assertFalse(WeatherRepository.isExactMatch("Харьков", "Харьковка"))
    }

    @Test
    fun `exact matches ranked first`() {
        fun city(id: Int, name: String) = GeocodingResult(
            id = id, name = name, latitude = 0.0, longitude = 0.0
        )
        val ranked = WeatherRepository.rankResults(
            "Харьков",
            listOf(city(1, "Харьковка"), city(2, "Харьков"), city(3, "Ростов"))
        )
        assertEquals(listOf(2, 1, 3), ranked.map { it.id })
    }
}
