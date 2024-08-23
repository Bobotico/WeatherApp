package com.bobot.termoapp.apiservice

import retrofit2.http.GET
import retrofit2.http.Query
import com.bobot.termoapp.data.model.WeatherResponse
import retrofit2.Response

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,precipitation,cloud_cover,wind_speed_10m,relative_humidity_2m",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_sum",
        @Query("timezone") timezone: String = "auto"
    ): Response<WeatherResponse>
}