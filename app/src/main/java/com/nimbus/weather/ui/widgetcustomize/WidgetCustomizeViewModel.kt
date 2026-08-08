package com.nimbus.weather.ui.widgetcustomize

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WidgetUpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WidgetCustomizeUiState(
    val bgColorHex: String? = null,
    val bgAlpha: Int = 100,
    val textOption: String = "auto"
)

@Stable
interface WidgetCustomizeActions {
    fun onBgColorSelected(colorHex: String?)
    fun onBgAlphaChanged(alpha: Int)
    fun onTextOptionSelected(option: String)
    fun onReset()
}

class WidgetCustomizeViewModel(
    application: Application
) : AndroidViewModel(application), WidgetCustomizeActions {

    private val settings = SettingsDataStore(application)

    private val _state = MutableStateFlow(WidgetCustomizeUiState())
    val state: StateFlow<WidgetCustomizeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.widgetBgColor.collect { color ->
                _state.update { it.copy(bgColorHex = color) }
            }
        }
        viewModelScope.launch {
            settings.widgetBgAlpha.collect { alpha ->
                _state.update { it.copy(bgAlpha = alpha) }
            }
        }
        viewModelScope.launch {
            settings.widgetTextColor.collect { option ->
                _state.update { it.copy(textOption = option) }
            }
        }
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            WidgetUpdateManager.refreshAllWidgets(getApplication())
        }
    }

    override fun onBgColorSelected(colorHex: String?) {
        persist { settings.setWidgetBgColor(colorHex) }
    }

    override fun onBgAlphaChanged(alpha: Int) {
        persist { settings.setWidgetBgAlpha(alpha) }
    }

    override fun onTextOptionSelected(option: String) {
        persist { settings.setWidgetTextColor(option) }
    }

    override fun onReset() {
        persist {
            settings.setWidgetBgColor(null)
            settings.setWidgetBgAlpha(SettingsDataStore.DEFAULT_WIDGET_BG_ALPHA)
            settings.setWidgetTextColor("auto")
        }
    }
}