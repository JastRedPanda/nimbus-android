package com.nimbus.weather.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.TimeZone

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
