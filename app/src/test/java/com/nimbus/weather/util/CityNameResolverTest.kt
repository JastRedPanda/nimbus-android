package com.nimbus.weather.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CityNameResolverTest {

    @Test
    fun `known city translates between languages`() {
        assertEquals("Київ", CityNameResolver.displayName("Киев", emptyMap(), "uk"))
        assertEquals("Kyiv", CityNameResolver.displayName("Київ", emptyMap(), "en"))
        assertEquals("Львів", CityNameResolver.displayName("Львов", emptyMap(), "uk"))
        assertEquals("Прага", CityNameResolver.displayName("Praha", emptyMap(), "ru"))
        assertEquals("Kharkiv", CityNameResolver.displayName("Харків", emptyMap(), "en"))
        assertEquals("Львов", CityNameResolver.displayName("Lviv", emptyMap(), "ru"))
        assertEquals("Киев", CityNameResolver.displayName("Kyiv", emptyMap(), "ru"))
    }

    @Test
    fun `local names take priority over dictionary`() {
        val localNames = mapOf("uk" to "Київ (тест)")
        assertEquals("Київ (тест)", CityNameResolver.displayName("Киев", localNames, "uk"))
    }

    @Test
    fun `unknown city falls back to original name`() {
        assertEquals("UnknownCity", CityNameResolver.displayName("UnknownCity", emptyMap(), "uk"))
    }
}
