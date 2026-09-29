package com.nimbus.weather.util

import android.content.Context
import androidx.annotation.DrawableRes
import com.nimbus.weather.R

@DrawableRes
fun weatherIcon(code: Int, isDay: Boolean = true): Int {
    return WEATHER_ICONS[code to isDay]
        ?: WEATHER_ICONS[code to true]
        ?: R.drawable.ic_weather_clear_day
}

fun weatherDescriptionRes(code: Int): Int {
    return WEATHER_DESCRIPTIONS[code] ?: R.string.wmo_0
}

fun weatherDescription(context: Context, code: Int): String {
    return context.getString(weatherDescriptionRes(code))
}

private val WEATHER_ICONS: Map<Pair<Int, Boolean>, Int> = buildMap {
    fun dayNight(code: Int, day: Int, night: Int) {
        put(code to true, day)
        put(code to false, night)
    }
    dayNight(0, R.drawable.ic_weather_clear_day, R.drawable.ic_weather_clear_night)
    dayNight(1, R.drawable.ic_weather_partly_day, R.drawable.ic_weather_partly_night)
    dayNight(2, R.drawable.ic_weather_partly_day, R.drawable.ic_weather_partly_night)
    for (code in listOf(3)) {
        put(code to true, R.drawable.ic_weather_overcast)
        put(code to false, R.drawable.ic_weather_overcast)
    }
    for (code in listOf(45, 48)) {
        put(code to true, R.drawable.ic_weather_fog)
        put(code to false, R.drawable.ic_weather_fog)
    }
    for (code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82)) {
        put(code to true, R.drawable.ic_weather_rain)
        put(code to false, R.drawable.ic_weather_rain)
    }
    for (code in listOf(56, 57, 66, 67)) {
        put(code to true, R.drawable.ic_weather_sleet)
        put(code to false, R.drawable.ic_weather_sleet)
    }
    for (code in listOf(71, 73, 75, 77, 85, 86)) {
        put(code to true, R.drawable.ic_weather_snow)
        put(code to false, R.drawable.ic_weather_snow)
    }
    for (code in listOf(95, 96, 99)) {
        put(code to true, R.drawable.ic_weather_thunderstorm)
        put(code to false, R.drawable.ic_weather_thunderstorm)
    }
}

private val WEATHER_DESCRIPTIONS: Map<Int, Int> = mapOf(
    0 to R.string.wmo_0,
    1 to R.string.wmo_1,
    2 to R.string.wmo_2,
    3 to R.string.wmo_3,
    45 to R.string.wmo_45,
    48 to R.string.wmo_48,
    51 to R.string.wmo_51,
    53 to R.string.wmo_53,
    55 to R.string.wmo_55,
    56 to R.string.wmo_56,
    57 to R.string.wmo_57,
    61 to R.string.wmo_61,
    63 to R.string.wmo_63,
    65 to R.string.wmo_65,
    66 to R.string.wmo_66,
    67 to R.string.wmo_67,
    71 to R.string.wmo_71,
    73 to R.string.wmo_73,
    75 to R.string.wmo_75,
    77 to R.string.wmo_77,
    80 to R.string.wmo_80,
    81 to R.string.wmo_81,
    82 to R.string.wmo_82,
    85 to R.string.wmo_85,
    86 to R.string.wmo_86,
    95 to R.string.wmo_95,
    96 to R.string.wmo_96,
    99 to R.string.wmo_99
)
