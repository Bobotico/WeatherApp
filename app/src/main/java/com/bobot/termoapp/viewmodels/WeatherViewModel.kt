package com.bobot.termoapp.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobot.termoapp.R
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
    private val _externalWeather = MutableStateFlow<List<WeatherParameter>>(emptyList())
    val externalWeather: StateFlow<List<WeatherParameter>> = _externalWeather


    private val _weatherData = MutableStateFlow<List<WeatherForecast>>(emptyList())
    val weatherData: StateFlow<List<WeatherForecast>> = _weatherData

    fun fetchWeather(lat: Double, lon: Double) {
        /*viewModelScope.launch {
            try {
                val response: Response<WeatherResponse> = RetrofitInstance.api.getWeatherForecast(
                    latitude = lat,
                    longitude = lon
                )
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    if (weatherResponse != null) {
                        println("ResponseBody == $weatherResponse")
                        val weatherParameters = listOf(
                            WeatherParameter(
                                title = "Temperature",
                                value = "${weatherResponse.hourly.temperature_2m.firstOrNull()}°C",
                                icon = R.drawable.ic_temperature,
                                description = "Temperature indicates how hot or cold the atmosphere is. It is measured in degrees Celsius (°C).",
                                dynamicText = " "
                            ),
                            /*${weatherResponse.hourly.relative_humidity_2m.firstOrNull()}*/
                            WeatherParameter(
                                title = "Humidity",
                                value = "null %",
                                icon = R.drawable.ic_humidity,
                                description = "Humidity is the amount of water vapor in the air. It is expressed as a percentage (%).",
                                dynamicText = " "
                            ),
                            /*${weatherResponse.hourly.wind_speed_10m.firstOrNull()}*/
                            WeatherParameter(
                                title = "Wind Speed",
                                value = "null km/h",
                                icon = R.drawable.ic_wind_speed,
                                description = "Wind speed is the speed at which air is moving in the atmosphere. It is measured in kilometers per hour (km/h).",
                                dynamicText = " "
                            ),
                            /*${weatherResponse.hourly.pressure_msl.firstOrNull()}*/
                            WeatherParameter(
                                title = "Pressure",
                                value = "null hPa",
                                icon = R.drawable.ic_pressure,
                                description = "Atmospheric pressure is the force exerted by the weight of the air above. It is measured in hectopascals (hPa).",
                                dynamicText = " "
                            )
                        )
                        _externalWeather.value = weatherParameters
                    } else {
                        println("ResponseBody is null")
                        _externalWeather.value = emptyList()
                    }
                }
            } catch (e: HttpException) {
                println("HTTP Exception: ${e.code()} ${e.message()}")
            } catch (e: Exception) {
                println("Exception: ${e.localizedMessage}")
            }
        }*/
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
                                    /*windSpeed = "${hourlyData.wind_speed_10m.getOrNull(index) ?: "N/A"} %",
                                    pressure = "${hourlyData.pressure_msl.getOrNull(index) ?: "N/A"} %",*/
                                )
                            }
                        println("API Response: ${weatherResponse}")
                        println("Wind Speed Data: ${hourlyData.wind_speed_10m}")
                        println("Pressure Data: ${hourlyData.pressure_msl}")

                        _weatherData.value = forecasts
                        println("Weather data set: $forecasts")
                    } else {
                        _weatherData.value = emptyList()
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

    // Function to generate dynamic text based on weather parameter type and value
    /*fun generateDynamicText(weatherParameter: WeatherParameter): String {
        return when (weatherParameter.title) {
            "Temperature" -> {
                val temperatureValue = weatherParameter.value.replace("°C", "").toFloatOrNull()
                if (temperatureValue != null) {
                    when {
                        temperatureValue < 0 -> "It's freezing! Stay warm."
                        temperatureValue in 0f..15f -> "It's quite cool, dress warmly."
                        temperatureValue in 15f..25f -> "Comfortable temperature."
                        temperatureValue > 25f -> "It's getting hot! Stay hydrated."
                        else -> "Temperature is moderate."
                    }
                } else {
                    "Temperature data unavailable."
                }
            }
            "Humidity" -> {
                val humidityValue = weatherParameter.value.replace("%", "").toIntOrNull()
                if (humidityValue != null) {
                    when {
                        humidityValue < 30 -> "The air is quite dry."
                        humidityValue in 30..60 -> "Humidity is comfortable."
                        humidityValue > 60 -> "It's quite humid."
                        else -> "Humidity data unavailable."
                    }
                } else {
                    "Humidity data unavailable."
                }
            }
            "Wind Speed" -> {
                val windSpeedValue = weatherParameter.value.replace("km/h", "").toFloatOrNull()
                if (windSpeedValue != null) {
                    when {
                        windSpeedValue < 10 -> "Light breeze."
                        windSpeedValue in 10f..20f -> "Moderate wind."
                        windSpeedValue > 20f -> "Strong wind, be cautious."
                        else -> "Wind speed data unavailable."
                    }
                } else {
                    "Wind speed data unavailable."
                }
            }
            "Pressure" -> {
                val pressureValue = weatherParameter.value.replace("hPa", "").toFloatOrNull()
                if (pressureValue != null) {
                    when {
                        pressureValue < 1000 -> "Low pressure, might indicate stormy weather."
                        pressureValue in 1000f..1020f -> "Normal atmospheric pressure."
                        pressureValue > 1020 -> "High pressure, generally stable weather."
                        else -> "Pressure data unavailable."
                    }
                } else {
                    "Pressure data unavailable."
                }
            }
            else -> "No additional information."
        }
    }*/

    @RequiresApi(Build.VERSION_CODES.O)
    fun findClosestForecast(currentDateTime: LocalDateTime, forecasts: List<WeatherForecast>): WeatherForecast? {
        return forecasts.minByOrNull { forecast ->
            val forecastDateTime = LocalDateTime.parse(
                "${forecast.date}T${forecast.time}",
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )
            ChronoUnit.MINUTES.between(currentDateTime, forecastDateTime).absoluteValue
        }
    }
}

data class WeatherParameter(
    val title: String,
    val value: String,
    val icon: Int, // Drawable resource ID for the icon (you can replace this with a painter or similar if needed)
    val description: String, // Explanation of the parameter
    val dynamicText: String // Additional dynamic text
)

data class WeatherForecast(
    val date: String,
    val time: String,
    val temperature: String,
    val minTemp: String,
    val maxTemp: String,
    val humidity: String,
)