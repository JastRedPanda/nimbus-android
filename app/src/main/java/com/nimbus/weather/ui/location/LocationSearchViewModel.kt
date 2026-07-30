package com.nimbus.weather.ui.location

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.model.GeocodingResult
import com.nimbus.weather.data.repository.WeatherRepository
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
    val noResults: Boolean = false
)

class LocationSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository()
    private val settings = SettingsDataStore(application)

    private val _state = MutableStateFlow(LocationSearchUiState())
    val state: StateFlow<LocationSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

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
            val results = repository.searchCities(query)
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
        viewModelScope.launch {
            settings.setLocation(
                name = result.name,
                lat = result.latitude,
                lon = result.longitude,
                tz = result.timezone ?: "Europe/Kiev"
            )
        }
    }
}
