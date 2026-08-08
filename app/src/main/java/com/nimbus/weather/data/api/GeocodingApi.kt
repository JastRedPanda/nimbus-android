package com.nimbus.weather.data.api

import com.nimbus.weather.data.model.GeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {

    @GET("v1/search")
    suspend fun searchCities(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String,
        @Query("format") format: String = "json"
    ): GeocodingResponse
}
