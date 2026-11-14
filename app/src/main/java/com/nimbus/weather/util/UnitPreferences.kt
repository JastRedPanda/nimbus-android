package com.nimbus.weather.util

/**
 * Единицы давления и ветра. Отдельного режима «авто» нет: пока пользователь
 * не выбрал явно, в настройках ничего не лежит и единица берётся из языка
 * ([defaultPressureUnit], [defaultWindSpeedUnit]). Первый выбор фиксируется
 * в DataStore и дальше от языка не зависит.
 */
enum class PressureUnit { HPA, MMHG }

enum class WindSpeedUnit { MS, KMH }

/** В русской и украинской традиции — мм рт. ст., в остальных локалях — гПа. */
fun defaultPressureUnit(language: String): PressureUnit =
    if (language == "ru" || language == "uk") PressureUnit.MMHG else PressureUnit.HPA

/** м/с — для RU/UK, км/ч — для EN/CS. */
fun defaultWindSpeedUnit(language: String): WindSpeedUnit =
    if (language == "ru" || language == "uk") WindSpeedUnit.MS else WindSpeedUnit.KMH

/** Open-Meteo отдаёт давление в гПа. 1 мм рт. ст. = 1.33322387415 гПа. */
fun Double.toPressure(unit: PressureUnit): Double =
    if (unit == PressureUnit.MMHG) this * 0.750061683 else this

/** Open-Meteo отдаёт скорость ветра в м/с. */
fun Double.toWindSpeed(unit: WindSpeedUnit): Double =
    if (unit == WindSpeedUnit.KMH) this * 3.6 else this
