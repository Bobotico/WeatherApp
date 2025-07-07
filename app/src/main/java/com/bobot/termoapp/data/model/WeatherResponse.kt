package com.bobot.termoapp.data.model

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val hourly: HourlyData,
    val daily: DailyData
)

data class HourlyData(
    val time: List<String>,
    val temperature2m: List<Double>,
    val relativeHumidity2m: List<Int>,
    val precipitationProbability: List<Int>,
    val snowfall: List<Int>,
    val windSpeed10m: List<Float>,
    val cloudCover: List<Int>
)

data class DailyData(
    val time: List<String>,
    val temperature2mMax: List<Double>,
    val temperature2mMin: List<Double>
)