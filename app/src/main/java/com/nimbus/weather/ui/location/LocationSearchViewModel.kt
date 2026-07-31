package com.nimbus.weather.ui.location

import android.app.Application
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.model.GeocodingResult
import com.nimbus.weather.data.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit

data class LocationSearchUiState(
    val query: String = "",
    val results: List<GeocodingResult> = emptyList(),
    val loading: Boolean = false,
    val noResults: Boolean = false,
    val locating: Boolean = false
)

class LocationSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository()
    private val settings = SettingsDataStore(application)
    private val geocoder = Geocoder(application, Locale.getDefault())

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

    fun onMyLocationClick() {
        viewModelScope.launch {
            _state.value = _state.value.copy(locating = true)
            try {
                val client = LocationServices.getFusedLocationProviderClient(getApplication())
                val task = client.lastLocation
                val location = withContext(Dispatchers.IO) {
                    Tasks.await(task, 30, TimeUnit.SECONDS)
                }
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val cityName = resolveCityName(lat, lon)
                    settings.setLocation(
                        name = cityName,
                        lat = lat,
                        lon = lon,
                        tz = "Europe/Kiev"
                    )
                }
            } catch (_: Exception) {
            } finally {
                _state.value = _state.value.copy(locating = false)
            }
        }
    }

    private fun resolveCityName(lat: Double, lon: Double): String {
        return try {
            val addresses: List<Address> = geocoder.getFromLocation(lat, lon, 1) ?: emptyList()
            val addr = addresses.firstOrNull()
            val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
            city ?: "%.4f, %.4f".format(lat, lon)
        } catch (_: Exception) {
            "%.4f, %.4f".format(lat, lon)
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


