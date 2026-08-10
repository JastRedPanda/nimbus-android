package com.nimbus.weather.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity
import com.nimbus.weather.data.model.AirQualityCurrent
import com.nimbus.weather.data.model.CurrentWeather
import com.nimbus.weather.data.model.HourlyWeather
import com.nimbus.weather.data.model.WeatherResponse
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.service.NotificationHelper
import com.nimbus.weather.ui.components.DailyForecastData
import com.nimbus.weather.ui.components.HourlyForecastData
import com.nimbus.weather.util.CityNameResolver
import com.nimbus.weather.util.LanguageHelper
import com.nimbus.weather.util.TemperatureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeUiState(
    val cityName: String = "",
    val current: CurrentWeather? = null,
    val hourly: List<HourlyForecastData> = emptyList(),
    val daily: List<DailyForecastData> = emptyList(),
    val aqi: AirQualityCurrent? = null,
    val sunrise: String = "",
    val sunset: String = "",
    val tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val useFeelsLike: Boolean = false,
    val showAqi: Boolean = true,
    val fromCache: Boolean = false,
    val appLanguage: String = "auto",
    val favouriteDisplayNames: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val favouriteCities: List<FavouriteCity> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository()
    private val settings = SettingsDataStore(application)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.favouriteCities.collect { cities ->
                _state.value = _state.value.copy(
                    favouriteCities = cities,
                    favouriteDisplayNames = buildDisplayNames(cities, _state.value.appLanguage)
                )
            }
        }
        viewModelScope.launch {
            settings.appLanguage.collect { lang ->
                val previous = _state.value.appLanguage
                _state.value = _state.value.copy(
                    appLanguage = lang,
                    favouriteDisplayNames = buildDisplayNames(_state.value.favouriteCities, lang)
                )
                if (previous != lang && _state.value.current != null) {
                    loadWeather()
                }
            }
        }
        viewModelScope.launch {
            settings.showAqi.collect { show ->
                _state.value = _state.value.copy(showAqi = show)
            }
        }
        loadWeather()
    }

    private fun buildDisplayNames(cities: List<FavouriteCity>, appLanguage: String): Map<String, String> {
        val lang = if (appLanguage == "auto") LanguageHelper.resolveLocale().language else appLanguage
        return cities.associate { city ->
            city.name to CityNameResolver.displayName(city.name, city.localNames, lang)
        }
    }

    fun loadWeather() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            performLoad()
        }
    }

    fun refresh() {
        if (_state.value.refreshing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(refreshing = true)
            performLoad()
            _state.value = _state.value.copy(refreshing = false)
        }
    }

    private suspend fun performLoad() {
        try {
                val loc = settings.getLocationSnapshot()
                val ctx = getApplication<Application>()
                val updateInterval = settings.updateIntervalHours.first()
                val showAqi = settings.showAqi.first()
                val appLanguage = settings.appLanguage.first()
                repository.setTtlHours(updateInterval * 2)
                val response = repository.getWeather(loc.lat, loc.lon, ctx)
                val tempUnit = settings.tempUnit.first()
                val feelsLike = settings.useFeelsLike.first()
                val hourlyInterval = settings.hourlyIntervalHours.first()

                if (settings.notificationsEnabled.first()) {
                    NotificationHelper.showWeatherNotification(ctx, response)
                }

                val aqi = if (showAqi) {
                    try {
                        repository.getAirQuality(loc.lat, loc.lon, ctx).current
                    } catch (_: Exception) { null }
                } else null

                val lang = if (appLanguage == "auto") {
                    LanguageHelper.resolveLocale().language
                } else {
                    appLanguage
                }

                _state.value = _state.value.copy(
                    cityName = CityNameResolver.displayName(loc.name, loc.localNames, lang),
                    current = response.current,
                    hourly = mapHourly(response, hourlyInterval),
                    sunrise = response.daily?.sunrise?.firstOrNull() ?: "",
                    sunset = response.daily?.sunset?.firstOrNull() ?: "",
                    daily = mapDaily(response),
                    aqi = aqi,
                    tempUnit = tempUnit,
                    useFeelsLike = feelsLike,
                    showAqi = showAqi,
                    fromCache = repository.showingCachedWeather,
                    loading = false,
                    refreshing = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    error = e.message ?: "Unknown error"
                )
            }
    }

    fun switchToCity(city: FavouriteCity) {
        viewModelScope.launch {
            settings.setLocation(city.name, city.lat, city.lon, city.tz, city.localNames)
            loadWeather()
        }
    }

    private fun mapHourly(response: WeatherResponse, intervalHours: Int): List<HourlyForecastData> {
        val hourly = response.hourly ?: return emptyList()
        val now = response.current?.time ?: return emptyList()
        val startIndex = hourly.time.indexOfFirst { it >= now }
        if (startIndex < 0) return emptyList()
        val step = intervalHours.coerceIn(1, 6)
        val endIndex = (startIndex + 24).coerceAtMost(hourly.time.size)
        return (startIndex until endIndex step step).map { i ->
            HourlyForecastData(
                time = hourly.time[i],
                temperature = hourly.temperature.getOrElse(i) { 0.0 },
                precipitation = hourly.precipitation.getOrElse(i) { 0.0 },
                weatherCode = hourly.weatherCode.getOrElse(i) { 0 },
                windSpeed = hourly.windSpeed.getOrElse(i) { 0.0 },
                windDirection = hourly.windDirection.getOrElse(i) { 0.0 },
                humidity = hourly.humidity.getOrElse(i) { 0.0 },
                apparentTemperature = hourly.apparentTemperature.getOrElse(i) { 0.0 },
                uvIndex = hourly.uvIndex.getOrElse(i) { 0.0 }
            )
        }
    }

    private fun mapDaily(response: WeatherResponse): List<DailyForecastData> {
        val daily = response.daily ?: return emptyList()
        val size = daily.time.size
        return List(size) { i ->
            DailyForecastData(
                date = daily.time[i],
                weatherCode = daily.weatherCode.getOrElse(i) { 0 },
                tempMax = daily.temperatureMax.getOrElse(i) { 0.0 },
                tempMin = daily.temperatureMin.getOrElse(i) { 0.0 },
                feelsLikeMax = daily.apparentTemperatureMax.getOrElse(i) { 0.0 },
                feelsLikeMin = daily.apparentTemperatureMin.getOrElse(i) { 0.0 },
                sunrise = daily.sunrise.getOrElse(i) { "" },
                sunset = daily.sunset.getOrElse(i) { "" },
                precipitation = daily.precipitationSum.getOrElse(i) { 0.0 },
                precipProbability = daily.precipitationProbabilityMax.getOrElse(i) { 0 },
                windMax = daily.windSpeedMax.getOrElse(i) { 0.0 },
                windGusts = daily.windGustsMax.getOrElse(i) { 0.0 },
                windDirection = daily.windDirectionDominant.getOrElse(i) { 0.0 },
                uvIndexMax = daily.uvIndexMax.getOrElse(i) { 0.0 }
            )
        }
    }
}
