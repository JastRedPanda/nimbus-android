package com.nimbus.weather.ui.location

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity
import com.nimbus.weather.data.model.GeocodingResult
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.CityNameTranslator
import com.nimbus.weather.util.LanguageHelper
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
    val recentCities: List<FavouriteCity> = emptyList(),
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
                _state.value = _state.value.copy(favouriteNames = cities.map { it.name }.toSet())
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
            _state.value = _state.value.copy(results = emptyList(), noResults = false)
        }
    }

    private suspend fun search(query: String) {
        _state.value = _state.value.copy(loading = true, noResults = false)
        try {
            val results = repository.searchCities(query, LanguageHelper.resolve(_state.value.appLanguage))
            _state.value = _state.value.copy(
                results = results,
                loading = false,
                noResults = results.isEmpty()
            )
        } catch (_: Exception) {
            _state.value = _state.value.copy(loading = false, noResults = true)
        }
    }

    fun selectCity(result: GeocodingResult) {
        applyCity(result.toCity())
    }

    fun selectRecentCity(city: FavouriteCity) {
        applyCity(city)
    }

    fun toggleFavourite(result: GeocodingResult) {
        toggleFavourite(result.toCity())
    }

    fun toggleRecentFavourite(city: FavouriteCity) {
        toggleFavourite(city)
    }

    private fun applyCity(city: FavouriteCity) {
        viewModelScope.launch {
            settings.setLocation(city.name, city.lat, city.lon, city.tz, city.localNames)
            settings.addRecentCity(city)
            translateAll(city)
        }
    }

    private fun toggleFavourite(city: FavouriteCity) {
        viewModelScope.launch {
            if (_state.value.favouriteNames.contains(city.name)) {
                settings.removeFavouriteCity(city.name)
            } else {
                settings.addFavouriteCity(city)
                translateAll(city)
            }
            WidgetUpdateManager.updateFromTargetCity(getApplication())
        }
    }

    private suspend fun translateAll(city: FavouriteCity) {
        try {
            CityNameTranslator(repository, settings).ensureTranslations(listOf(city), ALL_CITY_LANGUAGES)
        } catch (_: Exception) {
        }
    }

    fun removeRecentCity(name: String) {
        viewModelScope.launch {
            settings.removeRecentCity(name)
        }
    }

    private fun GeocodingResult.toCity(): FavouriteCity = FavouriteCity(
        name = name,
        lat = latitude,
        lon = longitude,
        tz = timezone ?: "Europe/Kiev",
        localNames = localNames.orEmpty()
    )

    companion object {
        private val ALL_CITY_LANGUAGES = listOf("ru", "uk", "en", "cs")
    }
}
