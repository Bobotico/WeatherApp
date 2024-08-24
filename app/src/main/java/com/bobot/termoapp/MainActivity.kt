package com.bobot.termoapp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.bobot.termoapp.ui.theme.TermoAppTheme
import com.bobot.termoapp.viewmodels.WeatherForecast
import com.bobot.termoapp.viewmodels.WeatherViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Get latitude and longitude from the Intent
            val latitude = intent.getDoubleExtra("latitude", 41.10649299650251) // default value
            val longitude = intent.getDoubleExtra("longitude", 16.877975322937075) // default value
            // Permission granted, proceed to fetch weather data
            fetchWeatherData(latitude, longitude)
        } else {
            Toast.makeText(
                this,
                "Permission denied. Cannot access weather data.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        val viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]

        viewModel.fetchWeather(latitude, longitude)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val cityName = intent.getStringExtra("cityName") ?: "Unknown City"

        setContent {
            TermoAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    MainScreen(this@MainActivity, cityName, latitude, longitude)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    activity: MainActivity,
    cityName: String,
    latitude: Double,
    longitude: Double,
) {
    val viewModel = ViewModelProvider(activity)[WeatherViewModel::class.java]
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val weatherData by viewModel.weatherData.collectAsState()

    // Get the current date and time
    val currentDateTime = LocalDateTime.now()

    // Find the closest forecast
    val closestForecast = viewModel.findClosestForecast(currentDateTime, weatherData)

    // Group weather data by date
    val groupedData = weatherData.groupBy { it.date }


    // Handle the loading and error state
    LaunchedEffect(Unit) {
        try {
            viewModel.fetchWeather(latitude, longitude)
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error fetching weather data"
            isLoading = false
        }
    }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (weatherData.isEmpty()) {
                item {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                }
            } else {
                // Box for the closest forecast to provide bounded width
                closestForecast?.let {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column {
                                Row {
                                    // Display the day of the week as a header
                                    Text(
                                        text = cityName,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "${currentDateTime.dayOfMonth}/${currentDateTime.monthValue}/${currentDateTime.year}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Display the day of the week as a header
                                    Text(
                                        text = "Lat: ",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    // Display the day of the week as a header
                                    Text(
                                        text = "$latitude",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Display the day of the week as a header
                                    Text(
                                        text = "Lon: ",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    // Display the day of the week as a header
                                    Text(
                                        text = "$longitude",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Spacer(modifier = Modifier.padding(8.dp))
                                WeatherCard(it, closestForecast.time)
                            }
                        }
                    }
                }

                groupedData.forEach { (date, weatherList) ->
                    // Box for all weather data to provide bounded width
                    item {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Column {
                                // Define the new date format
                                val inputFormatter =
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd") // Adjust if needed
                                val outputFormatter =
                                    DateTimeFormatter.ofPattern(
                                        "d MMMM, yyyy",
                                        Locale.UK
                                    ) // UK format

                                // Parse the date string into a LocalDate object
                                val localDate = try {
                                    LocalDate.parse(date, inputFormatter)
                                } catch (e: Exception) {
                                    // Handle parsing error
                                    LocalDate.now() // Fallback to current date
                                }

                                // Get today's date and tomorrow's date
                                val today = LocalDate.now()
                                val tomorrow = today.plusDays(1)

                                // Determine the prefix for today, tomorrow, or other dates
                                val datePrefix = when {
                                    localDate.isEqual(today) -> "Today, "
                                    localDate.isEqual(tomorrow) -> "Tomorrow, "
                                    else -> ""
                                }

                                // Get the day of the week and capitalize the first letter
                                val dayOfWeek = localDate.dayOfWeek.getDisplayName(
                                    TextStyle.FULL,
                                    Locale.UK
                                )
                                val capitalizedDayOfWeek =
                                    dayOfWeek.replaceFirstChar { it.uppercaseChar() }

                                // Format the date
                                val formattedDate = try {
                                    localDate.format(outputFormatter)
                                } catch (e: Exception) {
                                    // Handle formatting error
                                    localDate.toString() // Fallback to default ISO format
                                }

                                // Display the day of the week as a header
                                Text(
                                    text = "$datePrefix $capitalizedDayOfWeek $formattedDate",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(8.dp)
                                )

                                // Use LazyRow for horizontal scrolling within bounded width
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(weatherList) { weatherForecaster ->
                                        ContractedWeatherCard(
                                            weatherForecaster,
                                            weatherForecaster.time
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

// Define a function to get the appropriate icon based on cloud cover percentage
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun getCloudCoverIcon(
    cloudCover: Int,
    forecastTime: LocalTime,
    weatherForecast: WeatherForecast,
): Pair<Painter, String> {
    val isNight = isNight(forecastTime)
    val isRaining = isRaining(weatherForecast)

    return when {
        // Case for less than 25% cloud cover
        cloudCover < 25 -> {
            if (isNight) {
                Pair(painterResource(id = R.drawable.ic_night), "Clear night")
            } else {
                Pair(painterResource(id = R.drawable.ic_sunny), "Sunny")
            }
        }

        // Case for 25% to 50% cloud cover
        cloudCover in 25..50 -> {
            if (isRaining) {
                if (isNight) {
                    Pair(
                        painterResource(id = R.drawable.ic_partly_cloudy_rainy_night),
                        "Partly cloudy and rainy night"
                    )
                } else {
                    Pair(
                        painterResource(id = R.drawable.ic_partly_cloudy_rainy),
                        "Partly cloudy with rain"
                    )
                }
            } else {
                if (isNight) {
                    Pair(
                        painterResource(id = R.drawable.ic_partly_cloudy_night),
                        "Partly cloudy night"
                    )
                } else {
                    Pair(painterResource(id = R.drawable.ic_partly_cloudy), "Partly cloudy")
                }
            }
        }

        // Case for 51% to 75% cloud cover
        cloudCover in 51..75 -> {
            if (isRaining) {
                Pair(painterResource(id = R.drawable.ic_cloudy_rainy), "Cloudy with rain")
            } else {
                Pair(painterResource(id = R.drawable.ic_cloudy), "Cloudy")
            }
        }

        // Case for more than 75% cloud cover
        else -> {
            if (isRaining) {
                Pair(
                    painterResource(id = R.drawable.ic_fully_cloudy_rainy),
                    "Fully cloudy with rain"
                )
            } else {
                Pair(painterResource(id = R.drawable.ic_fully_cloudy), "Fully cloudy")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun parseTime(timeString: String): LocalTime {
    val formatter = DateTimeFormatter.ofPattern("HH:mm") // Assuming 24-hour format
    return LocalTime.parse(timeString, formatter)
}

@RequiresApi(Build.VERSION_CODES.O)
fun isNight(forecastTime: LocalTime): Boolean {
    val eveningStart = LocalTime.of(17, 0) // 6 PM
    val morningEnd = LocalTime.of(5, 0)    // 6 AM

    return forecastTime.isAfter(eveningStart) || forecastTime.isBefore(morningEnd)
}

@RequiresApi(Build.VERSION_CODES.O)
fun isRaining(weatherForecast: WeatherForecast): Boolean {
    Log.d("Debug", "Precipitation probability: ${weatherForecast.precipitationProbability}")
    // Assuming weatherForecast has a precipitation field which is a percentage
    return weatherForecast.precipitationProbability > 80 // Consider it raining if precipitation is over 80%
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeatherCard(
    weatherForecast: WeatherForecast,
    timeString: String,
) {
    val forecastTime = parseTime(timeString)
    val (cloudCoverIcon, cloudCoverDescription) = getCloudCoverIcon(
        weatherForecast.cloudCover.toInt(),
        forecastTime,
        weatherForecast
    )

    Card(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Temperature Row.
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Cloud Cover
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                // Icon
                                Image(
                                    painter = cloudCoverIcon,
                                    contentDescription = "Wind speed icon",
                                    modifier = Modifier
                                        .size(64.dp) // Set the size of the image
                                        .padding(4.dp), // Optional padding around the image
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                                )
                                // Entitlement
                                Text(
                                    cloudCoverDescription,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                /*// Value
                                Text(
                                    weatherForecast.cloudCover,
                                    style = MaterialTheme.typography.bodySmall
                                )*/
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon
                        Image(
                            painter = painterResource(id = R.drawable.ic_temperature),
                            contentDescription = "Temperature icon",
                            modifier = Modifier
                                .size(24.dp), // Set the size of the image
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                        )

                        // Temperature
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            // Min Temp
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Min: ",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    weatherForecast.minTemp,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Current Temp
                            Text(
                                weatherForecast.temperature,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    MaterialTheme.colorScheme.primary
                                )
                            )

                            // Max Temp
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Max: ",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    weatherForecast.maxTemp,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Text aligned to the start
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        /*Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Date: ",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            weatherForecast.date,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }*/

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                weatherForecast.time,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }

            // Row Humidity and WindSpeed
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Humidity
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Tile
                    Text(
                        "Humidity",
                        style = MaterialTheme.typography.titleSmall
                    )
                    // Icon
                    Image(
                        painter = painterResource(id = R.drawable.ic_humidity),
                        contentDescription = "Wind speed icon",
                        modifier = Modifier
                            .size(32.dp) // Set the size of the image
                            .padding(4.dp), // Optional padding around the image
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    )
                    // Value
                    Text(
                        weatherForecast.humidity,
                        style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.primary)
                    )
                }

                // Wind Speed
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Tile
                    Text(
                        "Wind Speed",
                        style = MaterialTheme.typography.titleSmall
                    )
                    // Icon
                    Image(
                        painter = painterResource(id = R.drawable.ic_wind_speed),
                        contentDescription = "Wind speed icon",
                        modifier = Modifier
                            .size(32.dp) // Set the size of the image
                            .padding(4.dp), // Optional padding around the image
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    )
                    // Value
                    Text(
                        weatherForecast.windSpeed,
                        style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ContractedWeatherCard(
    weatherForecast: WeatherForecast,
    timeString: String,
) {
    val forecastTime = parseTime(timeString)
    val (cloudCoverIcon) = getCloudCoverIcon(
        weatherForecast.cloudCover.toInt(),
        forecastTime,
        weatherForecast
    )

    Card(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Cloud Cover
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            Column(
                                horizontalAlignment = Alignment.Start,
                            ) {
                                // Icon
                                Image(
                                    painter = cloudCoverIcon,
                                    contentDescription = "Wind speed icon",
                                    modifier = Modifier
                                        .size(64.dp) // Set the size of the image
                                        .padding(4.dp), // Optional padding around the image
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                // Text aligned to the start
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    /*Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Date: ",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        weatherForecast.date,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    }*/

                    Text(
                        weatherForecast.time,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Temperature Row.
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon
                    Image(
                        painter = painterResource(id = R.drawable.ic_temperature),
                        contentDescription = "Temperature icon",
                        modifier = Modifier
                            .size(24.dp), // Set the size of the image
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    )

                    // Temperature
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        // Current Temp
                        Text(
                            weatherForecast.minTemp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        // Current Temp
                        Text(
                            weatherForecast.temperature,
                            style = MaterialTheme.typography.titleLarge.copy(MaterialTheme.colorScheme.primary)
                        )
                        // Current Temp
                        Text(
                            weatherForecast.maxTemp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Row Humidity and WindSpeed
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Humidity
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Tile
                    Text(
                        "Humidity",
                        style = MaterialTheme.typography.titleSmall
                    )
                    // Value
                    Text(
                        weatherForecast.humidity,
                        style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.primary)
                    )
                }

                // Wind Speed
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Tile
                    Text(
                        "Wind Speed",
                        style = MaterialTheme.typography.titleSmall
                    )
                    // Value
                    Text(
                        weatherForecast.windSpeed,
                        style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val cityName = "Bari"
    val latitude = 41.10648753859573
    val longitude = 16.8779752411189

    TermoAppTheme {
        MainScreen(MainActivity(), cityName, latitude, longitude)
    }
}

