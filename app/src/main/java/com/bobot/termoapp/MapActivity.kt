package com.bobot.termoapp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bobot.termoapp.ui.theme.TermoAppTheme
import com.bobot.termoapp.viewmodels.LocationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.net.HttpURLConnection
import java.net.URL


@AndroidEntryPoint
class MapActivity : ComponentActivity() {
    private var userMarker: Marker? = null
    private lateinit var mapView: MapView
    private lateinit var controller: MapController
    // Inject the ViewModel using Hilt
    private val viewModel: LocationViewModel by viewModels()

    private var lastUpdateTime: Long = 0
    private var isLocationUpdatesInitialized = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize OSMdroid configuration
        val sharedPreferences = getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        Configuration.getInstance().load(this, sharedPreferences)

        // Initialize mapView
        mapView = MapView(this)
        controller = mapView.controller as MapController
        controller.setZoom(15.0)  // Set an initial zoom level

        // Ensure that the mapView is not null before any operation
        // Add the map view to your layout (assuming you're using Compose layout)
        // Only then proceed with location updates
        viewModel.locationData.observe(this, Observer { location ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                Log.d("LocationActivity", "Updated location: Latitude: $latitude, Longitude: $longitude")

                // Call updateUserMarker directly instead of fetchAndCenterMap
                updateUserMarker(latitude, longitude)
            }
        })

        initializeLocationUpdates()
        isLocationUpdatesInitialized = true
    }

    private fun initializeLocationUpdates() {
        try {
            viewModel.locationService.startLocationUpdates() // Start location updates

            // Launch a coroutine to throttle updates every 10 seconds
            lifecycleScope.launch {
                while (true) {
                    viewModel.locationService.getLocationData { location ->
                        location?.let {
                            setMapLocation(it.latitude, it.longitude)
                            updateUserMarker(it.latitude, it.longitude)
                        }
                    }
                    delay(10_000L) // Delay for 10 seconds (10,000 milliseconds)
                }
            }
        } catch (e: Exception) {
            Log.e("LocationError", "Failed to start location updates", e)
        }
    }

    private fun setMapLocation(latitude: Double, longitude: Double) {
        setContent {
            TermoAppTheme {
                MapScreen(latitude, longitude)
            }
        }
    }

    @Composable
    fun MapScreen(latitude: Double, longitude: Double) {
        val context = LocalContext.current
        var userLocation by remember { mutableStateOf(GeoPoint(latitude, longitude)) }
        var selectedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
        var cityName by remember { mutableStateOf<String?>(null) }
        var showAlertDialog by remember { mutableStateOf(false) }
        var showConfirmCard by remember { mutableStateOf(false) }
        var userMarker by remember { mutableStateOf<Marker?>(null) }

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
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            Text(
                                text = "Are you sure you want to select this location?\n",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$cityName \n",
                                    style = MaterialTheme.typography.titleLarge.copy(MaterialTheme.colorScheme.primary)
                                )
                            }
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
                        Text("Confirm", color = MaterialTheme.colorScheme.onPrimary)
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
                    mapView.apply {
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        controller.setZoom(21.0) // Set default zoom level to 21

                        userMarker = Marker(this).apply {
                            position = userLocation
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_user_location_marker)
                            title = "Your Location"
                        }

                        // Set minimum and maximum zoom levels
                        minZoomLevel = 3.0
                        maxZoomLevel = 23.0
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                        // Handle long-tap to get latitude and longitude
                        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

                            @RequiresApi(Build.VERSION_CODES.O)
                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                p?.let {
                                    // Clear existing markers
                                    overlays.filterIsInstance<Marker>().forEach { marker ->
                                        if (marker.title != userMarker!!.title) {
                                            overlays.remove(marker)
                                        }
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

                                    // Make the phone vibrate
                                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if (vibrator.hasVibrator()) {
                                        val vibrationEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                                        } else {
                                            VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                                        }
                                        vibrator.vibrate(vibrationEffect)
                                    }
                                }
                                return true
                            }
                        })
                        overlays.add(mapEventsOverlay)

                        val rotationGestureOverlay = RotationGestureOverlay(mapView)
                        rotationGestureOverlay.isEnabled = true
                        overlays.add(rotationGestureOverlay)
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
                                Text("Confirm Location", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateUserMarker(latitude: Double, longitude: Double) {
        val userLocation = GeoPoint(latitude, longitude)

        // Check if the marker should be updated (every 10 seconds)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < 10_000) {
            return // Exit if the update is called within 10 seconds
        }
        lastUpdateTime = currentTime // Update the last update time

        // Ensure mapView is initialized before adding the marker
        // Use the same mapView instance from the AndroidView
        if (::mapView.isInitialized) {
            if (userMarker == null) {
                userMarker = Marker(mapView).apply {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(this@MapActivity, R.drawable.ic_user_location_marker)
                    title = "Your Location"
                    position = userLocation
                    mapView.overlays.add(this)
                }
            } else {
                userMarker?.position = userLocation
            }

            // Log for debugging
            Log.d("MapActivity", "Updating marker to: Latitude $latitude, Longitude $longitude")

            // Update marker position and center the map
            controller.setCenter(userLocation)
            mapView.invalidate()
        } else {
            Log.e("MapActivity", "mapView is not initialized, cannot update marker.")
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}