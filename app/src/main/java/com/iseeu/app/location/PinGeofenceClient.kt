package com.iseeu.app.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.iseeu.app.domain.model.Pin
import com.iseeu.app.service.PinGeofenceReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the device's registered geofences in sync with the family's current saved-places list.
 * A full remove-then-re-add on every sync is simple and correct for the handful of places a
 * family realistically has — no need for incremental diffing at this scale.
 */
@Singleton
class PinGeofenceClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, PinGeofenceReceiver::class.java).apply {
            action = ACTION_PIN_GEOFENCE
        }
        // FLAG_MUTABLE is required — the system fills this PendingIntent's Intent with the
        // GeofencingEvent extra when delivering it; FLAG_IMMUTABLE would silently drop it.
        PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @SuppressLint("MissingPermission")
    suspend fun sync(pins: List<Pin>) {
        runCatching { geofencingClient.removeGeofences(pendingIntent).await() }

        if (pins.isEmpty()) return

        val geofences = pins.map { pin ->
            Geofence.Builder()
                .setRequestId(pin.id)
                .setCircularRegion(pin.lat, pin.lng, pin.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.addGeofences(request, pendingIntent).await()
    }

    suspend fun stop() {
        runCatching { geofencingClient.removeGeofences(pendingIntent).await() }
    }

    companion object {
        const val ACTION_PIN_GEOFENCE = "com.iseeu.app.ACTION_PIN_GEOFENCE"
        private const val REQUEST_CODE = 2002
    }
}
