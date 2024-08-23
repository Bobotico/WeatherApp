package com.bobot.termoapp.data.model

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val hourly: HourlyData,
    val daily: DailyData
)

data class HourlyData(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val relative_humidity_2m: List<Int>,
    val wind_speed_10m: List<Float>,
    val cloud_cover: List<Int>
)

data class DailyData(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>

)