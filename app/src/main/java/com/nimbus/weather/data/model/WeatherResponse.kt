package com.nimbus.weather.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val current: CurrentWeather? = null,
    val daily: DailyWeather? = null
)

@Serializable
data class CurrentWeather(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("precipitation") val precipitation: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("surface_pressure") val pressure: Double,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("wind_direction_10m") val windDirection: Double,
    @SerialName("wind_gusts_10m") val windGusts: Double
)

@Serializable
data class DailyWeather(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerialName("apparent_temperature_max") val apparentTemperatureMax: List<Double>,
    @SerialName("apparent_temperature_min") val apparentTemperatureMin: List<Double>,
    @SerialName("sunrise") val sunrise: List<String>,
    @SerialName("sunset") val sunset: List<String>,
    @SerialName("precipitation_sum") val precipitationSum: List<Double>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>,
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double>,
    @SerialName("wind_gusts_10m_max") val windGustsMax: List<Double>,
    @SerialName("wind_direction_10m_dominant") val windDirectionDominant: List<Double>
)
