package com.bobot.termoapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobot.termoapp.services.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    val locationService: LocationService
) : ViewModel() {
    // Expose location data from the service to the UI
    val locationData = locationService.locationData

    init {
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            locationService.startLocationUpdates()
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationService.stopLocationUpdates() // Stop updates when ViewModel is cleared
    }
}