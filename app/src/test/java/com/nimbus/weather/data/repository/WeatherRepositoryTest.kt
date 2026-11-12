package com.nimbus.weather.data.repository

import com.nimbus.weather.data.model.GeocodingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {

    @Test
    fun `language chain contains all supported locales`() {
        val chain = WeatherRepository.buildLanguageChain("cs")
        assertEquals(4, chain.size)
        assertEquals("cs", chain[0])
        assertEquals("en", chain[1])
    }

    @Test
    fun `normalizeName handles cyrillic and spaces`() {
        val normalized = WeatherRepository.normalizeName("  Kyiv! ")
        assertEquals("kyiv", normalized)
    }

    @Test
    fun `exact match across alphabets`() {
        assertTrue(WeatherRepository.isExactMatch("Харків", "Kharkiv"))
        assertTrue(WeatherRepository.isExactMatch("Харьков", "Харьков"))
        assertFalse(WeatherRepository.isExactMatch("Харьков", "Харьковка"))
    }

    @Test
    fun `rankResults prioritizes exact matches`() {
        val results = listOf(
            GeocodingResult(id = 1, name = "Kyiv Oblast", latitude = 0.0, longitude = 0.0),
            GeocodingResult(id = 2, name = "Kyiv", latitude = 0.0, longitude = 0.0),
            GeocodingResult(id = 3, name = "Lviv", latitude = 0.0, longitude = 0.0)
        )
        val ranked = WeatherRepository.rankResults("Kyiv", results)
        assertEquals(2, ranked.first().id)
    }
}
