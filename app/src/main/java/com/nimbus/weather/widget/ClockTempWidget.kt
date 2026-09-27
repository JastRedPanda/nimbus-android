package com.nimbus.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WeatherAlarmScheduler
import com.nimbus.weather.service.WeatherUpdateScheduler
import com.nimbus.weather.service.WidgetUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Классический виджет на RemoteViews. Время и дату отрисовывает сам лаунчер
 * через TextClock — тикают каждую минуту даже с прибитым процессом, поэтому
 * никакой поминутный будильник из приложения не нужен. Провайдер и
 * [WidgetUpdateManager] обновляют только температуру, фон, шрифты и форматы:
 * при добавлении/ресайзе, при открытии приложения и по фоновому воркеру.
 */
class ClockTempWidgetReceiver : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        scope.launch {
            try {
                WidgetUpdateManager.renderWidgets(context, appWidgetIds)
                WidgetUpdateManager.updateFromTargetCity(context)
                val interval = SettingsDataStore(context).updateIntervalHours.first()
                WeatherAlarmScheduler.schedule(context, interval)
            } finally {
                pendingResult.finish()
            }
        }
        WeatherUpdateScheduler.enqueueImmediate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        scope.launch {
            try {
                WidgetUpdateManager.renderWidgets(context, intArrayOf(appWidgetId))
            } finally {
                pendingResult.finish()
            }
        }
    }
}
