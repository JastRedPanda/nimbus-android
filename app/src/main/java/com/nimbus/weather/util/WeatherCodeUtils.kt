package com.nimbus.weather.util

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Foggy
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.nimbus.weather.R

fun weatherIcon(code: Int): ImageVector {
    return when (code) {
        0, 1 -> Icons.Default.WbSunny
        2 -> Icons.Default.WbCloudy
        3 -> Icons.Default.WbCloudy
        45, 48 -> Icons.Default.Foggy
        51, 53, 55, 56, 57 -> Icons.Default.WaterDrop
        61, 63, 65, 66, 67 -> Icons.Default.WaterDrop
        71, 73, 75, 77 -> Icons.Default.AcUnit
        80, 81, 82 -> Icons.Default.WaterDrop
        85, 86 -> Icons.Default.AcUnit
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.WbSunny
    }
}

fun weatherDescriptionRes(code: Int): Int {
    return when (code) {
        0 -> R.string.wmo_0
        1 -> R.string.wmo_1
        2 -> R.string.wmo_2
        3 -> R.string.wmo_3
        45 -> R.string.wmo_45
        48 -> R.string.wmo_48
        51 -> R.string.wmo_51
        53 -> R.string.wmo_53
        55 -> R.string.wmo_55
        56 -> R.string.wmo_56
        57 -> R.string.wmo_57
        61 -> R.string.wmo_61
        63 -> R.string.wmo_63
        65 -> R.string.wmo_65
        66 -> R.string.wmo_66
        67 -> R.string.wmo_67
        71 -> R.string.wmo_71
        73 -> R.string.wmo_73
        75 -> R.string.wmo_75
        77 -> R.string.wmo_77
        80 -> R.string.wmo_80
        81 -> R.string.wmo_81
        82 -> R.string.wmo_82
        85 -> R.string.wmo_85
        86 -> R.string.wmo_86
        95 -> R.string.wmo_95
        96 -> R.string.wmo_96
        99 -> R.string.wmo_99
        else -> R.string.wmo_0
    }
}

fun weatherDescription(context: Context, code: Int): String {
    return context.getString(weatherDescriptionRes(code))
}
