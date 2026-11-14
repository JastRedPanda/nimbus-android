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
 * Неточный будильник фонового обновления погоды — второй контур доставки
 * рядом с периодическим WorkManager.
 *
 * Зачем: на агрессивных прошивках (HyperOS/MIUI и т.п.) периодический
 * WorkManager с прибитым процессом может не срабатывать часами даже с
 * отключённой экономией батареи и автозапуском. `setAndAllowWhileIdle`
 * проходит Doze и добуживается заметно чаще; точное время не гарантируется,
 * и это осознанно — SCHEDULE_EXACT_ALARM не запрашиваем (требование
 * Google Play), поэтому предупреждения в настройках нет.
 *
 * Receiver различает два вида сигналов:
 *  - [WeatherAlarmScheduler.ACTION_REFRESH] — сам будильник: ставит
 *    немедленную задачу WorkManager (сеть в `goAsync` не тянем — не
 *    укладывались в его лимит времени) и планирует следующий выстрел
 *    (алармы одноразовые, поэтому цепочкой);
 *  - BOOT_COMPLETED / MY_PACKAGE_REPLACED / TIME_SET — только перепланирование.
 *    Сеть на каждой перезагрузке не дёргаем: свежесть доберёт WorkManager.
 */
class WeatherAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val action = intent.action
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                if (action == WeatherAlarmScheduler.ACTION_REFRESH) {
                    // Сеть — отдельной задачей WorkManager. goAsync даёт около
                    // десяти секунд: медленный запрос не успевал, процесс
                    // убивался вместе с будильником, и следующий выстрел
                    // оставался неперепланированным.
                    WeatherUpdateScheduler.enqueueImmediate(appContext)
                } else {
                    Log.d("WeatherAlarm", "system event $action, reschedule only")
                }
            } catch (e: Exception) {
                Log.w("WeatherAlarm", "refresh failed", e)
            } finally {
                try {
                    WeatherAlarmScheduler.scheduleNext(appContext)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    suspend fun scheduleNext(context: Context) {
        val interval = SettingsDataStore(context).updateIntervalHours.first()
        schedule(context, interval)
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            ?.cancel(alarmPendingIntent(context))
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WeatherAlarmReceiver::class.java).setAction(ACTION_REFRESH)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
