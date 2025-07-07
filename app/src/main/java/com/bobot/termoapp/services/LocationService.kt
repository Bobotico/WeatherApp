package com.bobot.termoapp.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import javax.inject.Inject

/**
 * Location Service
 *
 * This class is responsible for handling location and positioning services within the application.
 * It utilizes the FusedLocationProviderClient to get the user's current location and
 * provide real-time location updates.
 *
 * @property fusedLocationClient The native Android client for location services,
 * provided through dependency injection.
 */
class LocationService @Inject constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    /**
     * Location Callback.
     * This callback is invoked every time the device's location is updated.
     * It receives a [LocationResult] which contains the latest location data.
     * The new location is then posted to [locationLiveData].
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            // Check if the last known location is available
            p0.lastLocation?.let { location ->
                // Update the locationLiveData with the new location data
                locationLiveData.postValue(location)
            }
        }
    }

    /**
     * Mutable LiveData variable to store the user's current location.
     * It can be observed by other components (like ViewModels) for real-time updates.
     * It's nullable as location might not always be immediately available.
     */
    private val locationLiveData = MutableLiveData<Location?>()

    /**
     * Immutable LiveData exposed to ViewModels or other classes.
     * This allows external components to observe location updates without being able to modify
     * the [locationLiveData] directly, ensuring data encapsulation.
     */
    val locationData: MutableLiveData<Location?>
        get() = locationLiveData

    /**
     * Retrieves the last known location of the device.
     * This method is suitable for scenarios where a quick, possibly stale, location is acceptable.
     * It requires the "android.permission.ACCESS_FINE_LOCATION" or "android.permission.ACCESS_COARSE_LOCATION"
     * permission, which is suppressed here with `@SuppressLint("MissingPermission")` for brevity,
     * but proper permission handling should be implemented in a real application.
     *
     * @param callback A lambda function to be executed with the retrieved [Location?] as its argument.
     */
    @SuppressLint("MissingPermission")
    fun getLocationData(callback: (Location?) -> Unit) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Invoke the callback with the last known location
                callback(location)
            }
    }

    /**
     * Starts the location update services.
     * This method configures a [LocationRequest] for high accuracy and frequent updates,
     * then registers the [locationCallback] to receive these updates.
     * It also attempts to immediately get the current location once to provide an initial value.
     *
     * Requires the "android.permission.ACCESS_FINE_LOCATION" or "android.permission.ACCESS_COARSE_LOCATION"
     * permission.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("LocationService", "Permission not granted")
            return
        }

        // Build a LocationRequest with high accuracy and specified update intervals.
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false) // Do not wait for an accurate location
            .setMinUpdateIntervalMillis(500) // Minimum interval between updates
            .setMaxUpdateDelayMillis(1000) // Maximum delay before an update is delivered
            .build()

        // Request location updates from the FusedLocationProviderClient,
        // passing the configured request and the callback to receive results.
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        // Get the current location immediately and assign the value to the mutable LiveData.
        fusedLocationClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY, // Request high accuracy for the current location
            object : CancellationToken() { // Provide a CancellationToken to allow cancellation of the request
                override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                    CancellationTokenSource().token // Returns a new cancellation token

                override fun isCancellationRequested() = false // Indicates if cancellation has been requested
            })
            .addOnSuccessListener { location: Location? ->
                // Check if a location was successfully retrieved
                if (location == null) {
                    println("Non ho preso location") // Log message if location is null
                } else {
                    // Post the current location to the LiveData
                    locationLiveData.postValue(location)
                }
            }
    }

    /**
     * Stops the location update service.
     * This method removes the registered [locationCallback] from the FusedLocationProviderClient,
     * effectively stopping the delivery of location updates and conserving battery.
     */
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}