package com.nimbus.weather.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nimbus.weather.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val settings = SettingsDataStore(context)
                val interval = settings.updateIntervalHours.first()
                WeatherUpdateScheduler.schedule(context, interval)

                if (settings.keepAliveEnabled.first()) {
                    runCatching { KeepAliveService.start(context) }
                }

                WeatherUpdateScheduler.enqueueImmediate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
