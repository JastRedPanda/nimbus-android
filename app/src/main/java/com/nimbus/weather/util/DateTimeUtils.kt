package com.nimbus.weather.util

import com.nimbus.weather.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.TimeZone

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dayMonthFormatter = DateTimeFormatter.ofPattern("d/M")

private val validTimeZoneIds: Set<String> = TimeZone.getAvailableIDs().toHashSet()

fun isValidTimeZoneId(id: String): Boolean = id in validTimeZoneIds

fun formatTime(isoString: String): String {
    return try {
        val timeStr = isoString.substringAfter("T").take(5)
        LocalTime.parse(timeStr).format(timeFormatter)
    } catch (_: Exception) {
        isoString.takeLast(5)
    }
}

/**
 * Полное название дня недели на языке приложения («Четверг», «Thursday»).
 * Через локаль — работает для всех языков, включая чешский.
 * Первая буква заглавная, чтобы было единообразно со словом «Сегодня».
 */
fun formatDayOfWeek(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val locale = Locale.forLanguageTag(LanguageHelper.getLocaleTag())
        date.dayOfWeek
            .getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    } catch (_: Exception) {
        ""
    }
}

/**
 * Короткое название дня недели для узких колонок недельного блока
 * («Ср», «Wed», «Út»). Точка после сокращения срезается — в русском
 * и чешском TextStyle.SHORT её добавляет.
 */
fun formatDayOfWeekShort(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val locale = Locale.forLanguageTag(LanguageHelper.getLocaleTag())
        date.dayOfWeek
            .getDisplayName(TextStyle.SHORT, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            .trimEnd('.')
    } catch (_: Exception) {
        ""
    }
}

/** Дата для колонки недельного блока: «29/4» без ведущих нулей. */
fun formatDayMonth(dateStr: String): String {
    return try {
        LocalDate.parse(dateStr).format(dayMonthFormatter)
    } catch (_: Exception) {
        ""
    }
}

fun formatHour(isoString: String): String {
    return try {
        isoString.substringAfter("T").take(5)
    } catch (_: Exception) {
        isoString.takeLast(5)
    }
}

fun isToday(dateStr: String): Boolean {
    return try {
        LocalDate.parse(dateStr) == LocalDate.now()
    } catch (_: Exception) {
        false
    }
}

/**
 * День ли сейчас: текущее время между восходом и закатом.
 * Все три параметра — ISO-строки Open-Meteo («2026-09-09T08:35»),
 * поэтому достаточно лексикографического сравнения.
 */
fun isDayNow(nowIso: String, sunriseIso: String, sunsetIso: String): Boolean {
    return try {
        if (nowIso.isBlank() || sunriseIso.isBlank() || sunsetIso.isBlank()) return true
        nowIso >= sunriseIso && nowIso <= sunsetIso
    } catch (_: Exception) {
        true
    }
}

/**
 * То же, но «сейчас» — системное время, а из восхода/заката берётся
 * только время суток. Небо должно отражать момент просмотра, а не момент
 * замера погоды: при несвежих данных (ночь не обновилась) иначе утром
 * показывается ночь.
 */
fun isDayNowByTime(now: LocalTime, sunriseIso: String, sunsetIso: String): Boolean {
    return try {
        val sunrise = LocalTime.parse(sunriseIso.substringAfter("T").take(5))
        val sunset = LocalTime.parse(sunsetIso.substringAfter("T").take(5))
        !now.isBefore(sunrise) && !now.isAfter(sunset)
    } catch (_: Exception) {
        true
    }
}

/**
 * Человекопонятный возраст кэша («данные 15 мин назад», «данные 3 ч назад» и т. д.).
 */
fun formatRelativeCacheAge(millis: Long, context: android.content.Context): String {
    if (millis <= 0L) return ""
    val diffMinutes = java.time.Duration.ofMillis(System.currentTimeMillis() - millis).toMinutes()
    if (diffMinutes < 1) return context.getString(R.string.cache_age_just_now)
    if (diffMinutes < 60) return context.getString(R.string.cache_age_minutes, diffMinutes)
    val diffHours = diffMinutes / 60
    if (diffHours < 24) return context.getString(R.string.cache_age_hours, diffHours)
    val diffDays = diffHours / 24
    return context.getString(R.string.cache_age_days, diffDays)
}

/** Короткие дата+время для метки «обновлено»: 28.09.26, 09:00. */
fun formatUpdateTime(millis: Long): String {
    return try {
        java.text.DateFormat
            .getDateTimeInstance(
                java.text.DateFormat.SHORT,
                java.text.DateFormat.SHORT
            )
            .format(java.util.Date(millis))
    } catch (_: Exception) {
        ""
    }
}
