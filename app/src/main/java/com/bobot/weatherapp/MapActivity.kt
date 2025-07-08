package com.bobot.weatherapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
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
import com.bobot.weatherapp.ui.theme.WeatherAppTheme
import com.bobot.weatherapp.viewmodels.LocationViewModel
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent


@AndroidEntryPoint
class MapActivity : ComponentActivity() {
    // Inject the ViewModel using Hilt
    private val viewModel: LocationViewModel by viewModels()

    // Launcher for requesting location permissions
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, start location updates
                Log.d("MapActivity", "Location permission granted. Starting updates.")
                viewModel.locationService.startLocationUpdates()
            } else {
                // Permission denied, handle accordingly (e.e., show a message)
                Log.w("MapActivity", "Location permission denied.")
                // Optionally, show a dialog explaining why permission is needed
                // and directing the user to app settings.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize OSMdroid configuration once
        val sharedPreferences = getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        Configuration.getInstance().load(this, sharedPreferences)

        // Check and request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, start location updates
            Log.d("MapActivity", "Location permission already granted. Starting updates.")
            viewModel.locationService.startLocationUpdates()
        } else {
            // Request permission
            Log.d("MapActivity", "Requesting location permission.")
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Set the content of the activity using Jetpack Compose
        setContent {
            WeatherAppTheme {
                // Observe the location data from the ViewModel
                val currentLocation by viewModel.locationData.observeAsState(initial = null)

                // Pass the current location to the MapScreen Composable
                MapScreen(currentLocation)
            }
        }
    }

    @Composable
    fun MapScreen(currentLocation: Location?) {
        val context = LocalContext.current
        var selectedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
        var cityName by remember { mutableStateOf<String?>(null) }
        var showAlertDialog by remember { mutableStateOf(false) }
        var showConfirmCard by remember { mutableStateOf(false) }

        // State to track if the map should automatically follow the user's location
        var isMapFollowingUser by remember { mutableStateOf(true) }

        // NEW: State to temporarily ignore user input during programmatic map movements
        var isProgrammaticMapMove by remember { mutableStateOf(false) }
        val ANIMATION_DURATION = 500L // Consistent with the animateTo call

        val coroutineScope = rememberCoroutineScope()
        // Use remember to keep track of the MapView instance
        val mapView = remember { MapView(context) }

        // Manage MapView lifecycle with DisposableEffect
        DisposableEffect(mapView) {
            mapView.onResume() // Call onResume when the composable enters the composition
            onDispose {
                mapView.onPause() // Call onPause when the composable leaves the composition
                mapView.onDetach() // Call onDetach to release resources
            }
        }

        // Function to fetch city name using reverse geocoding
        suspend fun fetchCityName(lat: Double, lon: Double): String {
            return withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=10")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()
                    val inputStream = connection.inputStream
                    val response = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    val address = jsonResponse.optJSONObject("address")
                    address?.optString("city", address.optString("town", address.optString("village", "Unknown City")))
                        ?: "Unknown City"
                } catch (e: Exception) {
                    Log.e("MapScreen", "Error fetching city name: ${e.localizedMessage}")
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

        // AlertDialog for location confirmation
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
                            showConfirmCard = false
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
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        setZoomLevel(18.0)

                        minZoomLevel = 8.0
                        maxZoomLevel = 23.0
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                        val rotationGestureOverlay = RotationGestureOverlay(this)
                        rotationGestureOverlay.isEnabled = true
                        overlays.add(rotationGestureOverlay)

                        setMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                if (!isProgrammaticMapMove) {
                                    isMapFollowingUser = false
                                }
                                return false
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                if (!isProgrammaticMapMove) {
                                    isMapFollowingUser = false
                                }
                                return false
                            }
                        })

                        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                isMapFollowingUser = false
                                return false
                            }

                            @RequiresApi(Build.VERSION_CODES.O)
                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                // User long pressed, so the map is no longer following automatically
                                isMapFollowingUser = false // This is correct, user took manual control

                                p?.let {
                                    // Set the selected GeoPoint
                                    selectedGeoPoint = it
                                    showConfirmCard = true

                                    // NEW: Center map on the long-pressed point immediately
                                    // Also mark as programmatic move to avoid immediate isMapFollowingUser = false from MapListener
                                    isProgrammaticMapMove = true
                                    mapView.controller.animateTo(it, mapView.zoomLevelDouble, ANIMATION_DURATION) // Center and keep current zoom
                                    mapView.invalidate()

                                    // Reset programmatic move flag after animation
                                    coroutineScope.launch {
                                        delay(ANIMATION_DURATION + 50) // Add a small buffer
                                        isProgrammaticMapMove = false
                                        Log.d("MapActivity", "Long press animation finished. isProgrammaticMapMove set to false.")
                                    }


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
                    }
                    mapView
                },
                update = { mapView ->
                    currentLocation?.let { location ->
                        val userGeoPoint = GeoPoint(location.latitude, location.longitude)

                        var existingUserMarker: Marker? = mapView.overlays.filterIsInstance<Marker>()
                            .firstOrNull { it.title == "Your Location" }

                        if (existingUserMarker == null) {
                            existingUserMarker = Marker(mapView).apply {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = ContextCompat.getDrawable(context,
                                    R.drawable.ic_user_location_marker
                                )
                                title = "Your Location"
                            }
                            mapView.overlays.add(existingUserMarker)
                        }
                        existingUserMarker.position = userGeoPoint
                        mapView.invalidate()
                    }

                    mapView.overlays.removeAll { it is Marker && it.title != "Your Location" }

                    selectedGeoPoint?.let { geoPoint ->
                        // Get the drawable (which is now configured with black and white paths)
                        val drawable = ContextCompat.getDrawable(context,
                            R.drawable.ic_picking_marker
                        )

                        drawable?.let {
                            val wrappedDrawable = DrawableCompat.wrap(it).mutate()
                            //DrawableCompat.setTint(wrappedDrawable, primaryColor)

                            val customMarker = Marker(mapView).apply {
                                position = geoPoint
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = wrappedDrawable // Set the now tinted drawable
                                setOnMarkerClickListener { marker, _ ->
                                    isMapFollowingUser = false
                                    mapView.controller.setCenter(marker.position)
                                    true
                                }
                            }
                            mapView.overlays.add(customMarker)
                        }
                    }
                    mapView.invalidate()
                }
            )

            // LaunchedEffect to handle initial centering and automatic recentering if isMapFollowingUser is true
            LaunchedEffect(currentLocation, isMapFollowingUser) {
                currentLocation?.let { location ->
                    val userGeoPoint = GeoPoint(location.latitude, location.longitude)

                    // Only recenter if the map is currently set to follow the user
                    if (isMapFollowingUser) {
                        val mapController = mapView.controller as MapController
                        // Set programmatic flag before setting center/zoom
                        isProgrammaticMapMove = true
                        mapController.setZoom(18.0) // Ensure desired zoom when recentering
                        mapController.setCenter(userGeoPoint)
                        Log.d("MapScreen", "Map centered on user location: Lat ${location.latitude}, Lon ${location.longitude}")
                        mapView.invalidate()
                        // Reset programmatic flag after a small delay to allow map to settle
                        // This handles the initial centering.
                        delay(ANIMATION_DURATION) // Or slightly more if needed
                        isProgrammaticMapMove = false
                    }
                }
            }

            // Explanatory card on top of the map
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Changed from TopCenter
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

            // NEW: Recenter Button (top right)
            currentLocation?.let { location -> // Only show button if we have a current location
                FloatingActionButton(
                    onClick = {
                        // Set programmatic flag BEFORE animating
                        isProgrammaticMapMove = true
                        isMapFollowingUser = true // Re-enable following
                        val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                        val mapController = mapView.controller as MapController

                        // Use the animateTo overload that matches the desired parameters
                        val zoomLevel = 18.0 // Double
                        val animationSpeed = 500L // Long

                        mapController.animateTo(
                            userGeoPoint,
                            zoomLevel,
                            animationSpeed
                        )
                        mapView.invalidate()

                        // Launch a coroutine to reset the flag after the animation duration
                        // This ensures the MapListener doesn't immediately disable following.
                        lifecycleScope.launch { // Use lifecycleScope or rememberCoroutineScope
                            delay(animationSpeed + 50) // Add a small buffer
                            isProgrammaticMapMove = false
                            Log.d("MapActivity", "Recenter button animation finished. isProgrammaticMapMove set to false.")
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // Align to top right
                        .padding(top = 16.dp, end = 16.dp, bottom = 64.dp), // Adjust padding to avoid overlapping with the card
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        painter = if (isMapFollowingUser) painterResource(id = R.drawable.ic_gps_on) else painterResource(id = R.drawable.ic_gps_off),
                        contentDescription = "Recenter Map"
                    )
                }
            }

            // Custom Card for displaying latitude, longitude, city name, and confirm button
            if (showConfirmCard) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp) // Adjust padding to avoid overlap with explanatory card
                        .fillMaxWidth()
                        .shadow(8.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    selectedGeoPoint?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Selected Location",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    cityName?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    } ?: Text(
                                        text = "City: Loading...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Corrected Button Column
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Button(
                                    onClick = { showAlertDialog = true },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(end = 0.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    // Add this line to remove the button's default internal content padding
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        modifier = Modifier
                                            .size(32.dp), // Now, this 56.dp will utilize the full 56dp of the button's internal area
                                        contentDescription = "Confirm Location",
                                        tint = MaterialTheme.colorScheme.onPrimary
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