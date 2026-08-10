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
    val textOption: String = "auto",
    val dateFormat: String = SettingsDataStore.DEFAULT_WIDGET_DATE_FORMAT,
    val fontScale: Int = SettingsDataStore.DEFAULT_WIDGET_FONT_SCALE
)

@Stable
interface WidgetCustomizeActions {
    fun onBgColorSelected(colorHex: String?)
    fun onBgAlphaPreview(alpha: Int)
    fun onBgAlphaCommit()
    fun onTextOptionSelected(option: String)
    fun onDateFormatSelected(format: String)
    fun onFontScalePreview(scale: Int)
    fun onFontScaleCommit()
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
        viewModelScope.launch {
            settings.widgetDateFormat.collect { format ->
                _state.update { it.copy(dateFormat = format) }
            }
        }
        viewModelScope.launch {
            settings.widgetFontScale.collect { scale ->
                _state.update { it.copy(fontScale = scale) }
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

    override fun onBgAlphaPreview(alpha: Int) {
        _state.update { it.copy(bgAlpha = alpha) }
    }

    override fun onBgAlphaCommit() {
        persist { settings.setWidgetBgAlpha(_state.value.bgAlpha) }
    }

    override fun onTextOptionSelected(option: String) {
        persist { settings.setWidgetTextColor(option) }
    }

    override fun onDateFormatSelected(format: String) {
        persist { settings.setWidgetDateFormat(format) }
    }

    override fun onFontScalePreview(scale: Int) {
        _state.update { it.copy(fontScale = scale) }
    }

    override fun onFontScaleCommit() {
        persist { settings.setWidgetFontScale(_state.value.fontScale) }
    }

    override fun onReset() {
        persist {
            settings.setWidgetBgColor(null)
            settings.setWidgetBgAlpha(SettingsDataStore.DEFAULT_WIDGET_BG_ALPHA)
            settings.setWidgetTextColor("auto")
            settings.setWidgetDateFormat(SettingsDataStore.DEFAULT_WIDGET_DATE_FORMAT)
            settings.setWidgetFontScale(SettingsDataStore.DEFAULT_WIDGET_FONT_SCALE)
        }
    }
}