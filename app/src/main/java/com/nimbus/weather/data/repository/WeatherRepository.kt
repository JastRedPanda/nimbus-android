package com.nimbus.weather.data.repository

import com.nimbus.weather.data.api.ApiClient
import com.nimbus.weather.data.model.GeocodingResult
import com.nimbus.weather.data.model.WeatherResponse

class WeatherRepository {

    private val weatherApi = ApiClient.weatherApi
    private val geocodingApi = ApiClient.geocodingApi

    suspend fun getWeather(lat: Double, lon: Double): WeatherResponse {
        return weatherApi.getForecast(latitude = lat, longitude = lon)
    }

    suspend fun searchCities(query: String): List<GeocodingResult> {
        return geocodingApi.searchCities(name = query).results.orEmpty()
    }
}
