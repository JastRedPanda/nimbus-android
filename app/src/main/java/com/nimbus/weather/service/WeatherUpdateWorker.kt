package com.nimbus.weather.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.repository.WeatherRepository
import kotlinx.coroutines.flow.first
import com.nimbus.weather.util.Constants
import java.util.concurrent.TimeUnit

class WeatherUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = SettingsDataStore(applicationContext)
            val repository = WeatherRepository()
            repository.setTtlHours(SettingsDataStore.DEFAULT_UPDATE_INTERVAL_HOURS * 2)

            val loc = settings.getLocationSnapshot()
            val ctx = applicationContext
            val response = repository.getWeather(loc.lat, loc.lon, ctx)

            WidgetUpdateManager.updateAllWidgets(applicationContext, response)

            if (settings.notificationsEnabled.first()) {
                NotificationHelper.showWeatherNotification(applicationContext, response)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object WeatherUpdateScheduler {

    private fun buildRequest(intervalHours: Int) =
        PeriodicWorkRequestBuilder<WeatherUpdateWorker>(intervalHours.toLong(), TimeUnit.HOURS).build()

    fun schedule(context: Context, intervalHours: Int = 2) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.WEATHER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            buildRequest(intervalHours)
        )
    }

    fun reschedule(context: Context, intervalHours: Int) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.WEATHER_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            buildRequest(intervalHours)
        )
    }
}
