package com.nimbus.weather.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

fun formatDayOfWeek(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val lang = LanguageHelper.getLocaleTag()
        when (lang) {
            "ru" -> when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "Пн"
                DayOfWeek.TUESDAY -> "Вт"
                DayOfWeek.WEDNESDAY -> "Ср"
                DayOfWeek.THURSDAY -> "Чт"
                DayOfWeek.FRIDAY -> "Пт"
                DayOfWeek.SATURDAY -> "Сб"
                DayOfWeek.SUNDAY -> "Вс"
            }
            "uk" -> when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "Пн"
                DayOfWeek.TUESDAY -> "Вт"
                DayOfWeek.WEDNESDAY -> "Ср"
                DayOfWeek.THURSDAY -> "Чт"
                DayOfWeek.FRIDAY -> "Пт"
                DayOfWeek.SATURDAY -> "Сб"
                DayOfWeek.SUNDAY -> "Нд"
            }
            else -> when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "Mon"
                DayOfWeek.TUESDAY -> "Tue"
                DayOfWeek.WEDNESDAY -> "Wed"
                DayOfWeek.THURSDAY -> "Thu"
                DayOfWeek.FRIDAY -> "Fri"
                DayOfWeek.SATURDAY -> "Sat"
                DayOfWeek.SUNDAY -> "Sun"
            }
        }
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
