package com.nimbus.weather.ui.location

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.model.GeocodingResult
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.util.CityNameTranslator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocationSearchUiState(
    val query: String = "",
    val results: List<GeocodingResult> = emptyList(),
    val loading: Boolean = false,
    val noResults: Boolean = false,
    val favouriteNames: Set<String> = emptySet(),
    val recentCities: List<SettingsDataStore.FavouriteCity> = emptyList(),
    val appLanguage: String = "auto"
)

class LocationSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository()
    private val settings = SettingsDataStore(application)

    private val _state = MutableStateFlow(LocationSearchUiState())
    val state: StateFlow<LocationSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            settings.favouriteCities.collect { cities ->
                _state.value = _state.value.copy(
                    favouriteNames = cities.map { it.name }.toSet()
                )
            }
        }
        viewModelScope.launch {
            settings.recentCities.collect { cities ->
                _state.value = _state.value.copy(recentCities = cities)
            }
        }
        viewModelScope.launch {
            settings.appLanguage.collect { lang ->
                _state.value = _state.value.copy(appLanguage = lang)
            }
        }
    }

    fun onQueryChanged(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300)
                search(query)
            }
        } else {
            _state.value = _state.value.copy(
                results = emptyList(),
                noResults = false
            )
        }
    }

    private suspend fun search(query: String) {
        _state.value = _state.value.copy(loading = true, noResults = false)
        try {
            val lang = resolveLanguage(_state.value.appLanguage)
            val results = repository.searchCities(query, lang)
            _state.value = _state.value.copy(
                results = results,
                loading = false,
                noResults = results.isEmpty()
            )
        } catch (_: Exception) {
            _state.value = _state.value.copy(loading = false, noResults = true)
        }
    }

    private fun resolveLanguage(appLanguage: String): String {
        return if (appLanguage == "auto") {
            com.nimbus.weather.util.LanguageHelper.resolveLocale().language
        } else {
            appLanguage
        }
    }

    fun selectCity(result: GeocodingResult) {
        viewModelScope.launch {
            val city = SettingsDataStore.FavouriteCity(
                name = result.name,
                lat = result.latitude,
                lon = result.longitude,
                tz = result.timezone ?: "Europe/Kiev",
                localNames = result.localNames.orEmpty()
            )
            settings.setLocation(city.name, city.lat, city.lon, city.tz, city.localNames)
            settings.addRecentCity(city)
            translateAll(city)
        }
    }

    fun selectRecentCity(city: SettingsDataStore.FavouriteCity) {
        viewModelScope.launch {
            settings.setLocation(
                name = city.name,
                lat = city.lat,
                lon = city.lon,
                tz = city.tz,
                localNames = city.localNames
            )
            settings.addRecentCity(city)
            translateAll(city)
        }
    }

    fun toggleFavourite(result: GeocodingResult) {
        viewModelScope.launch {
            val city = SettingsDataStore.FavouriteCity(
                name = result.name,
                lat = result.latitude,
                lon = result.longitude,
                tz = result.timezone ?: "Europe/Kiev",
                localNames = result.localNames.orEmpty()
            )
            if (_state.value.favouriteNames.contains(result.name)) {
                settings.removeFavouriteCity(result.name)
            } else {
                settings.addFavouriteCity(city)
                translateAll(city)
            }
        }
    }

    fun toggleRecentFavourite(city: SettingsDataStore.FavouriteCity) {
        viewModelScope.launch {
            if (_state.value.favouriteNames.contains(city.name)) {
                settings.removeFavouriteCity(city.name)
            } else {
                settings.addFavouriteCity(city)
                translateAll(city)
            }
        }
    }

    private suspend fun translateAll(city: SettingsDataStore.FavouriteCity) {
        try {
            CityNameTranslator(repository, settings)
                .ensureTranslations(listOf(city), ALL_CITY_LANGUAGES)
        } catch (_: Exception) {
        }
    }

    companion object {
        private val ALL_CITY_LANGUAGES = listOf("ru", "uk", "en", "cs")
    }

    fun removeRecentCity(name: String) {
        viewModelScope.launch {
            settings.removeRecentCity(name)
        }
    }
}
