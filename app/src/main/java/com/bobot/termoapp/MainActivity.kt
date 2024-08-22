package com.bobot.termoapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.bobot.termoapp.ui.theme.TermoAppTheme
import com.bobot.termoapp.viewmodels.WeatherForecast
import com.bobot.termoapp.viewmodels.WeatherParameter
import com.bobot.termoapp.viewmodels.WeatherViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed to fetch weather data
            fetchWeatherData()
        } else {
            Toast.makeText(this, "Permission denied. Cannot access weather data.", Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchWeatherData() {
        val viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]

        // Use fixed latitude and longitude for testing
        val latitude = 41.10649299650251
        val longitude = 16.877975322937075

        viewModel.fetchWeather(latitude, longitude)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        setContent {
            TermoAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(this@MainActivity, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    activity: MainActivity,
    modifier: Modifier = Modifier
) {
    val viewModel = ViewModelProvider(activity)[WeatherViewModel::class.java]
    val weatherData by viewModel.weatherData.collectAsState()

    // Get the current date and time
    val currentDateTime = LocalDateTime.now()

    // Find the closest forecast
    val closestForecast = viewModel.findClosestForecast(currentDateTime, weatherData)

    // Group weather data by date
    val groupedData = weatherData.groupBy { it.date }

    LazyColumn (
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (weatherData.isEmpty()) {
            item {
                Text("No weather data available", style = MaterialTheme.typography.bodyLarge)
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
                        WeatherCard(it, viewModel)
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
                            // Convert the date string to a LocalDate object
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            val localDate = LocalDate.parse(date, formatter)

                            // Get the day of the week and capitalize the first letter
                            val dayOfWeek = localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                            val capitalizedDayOfWeek = dayOfWeek.replaceFirstChar { it.uppercaseChar() }

                            // Display the day of the week as a header
                            Text(
                                text = "$capitalizedDayOfWeek $date",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Use LazyRow for horizontal scrolling within bounded width
                            LazyRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(weatherList) { weatherForecaster ->
                                    WeatherCard(weatherForecaster, viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherCard(weatherForecast: WeatherForecast, weatherViewModel: WeatherViewModel) {
    //val dynamicText =  weatherViewModel.generateDynamicText(weatherParameter)
    Card(modifier = Modifier
        .padding(horizontal = 8.dp)
        .fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Min: ",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            weatherForecast.minTemp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        weatherForecast.temperature,
                        style = MaterialTheme.typography.titleLarge.copy(MaterialTheme.colorScheme.primary)
                    )

                    Row (verticalAlignment = Alignment.CenterVertically) {
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
            // Text aligned to the start
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Humidity:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            weatherForecast.humidity,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Date: ",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            weatherForecast.date,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Time: ",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            weatherForecast.time,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TermoAppTheme {
        MainScreen(MainActivity(), Modifier)
    }
}

