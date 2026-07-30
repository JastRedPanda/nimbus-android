package com.nimbus.weather.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
        val locale = Locale.getDefault()
        if (locale.language == "ru") {
            when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "Пн"
                DayOfWeek.TUESDAY -> "Вт"
                DayOfWeek.WEDNESDAY -> "Ср"
                DayOfWeek.THURSDAY -> "Чт"
                DayOfWeek.FRIDAY -> "Пт"
                DayOfWeek.SATURDAY -> "Сб"
                DayOfWeek.SUNDAY -> "Вс"
            }
        } else {
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        }
    } catch (_: Exception) {
        ""
    }
}

fun isToday(dateStr: String): Boolean {
    return try {
        LocalDate.parse(dateStr) == LocalDate.now()
    } catch (_: Exception) {
        false
    }
}
