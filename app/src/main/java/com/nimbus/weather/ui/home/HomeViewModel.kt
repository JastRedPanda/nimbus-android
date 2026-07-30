package com.nimbus.weather.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.model.CurrentWeather
import com.nimbus.weather.data.model.WeatherResponse
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.ui.components.DailyForecastData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeUiState(
    val cityName: String = "",
    val current: CurrentWeather? = null,
    val daily: List<DailyForecastData> = emptyList(),
    val sunrise: String = "",
    val sunset: String = "",
    val loading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository()
    private val settings = SettingsDataStore(application)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadWeather()
    }

    fun loadWeather() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            try {
                val loc = settings.getLocationSnapshot()
                val response = repository.getWeather(loc.lat, loc.lon)

                _state.value = _state.value.copy(
                    cityName = loc.name,
                    current = response.current,
                    sunrise = response.daily?.sunrise?.firstOrNull() ?: "",
                    sunset = response.daily?.sunset?.firstOrNull() ?: "",
                    daily = mapDaily(response),
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
                windDirection = daily.windDirectionDominant.getOrElse(i) { 0.0 }
            )
        }
    }
}
