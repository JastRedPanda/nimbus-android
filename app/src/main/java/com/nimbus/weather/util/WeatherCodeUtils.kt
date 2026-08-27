package com.nimbus.weather.util

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.nimbus.weather.R

fun weatherIcon(code: Int): ImageVector {
    return WEATHER_ICONS[code] ?: Icons.Default.WbSunny
}

fun weatherDescriptionRes(code: Int): Int {
    return WEATHER_DESCRIPTIONS[code] ?: R.string.wmo_0
}

fun weatherDescription(context: Context, code: Int): String {
    return context.getString(weatherDescriptionRes(code))
}

private val WEATHER_ICONS: Map<Int, ImageVector> = mapOf(
    0 to Icons.Default.WbSunny,
    1 to Icons.Default.WbSunny,
    2 to Icons.Default.WbCloudy,
    3 to Icons.Default.WbCloudy,
    45 to Icons.Default.Cloud,
    48 to Icons.Default.Cloud,
    51 to Icons.Default.WaterDrop,
    53 to Icons.Default.WaterDrop,
    55 to Icons.Default.WaterDrop,
    56 to Icons.Default.WaterDrop,
    57 to Icons.Default.WaterDrop,
    61 to Icons.Default.WaterDrop,
    63 to Icons.Default.WaterDrop,
    65 to Icons.Default.WaterDrop,
    66 to Icons.Default.WaterDrop,
    67 to Icons.Default.WaterDrop,
    71 to Icons.Default.AcUnit,
    73 to Icons.Default.AcUnit,
    75 to Icons.Default.AcUnit,
    77 to Icons.Default.AcUnit,
    80 to Icons.Default.WaterDrop,
    81 to Icons.Default.WaterDrop,
    82 to Icons.Default.WaterDrop,
    85 to Icons.Default.AcUnit,
    86 to Icons.Default.AcUnit,
    95 to Icons.Default.Thunderstorm,
    96 to Icons.Default.Thunderstorm,
    99 to Icons.Default.Thunderstorm
)

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
