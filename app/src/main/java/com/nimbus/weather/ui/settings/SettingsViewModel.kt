package com.nimbus.weather.ui.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.service.NotificationHelper
import com.nimbus.weather.service.WeatherUpdateScheduler
import com.nimbus.weather.util.CityNameResolver
import com.nimbus.weather.util.CityNameTranslator
import com.nimbus.weather.util.LanguageHelper
import com.nimbus.weather.util.ThemeMode
import com.nimbus.weather.util.TemperatureUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val useFeelsLike: Boolean = false,
    val tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cityName: String = "",
    val notificationsEnabled: Boolean = true,
    val updateIntervalHours: Int = 2,
    val appLanguage: String = "auto",
    val cityLocalNames: Map<String, String> = emptyMap(),
    val hourlyIntervalHours: Int = 1,
    val showAqi: Boolean = true,
    val favouriteCities: List<FavouriteCity> = emptyList(),
    val favouriteDisplayNames: Map<String, String> = emptyMap()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsDataStore(application)

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun <T> Flow<T>.intoState(reduce: SettingsUiState.(T) -> SettingsUiState) {
        viewModelScope.launch {
            collect { value -> _state.value = _state.value.reduce(value) }
        }
    }

    init {
        settings.useFeelsLike.intoState { copy(useFeelsLike = it) }
        settings.tempUnit.intoState { copy(tempUnit = it) }
        settings.themeMode.intoState { copy(themeMode = it) }
        settings.cityName.intoState { copy(cityName = it) }
        settings.cityLocalNames.intoState { copy(cityLocalNames = it) }
        settings.notificationsEnabled.intoState { copy(notificationsEnabled = it) }
        settings.updateIntervalHours.intoState { copy(updateIntervalHours = it) }
        settings.hourlyIntervalHours.intoState { copy(hourlyIntervalHours = it) }
        settings.showAqi.intoState { copy(showAqi = it) }
        settings.appLanguage.intoState { lang ->
            copy(
                appLanguage = lang,
                favouriteDisplayNames = buildDisplayNames(favouriteCities, lang)
            )
        }
        settings.favouriteCities.intoState { cities ->
            copy(
                favouriteCities = cities,
                favouriteDisplayNames = buildDisplayNames(cities, appLanguage)
            )
        }
    }

    private fun buildDisplayNames(cities: List<FavouriteCity>, appLanguage: String): Map<String, String> {
        val lang = LanguageHelper.resolve(appLanguage)
        return cities.associate { city ->
            city.name to CityNameResolver.displayName(city.name, city.localNames, lang)
        }
    }

    fun setUseFeelsLike(value: Boolean) {
        viewModelScope.launch {
            settings.setUseFeelsLike(value)
        }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch {
            translateCitiesForLanguage(language)
            settings.setAppLanguage(language)
            restartApp()
        }
    }

    private suspend fun translateCitiesForLanguage(language: String) {
        try {
            val translator = CityNameTranslator(WeatherRepository(), settings)
            val loc = settings.getLocationSnapshot()
            val cities = buildList {
                add(FavouriteCity(loc.name, loc.lat, loc.lon, loc.tz, loc.localNames))
                addAll(settings.favouriteCities.first())
                addAll(settings.recentCities.first())
            }.distinctBy { it.name }
            translator.ensureTranslations(cities, listOf(language))
        } catch (_: Exception) {
        }
    }

    private fun restartApp() {
        val ctx = getApplication<Application>()
        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ctx.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setNotificationsEnabled(enabled)
            if (enabled) {
                try {
                    val loc = settings.getLocationSnapshot()
                    val response = WeatherRepository().getWeather(loc.lat, loc.lon, getApplication())
                    NotificationHelper.showWeatherNotification(getApplication(), response)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun setTempUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            settings.setTempUnit(unit)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settings.setThemeMode(mode)
        }
    }

    fun setUpdateIntervalHours(hours: Int) {
        viewModelScope.launch {
            settings.setUpdateIntervalHours(hours)
            WeatherUpdateScheduler.reschedule(getApplication(), hours)
        }
    }

    fun removeFavouriteCity(name: String) {
        viewModelScope.launch {
            settings.removeFavouriteCity(name)
        }
    }

    fun moveFavouriteCity(name: String, up: Boolean) {
        viewModelScope.launch {
            val current = _state.value.favouriteCities
            val index = current.indexOfFirst { it.name == name }
            if (index < 0) return@launch
            val target = if (up) index - 1 else index + 1
            if (target < 0 || target >= current.size) return@launch
            settings.setFavouriteCities(
                current.toMutableList().apply { add(target, removeAt(index)) }
            )
        }
    }

    fun setHourlyIntervalHours(hours: Int) {
        viewModelScope.launch {
            settings.setHourlyIntervalHours(hours)
        }
    }

    fun setShowAqi(show: Boolean) {
        viewModelScope.launch {
            settings.setShowAqi(show)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settings.resetAll()
            WeatherUpdateScheduler.reschedule(getApplication(), SettingsDataStore.DEFAULT_UPDATE_INTERVAL_HOURS)
            restartApp()
        }
    }
}
