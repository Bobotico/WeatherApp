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


/**
 * ViewModel for managing temperature and weather data.
 *
 * This ViewModel is responsible for fetching weather forecasts from the Open-Meteo API,
 * processing the raw API response, and exposing formatted weather data to the UI via StateFlows.
 * It also includes utility functions for finding specific forecasts.
 */
class WeatherViewModel : ViewModel() {

    /**
     * A [MutableStateFlow] holding a list of [WeatherForecast] objects.
     * This represents the hourly weather forecast data that can be observed by the UI.
     * It's initialized as an empty list.
     */
    private val _weatherData = MutableStateFlow<List<WeatherForecast>>(emptyList())

    /**
     * An immutable [StateFlow] exposing the hourly weather data to the UI.
     * UI components should observe this property to react to changes in weather forecasts.
     */
    val weatherData: StateFlow<List<WeatherForecast>> = _weatherData

    /**
     * A [MutableStateFlow] holding a list of [DailyWeatherForecast] objects.
     * This is intended to hold daily weather summary data.
     * It's currently initialized as an empty list and its population logic is commented out.
     */
    private val _dailyWeatherData = MutableStateFlow<List<DailyWeatherForecast>>(emptyList())

    /**
     * An immutable [StateFlow] exposing the daily weather data to the UI.
     * UI components can observe this for daily weather summaries.
     */
    val dailyWeatherData: StateFlow<List<DailyWeatherForecast>> = _dailyWeatherData

    /**
     * Fetches weather forecast data from the Open-Meteo API based on the given latitude and longitude.
     *
     * This function launches a coroutine in the [viewModelScope] to perform the network request
     * asynchronously. It handles successful responses by parsing the data and updating the
     * [_weatherData] StateFlow. It also includes basic error handling for HTTP exceptions
     * and other general exceptions.
     *
     * @param lat The latitude for which to fetch weather data.
     * @param lon The longitude for which to fetch weather data.
     */
    fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                // Make the API call to get weather forecast
                val response: Response<WeatherResponse> = RetrofitInstance.api.getWeatherForecast(
                    latitude = lat,
                    longitude = lon
                )

                // Check if the API response was successful (HTTP 2xx status code)
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    // Process the response body if it's not null
                    if (weatherResponse != null) {
                        val hourlyData = weatherResponse.hourly

                        // Map the hourly raw data from the API response into a list of WeatherForecast objects.
                        // This combines time, temperature, and other hourly parameters.
                        val forecasts = hourlyData.time.zip(hourlyData.temperature_2m)
                            .mapIndexed { index, (time, temperature) ->
                                WeatherForecast(
                                    date = time.substring(0, 10), // Extract date part (YYYY-MM-DD)
                                    time = time.substring(11, 16), // Extract time part (HH:MM)
                                    temperature = "${temperature}°C", // Format temperature with unit
                                    minTemp = "${hourlyData.temperature_2m.minOrNull()}°C", // Get overall min temp
                                    maxTemp = "${hourlyData.temperature_2m.maxOrNull()}°C", // Get overall max temp
                                    humidity = "${hourlyData.relative_humidity_2m.getOrNull(index) ?: "N/A"} %", // Get humidity or "N/A"
                                    precipitationProbability = hourlyData.precipitation_probability.getOrNull(index) ?: 0, // Get precipitation probability or 0
                                    snowfall = hourlyData.snowfall.getOrNull(index) ?: 0, // Get snowfall or 0
                                    windSpeed = "${hourlyData.wind_speed_10m.getOrNull(index) ?: "N/A"} km/h", // Get wind speed or "N/A"
                                    cloudCover = "${hourlyData.cloud_cover.getOrNull(index) ?: "N/A"}" // Get cloud cover or "N/A"
                                )
                            }

                        // Print the full API response for debugging purposes
                        println("API Response: ${weatherResponse}")

                        /*
                        // This section is commented out, but it was intended to process daily weather data.
                        // It would map daily raw data into a list of DailyWeatherForecast objects.
                        val dailyData = weatherResponse.daily
                        val dailyForecasts = dailyData.time.mapIndexed { index, date ->
                            DailyWeatherForecast(
                                date = date,
                                sunrise = dailyData.sunrise.getOrNull(index) ?: "N/A",
                                sunset = dailyData.sunset.getOrNull(index) ?: "N/A",
                                maxTemp = "${dailyData.temperature2mMax.getOrNull(index) ?: "N/A"}°C",
                                minTemp = "${dailyData.temperature2mMin.getOrNull(index) ?: "N/A"}°C",
                                precipitationSum = "${dailyData.precipitationSum.getOrNull(index) ?: "0.0"} mm"
                            )
                        }
                        */

                        // Update the _weatherData StateFlow with the new hourly forecasts
                        _weatherData.value = forecasts
                        // _dailyWeatherData.value = dailyForecasts // This line is commented out
                        println("Weather data set: $forecasts")
                    } else {
                        // If the response body is null, clear the weather data and log a message
                        _weatherData.value = emptyList()
                        // _dailyWeatherData.value = emptyList() // This line is commented out
                        println("WeatherResponse is null")
                    }
                } else {
                    // If the API response was not successful, log the HTTP status code
                    println("API response unsuccessful: ${response.code()}")
                }
            } catch (e: HttpException) {
                // Catch and log HTTP-specific exceptions (e.g., 404, 500)
                println("HTTP Exception: ${e.code()} ${e.message()}")
            } catch (e: Exception) {
                // Catch and log any other general exceptions that might occur during the process
                println("Exception: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Finds the [WeatherForecast] that is closest in time to the given [currentDateTime].
     *
     * This function iterates through a list of forecasts and calculates the absolute
     * difference in minutes between the current time and each forecast's time.
     * It then returns the forecast with the minimum time difference.
     *
     * @param currentDateTime The [LocalDateTime] representing the current time.
     * @param forecasts A list of [WeatherForecast] objects to search through.
     * @return The [WeatherForecast] closest to the [currentDateTime], or null if the list is empty.
     */
    @RequiresApi(Build.VERSION_CODES.O) // Requires Android API level 26 (Oreo) for LocalDateTime
    fun findClosestForecast(
        currentDateTime: LocalDateTime,
        forecasts: List<WeatherForecast>,
    ): WeatherForecast? {
        /*
        // This commented-out section was intended to filter out future forecasts,
        // ensuring only past or current forecasts are considered.
        val validForecasts = forecasts.filter { forecast ->
            val forecastDateTime = LocalDateTime.parse(
                "${forecast.date}T${forecast.time}",
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )
            forecastDateTime <= currentDateTime // Ensure the forecast is not in the future
        }
        */

        // Find the closest forecast from the (potentially filtered) list.
        // It calculates the absolute difference in minutes between the current time
        // and each forecast's time, then picks the one with the smallest difference.
        return forecasts.minByOrNull { forecast ->
            val forecastDateTime = LocalDateTime.parse(
                "${forecast.date}T${forecast.time}",
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )
            ChronoUnit.MINUTES.between(currentDateTime, forecastDateTime).absoluteValue
        }
    }
}

/**
 * Data class representing a daily weather forecast summary.
 *
 * @property date The date of the forecast (e.g., "YYYY-MM-DD").
 * @property minTemp The minimum temperature for the day, formatted with units (e.g., "5°C").
 * @property maxTemp The maximum temperature for the day, formatted with units (e.g., "15°C").
 */
data class DailyWeatherForecast(
    val date: String,
    // val sunrise: String, // Sunrise time (e.g., "HH:MM") - currently commented out
    // val sunset: String, // Sunset time (e.g., "HH:MM") - currently commented out
    val minTemp: String,
    val maxTemp: String,
    // val precipitationSum: String, // Total precipitation for the day (e.g., "10.5 mm") - currently commented out
)

/**
 * Data class representing an hourly weather forecast.
 *
 * @property date The date of the forecast (e.g., "YYYY-MM-DD").
 * @property time The time of the forecast (e.g., "HH:MM").
 * @property temperature The temperature at the specific time, formatted with units (e.g., "10.2°C").
 * @property minTemp The overall minimum temperature for the day, formatted with units.
 * @property maxTemp The overall maximum temperature for the day, formatted with units.
 * @property humidity The relative humidity at the specific time, formatted with units (e.g., "75 %").
 * @property precipitationProbability The probability of precipitation at the specific time (e.g., 50).
 * @property snowfall The amount of snowfall at the specific time (e.g., 0).
 * @property windSpeed The wind speed at the specific time, formatted with units (e.g., "15.3 km/h").
 * @property cloudCover The cloud cover percentage at the specific time, formatted with units (e.g., "70").
 */
data class WeatherForecast(
    val date: String,
    val time: String,
    val temperature: String,
    val minTemp: String,
    val maxTemp: String,
    val humidity: String,
    val precipitationProbability: Int,
    val snowfall: Int,
    val windSpeed: String,
    val cloudCover: String,
)