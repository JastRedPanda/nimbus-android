package com.nimbus.weather.util

private val RU_ROSE = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")

private val UK_ROSE = listOf("Пн", "ПнСх", "Сх", "ПдСх", "Пд", "ПдЗх", "Зх", "ПнЗх")

private val EN_ROSE = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

private val CS_ROSE = listOf("S", "SV", "V", "JV", "J", "JZ", "Z", "SZ")

fun windDirection(degrees: Double): String {
    val index = (((degrees + 22.5) % 360) / 45).toInt()
    val rose = when (LanguageHelper.getLocaleTag()) {
        "ru" -> RU_ROSE
        "uk" -> UK_ROSE
        "cs" -> CS_ROSE
        else -> EN_ROSE
    }
    return rose[index]
}