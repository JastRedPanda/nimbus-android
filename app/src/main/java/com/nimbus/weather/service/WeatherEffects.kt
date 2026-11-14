package com.nimbus.weather.service

import android.content.Context
import com.nimbus.weather.data.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Побочные эффекты загрузки погоды: виджет и уведомление. Вынесены за
 * интерфейс, чтобы ViewModel можно было тестировать без AppWidgetManager,
 * RemoteViews и NotificationManager.
 */
interface WeatherEffects {
    suspend fun onWeatherLoaded(response: WeatherResponse, notificationsEnabled: Boolean)

    /** Изменился список избранного — виджет мог переехать на другой город. */
    suspend fun onFavouriteChanged()

    /** Изменилось оформление виджета — перерисовать без сетевого запроса. */
    suspend fun onWidgetSettingsChanged()
}

class AndroidWeatherEffects(private val context: Context) : WeatherEffects {

    override suspend fun onWeatherLoaded(response: WeatherResponse, notificationsEnabled: Boolean) {
        // Файловый кэш, RemoteViews и binder-вызовы AppWidgetManager на главном
        // потоке давали заметный провал в кадрах сразу после загрузки погоды.
        withContext(Dispatchers.Default) {
            WidgetUpdateManager.updateAllWidgets(context, response)
            if (notificationsEnabled) {
                NotificationHelper.showWeatherNotification(context, response)
            }
        }
    }

    override suspend fun onFavouriteChanged() {
        WidgetUpdateManager.updateFromTargetCity(context)
    }

    override suspend fun onWidgetSettingsChanged() {
        WidgetUpdateManager.refreshAllWidgets(context)
    }
}
