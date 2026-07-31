package com.nimbus.weather.util

enum class TemperatureUnit {
    CELSIUS, FAHRENHEIT
}

fun Double.toCelsiusOrFahrenheit(unit: TemperatureUnit): Double {
    return if (unit == TemperatureUnit.FAHRENHEIT) this * 9.0 / 5.0 + 32.0 else this
}

fun TemperatureUnit.displayString(): String {
    return when (this) {
        TemperatureUnit.CELSIUS -> "°C"
        TemperatureUnit.FAHRENHEIT -> "°F"
    }
}
