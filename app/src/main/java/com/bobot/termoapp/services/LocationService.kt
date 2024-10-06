package com.bobot.termoapp.services

import android.annotation.SuppressLint
import android.location.Location
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
 * Location service
 * Servizio di localizzazione e di posizionamento.
 *
 * @property fusedLocationClient client nativo di localizzazione android.
 * @constructor Create empty Location service
 */
class LocationService @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) {

    /**
     * Callback di localizzazione, ad ogni aggiornamento della posizione viene chiamato e aggiorna i dati della localizzazione corrente.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            p0.lastLocation?.let { location ->
                // Update the locationLiveData with the new location data
                locationLiveData.postValue(location)
            }
        }
    }

    // Variabile mutabile di tipo livedata per salvare la posizione attuale dell'utente.
    private val locationLiveData = MutableLiveData<Location?>()

    // Variabile immutabile da esporre ai viewmodel o altre classi in modo da poterne solo osservare gli aggiornamneti.
    val locationData: MutableLiveData<Location?>
        get() = locationLiveData

    @SuppressLint("MissingPermission")
    fun getLocationData(callback: (Location?) -> Unit) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Handle the location data here
                callback(location)
            }
    }

    /**
     * Avvia i servizi di localizzazione.
     *
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(1000)
            .build();

        // Richiedo gli update della localizzazione, passando anche la callback per poterne ricevere i risultati.
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

        // Ottengo la prima posizione corrente dal gps e assegno il valore alla variabile mutabile.
        fusedLocationClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            object : CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                    CancellationTokenSource().token

                override fun isCancellationRequested() = false
            })
            .addOnSuccessListener { location: Location? ->
                if (location == null)
                    println("Non ho preso location")
                else {
                    locationLiveData.postValue(location)
                }

            }
    }

    /**
     * Arresta il servizio di localizzazione.
     *
     */
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}