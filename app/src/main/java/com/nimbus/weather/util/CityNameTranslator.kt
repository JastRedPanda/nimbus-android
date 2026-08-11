package com.nimbus.weather.util

import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity
import com.nimbus.weather.data.repository.WeatherRepository

class CityNameTranslator(
    private val repository: WeatherRepository,
    private val settings: SettingsDataStore
) {

    suspend fun ensureTranslations(cities: List<FavouriteCity>, languages: List<String>) {
        for (city in cities) {
            for (lang in languages) {
                if (city.localNames[lang]?.isNotBlank() == true) continue
                val translated = repository.translateCityName(city.name, city.lat, city.lon, lang)
                    ?: continue
                settings.updateCityTranslations(city.name, mapOf(lang to translated))
            }
        }
    }
}
