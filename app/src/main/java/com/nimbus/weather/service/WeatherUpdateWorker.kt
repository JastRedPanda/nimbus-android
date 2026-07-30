package com.nimbus.weather.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.repository.WeatherRepository
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

            val loc = settings.getLocationSnapshot()
            val response = repository.getWeather(loc.lat, loc.lon)

            WidgetUpdateManager.updateAllWidgets(applicationContext, response)

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object WeatherUpdateScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(
            Constants.UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.WEATHER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
