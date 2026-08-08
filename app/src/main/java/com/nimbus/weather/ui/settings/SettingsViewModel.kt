package com.nimbus.weather.ui.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity
import com.nimbus.weather.service.WeatherUpdateScheduler
import com.nimbus.weather.util.CityNameResolver
import com.nimbus.weather.util.LanguageHelper
import com.nimbus.weather.util.ThemeMode
import com.nimbus.weather.util.TemperatureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        viewModelScope.launch {
            settings.useFeelsLike.collect { value ->
                _state.value = _state.value.copy(useFeelsLike = value)
            }
        }
        viewModelScope.launch {
            settings.tempUnit.collect { unit ->
                _state.value = _state.value.copy(tempUnit = unit)
            }
        }
        viewModelScope.launch {
            settings.themeMode.collect { mode ->
                _state.value = _state.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            settings.cityName.collect { name ->
                _state.value = _state.value.copy(cityName = name)
            }
        }
        viewModelScope.launch {
            settings.cityLocalNames.collect { localNames ->
                _state.value = _state.value.copy(cityLocalNames = localNames)
            }
        }
        viewModelScope.launch {
            settings.notificationsEnabled.collect { enabled ->
                _state.value = _state.value.copy(notificationsEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settings.updateIntervalHours.collect { hours ->
                _state.value = _state.value.copy(updateIntervalHours = hours)
            }
        }
        viewModelScope.launch {
            settings.appLanguage.collect { lang ->
                _state.value = _state.value.copy(
                    appLanguage = lang,
                    favouriteDisplayNames = buildDisplayNames(_state.value.favouriteCities, lang)
                )
            }
        }
        viewModelScope.launch {
            settings.hourlyIntervalHours.collect { hours ->
                _state.value = _state.value.copy(hourlyIntervalHours = hours)
            }
        }
        viewModelScope.launch {
            settings.showAqi.collect { show ->
                _state.value = _state.value.copy(showAqi = show)
            }
        }
        viewModelScope.launch {
            settings.favouriteCities.collect { cities ->
                _state.value = _state.value.copy(
                    favouriteCities = cities,
                    favouriteDisplayNames = buildDisplayNames(cities, _state.value.appLanguage)
                )
            }
        }
    }

    private fun buildDisplayNames(cities: List<FavouriteCity>, appLanguage: String): Map<String, String> {
        val lang = if (appLanguage == "auto") LanguageHelper.resolveLocale().language else appLanguage
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
            settings.setAppLanguage(language)
            val ctx = getApplication<Application>()
            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ctx.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setNotificationsEnabled(enabled)
            if (enabled) {
                try {
                    val loc = settings.getLocationSnapshot()
                    val repository = com.nimbus.weather.data.repository.WeatherRepository()
                    val response = repository.getWeather(loc.lat, loc.lon, getApplication())
                    com.nimbus.weather.service.NotificationHelper.showWeatherNotification(
                        getApplication(), response
                    )
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
            val updated = current.toMutableList().apply {
                add(target, removeAt(index))
            }
            settings.setFavouriteCities(updated)
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
            val ctx = getApplication<Application>()
            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ctx.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }
}
