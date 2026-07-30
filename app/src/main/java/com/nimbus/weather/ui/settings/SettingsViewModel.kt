package com.nimbus.weather.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbus.weather.data.local.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val useFeelsLike: Boolean = false,
    val cityName: String = ""
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
            settings.cityName.collect { name ->
                _state.value = _state.value.copy(cityName = name)
            }
        }
    }

    fun setUseFeelsLike(value: Boolean) {
        viewModelScope.launch {
            settings.setUseFeelsLike(value)
        }
    }
}
