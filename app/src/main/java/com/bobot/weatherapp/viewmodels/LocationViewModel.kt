package com.bobot.weatherapp.viewmodels

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobot.weatherapp.services.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing location-related data and interactions with the [LocationService].
 *
 * This ViewModel acts as a bridge between the UI and the [LocationService],
 * providing location data to the UI and managing the lifecycle of location updates.
 * It uses Hilt for dependency injection to provide the [LocationService] instance.
 */
@HiltViewModel
class LocationViewModel @Inject constructor(
    // Injects the LocationService dependency, provided by Hilt.
    val locationService: LocationService
) : ViewModel() {

    /**
     * Exposes the location data from the [LocationService] to the UI.
     * This LiveData can be observed by UI components (e.g., Activities or Fragments)
     * to react to real-time location updates.
     */
    val locationData: LiveData<Location?> get() = locationService.locationData

    /**
     * Initialization block for the ViewModel.
     * When an instance of [LocationViewModel] is created, it immediately
     * attempts to start location updates.
     */
    init {
        startLocationUpdates()
    }

    /**
     * Starts location updates by calling the [startLocationUpdates] method on the
     * injected [LocationService].
     *
     * This operation is launched within the [viewModelScope], which is a CoroutineScope
     * tied to the ViewModel's lifecycle. This ensures that the coroutine is
     * automatically cancelled when the ViewModel is cleared, preventing memory leaks.
     */
    private fun startLocationUpdates() {
        viewModelScope.launch {
            locationService.startLocationUpdates()
        }
    }

    /**
     * Called when the ViewModel is no longer used and will be destroyed.
     *
     * This overridden method is crucial for proper resource management.
     * It ensures that location updates are stopped when the ViewModel is cleared,
     * preventing unnecessary battery drain and potential issues if the service
     * continues to run without an active observer.
     */
    override fun onCleared() {
        super.onCleared()
        locationService.stopLocationUpdates() // Stop updates when ViewModel is cleared
    }
}