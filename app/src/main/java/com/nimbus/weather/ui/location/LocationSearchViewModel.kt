package com.nimbus.weather.ui.location

import android.app.Application
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.model.GeocodingResult
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.util.isValidTimeZoneId
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
    val locating: Boolean = false,
    val locationError: Boolean = false,
    val favouriteNames: Set<String> = emptySet(),
    val recentCities: List<SettingsDataStore.FavouriteCity> = emptyList(),
    val appLanguage: String = "auto"
)

class LocationSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository()
    private val settings = SettingsDataStore(application)
    private val geocoder = Geocoder(application, Locale.getDefault())

    companion object {
        // Google geocoder backend кладёт IANA-таймзону в extras под ключом "timezone"
        private const val TZ_EXTRA_KEY = "timezone"
    }

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

    fun onMyLocationClick() {
        viewModelScope.launch {
            _state.value = _state.value.copy(locating = true, locationError = false)
            try {
                val client = LocationServices.getFusedLocationProviderClient(getApplication())
                val location = withContext(Dispatchers.IO) {
                    try {
                        Tasks.await(
                            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null),
                            30, TimeUnit.SECONDS
                        )
                    } catch (_: Exception) {
                        Tasks.await(client.lastLocation, 10, TimeUnit.SECONDS)
                    }
                }
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val (cityName, tz) = resolvePlace(lat, lon)
                    settings.setLocation(
                        name = cityName,
                        lat = lat,
                        lon = lon,
                        tz = tz
                    )
                } else {
                    _state.value = _state.value.copy(locationError = true)
                }
            } catch (_: Exception) {
                _state.value = _state.value.copy(locationError = true)
            } finally {
                _state.value = _state.value.copy(locating = false)
            }
        }
    }

    fun consumeLocationError() {
        _state.value = _state.value.copy(locationError = false)
    }

    private fun resolvePlace(lat: Double, lon: Double): Pair<String, String> {
        return try {
            val addresses: List<Address> = geocoder.getFromLocation(lat, lon, 1) ?: emptyList()
            val addr = addresses.firstOrNull()
            val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
            val tz = addr?.extras
                ?.getString(TZ_EXTRA_KEY)
                ?.takeIf { isValidTimeZoneId(it) }
            (city ?: "%.4f, %.4f".format(lat, lon)) to (tz ?: SettingsDataStore.DEFAULT_TZ)
        } catch (_: Exception) {
            "%.4f, %.4f".format(lat, lon) to SettingsDataStore.DEFAULT_TZ
        }
    }

    fun selectCity(result: GeocodingResult) {
        viewModelScope.launch {
            settings.setLocation(
                name = result.name,
                lat = result.latitude,
                lon = result.longitude,
                tz = result.timezone ?: "Europe/Kiev",
                localNames = result.localNames.orEmpty()
            )
            settings.addRecentCity(
                SettingsDataStore.FavouriteCity(
                    name = result.name,
                    lat = result.latitude,
                    lon = result.longitude,
                    tz = result.timezone ?: "Europe/Kiev",
                    localNames = result.localNames.orEmpty()
                )
            )
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
            }
        }
    }

    fun toggleRecentFavourite(city: SettingsDataStore.FavouriteCity) {
        viewModelScope.launch {
            if (_state.value.favouriteNames.contains(city.name)) {
                settings.removeFavouriteCity(city.name)
            } else {
                settings.addFavouriteCity(city)
            }
        }
    }

    fun removeRecentCity(name: String) {
        viewModelScope.launch {
            settings.removeRecentCity(name)
        }
    }
}


