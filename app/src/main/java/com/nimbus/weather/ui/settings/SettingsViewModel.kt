package com.nimbus.weather.ui.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WeatherUpdateScheduler
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
    val appLanguage: String = "auto"
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
                _state.value = _state.value.copy(appLanguage = lang)
            }
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
}
