package com.iseeu.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Thin wrapper over FusedLocationProviderClient. Callers are only ever reached after the location permission flow completes. */
@Singleton
class LocationClient @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    /** Battery-conscious background stream — this is Android's closest equivalent to "significant location change." */
    @SuppressLint("MissingPermission")
    fun continuousUpdates(): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISPLACEMENT_METERS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, null)
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    /** One-shot high-accuracy fix for the explicit "refresh me now" flow — trades battery for freshness, deliberately. */
    @SuppressLint("MissingPermission")
    suspend fun oneShotHighAccuracy(): Location? {
        val cancellationSource = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
        return fusedClient.getCurrentLocation(request, cancellationSource.token).await()
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 5 * 60 * 1000L
        private const val MIN_UPDATE_INTERVAL_MS = 60 * 1000L
        private const val MIN_DISPLACEMENT_METERS = 75f
    }
}
