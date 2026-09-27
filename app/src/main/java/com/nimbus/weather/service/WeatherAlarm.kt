package com.nimbus.weather.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.nimbus.weather.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Точный будильник фонового обновления погоды — второй контур доставки
 * рядом с периодическим WorkManager.
 *
 * Зачем: на агрессивных прошивках (HyperOS/MIUI и т.п.) периодический
 * WorkManager с прибитым процессом может не срабатывать часами даже с
 * отключённой экономией батареи и автозапуском. Точный будильник
 * (`setExactAndAllowWhileIdle`, проходит Doze) добуживается заметно чаще.
 * При срабатывании качает погоду через общий [performWeatherRefresh]
 * и планирует следующий выстрел — цепочкой, т.к. точные алармы одноразовые.
 *
 * Нужно разрешение SCHEDULE_EXACT_ALARM (Android 12+): без него молча
 * откатываемся на неточный режим — тогда контур равен WorkManager
 * и погоду тянет только он.
 */
class WeatherAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val appContext = context.applicationContext
                performWeatherRefresh(appContext)
            } catch (e: Exception) {
                Log.w("WeatherAlarm", "refresh failed", e)
            } finally {
                try {
                    WeatherAlarmScheduler.scheduleNext(context.applicationContext)
                } catch (e: Exception) {
                    Log.w("WeatherAlarm", "reschedule failed", e)
                }
                pendingResult.finish()
            }
        }
    }
}

object WeatherAlarmScheduler {

    const val ACTION_REFRESH = "com.nimbus.weather.action.WEATHER_ALARM"

    internal fun nextTriggerMillis(nowMillis: Long, intervalHours: Int): Long =
        nowMillis + intervalHours.coerceAtLeast(1) * 3_600_000L

    fun schedule(context: Context, intervalHours: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextTriggerMillis(System.currentTimeMillis(), intervalHours)
        val pendingIntent = alarmPendingIntent(context)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    suspend fun scheduleNext(context: Context) {
        val interval = SettingsDataStore(context).updateIntervalHours.first()
        schedule(context, interval)
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            ?.cancel(alarmPendingIntent(context))
    }

    /** true, когда точные будильники недоступны и нужен запрос у пользователя. */
    fun needsExactPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return !alarmManager.canScheduleExactAlarms()
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WeatherAlarmReceiver::class.java).setAction(ACTION_REFRESH)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
