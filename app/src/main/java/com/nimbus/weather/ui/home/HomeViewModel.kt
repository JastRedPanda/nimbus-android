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
import com.nimbus.weather.ui.components.DailyForecastData
import com.nimbus.weather.ui.components.HourlyForecastData
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
    val loading: Boolean = true,
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
                _state.value = _state.value.copy(favouriteCities = cities)
            }
        }
        loadWeather()
    }

    fun loadWeather() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            try {
                val loc = settings.getLocationSnapshot()
                val ctx = getApplication<Application>()
                val response = repository.getWeather(loc.lat, loc.lon, ctx)
                val tempUnit = settings.tempUnit.first()
                val feelsLike = settings.useFeelsLike.first()

                val aqi = try {
                    repository.getAirQuality(loc.lat, loc.lon, ctx).current
                } catch (_: Exception) { null }

                _state.value = _state.value.copy(
                    cityName = loc.name,
                    current = response.current,
                    hourly = mapHourly(response),
                    sunrise = response.daily?.sunrise?.firstOrNull() ?: "",
                    sunset = response.daily?.sunset?.firstOrNull() ?: "",
                    daily = mapDaily(response),
                    aqi = aqi,
                    tempUnit = tempUnit,
                    useFeelsLike = feelsLike,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun switchToCity(city: FavouriteCity) {
        viewModelScope.launch {
            settings.setLocation(city.name, city.lat, city.lon, city.tz)
            loadWeather()
        }
    }

    fun addCurrentCityToFavourites() {
        viewModelScope.launch {
            val loc = settings.getLocationSnapshot()
            settings.addFavouriteCity(FavouriteCity(loc.name, loc.lat, loc.lon, loc.tz))
        }
    }

    fun removeFavouriteCity(name: String) {
        viewModelScope.launch {
            settings.removeFavouriteCity(name)
        }
    }

    private fun mapHourly(response: WeatherResponse): List<HourlyForecastData> {
        val hourly = response.hourly ?: return emptyList()
        val now = response.current?.time ?: return emptyList()
        val startIndex = hourly.time.indexOfFirst { it >= now }
        if (startIndex < 0) return emptyList()
        val endIndex = (startIndex + 8).coerceAtMost(hourly.time.size)
        return (startIndex until endIndex).map { i ->
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
