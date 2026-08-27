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
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.ui.components.DailyForecastData
import com.nimbus.weather.ui.components.HourlyForecastData
import com.nimbus.weather.util.CityNameResolver
import com.nimbus.weather.util.CityNameTranslator
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
    val favouriteCities: List<FavouriteCity> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val hourlyIntervalHours: Int = SettingsDataStore.DEFAULT_HOURLY_INTERVAL_HOURS
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
        viewModelScope.launch {
            settings.tempUnit.collect { unit ->
                _state.value = _state.value.copy(tempUnit = unit)
            }
        }
        viewModelScope.launch {
            settings.useFeelsLike.collect { feels ->
                _state.value = _state.value.copy(useFeelsLike = feels)
            }
        }
        viewModelScope.launch {
            settings.hourlyIntervalHours.collect { interval ->
                val current = _state.value
                _state.value = current.copy(hourlyIntervalHours = interval)
            }
        }
        viewModelScope.launch {
            settings.notificationsEnabled.collect { enabled ->
                _state.value = _state.value.copy(notificationsEnabled = enabled)
            }
        }
        loadWeather()
        ensureCityTranslations()
    }

    private fun ensureCityTranslations() {
        viewModelScope.launch {
            try {
                val translator = CityNameTranslator(repository, settings)
                val appLanguage = settings.appLanguage.first()
                val lang = LanguageHelper.resolve(appLanguage)
                val loc = settings.getLocationSnapshot()
                val cities = buildList {
                    add(FavouriteCity(loc.name, loc.lat, loc.lon, loc.tz, loc.localNames))
                    addAll(settings.favouriteCities.first())
                }.distinctBy { it.name }
                translator.ensureTranslations(cities, listOf(lang))
                val updatedLoc = settings.getLocationSnapshot()
                _state.value = _state.value.copy(
                    cityName = CityNameResolver.displayName(
                        updatedLoc.name, updatedLoc.localNames, lang
                    ),
                    favouriteDisplayNames = buildDisplayNames(
                        settings.favouriteCities.first(), appLanguage
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun buildDisplayNames(cities: List<FavouriteCity>, appLanguage: String): Map<String, String> {
        val lang = LanguageHelper.resolve(appLanguage)
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
            val home = settings.getHomeSettings()
            val ctx = getApplication<Application>()
            repository.setTtlHours(home.updateIntervalHours * 2)
            val response = repository.getWeather(home.lat, home.lon, ctx)
            WidgetUpdateManager.updateAllWidgets(ctx, response, home.cityName)

            if (home.notificationsEnabled) {
                NotificationHelper.showWeatherNotification(ctx, response)
            }

            val aqi = if (home.showAqi) {
                try {
                    repository.getAirQuality(home.lat, home.lon, ctx).current
                } catch (_: Exception) { null }
            } else null

            val lang = LanguageHelper.resolve(home.appLanguage)

            _state.value = _state.value.copy(
                cityName = CityNameResolver.displayName(home.cityName, home.localNames, lang),
                current = response.current,
                hourly = mapHourly(response, home.hourlyIntervalHours),
                sunrise = response.daily?.sunrise?.firstOrNull() ?: "",
                sunset = response.daily?.sunset?.firstOrNull() ?: "",
                daily = mapDaily(response),
                aqi = aqi,
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
        val size = endIndex - startIndex
        val result = ArrayList<HourlyForecastData>(size / step + 1)
        var i = startIndex
        while (i < endIndex) {
            result.add(
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
            )
            i += step
        }
        return result
    }

    private fun mapDaily(response: WeatherResponse): List<DailyForecastData> {
        val daily = response.daily ?: return emptyList()
        val size = daily.time.size
        val result = ArrayList<DailyForecastData>(size)
        for (i in 0 until size) {
            result.add(
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
            )
        }
        return result
    }
}
