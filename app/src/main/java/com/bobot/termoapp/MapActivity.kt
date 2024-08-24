package com.bobot.termoapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bobot.termoapp.ui.theme.TermoAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMdroid configuration using the non-deprecated method
        val sharedPreferences = getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        Configuration.getInstance().load(this, sharedPreferences)

        setContent {
            TermoAppTheme {
                MapScreen()
            }
        }
    }

    @Composable
    fun MapScreen() {
        val context = LocalContext.current
        var selectedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
        var cityName by remember { mutableStateOf<String?>(null) }
        var showAlertDialog by remember { mutableStateOf(false) }
        var showConfirmCard by remember { mutableStateOf(false) }

        // Function to fetch city name using reverse geocoding
        suspend fun fetchCityName(lat: Double, lon: Double): String {
            return withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=10")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    val address = jsonResponse.getJSONObject("address")
                    address.optString("city", address.optString("town", address.optString("village", "Unknown City")))
                } catch (e: Exception) {
                    "Error fetching city"
                }
            }
        }

        // LaunchedEffect to fetch city name when selectedGeoPoint changes
        LaunchedEffect(selectedGeoPoint) {
            selectedGeoPoint?.let {
                cityName = fetchCityName(it.latitude, it.longitude)
            }
        }

        // AlertDialog
        if (showAlertDialog) {
            AlertDialog(
                onDismissRequest = { showAlertDialog = false },
                title = { Text("Confirm Location") },
                text = {
                    selectedGeoPoint?.let {
                        Column {
                            Text(
                                text = "Are you sure you want to select this location?\n",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$cityName \n",
                                style = MaterialTheme.typography.titleMedium.copy(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "Latitude: ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${it.latitude},\n",
                                style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "Longitude: ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${it.longitude}",
                                style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedGeoPoint?.let {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    putExtra("latitude", it.latitude)
                                    putExtra("longitude", it.longitude)
                                    putExtra("cityName", cityName) // Pass cityName
                                }
                                context.startActivity(intent)
                            }
                            showAlertDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Confirm", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAlertDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setMultiTouchControls(true)

                        // Set initial view
                        val startPoint = GeoPoint(41.10649299650251, 16.877975322937075)
                        controller.setZoom(21.0) // Set default zoom level to 18
                        controller.setCenter(startPoint)

                        // Handle long-tap to get latitude and longitude
                        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                p?.let {
                                    // Clear existing markers
                                    overlays.filterIsInstance<Marker>().forEach { marker ->
                                        overlays.remove(marker)
                                    }

                                    // Set the selected GeoPoint
                                    selectedGeoPoint = it

                                    // Create and add the custom marker
                                    val marker = Marker(this@apply).apply {
                                        position = it
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        icon = ContextCompat.getDrawable(ctx, R.drawable.ic_custom_marker_icon)
                                        setOnMarkerClickListener { _, _ ->
                                            controller.setCenter(position)
                                            true // Return true to consume the click event
                                        }
                                    }
                                    overlays.add(marker)
                                    // Force map update
                                    invalidate()
                                    showConfirmCard = true
                                }
                                return true
                            }
                        })
                        overlays.add(mapEventsOverlay)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Explanatory card on top of the map
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .shadow(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Long tap on the map to set a location.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "A marker will be placed at the selected location. Tapping on the marker will center the map on it.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp)
                    )
                }
            }

            // Custom Card for displaying latitude, longitude, city name, and confirm button
            if (showConfirmCard) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .shadow(8.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        selectedGeoPoint?.let { geoPoint ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Selected Location",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    cityName?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        )
                                    } ?: Text(
                                        text = "City: Loading...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Latitude: ${geoPoint.latitude}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Longitude: ${geoPoint.longitude}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAlertDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Confirm Location", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}