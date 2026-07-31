package com.nimbus.weather.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AirQualityResponse(
    val current: AirQualityCurrent? = null
)

@Serializable
data class AirQualityCurrent(
    @SerialName("european_aqi") val europeanAqi: Double? = null,
    @SerialName("us_aqi") val usAqi: Double? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("pm10") val pm10: Double? = null,
    @SerialName("nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @SerialName("sulphur_dioxide") val sulphurDioxide: Double? = null,
    @SerialName("carbon_monoxide") val carbonMonoxide: Double? = null,
    @SerialName("ozone") val ozone: Double? = null
)
