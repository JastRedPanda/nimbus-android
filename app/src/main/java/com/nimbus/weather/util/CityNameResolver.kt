package com.nimbus.weather.util

object CityNameResolver {

    private val KNOWN_TRANSLATIONS = mapOf(
        "Kyiv" to mapOf(
            "ru" to "Киев",
            "uk" to "Київ",
            "cs" to "Kyiv",
            "en" to "Kyiv"
        ),
        "Киев" to mapOf(
            "ru" to "Киев",
            "uk" to "Київ",
            "cs" to "Kyiv",
            "en" to "Kyiv"
        ),
        "Київ" to mapOf(
            "ru" to "Киев",
            "uk" to "Київ",
            "cs" to "Kyiv",
            "en" to "Kyiv"
        )
    )

    fun displayName(
        name: String,
        localNames: Map<String, String> = emptyMap(),
        languageCode: String
    ): String {
        localNames[languageCode]?.takeIf { it.isNotBlank() }?.let { return it }
        KNOWN_TRANSLATIONS[name]?.get(languageCode)?.let { return it }
        return name
    }
}