package com.nimbus.weather

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.service.AndroidWeatherEffects
import com.nimbus.weather.service.WeatherEffects
import com.nimbus.weather.ui.home.HomeViewModel
import com.nimbus.weather.ui.location.LocationSearchViewModel
import com.nimbus.weather.ui.settings.SettingsViewModel
import com.nimbus.weather.ui.widgetcustomize.WidgetCustomizeViewModel

class NimbusApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/**
 * Ручной DI-контейнер: единственное место, где создаются репозиторий,
 * DataStore и побочные эффекты. ViewModel получают их через фабрику,
 * поэтому в тестах подменяются без Android-инфраструктуры.
 */
class AppContainer(val application: Application) {

    val settings: SettingsDataStore by lazy { SettingsDataStore(application) }

    val weatherRepository: WeatherRepository by lazy { WeatherRepository(application) }

    val weatherEffects: WeatherEffects by lazy { AndroidWeatherEffects(application) }

    val viewModelFactory: ViewModelProvider.Factory by lazy { NimbusViewModelFactory(this) }
}

class NimbusViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(container.settings, container.weatherRepository, container.weatherEffects)

        modelClass.isAssignableFrom(LocationSearchViewModel::class.java) ->
            LocationSearchViewModel(container.settings, container.weatherRepository, container.weatherEffects)

        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(
                container.application,
                container.settings,
                container.weatherRepository,
                container.weatherEffects
            )

        modelClass.isAssignableFrom(WidgetCustomizeViewModel::class.java) ->
            WidgetCustomizeViewModel(container.settings, container.weatherEffects)

        else -> error("Unknown ViewModel class: ${modelClass.name}")
    } as T
}
