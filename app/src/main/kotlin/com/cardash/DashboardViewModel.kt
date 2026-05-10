package com.cardash

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardState(
    val speedKmh: Float? = null,
    val altitudeM: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val client = LocationServices.getFusedLocationProviderClient(app)

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc: Location = result.lastLocation ?: return
            val prev = _state.value
            _state.value = prev.copy(
                speedKmh = if (loc.hasSpeed()) loc.speed * 3.6f else prev.speedKmh,
                altitudeM = if (loc.hasAltitude()) loc.altitude.toFloat() else prev.altitudeM,
                latitude = loc.latitude,
                longitude = loc.longitude,
            )
        }
    }

    fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        getApplication(),
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasLocationPermission()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    fun stop() {
        client.removeLocationUpdates(callback)
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
