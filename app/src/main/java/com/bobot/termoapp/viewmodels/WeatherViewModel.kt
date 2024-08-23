package com.bobot.termoapp.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobot.termoapp.data.model.WeatherResponse
import com.bobot.termoapp.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

// ViewModel to manage temperature and weather data
class WeatherViewModel : ViewModel() {
    private val _weatherData = MutableStateFlow<List<WeatherForecast>>(emptyList())
    val weatherData: StateFlow<List<WeatherForecast>> = _weatherData
    private val _dailyWeatherData = MutableStateFlow<List<DailyWeatherForecast>>(emptyList())
    val dailyWeatherData: StateFlow<List<DailyWeatherForecast>> = _dailyWeatherData

    fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val response: Response<WeatherResponse> = RetrofitInstance.api.getWeatherForecast(
                    latitude = lat,
                    longitude = lon
                )
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    if (weatherResponse != null) {
                        val hourlyData = weatherResponse.hourly

                        val forecasts = hourlyData.time.zip(hourlyData.temperature_2m)
                            .mapIndexed { index, (time, temperature) ->
                                WeatherForecast(
                                    date = time.substring(0, 10),
                                    time = time.substring(11, 16),
                                    temperature = "${temperature}°C",
                                    minTemp = "${hourlyData.temperature_2m.minOrNull()}°C",
                                    maxTemp = "${hourlyData.temperature_2m.maxOrNull()}°C",
                                    humidity = "${hourlyData.relative_humidity_2m.getOrNull(index) ?: "N/A"} %",
                                    windSpeed = "${hourlyData.wind_speed_10m.getOrNull(index) ?: "N/A"} km/h",
                                    cloudCover = "${hourlyData.cloud_cover.getOrNull(index) ?: "N/A"}"
                                )
                            }

                        println("API Response: ${weatherResponse}")

                        /*val dailyData = weatherResponse.daily
                        val dailyForecasts = dailyData.time.mapIndexed { index, date ->
                            DailyWeatherForecast(
                                date = date,
                                sunrise = dailyData.sunrise.getOrNull(index) ?: "N/A",
                                sunset = dailyData.sunset.getOrNull(index) ?: "N/A",
                                maxTemp = "${dailyData.temperature2mMax.getOrNull(index) ?: "N/A"}°C",
                                minTemp = "${dailyData.temperature2mMin.getOrNull(index) ?: "N/A"}°C",
                                precipitationSum = "${dailyData.precipitationSum.getOrNull(index) ?: "0.0"} mm"
                            )
                        }*/


                        _weatherData.value = forecasts
                        //_dailyWeatherData.value = dailyForecasts
                        println("Weather data set: $forecasts")
                    } else {
                        _weatherData.value = emptyList()
                        //_dailyWeatherData.value = emptyList()
                        println("WeatherResponse is null")
                    }
                } else {
                    println("API response unsuccessful: ${response.code()}")
                }
            } catch (e: HttpException) {
                println("HTTP Exception: ${e.code()} ${e.message()}")
            } catch (e: Exception) {
                println("Exception: ${e.localizedMessage}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun findClosestForecast(
        currentDateTime: LocalDateTime,
        forecasts: List<WeatherForecast>,
    ): WeatherForecast? {
        // Filter out forecasts that are in the future
        /*val validForecasts = forecasts.filter { forecast ->
            val forecastDateTime = LocalDateTime.parse(
                "${forecast.date}T${forecast.time}",
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )
            forecastDateTime <= currentDateTime // Ensure the forecast is not in the future
        }*/

        // Find the closest forecast from the filtered list
        return forecasts.minByOrNull { forecast ->
            val forecastDateTime = LocalDateTime.parse(
                "${forecast.date}T${forecast.time}",
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )
            ChronoUnit.MINUTES.between(currentDateTime, forecastDateTime).absoluteValue
        }
    }
}

data class DailyWeatherForecast(
    val date: String,
    //val sunrise: String,
    //val sunset: String,
    val minTemp: String,
    val maxTemp: String,
    //val precipitationSum: String,
)

data class WeatherForecast(
    val date: String,
    val time: String,
    val temperature: String,
    val minTemp: String,
    val maxTemp: String,
    val humidity: String,
    //val precipitation: String,
    val windSpeed: String,
    val cloudCover: String,
)